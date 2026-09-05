# 外部阻塞项清单 (Blockers)

## 最新关键阻塞（2026-09-05）

| 编号 | 阻塞与解除条件 | 证据 |
| --- | --- | --- |
| BLK-04 | 旧签名缺失及新版本发布已解除：v4.3.9 → v4.3.10 模拟器原签名覆盖升级通过；仍需物理设备覆盖升级和蜂窝网络验收 | [v4.3.10 报告](releases/v4.3.10-report.md) |
| BLK-05 | MSVC x64 已恢复，v4.3.10 Native 构建及本机 smoke 通过；仍缺第二台干净 Windows 和受信代码签名证书 | [v4.3.10 报告](releases/v4.3.10-report.md) |
| BLK-06 | 固定公网域名需账户、域名及受管隧道配置；临时 Quick Tunnel 不能提供固定地址承诺 | [WebDAV 联调](stages/webdav-integration-report.md) |

以上不能因为 HTTP 联调通过而关闭；以下为已有平台与签名约束。


本文件记录需要外部条件（用户凭据、专用硬件、第三方账号等）才能完全解除的阻塞项。

---

## 当前阻塞项

| 编号 | 涉及模块 | 阻塞现象与原因 | 解除条件 |
| :--- | :--- | :--- | :--- |
| **BLK-01** | Windows 桌面安装器 | Windows SmartScreen 在无受信数字签名时可能拦截新生成的 `.exe` 安装包 | 提供/采购正式 Windows EV/OV 代码签名证书或指引用户加入信任 |
| **BLK-02** | macOS 桌面版本 | macOS Gatekeeper 要求 App 必须经 Apple 官方公证（Notarization）并由受信开发者证书签名 | 提供 Apple Developer 开发者账号与 App-Specific 密码用于 `notarytool` 签名公证 |
| **BLK-03** | 动态 DEX 补丁验证 | 客户端默认没有配置受信公钥，因此拒绝动态 DEX；WebDAV 原生调用没有接入动态替换 | 配置受信公钥、离线签发和调用接入后独立验收；本次 WebDAV 修复走完整 APK |
