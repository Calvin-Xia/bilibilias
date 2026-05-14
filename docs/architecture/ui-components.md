# UI 组件与使用约定

本文档说明 BILIBILIAS 当前自定义 Compose 组件的分层、适用场景和选型方式。目标不是重复组件源码，而是帮助开发时快速判断“该用哪个组件”，并保持现有页面的一致交互。

## 总体原则

- 页面优先复用 `core/ui` 中的组件，避免在 `:app` 里重复包一层只服务单页的通用控件。
- 自定义组件不只是样式封装，还承载了触感反馈、宽屏适配、预览兼容、设置页排版等交互约束。
- 能用现有组件表达的场景，不直接落回原始 Material 3 组件；除非现有组件能力明显不够。
- `core/ui` 放跨页面可复用组件；`app/src/main/java/com/imcys/bilibilias/weight` 放更偏业务、下载、播放器、隐私、动画效果相关的应用级组件。

## 组件分层

当前仓库中的 UI 组件大致分两层：

- `core/ui/src/main/java/com/imcys/bilibilias/ui/weight`
  - 面向全局复用。
  - 典型内容：顶栏、按钮、卡片输入框、设置项、提示条、图片加载、切换按钮。
- `app/src/main/java/com/imcys/bilibilias/weight`
  - 面向具体业务页面或本应用专属交互。
  - 典型内容：下载任务卡片、播放器、分集选择、隐私弹窗、重排列表、存储环、番剧/用户信息展示。

新增组件时优先判断：

- 如果它脱离某个具体业务页面后仍然成立，放 `core/ui`。
- 如果它强依赖下载、解析、播放器、隐私授权、B 站业务语义，放 `:app`。

## 页面骨架组件

### `ASTopAppBar`

文件：`core/ui/src/main/java/com/imcys/bilibilias/ui/weight/ASTopAppBar.kt`

它是对 Material 3 `TopAppBar`、`LargeTopAppBar`、`CenterAlignedTopAppBar` 的统一封装，额外处理了：

- `Small / Large / CenterAligned` 三种样式切换。
- 基于 `rememberWidthSizeClass()` 的宽屏显示控制。
- `alwaysDisplay=false` 时在非紧凑宽度下可直接不展示顶栏。
- 顶栏内容统一通过 `contentPadding` 和 `windowInsets` 管理。

适合使用：

- 大多数有标题、返回按钮、操作菜单的标准页面。
- 需要随着屏幕宽度决定是否展示顶栏的双栏或大屏页面。
- 需要和 `Navigation 3` 页面状态保持一致的通用页面头部。

不建议使用：

- 仅局部区域需要一个小标题时，不要为了样式统一硬套顶栏。
- 完全沉浸式页面、播放器页面、可折叠头图页面，如果交互显著不同，可用业务组件单独实现。

选型建议：

- 普通设置页、列表页：优先 `Small`。
- 首页区块或强调层级较高页面：可用 `Large`。
- 对称操作、强调视觉居中的页面：用 `CenterAligned`。

### `AsBackIconButton`

用于返回行为统一的导航按钮。只要是标准“返回上一页”，优先配合 `ASTopAppBar` 使用，而不是手写 `IconButton`。

## 按钮与基础交互

### `ASIconButton`

文件：`core/ui/src/main/java/com/imcys/bilibilias/ui/weight/ASIconButton.kt`

它在 `IconButton` 外统一增加了点击触感反馈 `HapticFeedbackType.ContextClick`。

适合使用：

- 顶栏图标按钮。
- 卡片角标按钮。
- 颜色预览、删除、更多操作等轻量操作入口。

什么时候优先它而不是原生 `IconButton`：

- 只要你希望和现有页面保持一致的点击反馈，默认用它。
- 除非该按钮不能触发触感，或者你明确需要自定义更复杂的手势链路。

### `ASTextButton`

文件：`core/ui/src/main/java/com/imcys/bilibilias/ui/weight/ASTextButton.kt`

它是 `TextButton` 的统一封装，也内置了点击触感反馈。

适合使用：

- 对话框确认/取消。
- 页面上的轻量文字操作，例如“重试”“复制”“查看更多”。
- 不强调实体填充背景的次级操作。

