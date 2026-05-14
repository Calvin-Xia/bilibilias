# 网络请求与封装说明

本文档按当前仓库实现说明 BILIBILIAS 的网络分层、请求链路、Ktor 封装方式，以及 repository 如何消费网络数据。内容以 `core:network`、`core:data` 和相关数据库/DataStore 代码为准。

## 总览

当前网络层不是“一个 `HttpClient` 打天下”，而是三层结构：

- `service`
  - 面向接口域的 API 服务类，例如 `BILIBILIWebAPIService`、`BILIBILITVAPIService`、`BgmAPIService`、`GithubAPIService`。
- `adapter`
  - 面向“返回体不符合 B 站标准 `BiliApiResponse` 结构”的接口，把第三方响应适配进统一请求结果模型。
- `plugin`
  - 面向跨请求能力的 Ktor 插件，例如 Referer/UA 修正、漫游域名替换、风险事件监听、Firebase 性能上报。
- `repository`
  - 面向 UI 或业务流程，把网络结果和数据库、DataStore、登录状态、平台选择组合起来。

请求的推荐调用方向：

`ViewModel/UI -> Repository -> API Service -> HttpClient -> Plugin/Serializer/Cookies -> 远端接口`

不要跳层：

- UI 不直接调 `APIService`。
- ViewModel 不直接拼签名参数。
- 页面不直接读 Cookie DAO 拼请求。

## Module 职责

### `core:network`

负责：

- `HttpClient` 构建。
- Ktor 插件安装与配置。
- API Service 定义。
- 网络模型 `model/*`。
- 特殊序列化处理。
- B 站 WebI / App 签名、Cookie、Referer、UA 等请求细节。

### `core:data`

负责：

- Repository。
- 不同接口结果的组合与转换。
- Web / TV / Mobile 平台分流。
- 将网络模型映射成 UI 更容易消费的数据模型。

### `core:database` 与 `core:datastore`

在网络链路里主要承担：

- 登录用户、Token、Cookie 的持久化。
- 解析平台、漫游、Buvid 等网络行为开关。
- 风险控制、账号关联等本地状态支撑。

## `HttpClient` 分层

文件：`core/network/src/main/java/com/imcys/bilibilias/network/di/KtorDI.kt`

当前 Koin 中注册了三类 `HttpClient`。

### 1. 默认业务 `HttpClient`

这是大部分 B 站业务 API 用的客户端，特征包括：

- 引擎：`CIO`
- 默认超时：`10s`
- 安装 `SSE`
- 安装业务插件：
  - `AutoBILIInfoPlugin`
  - `RoamPlugin`
  - `RiskControlPlugin`
  - `FirebasePerfPlugin`
- 安装：
  - `ContentNegotiation(json + protobuf)`
  - `HttpRequestRetry`
  - `HttpCookies`
- 使用 `AsCookiesStorage`

适合：

- Web API
- TV API
- 需要走 Cookie、签名、风控、漫游、性能埋点的业务请求

### 2. `DownloadHttpClient`

Koin qualifier：`named("DownloadHttpClient")`

这是专门给下载链路准备的“纯净”客户端，特点：

- 不安装风控、漫游、Firebase 等业务插件。
- 超时时间更长：`60s`
- 仍保留 Cookie 与基础请求重试。
- 安装 `AutoBILIInfoPlugin`，确保 UA/Referer 行为与当前解析平台一致。

适合：

- 视频流、音频流、字幕、封面等资源下载。

为什么不能直接复用默认业务 `HttpClient`：

- 下载请求数量多、持续时间长。
- 不希望业务插件影响下载稳定性。
- 下载接口的失败重试和头信息策略通常不同于普通 JSON API。

### 3. `CommonApiHttpClient`

Koin qualifier：`named("CommonApiHttpClient")`

这是给通用第三方接口准备的客户端，特点：

- 不装业务插件。
- 不依赖 Cookie。
- 超时较长：`120s`
- 只装 JSON 解析和基础重试。

