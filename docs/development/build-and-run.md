# 构建与运行

## 环境要求

- Android Studio 或命令行 Android SDK。
- JDK 21 用于 Gradle daemon：由 `gradle/gradle-daemon-jvm.properties` 的 `toolchainVersion=21` 锁定，本机没有时 Gradle 会在首次构建自动下载（实测为 Adoptium 21.0.7），无需手动配置 `JAVA_HOME`。Android/Kotlin 编译目标为 Java 11。
- Android Gradle Plugin 9.2.0、Kotlin 2.3.0、Gradle wrapper 以仓库为准（wrapper 当前为 Gradle 9.4.1，`gradle-wrapper.properties` 中固定了 `distributionSha256Sum`）。
- Android SDK compileSdk 37：需要 `platforms;android-37.0`。本机缺失时 AGP 会在构建时自动下载（需 `android-sdk-license` 已接受），不需要 `sdkmanager`。
- 首次构建需要联网下载 Gradle 发行版、JDK 与全部 Maven 依赖，耗时较长。

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

`local.properties` 是本机 Android SDK 配置，不应提交（已在 `.gitignore` 中忽略）。需要指向本机 SDK 路径，否则构建会以 “SDK location not found” 失败：

```properties
sdk.dir=C:/Users/<用户名>/AppData/Local/Android/Sdk
```

也可以改用 `ANDROID_HOME` 环境变量指定，两者取其一即可。

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

- 支持 `arm64-v8a` 与 `x86_64`，两者同时由 `ndk.abiFilters`（debug）和 `splits`（release）约束。
- debug 构建禁用 ABI split，产出单个含上述两个 ABI 的 APK。
- release 构建启用 ABI split，产出每个 ABI 的拆分 APK，并额外生成 universal APK。
- 不再包含 `armeabi-v7a`：纯 32 位 ARM 设备（部分老机型与电视盒子）将无法安装。恢复支持需要同时改回 `app/build.gradle.kts` 的 `abiFilters` 与 `splits` 两处。

实际打包使用的 FFmpeg 来自 `app` 直接声明的 `ffmpeg-kit` 依赖；早期保留的 `core/ffmpeg` 模块与其 `ffmpegVerification` 插件已删除。

## 不要混跑 debug 与 release

`app/build.gradle.kts` 用 `isDebugBuild` 决定是否启用 ABI split，而它的判断依据是**整条 Gradle 命令里出现的任务名**：

```kotlin
val isDebugBuild = gradle.startParameter.taskNames.any { it.contains("debug", ignoreCase = true) }
```

因此下面这条命令会把 release 也当作 debug 处理，**静默关闭 ABI split**，只产出 1 个 APK 而不是 3 个：

```bash
# 不要这样写
./gradlew :app:assembleAlphaDebug :app:assembleAlphaRelease
```

需要 release 产物时单独执行：

```bash
./gradlew :app:assembleAlphaRelease
```

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
./gradlew :core:common:testAndroidHostTest :core:network:testAndroidHostTest \
  :core:database:testAndroidHostTest :shared:testAndroidHostTest \
  :app:testAlphaDebugUnitTest
```

`./gradlew test` 不覆盖 KMP 模块的用例（只有 `:app` 的会跑），`./gradlew check` 在 Windows 上会因 iOS klib 编译失败；原因与替代命令见 [测试与质量](../testing/testing.md)。

运行 Android lint：

```bash
./gradlew lint
```

只编译 Kotlin/Java 以快速暴露编译错误：

```bash
./gradlew :app:compileAlphaDebugKotlin
```

构建 iOS 可导入 `XCFramework`：

```bash
./gradlew :shared:assembleASSharedReleaseXCFramework
```

详细说明见 [iOS 导入产物](./ios-artifacts.md)。

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

`app` 使用 `bilibilias.baidu.jar` convention plugin 和 `baiduStatDependencies()`。

`BaiduJarDownloadConventionPlugin` 在配置阶段检查 `app/libs/Baidu_Mtj_android_4.0.11.0.jar`，缺失时从项目维护的地址自动下载，因此首次构建会在仓库内生成 `app/libs/`（该目录已在 `.gitignore` 中忽略）。下载失败时插件只打印错误、不中断配置；但 `enabledAnalytics=true` 时该 jar 是实现依赖，且 `BILIBILIASApplication`、`MainActivity` 直接引用 `com.baidu.mobstat.StatService`，失败会表现为编译错误——遇到 StatService 相关报错时先确认该 jar 是否下载成功。

当 `app/libs/Baidu_Mtj_android_*.jar` 存在且满足开关条件时会作为实现依赖，否则以 `compileOnly` 或不参与方式处理。不要手动复制不明来源 jar 到仓库。
