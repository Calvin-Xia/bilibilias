# 术语表

本仓库协作与文档中的统一用语。只定义概念，不记录实现细节。

- **上游（upstream）**：已 public archive 的 BILIBILIAS 原项目及其作者部署在 `api.misakamoe.com` 的私有服务。本仓库是其 fork，自编译自用。
- **登录态（login state）**：应用认为"当前有已登录 B 站账号"的状态，由 DataStore 的当前用户 id 标记；是否真的可用以数据库凭据和服务端响应为准。
- **凭据（credentials）**：一个账号在本地数据库里的全部身份材料：用户记录（含 TV/MOBILE 平台的 access/refresh token）与 Cookie 行，经加密 DAO 落盘。
- **登录态对账（reconcile）**：启动时核对"DataStore 说已登录"与"数据库凭据是否存在且可解密"，不一致时重置登录标记。
- **登录态失效（session invalidation）**：服务端返回 `code == -101`。处理约定是温和失效——重置登录标记、提示重新登录，但保留凭据记录（可能与风控误判混淆，不可删数据）。
- **Cookie 回写（cookie write-back）**：把响应 Set-Cookie 更新后的 Cookie 持久化回数据库的动作，保证进程重启后仍持有服务端最近下发的值。
- **来源警告（package source warning）**：原上游用于提示"安装包可能被第三方篡改"的弹窗；本 fork 已删除（fork 自编译场景下无意义）。
- **漫游（roam）**：原上游经由其私有代理访问区域限定番剧的能力；依赖已归档的上游服务器，本 fork 已移除入口。
