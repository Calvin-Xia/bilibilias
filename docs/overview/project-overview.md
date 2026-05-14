# 项目概览

BILIBILIAS 是一款第三方 B 站视频缓存工具，面向离线播放、学习资料缓存和原创视频剪辑素材整理等场景。项目 README 已明确：本项目自 2026 年 1 月 28 日起停止公开发布 APK，开发者可继续使用源码自行编译。

## 功能范围

当前 README 和代码结构体现的主要能力包括：

- 登录：扫码登录、Cookie 登录。
- 解析：普通视频、番剧影视、充电视频、互动视频，支持 AV、BV、EP、SS 等链接。
- 入口：分享文本到 App、内置网页解析、投稿/收藏夹/历史/点赞/追番列表解析。
- 下载：内置下载器、字幕下载、命名规则、媒体合并和下载任务管理。
- 工具：视频逐帧导出、番剧日历、导出、平台解析设置、存储管理等。
- 扩展：Google Play 更新/评价、Firebase、百度统计、Shizuku、Android App Functions、B 站漫游相关能力。

## 合规边界

项目 README 中的立场是仓库维护的硬约束：

- 缓存内容不得直接二次传播，仅允许在自己的终端设备播放或用于合理剪辑。
- 不得用于商业或非法用途。
- BILIBILIAS 未得到哔哩哔哩官方许可，使用者需自行遵守 B 站规则和政策。
- 因项目已停止公开分发，后续贡献不应引入“鼓励公开分发 APK”的文案、流程或自动化。

## 技术栈

- Kotlin、Jetpack Compose、Material 3。
- AndroidX Navigation 3，使用可序列化 back stack 做恢复。
- Koin 作为依赖注入框架。
- Ktor + kotlinx.serialization 作为网络层基础。
- Room 3、DataStore、protobuf-lite 作为本地数据基础。
- Gradle Kotlin DSL、version catalog、includeBuild `build-logic` 约定插件。
- Firebase、百度统计、Google Play 相关库通过构建开关和 flavor 组合启用或降级为 `compileOnly`。

## 包名与版本

- applicationId：`com.imcys.bilibilias`
- namespace：`com.imcys.bilibilias`
- 当前 `versionName`：`3.1.6`
- 当前 `versionCode`：`316`
- compileSdk：`37`
- targetSdk：`36`
- minSdk：`24`
