# 模块结构

BILIBILIAS 使用 Gradle 多 module。根 `settings.gradle.kts` 当前包含：

```text
:app
:core:ui
:core:common
:core:database
:core:datastore
:core:data
:core:network
:core:datastore-proto
:shared
```

当前 FFmpeg 能力由 `app` 直接依赖第三方库 `com.moizhassan.ffmpeg:ffmpeg-kit-16kb` 提供。

历史上曾保留 `core/ffmpeg`（自编译 FFmpeg 与逐帧工具）与 `core/ksp-processor`（Koin 原生导出处理器）两个未纳入构建的模块。两者已作为死代码删除，连同 `FFmpegVerificationConventionPlugin`、版本目录中仅服务它们的条目与 `gradle.properties` 的 `as.ffmpeg.version` 一并清理；如需找回可从 git 历史取。详见 [已知问题与技术债](./known-issues.md) 的 D2。

## Module 职责

| Module | 职责 |
| --- | --- |
| `:app` | Android application、Activity/Application、页面实现、下载管理、渠道 flavor、Firebase/百度/Google Play/Shizuku/App Functions 接入 |
| `:core:ui` | Compose 主题、公共 UI 组件、UI 工具 |
| `:core:common` | 通用事件、BuildConfig 运行时桥接、工具类、基础异常、跨 module 公共类型 |
| `:core:network` | Ktor client、API service、网络 model、网络插件、B 站签名/token 工具 |
| `:core:database` | Room 3 database、DAO、entity、converter、schema |
| `:core:datastore` | App/User/Google Play 设置 DataStore source、Okio serializer 与 Koin 注入 |
| `:core:datastore-proto` | Wire protobuf schema、生成代码与兼容扩展 |
| `:core:data` | repository、UI 可消费的 data model、数据源编排 |
| `:shared` | Kotlin Multiplatform 共享层，承载跨 Android/iOS 的 app 壳层、Navigation 3 页面、平台桥接、下载运行时抽象和业务 UI |
| `build-logic` | Gradle convention plugins、Kotlin/Android 公共构建配置、点击埋点字节码插桩、百度 jar 下载 |

## `:shared` 包结构

`:shared` 是当前跨端页面和平台能力的主要承载模块。它依赖 `:core:data`、`:core:common`、`:core:ui`，并导出 `:core:data` 以供 iOS framework 使用。

当前主要包结构：

| Package | 职责 |
| --- | --- |
| `shared.app` | 共享 app 入口和顶层 Compose 容器 |
| `shared.di` | shared 层 Koin module |
| `shared.navigation` | Navigation 3 顶层导航、route 注册和 back stack 处理 |
| `shared.feature.*` | 业务页面，按 feature 组织 Screen、ViewModel、navigation 和局部 components |
| `shared.ui.component` | BILIBILIAS 业务组件，例如下载卡片、分集选择、登录平台筛选、存储环、用户/稿件展示 |
| `shared.ui.model` | shared UI 层使用的展示 model |
| `shared.platform.*` | 平台能力 expect/actual 与平台组件封装 |
| `shared.download.*` | shared 下载相关 model、命名、运行时和格式转换工具 |
| `shared.util` | 仍依赖 shared 平台或业务上下文的工具 |

`shared.platform` 继续按能力拆分：

- `clipboard`：剪贴板桥接。
- `device`：设备信息。
- `firebase`：Firebase 相关平台入口。
- `permission`：权限请求和授权状态。
- `runtime`：运行时平台信息、Android Koin application 入口、下载运行时平台能力。
- `storage`：文件系统、文件打开、存储平台能力。
- `component`：需要平台实现的 Compose 组件，例如 WebView、BackHandler、HTMLText。

`shared.download` 继续按下载链路职责拆分：

- `model`：下载任务和本地媒体/字幕相关 model。
- `naming`：命名规则处理。
- `runtime`：跨端下载执行器和运行时管理。
- `util`：弹幕、字幕等格式转换工具。

新增 shared 代码时，优先按“页面 feature、平台能力、下载链路、业务 UI”四类归位。不要把纯公共 Compose 组件继续放入 `shared.ui.component`；脱离业务语义后仍可复用的组件应放到 `:core:ui`。

## 暂退模块与保留代码

`core/ffmpeg` 与 `core/ksp-processor` 两个未纳入构建的模块已删除（见上）。当前生效的媒体合并与逐帧相关能力来自 `:app` 下载链路中的 `FfmpegMerger`，以及 `app` 直接依赖的 `ffmpeg-kit`。

FFmpeg 的并发上限与会话历史上限统一由 `shared` 的 `DownloadRuntimePlatform.applyFfmpegRuntimeConfig` 设置（`app` 中曾有一份重复实现，已合并）。调整并发策略时只需改这一处。

## 依赖方向

当前主要依赖关系：

```text
:app
  -> :shared
  -> :core:common
  -> :core:data

:shared
  -> :core:data
  -> :core:common
  -> :core:ui

:core:data
  -> :core:common
  -> :core:database
  -> :core:datastore
  -> :core:network

:core:network
  -> :core:common
  -> :core:database
  -> :core:datastore

:core:common
  -> :core:ui

:core:datastore
  -> :core:datastore-proto
```

注意：`core:network` 目前会依赖 `core:database` 和 `core:datastore`，这比严格分层更宽松。新增代码时先沿用现状，不要为了“理想架构”做大规模重排；如果要收紧依赖，需要单独设计迁移。

## build-logic

`build-logic` 里的 convention plugin 是本项目工程一致性的核心：

- `bilibilias.android.application`：应用 module 公共配置，应用 Compose 插件，设置 targetSdk，并添加测试依赖。
- `bilibilias.android.library`：Android library 公共配置，设置 compileSdk/minSdk/JVM、test runner 和资源前缀。
- `bilibilias.android.koin`：统一添加 Koin 依赖，Compose module 会附加 koin-compose。
- `bilibilias.baidu.jar`：处理百度统计 jar。

Android/Kotlin 公共配置在 `com/imcys/bilibilias/buildlogic/KotlinAndroid.kt`：

- compileSdk：37
- minSdk：24
- Java/Kotlin target：11
- Android build-logic 自身 target：17
- Kotlin 编译参数包含 experimental coroutines、context parameters、explicit backing fields、return-value-checker 等。

## 新增模块建议

- 新增纯 UI 组件：优先放 `core:ui`。
- 新增跨功能工具或事件：放 `core:common`，避免依赖 app。
- 新增外部接口：放 `core:network`，并通过 repository 暴露。
- 新增持久化实体：放 `core:database`，同步维护 schema。
- 新增设置项：先改 `core:datastore-proto` proto，再改 serializer/source/repository/UI。
- 新增业务聚合：优先放 `core:data` repository。
- 新增跨端页面或 shared 业务页面：按 `shared.feature.<feature>` 组织。
- 新增 Android-only 入口能力：放 `:app`，不要反向污染 shared。
- 新增完整 feature module：当前仓库尚未拆独立 feature module；如果后续要拆，应先明确它和 `:shared` 的边界。
