# 架构说明

BILIBILIAS 当前是一个以 `:app` 为入口、多个 `core:*` module 提供基础能力的 Compose Android 应用。整体风格与 nowinandroid 一样偏向单 Activity、多 module、约定插件和声明式 UI，但本项目使用 Koin、Navigation 3、Room/DataStore/Ktor，并保留较多下载和平台接口相关逻辑。

## 启动流程

`BILIBILIASApplication` 负责进程级初始化：

- 写入 `CommonBuildConfig` 中的构建期配置。
- 在应用进程早期做基础初始化，例如百度统计初始化入口、Koin、内存调度和 FFmpeg 运行时配置。
- 启动 Koin，并注册 `dataStoreModule`、`netWorkModule`、`repositoryModule`、`databaseModule`、`appModule`。
- 初始化 `FairMemoryReceiver`，在内存压力时回收下载相关资源。
- 从 `AppSettingsRepository` 读取下载并发和合并配置，写入 `FfmpegRuntimeConfig`。
- 注册 Android App Functions，暴露 `BILIAnalysisAppFunctions`。

`MainActivity` 负责 UI 容器和系统入口：

- 调用 `enableEdgeToEdge()`。
- 挂载 `BILIBILIASTheme` 和 `BILIBILIASAppScreen()`。
- 处理分享文本和 `bilibilias://` deep link，并转成解析页或登录完成页导航事件。
- 基于 `AppSettings.agreePrivacyPolicy` 控制百度统计、Firebase 等能力的实际启用。
- 初始化设置监听、通知渠道和更新检查。

## UI 与状态

UI 主要位于 `app/src/main/java/com/imcys/bilibilias/ui`，公共 Compose 组件位于 `core/ui`。

推荐页面结构：

- `FeatureScreen.kt`：Compose UI、事件回调和局部 UI 组合。
- `FeatureViewModel.kt`：状态持有、业务调用、Flow 收集。
- `FeatureRoute` 或 navigation 文件：Navigation 3 的 `NavKey` 定义。
- `FeatureUiState.kt`：复杂页面状态单独拆出。

已有页面大量使用 `collectAsStateWithLifecycle()` 收集 `StateFlow`。新增 UI 应优先复用 `core/ui/src/main/java/com/imcys/bilibilias/ui/weight` 中的组件，例如 `ASTopAppBar`、`ASIconButton`、`ASTextButton`、`ASAlertDialog`、`ASAsyncImage`。

## 导航

主导航集中在 `BILIBILAISNavDisplay`：

- 使用 `rememberNavBackStack(HomeRoute())` 作为 back stack。
- 页面 key 基于 AndroidX Navigation 3 的 `NavKey`。
- 使用 `NavBackStackSerializer` 和 kotlinx.serialization 将 back stack 保存到 `AppSettings.navBackStack`。
- 通过 `navigatePageEventFlow`、`analysisHandleChannel`、`restoreBackStackEventFlow` 等事件流在非 UI 层触发导航。
- 使用 `rememberSaveableStateHolderNavEntryDecorator()` 保留页面状态，使用 `rememberViewModelStoreNavEntryDecorator()` 为每个页面隔离 ViewModelStore。
- 根据 `AppSettings.enabledNavAnimation` 和 `enabledNavOnBackInvokedCallback` 动态决定前进、返回和预测性返回动画。

新增页面时，优先把 route 定义放在对应 feature 的 `navigation` 包或页面同级，并在 `BILIBILAISNavDisplay` 的 `entryProvider` 中注册。

## 依赖注入

项目使用 Koin：

- `appModule` 注册应用层 ViewModel、下载管理器、App Functions、系统服务封装。
- `repositoryModule` 位于 `core:data`，注册 repository。
- `databaseModule` 位于 `core:database`，注册 Room 数据库和 DAO。
- `dataStoreModule` 位于 `core:datastore`。
- `netWorkModule` 位于 `core:network`，注册 Ktor client、API service 和插件。

新增依赖时按所有权放到对应 module 的 DI 文件，避免把所有对象都塞进 `appModule`。

## 数据层

数据层分为几类：

- `core:network`：API service、网络 adapter、Ktor plugin、网络 model、签名和 token 工具。
- `core:database`：Room 3 database、DAO、entity、converter、schema。
- `core:datastore` 与 `core:datastore-proto`：protobuf schema、serializer、DataStore source。
- `core:data`：repository 和面向 UI 的 data model，向上屏蔽网络、本地库和设置源细节。

推荐依赖方向是 UI/ViewModel 调用 repository，repository 组合 network/database/datastore。不要让 Compose UI 直接调用 API service 或 DAO。

## 下载链路

下载相关代码当前主要仍在 `:app`：

- `NewDownloadManager` 统筹下载任务。
- `VideoInfoFetcher` 获取可下载媒体信息。
- `FileOutputManager` 管理输出位置。
- `DownloadExecutor` 执行网络下载。
- `FfmpegMerger` 和 `FfmpegRuntimeConfig` 管理媒体合并。
- `SubtitleDownloader` 负责字幕下载。
- `NamingConventionHandler` 处理命名规则。

下载链路同时依赖用户设置、任务数据库、Ktor 下载 client、OkHttp、FFmpegKit 和 Android 文件系统能力。改动时要重点验证并发数、取消/暂停、失败恢复、文件名、字幕和合并行为。

## 错误与事件

项目使用 `core:common` 中的事件流承载 toast、页面跳转、解析、请求频繁、登录错误等跨层事件。新增事件应尽量语义化，避免把任意 lambda 或 UI 类型向 data/network 层泄露。
