# ADR-0002：B 站登录凭据生命周期——Cookie 回写与 -101 温和失效

- 状态：已接受（2026-10-24）
- 影响：`core:network`、`shared`

## 背景

用户以 WEB 扫码 / Cookie 导入方式登录，症状是"杀掉应用重开后经常要重新登录"。排查确认两个结构性根因：

1. `AsCookiesStorage.addCookie()` 只更新内存，从不回写数据库。B 站在响应里轮换 `SESSDATA` 等登录 Cookie，新值只存在内存；进程重启后从数据库加载的是登录时刻的旧值，服务端随即判定未登录。
2. 任何一次 `code == -101` 都会触发 `accountLoginStateError()` 里的 `deleteBILIUserByUid()`，直接删除本地账号记录。风控误判或瞬时失效也会造成不可恢复的"被登出"。

另有一个小缺陷：`addCookie()` 把无 `Expires` 的会话 Cookie（`expires == null`，时间戳按 0 处理）当作已过期丢弃。

## 决定

1. **Cookie 回写**：`addCookie()` 收到 bilibili 域（`bilibili.com` 及子域）的 Set-Cookie 后，防抖 2 秒把内存中该用户的全部 bilibili 域 Cookie 全量回写数据库（沿用登录时的 delete+insert 写法，经 `EncryptedBILIUserCookiesDao` 落盘）。回写以互斥锁串行，未登录时跳过。
2. **会话 Cookie 修复**：`expires == null` 的 Cookie 保留在内存，仅明确已过期的才丢弃；会话 Cookie 不落库。
3. **-101 温和失效**：移除 `accountLoginStateError()` 中的账号删除。保留"逐个尝试其他已存平台账号"的原有逻辑；全部失效时只重置登录标记并提示，账号记录与 Cookie 全部保留。

## 后果

- WEB/Cookie 登录在进程重启后使用最近一次服务端下发的 Cookie，`SESSDATA` 轮换不再导致掉线；buvid3/buvid4 随回写持久化，设备指纹跨启动稳定。
- 回写采用全量 delete+insert 而非逐条 diff：实现简单，代价是每次防抖窗口内一次小事务（Cookie 数量为个位到十几条），可接受。
- TV/MOBILE 平台的 `access_token` 过期仍无刷新实现（refresh_token 流程缺失），该平台用户到期仍需重新扫码；本轮不解决，见技术债清单。
- `AsCookiesStorage.get()` 仍不做请求域名匹配、不按过期时间过滤数据库加载的 Cookie（Cookie 表无 `expires` 列），为本决定接受并记录的遗留取舍。
- -101 不再删除记录后，失效账号的 Cookie 行会留在库里；下次登录同账号时按登录流程整体覆盖，无泄漏面扩大（数据本就已在本地加密库中）。
