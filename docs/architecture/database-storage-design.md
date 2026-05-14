# 数据库存储设计

本文档说明 BILIBILIAS 当前 Room 数据库的职责、表结构分工、下载任务树如何落库，以及为什么下载数据要拆成 `task / node / segment` 三层。内容以 `core:database` 和 `DownloadTaskRepository` 的当前实现为准。

## 总览

当前 Room 数据库由 `BILIBILIASDatabase` 提供，主要存三类数据：

- 登录用户
- 用户 Cookie
- 下载任务相关数据

数据库文件并不试图保存所有运行时状态，而是保存：

- 可恢复的账号状态
- 可持久化的下载结构和结果

运行时临时信息例如：

- 正在执行的协程
- 合并中的临时字幕路径
- 当前瞬时进度

仍保留在内存层。

## Database 定义

文件：`core/database/src/main/java/com/imcys/bilibilias/database/BILIBILIASDatabase.kt`

当前数据库版本：

- `version = 4`

当前实体：

- `BILIUsersEntity`
- `BILIUserCookiesEntity`
- `DownloadTask`
- `DownloadTaskNode`
- `DownloadSegment`

DAO：

- `BILIUsersDao`
- `BILIUserCookiesDao`
- `DownloadTaskDao`

## 为什么下载要拆三层

下载相关数据并没有只用一张表，而是拆成：

- `DownloadTask`
- `DownloadTaskNode`
- `DownloadSegment`

这是当前设计里最重要的数据库建模决策之一。

### 1. `DownloadTask`

表示顶层逻辑任务，例如：

- 一个普通视频
- 一个番剧
- 一个合集

它解决的问题：

- 给整组下载内容一个稳定归属
- 保存顶层标题、描述、封面、平台类型等公共信息

### 2. `DownloadTaskNode`

表示树结构中的中间节点，例如：

- 合集章节
- 番剧季度
- 分 P 容器
- 互动视频节点

它解决的问题：

- 下载结构并不天然是平的
- 同一个顶层任务下可能有层级关系

### 3. `DownloadSegment`

表示最终可执行下载片段，例如：

- 某个具体分 P
- 某个具体剧集
- 某个具体章节里的可下载项

它解决的问题：

- 真正的下载、暂停、恢复、完成都要落到最小执行单位

简化理解就是：

- `task` 是“下载什么主题”
- `node` 是“结构怎么组织”
- `segment` 是“最终执行哪一片”

## 用户相关表

### `BILIUsersEntity`

职责：

- 保存登录平台
- mid
- 用户名、头像、等级、会员态
- `refreshToken`
- `accessToken`

它为以下能力提供基础：

- 当前用户识别
- Web / TV 平台登录态切换
- 下载和解析时按当前用户能力分流

### `BILIUserCookiesEntity`

职责：

- 保存属于某个用户的 Cookie 集合

它和 `AsCookiesStorage` 的关系是：

- 数据库存长期状态
- `AsCookiesStorage` 启动后同步到内存用于请求

因此 Cookie 持久化不是“顺手存一下”，而是网络层登录能力的一部分。

## 下载相关表设计

### `DownloadTask`

文件：`core/database/src/main/java/com/imcys/bilibilias/database/entity/download/DownloadTask.kt`

主要字段：

- `taskId`
- `platformId`
- `downloadPlatform`
- `title`
- `description`
- `cover`
- `type`
- `createTime`
- `updateTime`

设计含义：

- `platformId`
  - 顶层任务的外部平台标识，例如 `bvid`、season id、合集 id
- `type`
  - 区分普通视频、番剧、合集等顶层任务类型

### `DownloadTaskNode`

主要字段：

- `nodeId`
- `taskId`
- `parentNodeId`
- `title`
- `pic`
- `platformId`
- `nodeType`

设计含义：

- `taskId`
  - 归属哪个顶层任务
- `parentNodeId`
  - 构成树结构
- `platformId`
  - 当前节点在平台侧的标识
- `nodeType`
  - 说明当前节点到底代表什么

