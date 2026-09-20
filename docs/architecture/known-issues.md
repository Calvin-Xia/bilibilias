# 已知问题与技术债

本文记录当前仓库中**已确认存在但尚未修复**的问题。每条都附代码位置与判断依据；"为何未修"说明它为何被推迟，而不是否认问题。

严重度分级：

- **A 级（数据/挂起）**：可能造成用户数据损坏或任务永久卡死
- **B 级（正确性）**：行为不符合预期，但影响可控或有绕行方式
- **C 级（可维护性）**：不影响当前运行，但提高后续改动成本
- **D 级（范围外）**：已知的功能缺失，属项目当前定位下的接受状态

修复状态会随代码演进而变化；改动本文所列代码前请先复核对应位置是否仍然成立。

## A 级

### A1. 下载任务可能因会话记录被清理而永久卡在 MERGING

`app/src/main/java/com/imcys/bilibilias/download/FfmpegMerger.kt`

FFmpegKit 的 `FFmpegKitConfig.clearSessions()` 会清除会话历史（**包含仍在运行的会话**），而被删除会话的完成回调不会触发。此前 `executeFfmpegCommand` 的 `finally` 中调用了它，在并发合并场景下会让其他任务的协程永远等不到回调。

**当前状态：已修复**（删除了该调用）。FFmpeg 会话历史已由 `FfmpegRuntimeConfig.apply()` 中的 `setSessionHistorySize(ffmpegConcurrency)` 限制，无需手动清理。

保留此条是因为 `FfmpegRuntimeConfig` 的并发上限设置与 `enabledConcurrentMerge` 开关耦合，若后续调整并发策略需重新确认这里的假设。

### A2. 账号凭据与 Cookie 明文存储

- `core/database/src/commonMain/kotlin/com/imcys/bilibilias/database/entity/BILIUsersEntity.kt`（`access_token` / `refresh_token`）
- `core/database/src/commonMain/kotlin/com/imcys/bilibilias/database/entity/BILIUserCookiesEntity.kt`（Cookie 值）
- `core/datastore/`（DataStore `.pb` 文件）

数据库与 DataStore 文件均为明文，未使用 SQLCipher 或任何字段级加密，root 设备或备份提取可直读登录态。

**为何未修**：需要引入加密库并设计密钥管理（Android Keystore / iOS Keychain），涉及数据库迁移与两端平台实现，属独立工程；需在能验证迁移正确性（当前无迁移测试）之后进行。

## B 级

### B1. `isAppInForeground` 依赖 `runningAppProcesses`

`app/src/main/java/com/imcys/bilibilias/download/NewDownloadManager.kt`

使用 `ActivityManager.runningAppProcesses` 判断前台状态。API 21+ 该方法只返回自身进程，因此它实际判断的是"自己是否处于前台"，对当前用途有效，但语义与命名不符，且在新系统上行为可能进一步收紧。

**为何未修**：功能正确，属写法陈旧。改用 `ProcessLifecycleOwner` 更规范，但会引入新依赖并需重新验证前后台切换时队列启停的行为，收益不足以支撑本轮风险。

### B2. 转换器缺少非法值兜底

`core/database/src/commonMain/kotlin/com/imcys/bilibilias/database/converter/`

枚举转换器使用 `valueOf`，`MediaContainerConverter` 使用 `first{}` 匹配扩展名。若数据库中存入非法枚举字符串（例如降级安装、手工改库），读取时会抛异常而非回退到默认值。

**为何未修**：正常路径不会产生非法值。加兜底会掩盖潜在的数据写入缺陷，更适合先明确"遇到非法值应视为数据损坏并上报"还是"静默回退"。

### B3. DataStore 未配置损坏处理

`core/datastore/src/commonMain/kotlin/com/imcys/bilibilias/datastore/DataStoreFactory.kt`

`DataStoreFactory.create` 未传 `corruptionHandler`。`.pb` 文件损坏时 `CorruptionException` 会直接抛给调用方，导致设置读取失败，应用表现为设置页异常。

**为何未修**：需要先决定损坏时的策略（重置为默认值还是提示用户），这属于产品决策而非纯技术修复。

### B4. `MIGRATION_3_4` 回填值与默认设置不一致

`core/database/src/commonMain/kotlin/com/imcys/bilibilias/database/Migration.kt`

`MIGRATION_3_4` 用硬编码 CASE 回填 `media_container`，其中 `AUDIO_ONLY → mp3`，而 `AppSettingsSerializer.appSettingsDefault` 中默认音频容器为 `m4a`；同时未回填 `quality_description`。

