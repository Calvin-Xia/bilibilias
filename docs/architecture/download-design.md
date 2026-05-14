# 下载链路设计

本文档说明 BILIBILIAS 当前下载系统的整体设计，重点覆盖任务建模、任务创建、排队执行、预处理、媒体下载、状态推进和最终落盘。内容以 `NewDownloadManager`、`DownloadTaskRepository` 及其协作组件的当前实现为准。

## 总览

下载链路不是“拿到 URL 直接存文件”，而是一个分层流程：

1. 分析页生成 `DownloadViewInfo`
2. `DownloadTaskRepository` 根据链接类型创建任务树
3. `NewDownloadManager` 把任务树转成运行时任务列表
4. 启动前台 `DownloadService` 执行队列
5. 前置任务处理封面、字幕、弹幕等附加资源
6. 下载媒体子任务
7. 如有需要执行 FFmpeg 合并
8. 将最终文件移动到下载目录并注册媒体库
9. 更新任务状态并清理临时文件

当前职责分布：

- `core:data`
  - 负责下载任务树和持久化模型的生成。
- `:app download/*`
  - 负责运行时调度、网络下载、临时文件、合并、通知和收尾。

## 关键对象

### `DownloadViewInfo`

文件：`core/data/src/main/java/com/imcys/bilibilias/data/model/download/DownloadViewInfo.kt`

它描述“用户想怎么下”，包括：

- 视频清晰度、编码、音频质量
- `DownloadMode`
  - `AUDIO_VIDEO`
  - `VIDEO_ONLY`
  - `AUDIO_ONLY`
- 勾选的分 P / 分集
- 是否下载媒体、封面、弹幕、字幕
- 是否嵌入封面、字幕
- 字幕文件类型
- 音视频容器配置 `MediaContainerConfig`

它是下载链路的输入配置对象，不承担执行职责。

### `DownloadTaskTree`

由 `DownloadTaskRepository.createDownloadTask()` 生成，表达“这个下载请求在业务结构上包含哪些节点和片段”。

它解决的问题：

- 视频、合集、互动视频、番剧季度、番外章节在结构上并不一样。
- 下载器需要统一的树状结构来遍历和展开为具体下载片段。

### `AppDownloadTask`

文件：`app/src/main/java/com/imcys/bilibilias/download/AppDownloadTask.kt`

它是运行时任务对象，组合了：

- `DownloadTask`
- `DownloadSegment`
- `DownloadSubTask`
- `DownloadViewInfo`
- 当前进度和状态
- 运行期附加信息 `TaskRuntimeInfo`

其中 `TaskRuntimeInfo` 主要存：

- 待嵌入字幕文件路径
- 待嵌入封面路径
- 合并期间的临时媒体资源

这个对象是“执行层视角”的任务，而不是纯数据库实体。

## 持久化层与运行时层的边界

### 持久化层

下载相关持久化数据主要由 `DownloadTaskRepository` 和 `DownloadTaskDao` 管理：

- `DownloadTask`
  - 逻辑任务，例如某个视频、某个番剧、某个合集。
- `DownloadTaskNode`
  - 任务树节点。
- `DownloadSegment`
  - 可被实际执行的片段级任务。

### 运行时层

`NewDownloadManager` 内存中维护 `_downloadTasks: StateFlow<List<AppDownloadTask>>`，它代表当前会话下的运行状态：

- 等待中
- 下载中
- 合并中
- 暂停
- 完成
- 错误

设计上是“数据库保存结构和结果，内存保存执行态”。

## 任务树创建

文件：`core/data/src/main/java/com/imcys/bilibilias/data/repository/DownloadTaskRepository.kt`

统一入口：

- `createDownloadTask(asLinkResultType, downloadViewInfo)`

根据链接类型分流：

- `ASLinkResultType.BILI.Donghua`
  - 走 `createDonghuaDownloadTask()`
