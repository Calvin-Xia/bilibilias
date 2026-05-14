# 构建与运行

## 环境要求

- Android Studio 或命令行 Android SDK。
- JDK 17 用于 Gradle/build-logic；Android/Kotlin 编译目标为 Java 11。
- Android Gradle Plugin 9.2.0、Kotlin 2.3.0、Gradle wrapper 以仓库为准。
- Android SDK compileSdk 37。

优先使用仓库内 wrapper：

```bash
./gradlew --version
```

## 本地配置

根目录 `gradle.properties` 包含默认构建开关：

```properties
org.gradle.parallel=true
org.gradle.configureondemand=false
org.gradle.caching=true
org.gradle.configuration-cache=true
org.gradle.configuration-cache.problems=warn
enabledPlayAppMode=false
enabledAnalytics=true
as.baidu.stat.id=0f9d51ff82
as.github.org=1250422131
as.github.repository=bilibilias
```

这些值会影响 app 的 BuildConfig、依赖启用方式和运行时行为：

- `org.gradle.*` 开关用于启用并行执行、构建缓存和配置缓存；`configuration-cache.problems=warn` 用于兼容部分第三方 Gradle 插件任务。
- `enabledPlayAppMode=true` 时启用 Google Play app-update/review 相关依赖和逻辑。
- `enabledAnalytics=true` 时启用 Firebase/百度统计相关能力；未启用时相关依赖多以 `compileOnly` 参与编译。
- `as.github.*` 用于应用内版本检查。
- FFmpeg 运行能力来自 `app` 中的第三方依赖 `libs.ffmpeg.kit.x6kb`。

但要注意：这些属性不是所有 flavor 都原样继承。比如 `alpha` flavor 会把 `ENABLED_PLAY_APP_MODE` 固定写成 `false`，因此仅修改 `gradle.properties` 并不会让 `alpha` 变体进入 Play 模式。详细组合规则见 [构建矩阵与开关组合](./build-matrix.md)。

`local.properties` 是本机 Android SDK 配置，不应提交。

## 构建变体

Product flavor：

- `official`：正式渠道，`app_channel=Official`。
- `alpha`：带 applicationId/versionName suffix，本地默认使用 debug signing，CI 可读取 `RUNNER_TEMP` 下的 `mxjs-debug.jks`。
- `beta`：Google Play 提交用途，`app_channel=Beta`。

日常开发、测试打包和本地验证优先使用 `alpha`。`official` 是最终正式发行渠道，不作为默认测试变体。

Build type：

- `debug`：用于本地调试。
- `release`：开启 R8 minify 和 resource shrink，并按 ABI split 输出。

ABI：

- 默认支持 `armeabi-v7a`、`arm64-v8a`、`x86_64`。
- debug 构建禁用 ABI split。
- release 构建启用 ABI split，并生成 universal APK。

当前 `core/ffmpeg` 目录没有参与构建；实际打包使用的是 `app` 中直接声明的 `ffmpeg-kit` 依赖。

## 常用命令

构建 debug APK：

```bash
./gradlew :app:assembleAlphaDebug
```

构建测试 release APK：

```bash
./gradlew :app:assembleAlphaRelease
```

最终正式发行前再构建 official release：

```bash
./gradlew :app:assembleOfficialRelease
```

运行单元测试：

```bash
./gradlew test
```

运行 Android lint：

```bash
./gradlew lint
```

只编译 Kotlin/Java 以快速暴露编译错误：

```bash
./gradlew :app:compileAlphaDebugKotlin
```

## 签名

`official` 和 `beta` flavor 使用名为 `BILIBILIASSigningConfig` 的 signing config，但仓库内没有硬编码 keystore。release 签名需要由本地或 CI 注入。

不要把 keystore、密码、个人证书、Play 上传凭据提交到仓库。

## Firebase 与 Google Services

仓库当前包含 `app/google-services.json`，并应用了：

- `com.google.gms.google-services`
- `com.google.firebase.crashlytics`
- `com.google.firebase.firebase-perf`

改动 Firebase、统计或隐私授权相关逻辑时，必须确认未同意隐私政策时不会主动采集或上传不该上传的数据。

## 百度统计 jar

`app` 使用 `bilibilias.baidu.jar` convention plugin 和 `baiduStatDependencies()`。当 `app/libs/Baidu_Mtj_android_*.jar` 存在且满足开关条件时会作为实现依赖，否则以 `compileOnly` 或不参与方式处理。不要手动复制不明来源 jar 到仓库。