适合：

- Bangumi API
- Github API
- 其他不需要 B 站业务上下文的通用请求

## Ktor 插件职责

### `AutoBILIInfoPlugin`

文件：`core/network/src/main/java/com/imcys/bilibilias/network/plugin/AutoBILIInfoPlugin.kt`

职责：

- 根据 `AppSettings.videoParsePlatform` 自动调整 `Referer` 和 `User-Agent`。
- 在 TV 解析模式下，对 `bilivideo`、`edge`、TV 二维码、TV 播放地址等白名单请求移除 `Referer` 与 `User-Agent`。
- 对未显式设置 `Referer` 的请求补默认 `https://www.bilibili.com`。

它解决的问题：

- 不同平台接口对头信息要求不同。
- 页面层不需要每次手动判断“这个请求要不要带 Referer/UA”。

开发约定：

- 新增 B 站 API 时，除非有非常明确的例外，不要在页面或 repository 层重复补默认 `Referer`。
- 如果某个接口需要特殊头策略，优先评估是扩展插件白名单，还是在 service 层针对该接口单独设置。

### `RoamPlugin`

职责：

- 在配置启用时替换域名，例如把 `api.bilibili.com` 替换为漫游域名。
- 依赖本地用户/设置数据决定是否生效。

开发约定：

- 新增直接写死完整 URL 的 B 站请求时，要确认不会绕过漫游插件的域名替换逻辑。

### `RiskControlPlugin`

文件：`core/network/src/main/java/com/imcys/bilibilias/network/plugin/RiskControlPlugin.kt`

职责：

- 读取响应体文本。
- 检测是否包含 `v_voucher`。
- 对播放相关白名单接口命中后，发送 `sendPlayVoucherErrorEvent()`。

它不是通用错误解析器，而是“特定业务风控信号监听器”。

开发约定：

- 新增播放器相关接口时，如果也可能返回同类风控标记，要考虑是否加入白名单。
- 这类跨层事件不要散落在页面里用字符串判断，优先集中在插件或统一扩展里。

### `FirebasePerfPlugin`

职责：

- 为请求接入 Firebase 网络性能追踪。

注意：

- 统计与性能采集仍受用户授权和构建配置约束，不能绕过隐私开关。

## Cookie 存储与登录状态

文件：`core/network/src/main/java/com/imcys/bilibilias/network/AsCookiesStorage.kt`

`AsCookiesStorage` 是项目的自定义 `CookiesStorage`，主要行为：

- 启动后从数据库同步当前登录用户的 Cookie 到内存。
- 请求时优先使用内存 Cookie，避免每次读库。
- 根据 `UsersDataSource` 判断是否登录。
- 当用户设置 `notUseBuvid3` 时，请求阶段过滤 `buvid3`。
- 支持清空、更新、读取指定 Cookie 值。

这意味着：

- 网络层的 Cookie 真正来源不是一次性登录结果，而是数据库 + 当前用户状态。
- 登录切换、登出、Cookie 更新时，不仅要写库，也要考虑内存 Cookie 是否需要同步。

开发约定：

- 不要在 service 层手写 `Cookie` 请求头来绕开 `HttpCookies`。
- 需要读取特定 Cookie 值时，优先通过 repository/数据源提供能力，不要把 Cookie DAO 暴露给 UI。

## 统一返回模型与请求扩展

文件：`core/network/src/main/java/com/imcys/bilibilias/network/NetWorkExt.kt`

### `NetWorkResult`

项目统一使用 `NetWorkResult<T>` 表达请求状态：

- `Loading`
- `Success`
- `Error`
- `Default`

其中同时保留：

- `data`
- 标准化后的 `responseData`
- `errorMsg`
- `ApiStatus`

这样做的目的：

- UI 可以统一处理加载、成功、失败。
- repository 可以在不丢失原始接口 `code/message/header` 的情况下做二次映射。

