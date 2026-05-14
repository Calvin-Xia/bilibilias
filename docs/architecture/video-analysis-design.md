# 视频解析链路设计

本文档说明 BILIBILIAS 当前“输入一个 B 站链接或编号后，如何解析成可展示、可选择、可下载的数据”的完整链路。内容以 `AnalysisViewModel`、`VideoInfoRepository`、相关网络 service 和模型实现为准。

## 总览

当前视频解析不是单接口直出，而是分成几个阶段：

1. 用户输入文本
2. 正则识别输入类型
3. 根据类型选择解析入口
4. 请求基础详情
5. 根据内容类型补请求播放信息、互动信息、字幕信息等
6. 生成 `ASLinkResultType`
7. 生成默认下载配置 `DownloadViewInfo`
8. 页面再基于这些状态允许用户切换清晰度、音轨、分 P、分集和容器

解析链路最终服务两个目标：

- 展示内容详情和结构
- 生成后续下载链路所需的最小完备配置

## 解析入口

文件：`app/src/main/java/com/imcys/bilibilias/ui/analysis/AnalysisViewModel.kt`

页面输入通过：

- `updateInputAsText(inputAsText)`

进入 ViewModel。

### 当前处理方式

- 输入会先写入 `AnalysisUIState.inputAsText`
- 再通过 `debounceJob` 做 1 秒防抖
- 防抖后调用 `analysisInputText()`

这说明：

- 页面层只负责收集输入
- 真正解析由 ViewModel 串起
- 避免用户输入过程中频繁请求网络

## 输入类型识别

`analysisInputText()` 会先调用：

- `AsRegexUtil.parse(inputAsText)`

目前重点识别这些类型：

- `AV`
- `BV`
- `EP`
- `SS`
- B 站短链
- 用户空间

### 各类型分流

- `AV / BV`
  - 走 `handleBILIAVAndBV()`
- `EP`
  - 走 `handleDonghuaEp()`
- `SS`
  - 走 `handleDonghuaSeasonId()`
- 短链
  - 走 `handleShareData()`
- 用户空间
  - 走 `handleUserSpace()`

## 解析状态核心对象

### `AnalysisUIState`

它是页面总状态容器，里面最关键的是：

- `asLinkResultType`
- `downloadInfo`
- `analysisBaseInfo`
- 登录态
- 当前选择模式

### `ASLinkResultType`

它是“当前解析结果类型”的核心抽象，至少覆盖：

- `BILI.Video`
- `BILI.Donghua`
- `BILI.User`

它的作用不是简单区分页面，而是把不同解析结果挂上各自的原始详情响应，供 UI 和下载链路继续使用。

## 视频解析链路

### 1. 基础视频详情

`handleBILIAVAndBV()` 会先调用：

- `videoInfoRepository.getVideoView(aid, bvId)`

这是视频解析的基础入口。

这个阶段主要拿到：

- `bvid`
- `aid`
- 标题、封面、简介
- `cid`
- 分 P 列表
- 合集信息 `ugcSeason`
- 互动视频标记 `isSteinGate`
- 作者信息
- 是否充电专属等附加信息

### 2. 重定向处理

如果 `viewInfo.data.redirectUrl` 不为空：

- 会再次把重定向目标喂回 `analysisInputText()`

这说明解析链路已经内建了“跳转到真实内容”的能力，而不是要求 UI 自己判断。

### 3. 播放信息请求

基础详情成功后，ViewModel 会调用：

- `asVideoPlayerInfo(cid, bvid, aid)`

它内部再走：

- `videoInfoRepository.getVideoPlayerInfo(...)`

这个阶段的目标是拿到真正的可下载媒体信息，例如：

- DASH 视频轨
- DASH 音频轨
- DURL 单文件流
- 支持清晰度列表
- 多语言音轨信息

### 4. V2 播放信息

ViewModel 还会调用：

- `updatePlayerInfoV2(cid, bvId, aid)`

它走的是：

- `videoInfoRepository.getVideoPlayerInfoV2(...)`

这一路主要用于补更多播放附加信息，例如：

- 字幕信息
- 互动视频相关信息
- 更完整的播放结构

它并不是替代主播放信息，而是作为补充信息源，尤其服务字幕和互动视频场景。

### 5. 互动视频处理

若视频详情中：

- `rights.isSteinGate != 0`

则会继续走：

- `handleInteractiveVideo(...)`

流程是：

1. 先请求 `getVideoPlayerInfoV2()` 获取 `graphVersion`
2. 再调用 `getSteinEdgeInfoV2(...)`
3. 把结果写入 `_interactiveVideo`

这说明互动视频解析依赖两步：

- 基础视频播放信息
- 互动图结构信息

不是单接口完成。

## 番剧解析链路

### `EP`

`handleDonghuaEp(epId)` 会并发做两件事：

- 直接请求播放信息 `asDonghuaPlayerInfo(null, epId)`
- 请求番剧详情 `getDonghuaSeasonViewInfo(epId = epId)`