- `ASLinkResultType.BILI.Video`
  - 走 `createVideoDownloadTask()`

### 视频任务

视频任务创建时会先请求视频详情，然后判断类型：

- 普通视频
- 合集 `ugcSeason`
- 互动视频 `SteinGate`

再分别构建不同树结构。

### 番剧任务

番剧任务会以季度和剧集为核心构建树：

- season
- episode
- section / 预告 / 特殊章节

如果用户勾选了其他季度的剧集，仓库会补请求对应季度信息，再把选中的剧集落入树中。

### 命名信息

任务树构建时就会写入 `NamingConventionInfo`，例如：

- 视频标题
- BV / AV
- 作者
- 番剧标题
- 季标题
- 集标题

这样后续文件命名不需要再回头请求详情。

### 分析埋点

仓库还会在用户同意隐私协议时，调用 `AppAPIService.submitASDownloadData()` 提交下载数据备份。

这说明：

- 下载创建阶段已经带有隐私约束。
- 后续改动不应绕过 `hasAgreedPrivacyPolicy()`。

## `NewDownloadManager` 的职责

文件：`app/src/main/java/com/imcys/bilibilias/download/NewDownloadManager.kt`

它是当前下载执行入口，负责：

- 管理运行中任务列表
- 与 `DownloadService` 协作
- 控制并发下载
- 任务暂停 / 恢复 / 取消
- 展开任务树
- 为片段创建媒体子任务
- 前置资源准备
- 调用下载执行器
- 调用 FFmpeg 合并器
- 最终文件落盘和媒体库注册

它本身不负责：

- 直接拼 API 请求参数
- 直接选择音视频流质量
- 直接构建 FFmpeg 命令
- 直接决定文件命名规则

这些都下沉给了专门组件。

## 初始化与恢复

`initDownloadList()` 会在初始化时把未处于稳定终态的 segment 重置为 `PAUSE`：

- 非 `PAUSE / COMPLETED / CANCELLED / ERROR` 的任务都会改成 `PAUSE`

这是为了避免应用重启后数据库里残留“看起来还在下载”的假状态。

## 队列与并发

当前 `NewDownloadManager` 内部有：

- `activeDownloadJobs`
  - 正在执行的任务 Job 集合
- `downloadScope`
  - IO + `SupervisorJob`
- `DEFAULT_MAX_CONCURRENT_DOWNLOADS`
  - 默认最大并发数

并发控制特点：

- 任务级别并发，而不是子任务无限并发。
- 单个 `AppDownloadTask` 内部如果同时有视频和音频两个子任务，会并行下载这两个子任务。
- 多个任务同时下载时，通知会聚合显示“下载中 / 合并中 / 等待中”的数量。

## 为什么要有 `DownloadService`

下载执行通过前台服务维持：

- 应用在前台时启动/绑定 `DownloadService`
- 通过通知展示当前下载状态和进度

这样做的目的：

- 降低下载过程中被系统回收的概率
- 满足长时间后台任务的用户感知需求

当前实现里，`startDownloadQueueService()` 还会检查应用是否在前台，不会在不合适时盲目拉起。

## 任务从树到运行时列表

`processDownloadTree()` 会遍历 `DownloadTaskTree`：

- 对每个 `DownloadSegment` 创建运行时任务
- 去重，避免相同下载请求重复加入
- 为每个 segment 生成对应的 `DownloadSubTask`

这一步的结果是把业务树结构压平成可调度的 `AppDownloadTask` 列表。

## 子任务设计

### 为什么有 `DownloadSubTask`

一个 segment 不一定只有一个媒体文件：

- DASH 视频通常是视频流 + 音频流
- 纯音频只有音频流
- DURL 只有单文件视频流

所以运行时会拆成 `DownloadSubTask`：

- `VIDEO`
- `AUDIO`

### 子任务路径

临时文件保存在应用私有目录：

