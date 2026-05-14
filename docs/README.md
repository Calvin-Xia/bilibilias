# BILIBILIAS 文档

这里记录 BILIBILIAS 当前仓库的工程结构、开发方式和维护约定。文档组织参考了 Android 官方示例项目 [nowinandroid](https://github.com/android/nowinandroid) 的思路，但内容按本仓库现状编写。

## 文档索引

- `overview/`：产品范围、项目背景、合规边界、版本与基础信息。
  - [项目概览](./overview/project-overview.md)
- `architecture/`：应用架构、模块职责、依赖方向、分层和导航等工程设计说明。
  - [架构说明](./architecture/architecture.md)
  - [模块结构](./architecture/modularization.md)
  - [设置与 DataStore 设计](./architecture/settings-and-datastore.md)
  - [UI 组件与使用约定](./architecture/ui-components.md)
  - [网络请求与封装说明](./architecture/networking.md)
  - [视频解析链路设计](./architecture/video-analysis-design.md)
  - [数据库存储设计](./architecture/database-storage-design.md)
  - [下载链路设计](./architecture/download-design.md)
  - [媒体合并与 FFmpeg 设计](./architecture/media-merge-design.md)
- `development/`：本地开发、构建、运行、配置开关和发布相关说明。
  - [构建与运行](./development/build-and-run.md)
  - [构建矩阵与开关组合](./development/build-matrix.md)
- `testing/`：测试策略、验证命令、质量约定和 schema 校验。
  - [测试与质量](./testing/testing.md)

新增文档时，优先放进已有主题目录；只有当一组文档已经形成稳定主题且数量持续增长时，才新增一级子目录。

## 快速开始

```bash
./gradlew :app:assembleAlphaDebug
```

常用变体来自 `app/build.gradle.kts` 中的 `official`、`alpha`、`beta` 三个 product flavor，以及 `debug`、`release` 两个 build type。
日常开发、测试打包和快速验证优先使用 `alpha`；`official` 仅用于最终正式发行。

默认配置位于根目录 `gradle.properties`：

- `enabledPlayAppMode=false`
- `enabledAnalytics=true`
- `as.github.org=1250422131`
- `as.github.repository=bilibilias`

`alpha`、`beta`、`official` 的行为差异，以及 `enabledPlayAppMode` / `enabledAnalytics` 对不同变体的实际影响，见 [构建矩阵与开关组合](./development/build-matrix.md)。

## 维护原则

- 本项目已经停止公开发布 APK，源码仍可用于学习和自行编译。
- 下载、解析、漫游、统计、更新等能力涉及外部平台规则，改动时必须优先考虑合规、用户授权和失败降级。
- 当前仓库已经采用多 module、version catalog 和 build-logic 约定插件；新增工程能力优先放入 `build-logic` 或合适的 `core:*` module。
- 不要提交本地环境文件、签名文件或个人凭据。`local.properties`、`.gradle/`、构建产物等应保持未跟踪。
