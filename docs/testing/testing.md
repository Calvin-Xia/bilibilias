# 测试与质量

当前仓库已经通过 convention plugin 为 Android application module 添加了常用测试依赖：

- JUnit4
- kotlinx-coroutines-test
- Turbine
- Truth
- AndroidX Test core / runner / rules / ext-junit
- Espresso
- Compose UI test

现有测试文件较少，新增功能时应按风险补测试，而不是只依赖手动点击。

## 推荐验证命令

快速编译：

```bash
./gradlew :app:compileAlphaDebugKotlin
```

单元测试：

```bash
./gradlew test
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

`official` 只在最终正式发行前使用，不作为日常测试打包的默认 flavor。

## 适合优先补测试的区域

- 链接解析、BV/AV/EP/SS 输入识别、deep link 分发。
- B 站 API response serializer、签名、token、cookie 处理。
- 下载任务状态机、命名规则、字幕转换、失败恢复。
- Room migration、converter、DAO 查询。
- DataStore serializer 和默认值迁移。
- ViewModel 中的 Flow 状态转换、分页、错误提示。
- 隐私授权前后的统计开关行为。

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