### `httpRequest`

`HttpClient.httpRequest { ... }` 是当前请求入口的事实标准。

默认行为：

1. 先发出 `Loading(true)`。
2. 执行真实请求。
3. 把响应解析为 `BiliApiResponse<Data>`。
4. 把响应头写回 `responseHeader`。
5. 根据 `code` 转成 `Success` 或 `Error`。
6. 捕获异常并转成 `NetWorkResult.Error`。

### 带 `adapter` 的 `httpRequest`

项目还提供了另一种入口：

- `HttpClient.httpRequest(adapter = ..., request = ...)`

这个重载是给“返回体不是 `BiliApiResponse<T>` 标准结构”的接口准备的。它的处理流程是：

1. 先发出 `Loading(true)`。
2. 直接把响应体解析成目标 `Data`。
3. 调用 `adapter.conversion(data)`，把原始响应和 HTTP 状态码转换成统一的 `BiliApiResponse<Data>`。
4. 调用 `adapter.handleSuccess(...)`，决定什么情况下算成功、什么情况下发出错误。

也就是说，`adapter` 的职责不是“再包一层 service”，而是把异构响应接入项目统一的状态流和错误处理模型。

### 统一错误处理

`handleSuccess()` 当前内置了两类特别处理：

- `code == -101`
  - 发送登录失效事件 `sendLoginErrorEvent()`
- `code == -509`
  - 发送请求频繁事件 `sendRequestFrequentEvent()`

这说明：

- 登录失效和请求频繁已经是跨页面统一语义，不应由单个页面自己重新判断。
- 若未来有新的全局错误语义，优先补到统一扩展或插件层。

### `mapData`

repository 常通过 `mapData` 把网络模型转换成数据层模型。

适合使用：

- 保留原有 `NetWorkResult` 状态不变，只替换 `data` 结构。
- 在 repository 中做轻量字段重组。

不适合使用：

- 需要同时合并多个请求、分平台回退、读本地库再二次请求的复杂流程；这类逻辑应直接在 repository 用 `Flow` 组合。

## API Service 分工

### `BILIBILIWebAPIService`

负责 Web 接口，包括：

- Web 登录二维码
- 登录信息
- WebI SPI 信息
- 用户空间
- 收藏夹、历史、点赞、追番
- 视频详情、播放信息、字幕、弹幕
- 番剧 Season / OGV 播放信息

代码特征：

- 大量请求会拼接 `BROWSER_FINGERPRINT`、`w_rid`、`wts` 等 WebI 签名参数。
- 通过 `WebiTokenUtils.encWbi(...)` 统一签名。
- 个别接口会单独设置 `Referer`。

开发约定：

- 新增 Web 接口，优先跟随现有 service 风格：在 service 里拼参数、签名、头信息。
- 不要把 `encWbi()` 下沉到 ViewModel 或 UI。

### `BILIBILITVAPIService`

负责 TV / App 侧接口，包括：

- TV 二维码登录
- TV 登录态查询
- TV 视频播放地址
- TV 番剧播放地址

代码特征：

- 通过 `setAppParams()` / `setAppFormData()` 统一补 `appkey`、`ts`、`sign`。
- 使用 `BiliAppSigner` 生成 App 签名。

开发约定：

- 新增 TV/App 接口时，优先复用 `setAppParams()` 或类似封装，不要每个请求手写 `sign`。

### `AppAPIService`

负责应用自身配置或服务端能力，例如版本、公告、横幅等。

### `BgmAPIService` / `GithubAPIService`

负责第三方通用接口，使用 `CommonApiHttpClient`，不携带 B 站业务插件。

## Adapter 层

关键文件：

- `core/network/src/main/java/com/imcys/bilibilias/network/adapter/NetWorkAdapter.kt`
- `core/network/src/main/java/com/imcys/bilibilias/network/adapter/BgmNetWorkAdapter.kt`
- `core/network/src/main/java/com/imcys/bilibilias/network/adapter/GithubNetWorkAdapter.kt`

