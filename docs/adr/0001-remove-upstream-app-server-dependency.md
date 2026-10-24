# ADR-0001：移除对上游私有服务器的全部运行时依赖

- 状态：已接受（2026-10-24）
- 影响：`core:network`、`core:data`、`shared`、`app`

## 背景

本仓库是已 public archive 的 BILIBILIAS 上游项目的 fork，自行编译自用。上游作者在 `api.misakamoe.com` 部署了一组私有服务，客户端在多处依赖它：

- 首页 banner、版本更新（`forcedUpdate = true` 硬更提示）、公告
- 捐助二维码、漫游申请、冻结 UP 主视频、充电专属视频"加速"信息
- 下载行为上报（把用户观看/下载的稿件信息与账号昵称发送到该服务器）
- `RoamPlugin` 会把 `api.bilibili.com` 的番剧请求改写到 `bili-api.misakamoe.com` 代理

上游归档后这组服务随时可能下线，且域名若被第三方接管，更新接口可以向本 fork 推送任意"新版本"下载链接，下载上报则持续外发用户数据。同源问题还有两个只对"防篡改包传播"有意义的本地提示：来源警告弹窗（"该程序由非开源仓库开发者打包而来…"，按 `版本号+gitCommitHash` 记忆，fork 每次重编译必弹）与签名未知横幅（硬编码比对上游两个签名 SHA1，fork 签名永不匹配）。

## 决定

1. 移除所有指向 `api.misakamoe.com` 的请求入口：首页三件套拉取、漫游设置入口、捐助入口、冻结视频对话框、充电加速信息拉取、下载行为上报，以及 `RoamPlugin` 的安装和 `AppAPIService` 的 Koin 注册。
2. 删除来源警告弹窗与签名未知横幅，连带删除 `rememberAppSignature` 平台 API。
3. 类与页面文件保留为死代码（`AppAPIService`、`RoamPlugin`、`RoamScreen`/`RoamViewModel`、`DonateScreen`/`DonateViewModel`），不再注册、不再可达；proto 字段 `package_source_warning_skip_key`、`unknown_app_sign_warning_close_time` 保留定义（proto 删字段有序号复用风险），仅删除 Kotlin 侧调用。

## 后果

- 本 fork 不再向上游服务器发送任何请求或用户数据；更新/公告/捐助能力随之消失，属预期放弃。
- 上游的更新通道不再可用，后续版本跟进只能靠手动 diff 上游仓库（上游已归档，本就不再发版）。
- 死代码与两个无人读写的 proto 字段留在仓库里，删除它们的成本（迁移与回归验证）高于保留成本；若未来做 schema 大清理可一并处理。
- 用户若在旧版本把 `RoamRoute`/`DonateRoute` 存进了导航栈存档，恢复导航栈时这两个 key 不再有对应页面；恢复逻辑本身有反序列化失败兜底，接受的残余风险。
