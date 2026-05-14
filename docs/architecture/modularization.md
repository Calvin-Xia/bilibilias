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
```

当前 FFmpeg 能力由 `app` 直接依赖第三方库 `com.moizhassan.ffmpeg:ffmpeg-kit-16kb` 提供。

仓库里仍保留 `core/ffmpeg` 目录和相关源码，但 `settings.gradle.kts` 当前没有 `include(":core:ffmpeg")`，而是以注释形式标注“暂时废除”。因此它目前不是参与构建的正式 module，只能视为保留中的历史实现。

## Module 职责

| Module | 职责 |
| --- | --- |
| `:app` | Android application、Activity/Application、页面实现、下载管理、渠道 flavor、Firebase/百度/Google Play/Shizuku/App Functions 接入 |
| `:core:ui` | Compose 主题、公共 UI 组件、UI 工具 |
| `:core:common` | 通用事件、BuildConfig 运行时桥接、工具类、基础异常、跨 module 公共类型 |
| `:core:network` | Ktor client、API service、网络 model、网络插件、B 站签名/token 工具 |
| `:core:database` | Room 3 database、DAO、entity、converter、schema |
| `:core:datastore` | App/User/Google Play 设置 DataStore source 与 serializer |
| `:core:datastore-proto` | protobuf schema 与生成代码 |
| `:core:data` | repository、UI 可消费的 data model、数据源编排 |
| `build-logic` | Gradle convention plugins、Kotlin/Android 公共构建配置、点击埋点字节码插桩、百度 jar 下载 |

## 暂退模块与保留代码

- `core/ffmpeg`：当前未纳入 `settings.gradle.kts`，不会参与正常编译、测试或打包。
- 当前生效的媒体合并与逐帧相关能力，主要分别来自 `:app` 下载链路中的 `FfmpegMerger` / `FfmpegRuntimeConfig`，以及 `app` 直接依赖的 `ffmpeg-kit`。
- 如果后续要重新启用 `core/ffmpeg`，应先同步更新 `settings.gradle.kts`、模块依赖关系、构建文档和媒体链路文档，而不是只恢复目录引用。

## 依赖方向

当前主要依赖关系：

```text
:app
  -> :core:common
  -> :core:data

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
- `bilibilias.jvm.library`：JVM module 公共配置。
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
- 新增完整 feature：若代码规模较大，可考虑新增 feature module；当前仓库尚未拆 feature module，所以小改动先放 `:app` 对应 `ui/*` 包。