- 视频：`externalFilesDir("video")`
- 音频：`externalFilesDir("audio")`

命名格式大致为：

- `${segment.platformId}_VIDEO.xxx`
- `${segment.platformId}_AUDIO.xxx`

扩展名由 `MediaContainerConfig` 决定。

这说明：

- 子任务文件是临时工作文件，不是最终交付文件名。
- 最终命名在合并/落盘之后才真正应用。

## 前置任务设计

执行真实媒体下载前，`handlePredecessor()` 会做若干“非主媒体但影响结果”的准备：

- 下载嵌入字幕
- 下载嵌入封面
- 下载封面到相册
- 下载弹幕 XML
- 下载外挂字幕文件

### 为什么前置处理和媒体下载分开

原因有三点：

- 嵌入字幕和封面需要在合并阶段作为额外输入。
- 弹幕和外挂字幕不依赖主媒体下载完成，可以提前产出。
- 主媒体下载失败时，这些附加资源仍可以独立处理或清理。

## 媒体信息获取与流选择

### `VideoInfoFetcher`

文件：`app/src/main/java/com/imcys/bilibilias/download/VideoInfoFetcher.kt`

它负责：

- 根据节点类型请求播放信息
- 从不同接口结果中提取统一的可下载媒体结构
- 从 DASH / DURL 中选出最终下载 URL
- 把质量描述回传给下载管理器

它解决的问题：

- 视频、番剧、合集、互动视频的播放信息返回结构不完全一致。
- Web / TV 平台分流已经在 repository 中做过，但下载层仍需要统一拿到“最终流”。

### 支持的数据形态

- `BILIVideoDash`
  - 音视频分离，可分别下载视频轨和音频轨
- `BILIVideoDurl`
  - 单文件直链，通常只能按视频单流处理

### 质量选择

对于 DASH：

- 视频轨按 `selectVideoQualityId + selectVideoCode` 优先匹配
- 音频轨按 `selectAudioQualityId` 优先匹配
- 若用户选择不可用，则回落到第一条可用流

对于 DURL：

- 基本按单流视频下载
- 仓库会把模式收敛成 `VIDEO_ONLY`

## 下载执行器设计

### `DownloadExecutor`

文件：`app/src/main/java/com/imcys/bilibilias/download/DownloadExecutor.kt`

它只负责一件事：

- 把一个下载 URL 稳定写入到指定本地路径

它不负责：

- 请求播放地址
- 选择清晰度
- 决定最终文件名
- 合并媒体

### 当前下载策略

- 下载前先 `HEAD` 请求远端长度
- 若最终文件已存在且长度足够，直接视为成功
- 真实下载写入 `*.downloading` 临时文件
- 成功后重命名为目标文件
- 支持最多 5 次重试
- 每次重试延迟 3 秒
- 支持续传：若临时文件存在，会带 `Range` 头继续请求

### CDN 替换

下载前会根据设置中的 `biliLineHost` 替换匹配到的 `upos-sz-estg...bilivideo.com` 域名。

这说明：

- CDN 切线是下载层能力，不应由页面手工改 URL。

## 单任务下载流程

`executeTaskDownload()` 的主流程是：

1. 更新通知为“准备下载”
2. `handlePredecessor()`
3. 如果需要媒体下载，则进入 `downloadAppTask()`
4. 若下载成功，再执行 `handleSuccessor()`
5. 若只是非媒体任务，则直接完成

### 单流与双流

`downloadAppTask()` 分两种：

- 单子任务
  - 例如纯音频或 DURL 单视频
- 多子任务
  - 典型是 DASH 的视频 + 音频并行下载

多子任务场景下：

- 视频和音频使用 `async` 并行执行
- 总进度用两路进度平均值计算

## 状态推进

运行中的典型状态包括：

- `WAITING`
- `PRE_TASK`
- `DOWNLOADING`
- `MERGING`
- `PAUSE`
- `COMPLETED`
- `ERROR`
- `CANCELLED`