不适合使用：

- 主要 CTA 按钮。如果需要显著主操作，优先评估是否应直接使用 Material 3 `Button` 或业务已有按钮样式。

### `ASCheckThumbSwitch` 与 `ASCheckBox`

文件：`core/ui/src/main/java/com/imcys/bilibilias/ui/weight/ASCheckThumbSwitch.kt`

`ASCheckThumbSwitch` 是设置页首选开关，统一了：

- 开关切换触感反馈。
- thumb 内的勾选/关闭图标。
- 与 `SettingsItem` 一起使用时的交互一致性。

适合使用：

- 设置页布尔开关。
- 需要明确“开/关”状态的单一功能切换。

不建议使用：

- 列表项里的临时筛选状态。如果是短时筛选，优先考虑 `FilterChip` / `ToggleButtonGroup` 一类更贴近筛选语义的组件。

## 弹窗与对话框

### `ASAlertDialog`

文件：`core/ui/src/main/java/com/imcys/bilibilias/ui/weight/ASAlertDialog.kt`

这是对 Material 3 `AlertDialog` 的轻封装，重点统一了：

- 用 `showState` 控制显示。
- `clickBlankDismiss` 控制点击空白是否可关闭。
- 提供与 `DialogSortBuilder` 配合的上下文版本，方便多个对话框排序与管理。

适合使用：

- 确认删除、确认退出、权限提示、风险确认等标准弹窗。
- 页面里有多个互斥对话框，需要统一弹出顺序时。

选型建议：

- 有破坏性操作时，`clickBlankDismiss` 建议关闭，避免误触。
- 只是信息确认、可随时撤销的操作，可以允许空白处关闭。

## 图片与媒体展示

### `ASAsyncImage`

文件：`core/ui/src/main/java/com/imcys/bilibilias/ui/weight/ASAsyncImage.kt`

它是基于 Coil 3 `AsyncImage` 的统一图片组件，额外处理了：

- 预览模式下不真正发起网络请求，改用 `Surface` 占位，避免 Compose Preview 报错。
- 可点击版本内置 `Surface(onClick=...)` 包裹。
- 默认背景色走 `primaryContainer`，图片未就绪时视觉更一致。

适合使用：

- 网络封面、头像、番剧海报、视频缩略图等。
- 需要在 Preview 中稳定显示占位而不是直接失败的图片区域。

选型建议：

- 纯展示图片：用无点击版本。
- 整张图可点进详情：用带 `onClick` 版本，不要外层再重复包一层点击容器。

注意事项：

- 如果只需要一张本地静态资源图标，不要为了统一强行用 `ASAsyncImage`，直接 `Icon` / `Image` 更合适。
- 图片裁剪、圆角、缩放方式优先通过参数传入，不要每个页面复制一套相同包装。

## 表单与输入

### `ASCardTextField`

文件：`core/ui/src/main/java/com/imcys/bilibilias/ui/weight/ASCardTextField.kt`

这是一个面向搜索/解析输入场景的卡片式单行输入框，已内置：

- 卡片式主色背景。
- 搜索图标前缀。
- 自动聚焦与延迟请求焦点。
- 焦点进入时把光标移动到文本末尾。
- 右侧清空按钮和清空触感反馈。
- 自定义 hint、只读、禁用、自动清焦点等能力。

适合使用：

- BV / AV / EP / SS 号输入。
- 搜索框、链接粘贴框、关键字输入框。

不适合使用：

- 多行文本编辑。
- 需要复杂校验、错误提示、前后缀说明的完整表单项，这类场景应评估是否另建更通用的表单组件。

## 设置页组件体系

文件：`core/ui/src/main/java/com/imcys/bilibilias/ui/weight/SettingsItem.kt`

这是当前项目最成体系的一组公共组件，推荐用于设置、偏好项、列表化配置页。

### 基础原则

- 设置页优先用 `BaseSettingsItem` 体系，不要每个页面自己手搓 `Row + Icon + Text + Switch`。
- 标题、说明文案、右侧控件、点击区域、图标尺寸已经在这里统一。
- 若只是“设置项 + 某个尾部控件”，通常在 `BaseSettingsItem` 上扩展即可，不必新造布局。

