# 构建矩阵与开关组合

本文档说明 BILIBILIAS 当前 flavor、build type 与 `gradle.properties` 开关的实际组合规则。目标是避免只看单个配置文件就误判最终产物行为。

## 总览

当前构建结果主要由三层共同决定：

1. `gradle.properties`
2. `app/build.gradle.kts` 中的 `productFlavors`
3. `app/build.gradle.kts` 中的 `buildTypes`

其中 `enabledPlayAppMode`、`enabledAnalytics` 虽然来自 `gradle.properties`，但并不是对所有 flavor 都直接生效。

## Flavor 定位

### `alpha`

- 日常开发、快速验证、测试打包首选
- 带 `applicationIdSuffix` 和 `versionNameSuffix`
- 本地默认使用 debug signing
- 在 CI 中可读取 `RUNNER_TEMP/mxjs-debug.jks`
- `ENABLED_PLAY_APP_MODE` 被 flavor 固定写成 `false`

结论：

即使 `gradle.properties` 把 `enabledPlayAppMode` 改成 `true`，`alpha` 仍不会进入 Play 模式。

### `beta`

- Google Play 提交用途
- 带 `applicationIdSuffix` 和 `versionNameSuffix`
- 使用 `BILIBILIASSigningConfig`
- `ENABLED_PLAY_APP_MODE` 读取 `gradle.properties` 中的 `enabledPlayAppMode`

### `official`

- 最终正式发行渠道
- 使用正式 `applicationId`
- 使用 `BILIBILIASSigningConfig`
- `ENABLED_PLAY_APP_MODE` 读取 `gradle.properties` 中的 `enabledPlayAppMode`

## Build Type 行为

### `debug`

- 用于本地调试
- 写入 `ENABLED_PLAY_APP_MODE`
- 写入 `ENABLED_ANALYTICS`
- ABI split 关闭

### `release`

- 开启 R8 压缩
- 开启资源裁剪
- 写入 `ENABLED_PLAY_APP_MODE`
- 写入 `ENABLED_ANALYTICS`
- ABI split 开启
- 生成 universal APK

## 开关影响范围

### `enabledPlayAppMode`

影响：

- Google Play 更新/评价相关依赖是 `implementation` 还是 `compileOnly`
- `BuildConfig.ENABLED_PLAY_APP_MODE`
- 运行时是否走 Google Play 相关逻辑
- 百度统计逻辑是否可用

额外注意：

- `alpha` flavor 会强制关闭 Play 模式
- `official` / `beta` 才会真实读取该属性

### `enabledAnalytics`

影响：

- Firebase Crashlytics / Analytics / Config / Messaging / Perf / In-App Messaging 是 `implementation` 还是 `compileOnly`
- `BuildConfig.ENABLED_ANALYTICS`
- 百度统计 jar 是否作为真实实现依赖参与

但运行时是否真的采集，还要再受用户隐私同意控制，不能只看依赖是否被打进包内。

### `as.github.org` / `as.github.repository`

影响：

- 应用内版本检查所访问的 GitHub 仓库信息

### `as.baidu.stat.id`

影响：

- Manifest placeholder
- `BuildConfig.BAIDU_STAT_ID`

## 常见组合

### 日常开发

推荐：

```bash
./gradlew :app:assembleAlphaDebug
```

特点：

- Play 模式固定关闭
- 适合大多数页面、下载、解析、导航和设置变更

### 本地验证 release 行为

推荐：

```bash
./gradlew :app:assembleAlphaRelease
```

特点：

- 仍是 `alpha` 渠道
- 可以提前验证 R8、资源裁剪、ABI split

### 发布前核验

推荐：

```bash
./gradlew :app:assembleOfficialRelease
```

或根据用途：

```bash
./gradlew :app:assembleBetaRelease
```

这类构建通常还会受签名与本地/CI 凭据影响。

## 依赖启停规则

### Google Play 依赖

`googlePlayDependencies(enabled)` 的行为是：

- `enabled=true` 时使用 `implementation`
- `enabled=false` 时使用 `compileOnly`

因此“代码能编译”不等于“对应能力在该变体里一定可运行”。

### Firebase 依赖

`firebaseDependencies(enabled)` 的行为是：

- `enabled=true` 时使用 `implementation`
- `enabled=false` 时使用 `compileOnly`

同时运行时还要满足：

- `BuildConfig.ENABLED_ANALYTICS=true`
- 用户已同意隐私政策

### 百度统计

百度统计除构建开关外，还要求：

- `app/libs/Baidu_Mtj_android_*.jar` 实际存在
- 非 Play 模式
- 用户已同意隐私政策

## 签名与本地限制

- `official` / `beta` 使用 `BILIBILIASSigningConfig`
- 仓库未内置 keystore 与密码
- `alpha` 本地默认回落到 debug signing
- `alpha` CI 可通过 `RUNNER_TEMP` 提供单独签名文件

因此如果某个 release 构建因签名失败，不要在文档或回复里假定“构建成功”，应直接说明缺少本地签名条件。

## 文档维护约定

以下改动应同步更新本文档：

- flavor / build type 调整
- `enabledPlayAppMode` / `enabledAnalytics` 行为变化
- Google Play / Firebase / 百度统计依赖启停逻辑变化
- ABI split、签名、发布流程变化
