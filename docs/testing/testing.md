# 测试与质量

## 测试源集与运行方式

KMP 模块（`:core:common`、`:core:network`、`:core:database`、`:shared`）通过 AGP 的 KMP host test 支持运行单元测试，源码放在各模块的 `src/commonTest/`，断言使用 `kotlin("test")`。该编译的 sourceSet tree 包含 `commonTest`，因此 `commonTest` 中的测试会自动纳入 Android host test，无需为测试单独添加 `jvm()` target。

模块构建脚本中的启用方式：

```kotlin
kotlin {
    android {
        namespace = "..."
        withHostTest {}
    }
    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
```

对应任务为 `:core:common:testAndroidHostTest`、`:core:network:testAndroidHostTest`、`:core:database:testAndroidHostTest`、`:shared:testAndroidHostTest`。

需要 `runTest` 时（例如 DAO 是 suspend 方法）在 `commonTest` 追加 `implementation(libs.kotlinx.coroutines.test)`。

`:app` 是纯 Android 模块，测试放在 `src/test/`，通过 convention plugin 已具备 JUnit4、kotlinx-coroutines-test、Turbine、Truth 依赖。

**限制**：`commonTest` 也要为 iOS 目标编译，因此 Android Keystore 相关代码不能放进 `commonTest`。凭据加解密的测试通过 `FakeCredentialCipher` 在 commonTest 中覆盖算法与边界，Keystore 实现本身不做 host test。

## 当前测试基线

新增测试前先了解现状，避免重复覆盖或误判空白：

| 测试 | 模块 | 用例数 | 覆盖内容 |
|---|---|---:|---|
| `BiliAppSignerTest` | `core:network` | 5 | APP 签名（appkey 注入、参数排序无关性、MD5 摘要）、TV 设备指纹字段 |
| `WebiTokenUtilsTest` | `core:network` | 3 | WBI mixinKey 生成（64 位置换表）、超短 mixKey 返回 null |
| `AsRegexUtilTest` | `core:common` | 8 | BV/AV/EP/SS/短链/用户空间识别与优先级 |
| `NumberUtilsTest` | `core:common` | 5 | 万/亿格式化与截断（非四舍五入） |
| `NamingConventionHandlerTest` | `shared` | 9 | 占位符替换、`/` 替换、下划线折叠、扩展名去重、兜底分支 |
| `EncryptedBILIUsersDaoTest` | `core:database` | 7 | token 写入加密、读取解密、null 保持、密文不可解时按缺失处理 |
| `EncryptedBILIUserCookiesDaoTest` | `core:database` | 4 | Cookie 值加解密、加解密不波及 name/domain/path、不可解时剔除 |
| `LoginPlatformConverterTest` | `core:database` | 4 | 非法值与 null 回退默认平台 |
| `CookieEncodingConverterTest` | `core:database` | 3 | 非法值与 null 回退默认编码 |
| `DownloadConvertersFallbackTest` | `core:database` | 8 | 7 个下载枚举 + 容器的非法值/未知扩展名回退 |
| `DatabaseModuleBindingTest` | `core:database` | 2 | DAO 可按接口类型解析、解析结果确实是加密装饰器 |
| `FairMemoryReceiverTest` | `app` | 3 | 厂商内存回收广播动作解析 |
| `NavRouteSerializationNameTest` | `shared` | 3 | Route 的序列化名保持旧类名（旧导航栈存档可恢复） |

合计 64 个用例。下载链路、Room migration、`core/ui`、网络 adapter 的错误分支目前**没有测试**——补测试前请参阅 [已知问题与技术债](../architecture/known-issues.md) 的 C3 节。

## 推荐验证命令

快速编译：

```bash
./gradlew :app:compileAlphaDebugKotlin
```

**注意 `./gradlew test` 并不覆盖 KMP 模块的测试**：`:core:common`、`:core:network`、`:core:database`、`:shared` 的用例注册在 host test（`testAndroidHostTest`）上，只有 `:app` 的用例会随根 `test` 任务运行。跑测试请显式列出任务：