### 常用组件

- `SwitchSettingsItem`
  - 布尔开关首选。
  - 行点击和右侧开关共享同一交互源，避免点整行与点开关反馈不一致。
- `RadioSettingsItem`
  - 多选一配置项首选。
  - 适合弹出式单选列表、模式切换页。
- `SliderSettingsItem`
  - 数值区间配置首选。
  - 已内置当前值展示。
- `ColorSettingsItem`
  - 用于颜色配置或颜色预览入口。
- `CategorySettingsItem`
  - 设置页分组标题。
- `TipSettingsItem`
  - 设置页中的说明提示，不承担操作行为。
- `BannerItem`
  - 用于比普通设置项更强调的信息块或说明块。

### `BaseSettingsItem` 适用场景

适合：

- 左侧图标 + 中间标题/描述 + 右侧状态/控件 的标准行。
- 点击进入子页面。
- 行级点击配合右侧二次操作。

不适合：

- 高度不固定、内容非常复杂、含大量富文本或网格布局的块级内容。
- 完全不属于“设置项”的卡片型业务展示。

## 卡片与组合容器

### `SurfaceColorCard`

文件：`core/ui/src/main/java/com/imcys/bilibilias/ui/weight/ASCards.kt`

适合作为统一的表面色卡片容器。页面里需要标准卡片外观时优先用它，而不是重复写 `CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)`。

### `ASCardGroups`

文件：`core/ui/src/main/java/com/imcys/bilibilias/ui/weight/ASCardGroups.kt`

它用于把多个卡片块横向并列成一组，并自动处理首尾圆角差异。

适合使用：

- 两到三个并列展示的统计卡片、快捷入口卡片、能力卡片。
- 需要视觉上表现为“一组相关项”的横向块。

### 什么时候用卡片组，什么时候直接 `Row`

- 如果只是普通横向排布，没有组内统一背景和圆角关系，直接 `Row`。
- 如果要表达“这些卡片属于同一组”，优先 `ASCardGroups`。

## 提示与状态组件

当前 `core/ui` 里还有：

- `ASErrorTip`
- `ASInfoTip`
- `ASWarringTip`
- `Shimmer`

使用建议：

- 错误、说明、警告状态优先用对应 tip 组件，而不是各页散落自定义配色提示块。
- 骨架屏或占位加载优先复用 `shimmer` 体系，避免项目里出现多套不一致的加载动画。

## `app/weight` 里的应用级组件怎么用

`app/src/main/java/com/imcys/bilibilias/weight` 中组件更偏应用业务，不建议轻易当成全局规范，但下面几类已经形成事实标准：

- 下载相关：`DownloadTaskCard`
  - 下载列表、任务卡片展示优先复用。
- 播放相关：`ASPlayer`、`ASFramePlayer`
  - 媒体预览与逐帧工具场景优先复用。
- 分集与剧集信息：`ASEpisodeSelection`、`ASEpisodeTitle`
  - 番剧/视频分 P 选择页面优先复用。
- 页面加载与错误：`ASCommonLoadingScreen`、`AsAutoError`
  - 大面积加载或错误页优先统一展示方式。
- 页面结构：`ASCollapsingToolbar`、`ASPrimaryTabRow`
  - 有明确对应交互时再用，不要只因为名字看起来高级就套进去。

## 新增页面时的选型顺序

建议按这个顺序判断：

1. 先看 `core/ui` 有没有现成组件能直接表达需求。
2. 如果页面属于下载、播放器、分集、隐私等 BILIBILIAS 特有业务，再看 `app/weight`。
3. 如果只是差一点点，优先扩展现有组件参数，而不是复制一份近似组件。
4. 只有当现有组件确实无法覆盖，并且具备跨页复用价值时，才新增到 `core/ui`。

## 新增公共组件的约定

- 命名优先沿用 `AS*` 前缀。
- 能包进统一交互就包进去，例如点击触感、预览兼容、宽屏适配、主题色默认值。
- 参数设计优先暴露行为和插槽，不要让页面层传大量内部实现细节。
- 如果组件只是某一页专用，不要放进 `core/ui`。
- 新增后应补充到本文档，至少说明“它替代什么、什么时候用、什么时候别用”。
