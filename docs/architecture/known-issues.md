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

**当前状态：已修复**（删除了该调用）。FFmpeg 会话历史已由 `DownloadRuntimePlatform.applyFfmpegRuntimeConfig` 中的 `setSessionHistorySize(ffmpegConcurrency)` 限制，无需手动清理。

保留此条是因为该并发上限与 `enabledConcurrentMerge` 开关耦合，若后续调整并发策略需重新确认这里的假设。

### A2. 账号凭据明文存储

- `core/database/src/androidMain/kotlin/com/imcys/bilibilias/database/crypto/AndroidKeystoreCredentialCipher.kt`（加密实现）
- `core/database/src/commonMain/kotlin/com/imcys/bilibilias/database/dao/`（`EncryptedBILIUsersDao` / `EncryptedBILIUserCookiesDao`）
- `core/database/src/commonMain/kotlin/com/imcys/bilibilias/database/Migration.kt`（`MIGRATION_4_5`）

**当前状态：已修复**。账号 token 与 Cookie 值改为在 DAO 边界加密落库：密钥由 Android Keystore 生成保管（AES-GCM，密钥不可导出），写入前加密、读取后解密，业务代码拿到的始终是明文实体。密文带 `v1:` 前缀以支持后续算法演进；解密失败（密钥失效、数据被篡改、格式不符）一律按凭据缺失处理，不回退明文、不抛异常。

存量明文数据由 `MIGRATION_4_5` 直接清除（`bili_users` 的 token 置 NULL、`bili_user_cookies` 清空），升级用户需要重新登录；启动时 `BILIBILIASAppViewModel.reconcileLoginState()` 会对账 DataStore 登录标记与数据库凭据，避免出现"显示已登录但请求无凭据"。

**已知取舍**：Cookie 值是非空字段，解密失败的记录会被整体剔除（而非以 null 表达）；`MediaContainer` 的扩展名兜底统一按 mp4，仅音频但扩展名非法的数据会得到视频容器。iOS 侧 actual 为明文直通，见 D1。

**与登出的交互**：`SettingViewModel.logout()` 依赖读到 `bili_jct` 才继续，Cookie 不可解密时它会提前 return（不清理内存 Cookie、不调登出接口）。正常路径不受影响（密文可解密），但若密钥失效，用户需要先重新登录再登出，或直接清除应用数据。

## B 级

### B1. `isAppInForeground` 依赖 `runningAppProcesses`

`app/src/main/java/com/imcys/bilibilias/download/NewDownloadManager.kt`

使用 `ActivityManager.runningAppProcesses` 判断前台状态。API 21+ 该方法只返回自身进程，因此它实际判断的是"自己是否处于前台"，对当前用途有效，但语义与命名不符，且在新系统上行为可能进一步收紧。

**为何未修**：功能正确，属写法陈旧。改用 `ProcessLifecycleOwner` 更规范，但会引入新依赖并需重新验证前后台切换时队列启停的行为，收益不足以支撑本轮风险。

### B2. 转换器缺少非法值兜底

`core/database/src/commonMain/kotlin/com/imcys/bilibilias/database/converter/`

**当前状态：已修复**。枚举转换器改用 `runCatching { valueOf(...) }` 并在失败时回退到该类型的默认值；`MediaContainerConverter` 的扩展名匹配改为 `firstOrNull` 并回退到 mp4。

需要注意的是**不能回退为 null**：这些字段在实体中都是非空，Room 生成的读取代码在转换结果为 null 时会直接 `error("Expected NON-NULL ...")`，所以兜底必须给出具体默认值。回退值分别是 `LoginPlatform.WEB`、`ASSharedCookieEncoding.URI_ENCODING`、`DownloadMode.AUDIO_VIDEO`、`DownloadPlatform.BILIBILI`、`DownloadStage.DOWNLOAD`、`DownloadState.WAITING`、`DownloadTaskNodeType.BILI_VIDEO_PAGE`、`DownloadTaskType.BILI_VIDEO`、`MediaContainer.MP4`。测试见 `core/database/src/commonTest/.../converter/`。

### B3. DataStore 未配置损坏处理

`core/datastore/src/commonMain/kotlin/com/imcys/bilibilias/datastore/DataStoreFactory.kt`

**当前状态：已修复**。`createDataStore` 现传入 `ReplaceFileCorruptionHandler`，`.pb` 损坏时重置为各 store 的默认实例（`AppSettings` / `User` / `GooglePlaySettings` 的 `getDefaultInstance()`），而不是把 `CorruptionException` 抛给调用方。

**取舍**：选择"重置为默认值"而非"提示用户"，因为设置类数据丢失影响可控，而设置页持续异常对用户更糟；代价是损坏时的数据丢失是静默的。

### B4. `MIGRATION_3_4` 回填值与默认设置不一致

**当前状态：降级为 D 级接受状态**（原 B 级）。`MIGRATION_3_4` 用硬编码 CASE 回填 `media_container`，其中 `AUDIO_ONLY → mp3`，而 `AppSettingsSerializer.appSettingsDefault` 中默认音频容器为 `m4a`；同时未回填 `quality_description`。