状态切换大致是：

`WAITING -> PRE_TASK -> DOWNLOADING -> MERGING -> COMPLETED`

异常、取消、暂停会从中间打断。

## 非媒体附加资源

### 封面

- 可下载到相册
- 也可预先下载到缓存目录，供后续嵌入媒体文件

### 字幕

由 `SubtitleDownloader` 负责：

- 嵌入场景：先拉取 CC，再转 SRT，存到缓存目录供 FFmpeg 使用
- 外挂场景：转成 ASS 或 SRT，直接输出到下载目录

### 弹幕

- 通过分页请求获取全部弹幕段
- 汇总成 B 站 XML 格式
- 直接写入下载目录

## 最终落盘设计

媒体下载和合并阶段使用的主要是：

- 应用私有目录临时文件
- 缓存目录里的字幕/封面临时文件

最终完成后，`FileOutputManager.moveToDownloadAndRegister()` 会：

- Android 10+：
  - 使用 `MediaStore.Downloads`
  - 写入 `Download/BILIBILIAS/...`
- Android 10 以下：
  - 直接移动到公共下载目录

这样做的目的：

- 临时工作文件和最终交付文件分离
- 避免未完成文件过早暴露给系统媒体库

## 命名设计

### `NamingConventionHandler`

文件：`app/src/main/java/com/imcys/bilibilias/download/NamingConventionHandler.kt`

最终文件名不是子任务文件名，而是按命名规则动态生成：

- 视频走 `videoNamingRule`
- 番剧走 `bangumiNamingRule`

可用占位符来自：

- 视频规则集合
- 番剧规则集合

命名处理时会：

- 把 `/` 替换成 `_`
- 清理重复下划线
- 自动补扩展名

这说明：

- 命名规则是最终交付层能力，不应影响子任务临时文件结构。

## 取消、暂停与清理

### 暂停

- 取消活动 Job
- 更新状态为 `PAUSE`
- 保留已下载的临时文件，后续可继续续传

### 取消

- 取消活动 Job
- 删除子任务文件和 `*.downloading`
- 标记为 `CANCELLED`
- 从运行时列表移除

### 错误

- 更新状态为 `ERROR`
- 保留现场以便观察和后续恢复策略扩展

## 当前设计的几个关键取舍

### 1. 任务树和执行层分离

优点：

- 业务结构清晰
- 便于支持合集、番剧、互动视频这类复杂下载对象

代价：

- 实现上要维护 repository 和 manager 两套视角

### 2. 临时文件与最终文件分离

优点：

- 便于断点续传和失败回滚
- 避免半成品污染下载目录

### 3. 组件化下载协作

当前下载系统拆成：

- `VideoInfoFetcher`
- `DownloadExecutor`
- `FileOutputManager`
- `FfmpegMerger`
- `NamingConventionHandler`
- `SubtitleDownloader`

这比旧式“下载管理器一个类包办全部”更便于维护。

## 改动下载链路时重点检查

- `DownloadTaskRepository`
  - 任务树是否正确，segment 是否完整
- `VideoInfoFetcher`
  - 流选择是否正确
- `DownloadExecutor`
  - 断点续传、重试、CDN 替换是否受影响
- `NewDownloadManager`
  - 状态流转、通知、并发、取消/暂停是否受影响
- `FileOutputManager`
  - 最终落盘路径和 MediaStore 注册是否正常
- `NamingConventionHandler`
  - 文件名是否符合用户设置

## 建议的验证范围

- 普通视频：音视频合并下载
- 纯音频：仅音频下载
- 仅视频：无音轨场景
- 番剧：多季度或多剧集选择
- DURL 回退：无法获取 DASH 时
- 暂停/恢复：检查 `Range` 续传
- 取消：检查临时文件清理
- 下载封面、弹幕、字幕：检查独立输出
- Android 10+ 与旧版存储落盘差异