### 为什么需要 adapter

项目默认的 `httpRequest { ... }` 假设响应体可以直接反序列化成：

- `BiliApiResponse<T>`

这很适合 B 站自身接口，因为它们通常有：

- `code`
- `message`
- `data` 或 `result`

但第三方接口不一定遵守这套格式。例如：

- Bangumi 接口更多依赖 HTTP 状态码判断成功。
- Github 接口返回的往往就是资源对象数组或对象本身，不会额外包一层 `code/data`。

如果没有 adapter，就只能：

- 在每个第三方 service 里单独写一套成功/失败判断。
- repository 层拿到的数据模型和 B 站接口完全两套处理方式。

引入 adapter 后，可以继续复用统一的：

- `FlowNetWorkResult<T>`
- `Loading / Success / Error`
- `mapData`
- repository 消费方式

### `NetWorkAdapter` 接口职责

`NetWorkAdapter<Data>` 定义了两件事：

1. `conversion(data)`
   - 把原始响应数据和 `HttpResponse` 转成统一的 `BiliApiResponse<Data>`。
2. `handleSuccess(data, apiResponse, response)`
   - 定义什么条件下应发出 `NetWorkResult.Success`。

这意味着 adapter 既负责：

- 结构适配

也负责：

- 成功语义适配

### `BgmNetWorkAdapter`

它当前的策略是：

- 用 HTTP 状态码作为 `BiliApiResponse.code`
- 用 HTTP 状态描述作为 `message`
- 把真正响应体放进 `data`
- 当 `code == 200` 时发出 `Success`

对应扩展函数：

- `HttpClient.bgmHttpRequest { ... }`

它的意义是把 Bangumi 这类“靠 HTTP 语义而不是业务 `code` 字段”的接口，接成与 B 站接口一致的 Flow 结果模型。

### `GithubNetWorkAdapter`

策略与 `BgmNetWorkAdapter` 类似：

- `response.status.value` 作为 `code`
- `response.status.description` 作为 `message`
- `200` 视为成功

对应扩展函数：

- `HttpClient.githubHttpRequest { ... }`

适合 Github 这类标准 REST 接口。

### Adapter 与 Service 的边界

职责分工应保持这样：

- `service`
  - 负责 URL、参数、请求方法、请求头、签名、请求体。
- `adapter`
  - 负责把“返回格式”适配进统一结果模型。
- `repository`
  - 负责组合请求、映射数据、分平台分流、结合本地状态。

不要把 adapter 用成 repository，也不要把 repository 里的业务逻辑挪进 adapter。

### 什么时候该新增 adapter

满足下面情况时，优先考虑新增 adapter：

- 返回体不是 `BiliApiResponse<T>`。
- 成功条件主要依赖 HTTP 状态码，而不是业务字段。
- 你希望上层仍然继续使用 `FlowNetWorkResult<T>` 和 `mapData`。

不需要新增 adapter 的情况：

- 接口本身已经是标准 `BiliApiResponse<T>`。
- 只是某个字段命名不同，但整体仍可通过序列化模型直接承接。
- 只是 repository 需要把字段重新组合，那应该用 `mapData` 或 repository 映射解决。

## 签名与鉴权

### WebI 签名

关键文件：

- `core/network/src/main/java/com/imcys/bilibilias/network/utils/WebiTokenUtils.kt`
- `core/data/src/main/java/com/imcys/bilibilias/data/repository/QRCodeLoginRepository.kt`

当前流程：

- Web 登录成功或获取用户信息后，repository 会在需要时初始化 `WebiTokenUtils` 的 key。
- Web service 请求通过 `encWbi()` 自动生成带签名的参数。

意味着：

- 只要是 WebI 接口，请求成功与否常常依赖登录态和当前 key 是否可用。
- 新增需要 WebI 的接口时，要确认 key 的初始化时机已经覆盖。