```bash
./gradlew :core:common:testAndroidHostTest \
  :core:network:testAndroidHostTest \
  :core:database:testAndroidHostTest \
  :shared:testAndroidHostTest \
  :app:testAlphaDebugUnitTest
```

**也不要直接依赖 `./gradlew check`**：它会连带拉起 iOS 目标的 klib 编译（Kotlin 2.x 允许在非 macOS 主机上交叉编译 Apple 目标 klib），而 `core:database` 的 `BILIBILIASDatabaseConstructor` 的 iOS actual 由 Room KSP 生成、在 Windows 上不产出，因此 `check` 在 Windows 上必然以该错误失败——这是我们当前的已知环境问题，与改动本身无关，详见 [已知问题与技术债](../architecture/known-issues.md) 的 V2 节。

只跑某个模块的 host test（更快）：

```bash
./gradlew :core:network:testAndroidHostTest
./gradlew :shared:testAndroidHostTest
./gradlew :core:database:testAndroidHostTest
```

Lint：

```bash
./gradlew lint
```

构建 APK：

```bash
./gradlew :app:assembleAlphaDebug
```

涉及 release、R8、资源裁剪、ABI split 或签名的改动，至少补跑：

```bash
./gradlew :app:assembleAlphaRelease
```

**release 必须单独执行**：`app/build.gradle.kts` 的 `isDebugBuild` 会扫描同一命令里的所有任务名，若与 debug 任务混跑，release 的 ABI split 会被静默关闭（3 个 APK 变成 1 个）。

`official` 只在最终正式发行前使用，不作为日常测试打包的默认 flavor。

## CI 覆盖

`.github/workflows/build-apk-alpha.yml` 在构建 APK 前会先运行上述 5 个测试任务，测试失败即中止构建。CI 目前**不运行 lint**；本地收尾的门禁是那组测试任务 + `./gradlew lint`（`check` 因 iOS klib 问题不可用）。

## 适合优先补测试的区域

- 链接解析、BV/AV/EP/SS 输入识别、deep link 分发。
- B 站 API response serializer、token、cookie 处理。
- 下载任务状态机、命名规则、字幕转换、失败恢复。
- Room migration、converter、DAO 查询。
- DataStore serializer 和默认值迁移。
- ViewModel 中的 Flow 状态转换、分页、错误提示。
- 隐私授权前后的统计开关行为。

改动以下位置时请优先补测试，它们属于已知的高风险区：

- `app/src/main/java/com/imcys/bilibilias/download/`：并发与状态机逻辑复杂，纯逻辑部分需先做依赖抽象才可测。
- `core/database/.../Migration.kt`：迁移错误会直接导致升级用户崩溃。
- `core/network/.../WebiTokenUtils.kt`、`BiliAppSigner.kt`：已有基线测试，改算法时先跑现有用例。

## Compose UI 验证

新增页面或公共组件时：

- 复用 `core:ui` 现有组件和主题 token。
- 确认 edge-to-edge、navigation bar padding、暗色模式和动态色。
- 对长文本、多语言字符串和空状态做预览或手动验证。
- 不要在 UI 里直接调用网络或数据库；通过 ViewModel/repository 暴露状态。

## 数据库 schema

`core/database/schemas` 已保存 Room schema。改动 entity、DAO、database version 或 migration 时，必须同步生成并检查 schema JSON。

推荐验证：

```bash
./gradlew :core:database:kspDebugKotlin
```

如 Gradle task 名称因 AGP/KSP 变化不可用，以 `./gradlew :core:database:tasks` 查询当前实际任务。

## 质量约定

- 小改动至少跑相关 module 的编译或测试。
- 下载、数据库、网络、隐私和 release 构建相关改动要扩大验证范围。
- 涉及 `gradle.properties`、flavor、Firebase、百度统计、Google Play 依赖切换时，至少对照一次 [构建矩阵与开关组合](../development/build-matrix.md)，避免只在单一变体验证。
- 若某项验证因本地 SDK、签名或网络不可用无法执行，应在提交说明或回复中写明原因。
