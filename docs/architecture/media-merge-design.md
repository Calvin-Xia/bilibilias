# 媒体合并与 FFmpeg 设计

本文档单独说明 BILIBILIAS 当前媒体合并设计，重点覆盖音视频合流、字幕嵌入、封面嵌入、容器选择、运行时并发和临时文件清理。内容以 `FfmpegMerger`、`FfmpegRuntimeConfig`、`SubtitleDownloader` 及 `NewDownloadManager` 的协作为准。

## 为什么单独设计“合并层”

对 B 站下载来说，下载完成不等于用户拿到最终成品。原因包括：

- DASH 常见为分离的视频轨和音频轨
- 用户可能选择嵌入字幕
- 用户可能选择嵌入封面
- 最终输出容器可能和下载流的原始扩展名不同

因此系统把“下载媒体”和“生成最终成品”拆成两个阶段：

- 下载阶段
- 合并阶段

合并阶段的目标不是再次转码全部内容，而是尽量在保持质量的前提下做最小必要处理。

## 相关组件

### `FfmpegMerger`

文件：`app/src/main/java/com/imcys/bilibilias/download/FfmpegMerger.kt`

职责：

- 构建 FFmpeg 命令
- 根据媒体时长计算合并进度
- 执行 FFmpeg 会话
- 在失败或取消时删除无效输出

### `FfmpegRuntimeConfig`

文件：`app/src/main/java/com/imcys/bilibilias/download/FfmpegRuntimeConfig.kt`

职责：

- 根据用户设置的下载并发和“是否允许并发合并”控制 FFmpegKit 并发上限

### `SubtitleDownloader`

文件：`app/src/main/java/com/imcys/bilibilias/download/SubtitleDownloader.kt`

职责：

- 为嵌入场景下载并生成本地字幕文件
- 为外挂场景直接输出字幕文件

## 合并阶段入口

媒体下载成功后，`NewDownloadManager.handleSuccessor()` 会进入合并阶段：

1. 创建合并进度回调
2. 创建临时输出文件
3. 调用 `ffmpegMerger.mergeMedia(...)`
4. 清理嵌入字幕和封面的临时缓存
5. 将合并产物移动到最终下载目录

只有在 `downloadMedia = true` 时才会走这个阶段。

## 合并输入来源

FFmpeg 合并阶段的输入可能包括四类：

### 1. 媒体子任务文件

来自 `DownloadSubTask.savePath`：

- 视频轨
- 音频轨

这是合并的主输入。

### 2. 本地字幕文件

来自 `SubtitleDownloader.downloadSubtitlesForEmbed()`：

- 远端 CC JSON 先转成本地 SRT
- 保存到缓存目录 `externalCacheDir/cc`
- 作为额外输入传给 FFmpeg

### 3. 本地封面文件

来自 `handlePredecessor()`：

- 先把封面下载到缓存目录 `externalCacheDir/cover`
- 合并时作为附加图片输入

### 4. 任务元数据

来自 `AppDownloadTask` 与 `DownloadSegment`：

- 标题
- 描述
- 引用来源 `copyright`
- 命名规则
- 输出容器配置

## 输出文件策略

### 临时输出文件

合并时不会直接往公共下载目录写最终文件，而是先创建临时输出文件：

- 路径位于媒体临时目录附近
- 文件名使用 `segmentId + timestamp + extension`

这样做的原因：

- 合并失败时容易直接回滚
- 避免最终目录出现半成品
- 在最终命名尚未确定前，先完成媒体生产

### 最终文件

合并成功后才会：

1. 根据命名规则生成最终名称
2. 移动到系统下载目录
3. 注册到 `MediaStore`

## 容器选择设计

输入配置来自 `MediaContainerConfig`：

- `audioContainer`
- `videoContainer`

它由分析页生成，并传入下载链路。

### 输出扩展名

最终扩展名由下载模式决定：

- `AUDIO_VIDEO / VIDEO_ONLY`
  - 使用 `videoContainer.extension`
- `AUDIO_ONLY`
  - 使用 `audioContainer.extension`

### MIME Type

最终落盘时的 MIME Type 同样根据容器决定。

这意味着：

- 子任务文件扩展名和最终输出扩展名可能相同，也可能只是中间态。
- 最终“交付格式”由合并阶段统一收口。

## FFmpeg 命令构建思路

`FfmpegMerger.buildFfmpegCommand()` 的命令构建大致分为几步。

### 1. 基础参数

会先加入：

- `-y`

用于覆盖输出。

### 2. 输入文件列表

按顺序加入：

- 所有媒体子任务文件
- 所有字幕文件
- 封面文件

输入顺序非常重要，因为后续 `-map` 依赖这个顺序来引用流索引。

### 3. 流映射

根据模式决定映射哪些流：

- 视频模式启用时：`-map 0:v:0`
- 音频模式启用时：把音频输入逐个映射进去
- 字幕存在时：逐条映射字幕流
- 封面存在时：再额外映射封面图流

这说明当前设计是“显式 map”，而不是依赖 FFmpeg 自动猜流。

显式映射的好处：

- 可控
- 便于支持多音轨、多字幕、附图

## 编码策略

### 视频

只要启用视频，默认：

- `-c:v copy`

意味着：

- 不重新编码视频
- 保留原始画质
- 降低耗时

### 音频

默认：

- `-c:a copy`

但有一个例外：

- 如果最终是纯音频，且输出容器是 `MP3`
  - 会改为 `libmp3lame`
  - 使用 `-q:a 2`

这说明：

- 大多数场景尽量无损拷贝流
- 只有容器兼容性需要时才做必要转码