### TV/App 签名

关键文件：

- `core/network/src/main/java/com/imcys/bilibilias/network/utils/BiliAppSigner.kt`
- `core/network/src/main/java/com/imcys/bilibilias/network/service/BILIBILITVAPIService.kt`

当前流程：

- service 在请求前统一加入 `appkey`、`ts` 等参数。
- 然后计算 `sign`。
- 登录态通过 `access_key` 参与请求。

## Repository 如何消费网络层

### 1. 直接透传

简单接口通常直接透传，例如：

- `VideoInfoRepository.getVideoView()`
- `UserInfoRepository.getUserPageInfo()`

适用于：

- UI 需要的数据结构和网络模型差异不大。

### 2. 结果映射

典型例子：

- `QRCodeLoginRepository` 把 TV 二维码模型映射成统一 `QRCodeInfo` / `QRCodePollInfo`
- `UserInfoRepository` 把用户空间接口映射成 `BILISpaceArchiveModel`

适用于：

- Web / TV 返回结构不同，但上层希望用统一模型。

### 3. 多请求组合

典型例子：

- `UserInfoRepository.getUserStatInfo()` 用 `combine` 把投稿统计和关系统计组合成 `BILIUserStatModel`

适用于：

- 一个页面依赖多个接口，但希望上层只收一个流。

### 4. 分平台分流

典型例子：

- `VideoInfoRepository.getVideoPlayerInfo()`
- `VideoInfoRepository.getDonghuaPlayerInfo()`

这里 repository 会根据 `AppSettings.videoParsePlatform`：

- 选择走 Web 还是 TV API
- 在 TV 模式下补齐 `aid/cid`
- 自动选择当前用户对应平台的 `accessToken`
- 对返回音轨做二次整理，例如把杜比或 Hi-Res 音轨插入音频列表

这类逻辑说明：

- “平台差异处理”应放在 repository，而不是页面层。
- 页面层不应该知道当前到底走 Web 还是 TV 请求。

## 新增网络请求时的推荐流程

1. 先判断接口属于 Web、TV/App、还是第三方通用接口。
2. 在对应 `APIService` 中新增方法，统一处理 URL、参数、签名、头信息。
3. 如果需要跨请求行为，优先补到 Ktor plugin，而不是散落在每个 service 方法里。
4. 判断响应格式是否标准：
   - 如果是 `BiliApiResponse<T>`，走默认 `httpRequest { ... }`
   - 如果不是，先补 `adapter`，再走 `httpRequest(adapter = ...)`
5. 在 repository 中决定是否：
   - 直接透传
   - `mapData`
   - 合并多个请求
   - 根据登录平台或设置做分流
6. 只把 repository 暴露给 ViewModel。

## 什么时候不能只写在 service 里

下面这些逻辑不要只塞进 service：

- 依赖当前登录平台切换 Web / TV 请求。
- 需要从数据库取当前用户或 Cookie。
- 需要读取 DataStore 设置决定解析平台、漫游、Buvid 等行为。
- 需要把多个接口结果整合成一个页面模型。

这些都应该在 repository。

## 现有事实规范

- B 站 JSON API 优先走 `httpRequest`，返回 `FlowNetWorkResult<T>`。
- 第三方非标准响应优先通过 adapter 接入统一 `httpRequest` 流程。
- 第三方通用接口走 `CommonApiHttpClient`。
- 下载链路走 `DownloadHttpClient`。
- WebI 签名放在 Web service。
- App/TV 签名放在 TV service。
- Cookie 统一由 `AsCookiesStorage` 管理。
- 登录失效、请求频繁、播放白名单风控等全局语义通过统一扩展或插件发事件。

## 后续维护建议

- 新增接口后，如果它体现了新的请求模式，应同步更新本文档。
- 如果后续把网络层进一步拆成 `datasource` / `remote` 层，也要先说明迁移策略，避免文档和代码风格长期脱节。