这样做的好处是：

- 播放信息能更早回来，尽快生成默认下载配置
- 番剧结构信息随后补齐页面内容

### `SS`

`handleDonghuaSeasonId(seasonId)` 会先请求：

- `getDonghuaSeasonViewInfo(seasonId = seasonId)`

再从返回结果里拿默认剧集：

- 优先 `episodes.firstOrNull()?.epId`
- 如果没有，再看 `section`

然后再调用：

- `asDonghuaPlayerInfo(viewInfo, defaultEpId)`

### 番剧播放信息

`asDonghuaPlayerInfo()` 内部走：

- `videoInfoRepository.getDonghuaPlayerInfo(epId, seasonId)`

它会拿到统一化后的 `BILIDonghuaPlayerSynthesize`，从而屏蔽 Web / TV / OGV 播放信息差异。

## 用户空间解析

`handleUserSpace(text)` 直接走：

- `userInfoRepository.getUserPageInfo(mid)`

再把结果包装成：

- `ASLinkResultType.BILI.User`

用户空间解析和视频/番剧解析不同，它主要服务展示和入口跳转，不直接进入下载媒体链路。

## 短链解析

`handleShareData(url)` 会调用：

- `videoInfoRepository.shortLink(url)`

成功后再把解析出的真实链接重新喂回：

- `analysisInputText(realUrl)`

因此短链处理本质上是“先解短链，再进入正常解析链路”。

## Repository 在解析链路里的职责

### `VideoInfoRepository`

它是解析层最重要的 repository，负责：

- 获取基础视频详情
- 获取番剧详情
- 获取视频/番剧播放信息
- 获取 V2 播放信息
- 在 Web / TV 解析平台之间分流
- 在 TV 模式下补齐 `aid/cid/accessToken`
- 对返回音轨做整理，例如把杜比和 Hi-Res 插入音轨列表

这说明：

- ViewModel 不需要知道当前到底走 Web 还是 TV
- 平台差异和回退策略集中在 repository

### `UserInfoRepository`

主要负责用户空间类信息解析。

## 默认下载配置生成

解析成功后，ViewModel 不会等用户自己慢慢选，而是会主动生成默认下载配置。

### 核心函数

- `getDefaultDownloadInfoConfig(...)`

它会从播放信息中推导：

- 默认视频清晰度
- 默认视频编码
- 默认音频质量

### DASH 与 DURL 差异

- 若有 DASH
  - 从 `supportFormats` 和 `dash.video/audio` 里找匹配项
- 若是 DURL
  - 从 `durls` 里回退支持项

### 生成 `DownloadViewInfo`

然后通过：

- `DownloadViewInfo.updateForMediaType(...)`

把这些默认值写回 `AnalysisUIState.downloadInfo`，同时：

- 视频类型默认补当前 `cid`
- 番剧类型默认补当前 `epId`
- 清掉不属于当前类型的旧选择

## 登录态对解析的影响

`AnalysisViewModel` 在初始化时会监听：

- `usersDataSource.users`

登录状态变化时，如果当前输入不为空，会自动重新解析。

这是因为：

- 登录前后可见清晰度、会员内容、试看能力、音轨和番剧可访问性可能变化

所以解析结果不是完全静态的，而是和当前账号状态有关。

## 用户选择与二次解析

解析完成后，用户还可以继续影响结果：

- 切换分 P / 分集
- 选择清晰度
- 选择编码
- 选择音质
- 选择音轨语言
- 切换下载模式
- 切换容器

其中切换分 P / 分集时，会再次触发对应播放信息请求，例如：

- `updateSelectedPlayerInfo(...)`

这样做的原因是：

- 不同 `cid/epId` 对应的播放信息和字幕不一定相同

## 解析链路中的几个重要取舍

### 1. 基础详情和播放信息分开

优点：

- 结构信息和媒体信息职责清晰
- 可先展示页面，再逐步补齐下载能力

### 2. V2 播放信息作为补充源

优点：

- 不破坏现有主播放链路
- 需要字幕、互动信息时再补充

### 3. 解析完成即生成默认下载配置

优点：

- 用户无需每次从零选择
- 页面初始状态更完整

### 4. 登录态变化自动重解析

优点：

- 会员、音轨、试看、番剧权限切换更及时

## 改动视频解析时重点检查

- `AnalysisViewModel`
  - 输入类型分流是否正确
- `VideoInfoRepository`
  - 平台分流和回退是否被破坏
- `getDefaultDownloadInfoConfig()`
  - 默认清晰度和编码选择是否仍正确
- `updatePlayerInfoV2()`
  - 字幕和互动视频信息是否还能补齐
- 登录态变化后是否会自动重解析

## 建议验证场景

- BV / AV 普通视频
- 短链跳转解析
- 合集视频
- 互动视频
- EP 番剧
- SS 番剧季度
- 登录前后同一视频解析结果变化
- 多语言音轨视频
- 充电 / 会员视频的降级或限制表现