### `DownloadSegment`

主要字段：

- `segmentId`
- `nodeId`
- `taskId`
- `title`
- `cover`
- `segmentOrder`
- `platformId`
- `platformUniqueId`
- `platformInfo`
- `downloadMode`
- `mediaContainer`
- `qualityDescription`
- `savePath`
- `fileSize`
- `duration`
- `downloadState`
- `namingConventionInfo`

这是最重的一张表，因为它需要同时承接：

- 平台信息
- 下载配置结果
- 执行结果
- 最终输出位置

## 为什么 `DownloadSegment` 需要这么多字段

### `platformId`

保存“当前片段在平台语义上的主标识”，例如：

- 番剧 epId
- 视频某一层的逻辑 id

### `platformUniqueId`

保存真正用于资源侧识别的唯一 id，例如：

- CID

之所以拆成两个字段，是因为：

- 页面/结构层的标识
- 媒体资源层的标识

并不总是同一个值。

### `platformInfo`

保存平台原始信息的 JSON 字符串。

这样做的目的：

- 某些字段不值得单独拆列
- 但下载恢复、命名、补算时仍可能需要原始平台上下文

### `downloadMode`

记录最终这个片段是：

- 音视频
- 仅视频
- 仅音频

注意这是 segment 级别字段，不是全局设置。因为不同片段在回退后可能出现模式变化，例如：

- 原本想下音视频
- 但实际只有 DURL 单文件，最后收敛成 `VIDEO_ONLY`

### `mediaContainer`

记录该片段最终面向用户的目标容器。

它和运行时的 `MediaContainerConfig` 相关，但最终要固化进数据库，方便：

- 后续展示
- 恢复
- 迁移旧数据

### `qualityDescription`

记录最终下载质量的人类可读描述，例如：

- 视频清晰度
- 音频品质

它不是选项输入，而是结果落库。

### `savePath`

保存最终产物位置。

注意这个字段在任务执行过程中会经历几个阶段：

- 初始可能为空或临时路径
- 合并成功后先写入临时输出文件路径
- 最终移动到下载目录后再更新为 `MediaStore uri` 或公共路径

### `namingConventionInfo`

它是非常重要的持久化字段，用来保存该片段对应的命名变量上下文。

这样做的原因：

- 最终文件命名发生在较后阶段
- 不能依赖那时还一定能重新请求原始详情
- 命名规则变更后也需要基于已有上下文重新生成

## 外键与级联删除

### `DownloadTaskNode`

外键：

- `task_id -> download_task.task_id`
- `parent_node_id -> download_task_node.node_id`

删除策略：

- 父任务删掉，节点级联删除
- 父节点删掉，子节点级联删除

### `DownloadSegment`

外键：

- `node_id -> download_task_node.node_id`
- `task_id -> download_task.task_id`

删除策略：

- 节点删掉，片段级联删除
- 顶层任务删掉，片段也会删掉

这就是为什么 DAO 可以提供：

- 删除 task
- 删除 node
- 删除 segment

而不需要手工维护整棵树的删除顺序。

## DAO 设计

文件：`core/database/src/main/java/com/imcys/bilibilias/database/dao/DownloadTaskDao.kt`

### 查询能力

当前 DAO 支持按几种关键路径查：

- `platformId -> task`
- `taskId -> task`
- `nodeId -> task`
- `taskId + platformId -> node`
- `nodeId -> node`
- `nodeId + platformId -> segment`
- `segmentId -> segment`
- 全部 segment 流式监听

这说明数据库访问模式主要围绕：

- 创建时查重
- 执行时按节点和片段定位
- UI 层观察片段列表

### 更新时间策略

DAO 没有简单直接暴露裸 `update`，而是包装了：

- `updateTask()`
- `updateNode()`
- `updateSegment()`

它们会自动刷新：

- `updateTime`

这让业务层不需要每次自己手动改更新时间字段。

## 下载树如何落库

`DownloadTaskRepository` 在创建下载任务时，会做几件事：

