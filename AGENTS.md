# AGENTS.md

## 基本要求

- 先阅读当前仓库实际代码和配置，再给结论或改文件；不要只套用外部项目模板。
- 本仓库文档参考 Android 官方 nowinandroid 的工程组织方式，但实现必须以 BILIBILIAS 当前代码为准。
- 工作时尊重用户已有改动，不要回滚、覆盖或整理与任务无关的文件。

## 项目背景

BILIBILIAS 是第三方 B 站视频缓存工具，当前已停止公开发布 APK，源码仍可用于学习和自行编译。任何改动都必须尊重 README 中的合规边界：不得鼓励二次传播缓存内容，不得引入商业或非法用途导向，不得暗示本项目获得哔哩哔哩官方许可。

## 技术栈与结构

- Android application module：`:app`。
- 核心 module：`:core:ui`、`:core:common`、`:core:data`、`:core:network`、`:core:database`、`:core:datastore`、`:core:datastore-proto`。
- 构建约定集中在 `build-logic`，依赖版本集中在 `gradle/libs.versions.toml`。
- 当前 Android SDK 基线：compileSdk 37、targetSdk 36、minSdk 24。
- FFmpeg 能力来自 `app` 的第三方依赖 `libs.ffmpeg.kit.x6kb`。
- UI 使用 Jetpack Compose、Material 3、Navigation 3。
- 依赖注入使用 Koin。
- 网络层使用 Ktor 和 kotlinx.serialization。
- 本地数据使用 Room 3、DataStore、protobuf-lite。

## 开发规则

- 新增页面优先沿用现有 `Screen + ViewModel + Route/NavKey + UiState` 风格。
- 新增公共 UI 放 `core:ui`，新增跨层工具/事件放 `core:common`。
- 新增 API service、网络 model、签名/token 相关代码放 `core:network`。
- 新增 repository 或数据源编排放 `core:data`。
- 新增 Room entity/DAO/converter/migration 放 `core:database`，并维护 schema。
- 新增 DataStore 字段先改 `core:datastore-proto` 的 proto，再改 serializer/source/repository/UI。
- 下载链路改动要重点检查 `NewDownloadManager`、`DownloadExecutor`、`VideoInfoFetcher`、`FfmpegMerger`、`SubtitleDownloader`、`NamingConventionHandler` 及相关设置。
- 不要让 Compose UI 直接访问 API service、DAO 或文件下载执行器；通过 ViewModel/repository 暴露状态和动作。
- 统计、Firebase、百度、Google Play、隐私授权相关逻辑必须保留“未授权时不主动采集”的约束。

## 构建命令

优先使用仓库内 Gradle wrapper：

```bash
./gradlew :app:assembleAlphaDebug
```

常用验证：

```bash
./gradlew :app:compileAlphaDebugKotlin
./gradlew test
./gradlew lint
./gradlew :app:assembleAlphaRelease
```

日常开发、测试打包和快速验证优先使用 `alpha` flavor。`official` 是最终正式发行渠道，只有发行前再使用。

如果 release 构建需要签名或本地凭据，不要编造结果；说明本地缺失的配置。

## 配置与敏感信息

- 不要提交 `local.properties`、`.gradle/`、构建产物、签名文件、个人凭据。
- `app/google-services.json` 当前存在于仓库，改动前要确认是否确有必要。
- `gradle.properties` 中的 `enabledPlayAppMode`、`enabledAnalytics`、`as.github.*` 会影响构建和运行行为，修改后要说明影响范围。
- 不要把 keystore 密码、Play 上传凭据、API token 或个人账号 cookie 写入文档、源码或测试。

## 文档维护

- 文档入口在 `docs/README.md`。
- `docs/overview/`：存放项目背景、产品范围、合规边界、版本信息这类“先帮助读者理解项目是什么”的文档。
- `docs/architecture/`：存放应用架构、模块职责、依赖方向、导航、分层设计这类“解释工程如何组织”的文档。
- 涉及公共 Compose 组件、自定义控件选型、网络分层、请求封装、repository 编排方式时，优先补充到 `docs/architecture/`，不要只散落在代码注释里。
- `docs/development/`：存放本地开发、构建、运行、配置开关、渠道与发布流程这类“指导怎么开发和打包”的文档。
- `docs/testing/`：存放测试策略、验证命令、质量门槛、schema 校验这类“说明如何验证改动”的文档。
- 新增文档优先归入上述目录；只有形成稳定新主题时再新增一级目录，并先更新 `docs/README.md` 与本文件说明。
- 改架构、模块、构建开关、测试方式时，同步更新 `docs/`。
- 文档应写当前事实；如果是建议或计划，需要明确标注。
- 对用户回复保持短、具体、落到文件和命令。