## 字幕嵌入设计

### 字幕来源

当前嵌入字幕流程是：

1. 调用视频详情中的字幕列表
2. 拉取字幕 JSON
3. 转为本地 SRT
4. 把 SRT 作为 FFmpeg 输入

### 字幕编码策略

若有字幕且启用了视频：

- 非 `MKV`
  - 使用 `mov_text`
- `MKV`
  - 使用 `copy`

这是一个容器兼容性设计：

- `MP4` 等容器更适合 `mov_text`
- `MKV` 对原生字幕流包容度更高

### 字幕元数据

每条字幕还会写入：

- `language`
- `title`

来源是：

- `LocalSubtitle.lang`
- `LocalSubtitle.langDoc`

这让播放器里能识别字幕语言和显示名称。

## 封面嵌入设计

如果启用了封面嵌入：

- 封面图片会作为额外输入
- 其视频流会被标记为 `attached_pic`
- 编码器使用 `mjpeg`
- 同时写入 `title=Cover`

这是典型的“专辑封面 / 附图”处理方式。

### 封面索引为什么复杂

封面的流索引并不是固定的，因为它取决于：

- 前面有多少主媒体输入
- 有多少字幕输入

所以命令构建里会先计算：

- 媒体输入数量
- 字幕输入起始索引
- 封面索引

这块改动时要格外小心，最容易因为 map 索引错位导致合并失败。

## 元数据写入设计

当前最终文件会写入至少三类元数据：

- `title`
  - segment 标题
- `description`
  - 任务描述
- `copyright`
  - 使用下载任务推导出的原始 Referer 页面地址

其中 `copyright`
实际上更接近“来源引用信息”，会指向：

- 番剧详情页
- 视频详情页

这让最终文件保留来源上下文。

## 进度计算

### 为什么不能直接用 FFmpeg 百分比

FFmpeg 并不会直接给出稳定百分比，因此当前实现会：

1. 先用 `FFprobeKit` 获取主媒体时长
2. 在 FFmpeg statistics 回调中读取已处理时间
3. 用 `statistics.time / duration` 计算进度

### 限流

进度回调不是每次都上报，而是做了约 100ms 的限流，避免：

- UI 刷新过于频繁
- 通知更新过多

## 取消与异常处理

### 取消

如果协程取消：

- `invokeOnCancellation` 会调用 `FFmpegKit.cancel(sessionId)`
- 输出文件会被删除

### 失败

如果 FFmpeg 返回失败：

- 删除输出文件
- 抛出异常，交给上层处理

### 成功校验

即使 FFmpeg 返回 success，仍会进一步检查：

- 输出文件是否存在
- 输出文件长度是否非 0

否则仍视为失败。

## 运行时并发控制

`FfmpegRuntimeConfig.apply(maxConcurrentDownloads, enabledConcurrentMerge)` 会把 FFmpeg 异步并发上限设置为：

- 如果允许并发合并且下载并发数大于 1
  - 用下载并发数作为 FFmpeg 并发上限
- 否则
  - 固定为 1

这说明当前设计默认更保守：

- 下载并发可以高一些
- 合并并发要视设备能力和用户设置决定

## 合并前后的清理策略

### 合并前保留

必须保留：

- 视频/音频子任务文件
- 本地字幕缓存
- 本地封面缓存

### 合并成功后

`updateTaskAndCleanup()` 会：

- 先把 segment 的 `savePath` 更新为临时输出文件路径
- 删除所有子任务媒体文件

随后 `cleanupMergeRuntimeArtifacts()` 会：

- 删除本地字幕缓存
- 删除封面缓存
- 清空 `TaskRuntimeInfo`

### 合并失败后

- 删除临时输出文件
- 同样清理字幕和封面缓存
- 异常继续向上抛

这个策略保证：

- 缓存文件不会在成功或失败后长期堆积
- 成功后只留下最终媒体文件

## 字幕文件的双路径设计

`SubtitleDownloader` 现在实际上有两条路径：

### 1. 嵌入路径

- 转成临时 SRT
- 只为 FFmpeg 合并服务
- 合并后删除

### 2. 外挂路径

- 转成用户选择的 `ASS` 或 `SRT`
- 直接写入下载目录
- 不参与 FFmpeg 合并

这两条路径不能混淆：

- 嵌入字幕是“合并输入”
- 外挂字幕是“独立产物”

## 当前设计的关键取舍

### 1. 以 `copy` 为主，必要时转码

优点：

- 更快
- 保真

风险：

- 更依赖输入流和目标容器兼容性

### 2. 合并和最终落盘分开

优点：

- 失败回滚更清晰
- 最终命名与媒体生产解耦

### 3. 字幕统一先转本地文件再嵌入

优点：

- FFmpeg 输入稳定
- 不需要在合并时再次请求网络

## 改动这块时重点检查

- `buildFfmpegCommand()` 的输入顺序和 `-map` 索引
- 字幕容器兼容性
- MP3 纯音频转码分支
- 封面嵌入的流索引和 disposition
- 取消时是否正确清理输出文件
- 合并后是否正确删除临时字幕/封面/子任务文件
- `FfmpegRuntimeConfig` 是否与下载并发逻辑一致

## 建议验证场景

- DASH 视频 + 音频合并
- 纯音频导出到 M4A
- 纯音频导出到 MP3
- 视频嵌入字幕
- 视频嵌入封面
- 视频同时嵌入字幕和封面
- 多音轨选择后的合并结果
- 合并中取消任务
- 多任务并发下载 + 串行/并发合并
