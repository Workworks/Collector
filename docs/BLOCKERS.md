# 外部阻塞项清单 (Blockers)

## 最新关键阻塞（2026-08-31）

| 编号 | 阻塞与解除条件 | 证据 |
| --- | --- | --- |
| BLK-04 | Android 旧签名缺失：需原 v4.3.0 keystore 匹配证书、重签并验覆盖升级。不得用卸载解决 | [发布报告](releases/v4.3.3-report.md) |
| BLK-05 | 本轮重新构建仍缺 MSVC x64，当前会话非管理员，未安装系统工具链；需工具链及干净安装/升级环境 | [最新原生失败记录](stages/evidence-454/native-build.log) |
| BLK-06 | 固定公网域名需账户、域名及受管隧道配置；临时 Quick Tunnel 不能提供固定地址承诺 | [WebDAV 联调](stages/webdav-integration-report.md) |

以上不能因为 HTTP 联调通过而关闭；以下为已有平台与签名约束。


本文件记录需要外部条件（用户凭据、专用硬件、第三方账号等）才能完全解除的阻塞项。

---

## 当前阻塞项

| 编号 | 涉及模块 | 阻塞现象与原因 | 解除条件 |
| :--- | :--- | :--- | :--- |
| **BLK-01** | Windows 桌面安装器 | Windows SmartScreen 在无受信数字签名时可能拦截新生成的 `.exe` 安装包 | 提供/采购正式 Windows EV/OV 代码签名证书或指引用户加入信任 |
| **BLK-02** | macOS 桌面版本 | macOS Gatekeeper 要求 App 必须经 Apple 官方公证（Notarization）并由受信开发者证书签名 | 提供 Apple Developer 开发者账号与 App-Specific 密码用于 `notarytool` 签名公证 |
| **BLK-03** | 动态 DEX 补丁验证 | 生产热补丁必须使用正式私钥签名，当前客户端已严格校验公钥指纹 | 生产发版时配置私钥签名生成 `patch.zip` |