**为何接受**：修正回填逻辑不会改变已升级设备的数据，需要单独设计一次数据订正迁移；而项目已停止公开发布 APK，仍处于 3→4 版本的升级用户极少，订正迁移的收益不足以支撑其风险（当前仍无迁移测试）。

## C 级

### C1. `NewDownloadManager` 职责过载

`app/src/main/java/com/imcys/bilibilias/download/NewDownloadManager.kt`（998 行、12 个构造依赖、约 59 个成员）

调度、持久化、FFmpeg 调用、通知文案、文件 IO 集中在单个类中，且直接依赖 `DownloadService` 具体类（非接口）。

**为何未修**：按本轮范围，只修复了并发正确性（原子标志位、进度双写、调度竞态、先登记后启动），未做结构拆分——拆分需要搬迁大量方法，在测试覆盖不足时回归风险高于收益。建议在补足下载链路测试后再拆分。

### C2. 平台接口依赖强转

`app/src/main/java/com/imcys/bilibilias/BILIBILIASApplication.kt`

共享层定义的平台接口与 Android 实现之间通过强制类型转换连接，类型不匹配时是运行期失败而非编译期错误。

### C3. 测试覆盖仍然偏薄

全仓库 64 个测试用例覆盖约 44,000 行代码。纯逻辑基线覆盖了签名、WBI、正则、数字格式化、命名规则，以及 4 版本起的凭据加解密与转换器回退（`core:database` 新增 28 例），但以下高风险区域**仍无任何测试**：

- 下载链路（`core/data/`、`app/download/`）——纯逻辑部分需要先做依赖抽象才能测（`shared/` 已有 `NamingConventionHandlerTest`）
- 数据库迁移（`Migration.kt`）——`core:database` 已具备 host test 基建，但仍未覆盖迁移本身
- `core/ui` 全部 46 个文件
- `BILIImageUrl`（190 行 URL 拼装 DSL）、`MinEvent` 事件通道

### C4. 重复实现与命名不一致

- `shared/src/commonMain/kotlin/com/imcys/bilibilias/shared/feature/user/source/` 下三个 `PagingSource` 结构高度雷同
- Route 定义位置分裂：仅部分 feature 放在 `navigation/` 子包，其余散落在 Screen 文件中
- `core/network` 中约 851 条（共 1555 条）`@SerialName` 与属性名完全一致，属冗余映射

**已修复**：命名笔误 `CookeLoginRoute`（Cookie）、`DongmhuaDownloadScreen`（动画）、`AboutRouter`（应为 Route）已在本轮更正为 `CookieLoginRoute`、`DonghuaDownloadScreen`、`AboutRoute`，同族的 `CookeLoginScreen`/`CookeLoginContent`/`CookeLoginScaffold` 一并修正。

**改 Route 类名时必须保留序列化名**：导航栈以多态序列化写入 `AppSettings.navBackStack`（保存/恢复逻辑在 `shared/navigation/BILIBILAISNavDisplay.kt`），`CookieLoginRoute` 与 `AboutRoute` 上因此加了 `@SerialName` 指向旧类名。新增/重命名 Route 时若改了序列化名，升级用户恢复旧导航栈会反序列化失败；`NavRouteSerializationNameTest` 会卡住这种退化。

恢复逻辑同时改为「失败即清除存档」：原实现反序列化抛异常时会跳过 `updateNavBackStack("")`，同一份坏存档每轮恢复都失败，导航栈恢复功能会永久失效。

### C5. 网络层细节

- `KtorDI.kt` 默认客户端的 `LogLevel.ALL` 会记录 Cookie / Authorization 头，仅靠 `enabledNetworkLogging` 在 release 关闭，缺少编译期隔离
- `HttpRequestRetry` 的实际重试范围：`retryOnServerErrors` 在 Ktor 3.5.0 内部走 `retryOnExceptionOrServerErrors`，因此 **5xx 与 IO/网络异常都会重试**，只有超时（`HttpRequestTimeoutException` 等）不重试
- `adapter/BgmNetWorkAdapter.kt`、`GithubNetWorkAdapter.kt` 只处理 code 200，其他状态码不 emit 内容；由于 `httpRequest` 会先 emit `Loading(true)`，非 200 响应最终停在 loading 态（不会抛 `NoSuchElementException`，当前调用方用 `.collect` 而非 `.last()`）
- 无 HTTP 响应缓存，重复请求不落盘
- `core/data` 的多个 repository 直接把网络模型返回给上层（如 `getVideoView` 返回 `BILIVideoViewInfo`），网络 DTO 未做转换

### C6. FfmpegKit 第三方分叉

使用 `com.moizhassan.ffmpeg:ffmpeg-kit-16kb`（原版 FFmpegKit 已归档）。该分叉的 API 与维护状态不受控，升级或替换需要重新验证合并链路。

### C7. FFmpeg 并发配置的重复实现