**为何未修**：影响的是升级用户的既有数据，修正回填逻辑不会改变已升级设备的数据，需要单独设计一次数据订正迁移，且当前无迁移测试可验证。

## C 级

### C1. `NewDownloadManager` 职责过载

`app/src/main/java/com/imcys/bilibilias/download/NewDownloadManager.kt`（约 990 行、11 个构造依赖、约 60 个成员）

调度、持久化、FFmpeg 调用、通知文案、文件 IO 集中在单个类中，且直接依赖 `DownloadService` 具体类（非接口）。

**为何未修**：按本轮范围，只修复了并发正确性（原子标志位、进度双写、调度竞态、先登记后启动），未做结构拆分——拆分需要搬迁大量方法，在测试覆盖不足时回归风险高于收益。建议在补足下载链路测试后再拆分。

### C2. 平台接口依赖强转

`app/src/main/java/com/imcys/bilibilias/BILIBILIASApplication.kt`

共享层定义的平台接口与 Android 实现之间通过强制类型转换连接，类型不匹配时是运行期失败而非编译期错误。

### C3. 测试覆盖仍然偏薄

全仓库 33 个测试用例覆盖约 44,000 行代码。本轮新增了纯逻辑基线（签名、WBI、正则、数字格式化、命名规则），但以下高风险区域**仍无任何测试**：

- 下载链路（`shared/`、`core/data/`、`app/download/`）——纯逻辑部分需要先做依赖抽象才能测
- 数据库迁移（`Migration.kt`）
- `core/ui` 全部 49 个文件
- `BILIImageURL`（190 行 URL 拼装 DSL）、`MinEvent` 事件通道

### C4. 重复实现与命名不一致

- `shared/src/commonMain/kotlin/com/imcys/bilibilias/shared/feature/user/source/` 下三个 `PagingSource` 结构高度雷同
- Route 定义位置分裂：仅部分 feature 放在 `navigation/` 子包，其余散落在 Screen 文件中
- 命名笔误：`CookeLoginRoute`（Cookie）、`DongmhuaDownloadScreen`（动画）、`AboutRouter`（应为 Route）
- `core/network` 中约半数 `@SerialName` 与属性名完全一致，属冗余映射

### C5. 网络层细节

- `KtorDI.kt` 默认客户端的 `LogLevel.ALL` 会记录 Cookie / Authorization 头，仅靠 `enabledNetworkLogging` 在 release 关闭，缺少编译期隔离
- `HttpRequestRetry` 仅对服务端错误重试，超时与 IO 异常不重试
- `adapter/BgmNetWorkAdapter.kt`、`GithubNetWorkAdapter.kt` 只处理 code 200，其他状态码不 emit，调用方 `.last()` 会抛 `NoSuchElementException`
- 无 HTTP 响应缓存，重复请求不落盘
- `core/data` 的多个 repository 直接把网络模型返回给上层（如 `getVideoView` 返回 `BILIVideoViewInfo`），网络 DTO 未做转换

### C6. FfmpegKit 第三方分叉

使用 `com.moizhassan.ffmpeg:ffmpeg-kit-16kb`（原版 FFmpegKit 已归档）。该分叉的 API 与维护状态不受控，升级或替换需要重新验证合并链路。

## D 级（范围外，接受状态）

### D1. iOS 端下载功能未实现

`IOSDownloadExecutor.downloadFile` 恒返回 `false`；`IOSDownloadManager` 除相册保存外全部为"暂不实现"。iOS 侧当前只有 UI 与解析能力。

**状态说明**：上游源仓库已归档，本项目当前定位为**仅维护 Android**。iOS 目标与 `iosMain` 代码原样保留（Windows 上构建时会被 Kotlin Multiplatform 插件自动跳过，不触发 Kotlin/Native 工具链下载，对 Android 构建零成本），但不再继续实现。这属于**已知且已接受**的状态，不是待修缺陷。

### D2. 冻结的模块与插件

- `core/ffmpeg`：未纳入 `settings.gradle.kts`，详见 [模块结构](../architecture/modularization.md) 的"暂退模块与保留代码"
- `core/ksp-processor`：Koin 原生导出处理器，未纳入构建，配套的 `@KoinNativeExport` 注解当前无任何使用点

## 相关文档

- [构建与运行](../development/build-and-run.md)：环境要求与常用命令
- [测试与质量](../testing/testing.md)：测试策略与验证命令
- [模块结构](../architecture/modularization.md)：模块职责与依赖方向