1. 先按 `platformId` 查顶层 `DownloadTask`
2. 已存在则更新 `updateTime`
3. 不存在则新建 `DownloadTask`
4. 再按视频/番剧/合集结构递归创建 node
5. 最后为每个可执行片段创建 segment

也就是说数据库里落的是“内容结构 + 可执行片段”，而不是只有最终文件列表。

## 为什么数据库不直接存运行时子任务

当前运行时还有 `DownloadSubTask` 概念，但它没有进 Room。

原因很明确：

- 它只服务当前执行期
- 它是由播放信息和模式实时推导出来的
- 它的路径多是临时文件路径

真正需要持久化的是：

- segment 的业务身份
- segment 的最终结果

而不是下载过程中的瞬时拆分细节。

## 类型转换器设计

下载相关字段里有很多不是 SQLite 原生类型，因此用了多组 `TypeConverter`，例如：

- `DownloadModeConverter`
- `DownloadPlatformConverter`
- `DownloadStateConverter`
- `DownloadStageConverter`
- `DownloadTaskNodeTypeConverter`
- `DownloadTaskTypeConverter`
- `MediaContainerConverter`
- `NamingConventionConverter`

其中最值得注意的是：

- `NamingConventionConverter`
  - 负责把 `NamingConventionInfo` 序列化入库
- `MediaContainerConverter`
  - 负责把容器类型稳定持久化

这也是为什么实体层可以保持较强类型，而不是到处手写字符串。

## Migration 设计

文件：`core/database/src/main/java/com/imcys/bilibilias/database/Migration.kt`

当前已有迁移：

- `1 -> 2`
  - 增加 `platform_unique_id`
- `2 -> 3`
  - 增加 `naming_convention_info`
- `3 -> 4`
  - 增加 `media_container`
  - 增加 `quality_description`
  - 并根据旧 `download_mode` 回填默认容器

这说明下载表结构是在不断增强的，演进方向很明确：

- 从“只存能下什么”
- 逐渐扩展到“还要存怎么命名、最终是什么容器、实际是什么质量”

## Schema 文件的作用

目录：

- `core/database/schemas/com.imcys.bilibilias.database.BILIBILIASDatabase/`

这些 JSON 是当前事实数据库结构快照，用于：

- 验证迁移
- 回顾字段演进
- 防止 Room 结构变更无记录

涉及数据库实体改动时，不能只改 Kotlin 实体，还要同步关注 schema 变化是否合理。

## 解析链路与数据库的关系

视频解析阶段本身大多不直接写 Room，但会生成后续落库所需信息：

- `NamingConventionInfo`
- `DownloadViewInfo`
- `platformInfo`
- `cid / epId / seasonId / bvid`

真正写入下载表是在：

- `DownloadTaskRepository.createDownloadTask()`

这意味着：

- 解析层负责“把内容理解清楚”
- 仓库层负责“把可下载结构持久化”

## 当前设计的关键取舍

### 1. 顶层任务、结构节点、执行片段分离

优点：

- 能表达复杂内容结构
- 便于局部删除、恢复和重建

### 2. 运行时子任务不入库

优点：

- 数据库更稳定
- 不把临时实现细节持久化

### 3. 命名和容器信息直接落 segment

优点：

- 最终产物和业务上下文绑定更紧
- 后续重命名、展示、恢复更容易

## 改数据库相关代码时重点检查

- 外键级联是否仍正确
- `updateTime` 是否还会自动刷新
- 新增字段是否需要 migration
- 新增字段是否需要 schema 导出
- 命名规则信息是否会在创建 segment 时同步写入
- `savePath` 的生命周期是否仍符合“临时 -> 最终”的设计

## 建议验证范围

- 新建普通视频下载任务后，三张下载表是否都正确插入
- 合集 / 番剧是否正确形成树结构
- 删除 task 或 node 是否正确级联删除
- 旧版本升级到新版本后的 migration 是否正确
- 暂停 / 恢复 / 完成后 `download_segment` 状态是否正确
- 合并完成后 `savePath`、`media_container`、`quality_description` 是否正确