**当前状态：已修复**。`applyFfmpegRuntimeConfig` 原本在 `app`（`FfmpegRuntimeConfig`）与 `shared`（`DownloadRuntimePlatform.android.kt`）各有一份完全相同的实现，两个入口分别被 `BILIBILIASApplication` 与 `DownloadConfigViewModel` 调用，改一处漏一处会导致并发上限与会话历史上限不一致。

现在只保留 shared 的 `DownloadRuntimePlatform.applyFfmpegRuntimeConfig` 作为唯一入口，`app` 的重复对象已删除。这也是 A1 那条"调整并发策略需重新确认假设"提醒的唯一变更点。

## 验证链路的已知问题

### V1. `./gradlew test` 不覆盖 KMP host test

`:core:common`、`:core:network`、`:core:database`、`:shared` 的测试注册在 **host test**（`testAndroidHostTest`）上，根任务 `test` 不会触达它们——实测 `./gradlew test` 只运行 `:app:testAlphaDebugUnitTest`（3 个用例）。

要跑全量用例请显式列出 5 个测试任务，见 [测试与质量](../testing/testing.md)。

### V2. Windows 上 `./gradlew check` 必然失败（iOS klib 编译）

Kotlin 2.x 允许在非 macOS 主机上交叉编译 Apple 目标的 klib，因此 `check` 会拉起 `:core:database:compileKotlinIosSimulatorArm64` 等任务；而 `BILIBILIASDatabaseConstructor` 的 iOS actual 由 Room KSP 生成，在 Windows 上不产出，编译以如下错误失败：

```text
Expected BILIBILIASDatabaseConstructor has no actual declaration in module <commonMain> for Native
```

**已验证这是既有问题**：在 `main` 的干净检出（`4853efac`）上单独跑该任务同样失败。影响是 `check` 不能作为 Windows 上的验证门禁——请改用显式的测试任务列表 + `./gradlew lint` + `assembleAlphaDebug`。

### V3. lint 的修复约束（本轮已清零）

`./gradlew lint` 曾因 6 个 error 失败且无 baseline，本轮已逐个修掉：3 个 `NewApi` 是私有方法缺少 `@RequiresApi(Q)`（调用点本来就有 `SDK_INT` 保护）、百度清单的 `CoarseFineLocation`（已移除 `ACCESS_FINE_LOCATION`）、`strings.xml` 的 `MissingTranslation`（`tools:ignore` 豁免）、本机 `local.properties` 的 `PropertyEscape`。

需要注意的是：`app` 模块之外模块的 lint 曾在失败前未跑完，后续改动若触碰其他模块，应重新确认 lint 全绿。

### V4. `isDebugBuild` 会扫描整条命令行

`app/build.gradle.kts` 的 `isDebugBuild` 从 `gradle.startParameter.taskNames` 判断构建类型，因此**同一命令里混跑 debug 与 release 任务会把 release 也当成 debug**，静默关闭 release 的 ABI split（3 个 APK 变成 1 个）。

构建 release 时请单独执行 `./gradlew :app:assembleAlphaRelease`，详见 [构建与运行](../development/build-and-run.md)。

## D 级（范围外，接受状态）

### D1. iOS 端下载功能未实现

`IOSDownloadExecutor.downloadFile` 恒返回 `false`；`IOSDownloadManager` 除相册保存外全部为"暂不实现"（5 处 stub）。iOS 侧当前只有 UI 与解析能力。

**状态说明**：上游源仓库已归档，本项目当前定位为**仅维护 Android**。iOS 目标与 `iosMain` 代码原样保留（Windows 上构建时会被 Kotlin Multiplatform 插件自动跳过，不触发 Kotlin/Native 工具链下载，对 Android 构建零成本），但不再继续实现。这属于**已知且已接受**的状态，不是待修缺陷。

**凭据加密同样不覆盖 iOS**：`core/database/src/iosMain/.../IosCredentialCipher.kt` 的 actual 是明文直通（`PassthroughCredentialCipher`）。若后续恢复 iOS 维护，需要改为 Keychain 保管密钥的实现并设计存量数据迁移。

### D2. 已删除的冻结模块与死配置

以下内容曾以"未纳入构建"的形式长期保留，本轮已删除（可从 git 历史取回）：

- `core/ffmpeg`：真正的 FFmpeg 运行能力来自 `app` 的 `libs.ffmpeg.kit.x6kb` 依赖，该模块的 `abiFilters` 还残留已废弃的 `armeabi-v7a`，属误导性死代码
- `core/ksp-processor` 与 `@KoinNativeExport` 注解：注解全仓库无任何使用点，即使接入也不会产出代码
- 连带清理的配置：`FFmpegVerificationConventionPlugin` 及其在 build-logic 的注册、版本目录的 `bilibilias-android-ffmpegVerification` / `ksp-api` / `kotlinpoet` 条目、`gradle.properties` 的 `as.ffmpeg.version`

## 相关文档

- [构建与运行](../development/build-and-run.md)：环境要求与常用命令
- [测试与质量](../testing/testing.md)：测试策略与验证命令
- [模块结构](../architecture/modularization.md)：模块职责与依赖方向
