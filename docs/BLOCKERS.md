# 外部阻塞项清单 (Blockers)

本文件记录需要外部条件（用户凭据、专用硬件、第三方账号等）才能完全解除的阻塞项。

---

## 当前阻塞项

| 编号 | 涉及模块 | 阻塞现象与原因 | 解除条件 |
| :--- | :--- | :--- | :--- |
| **BLK-01** | Windows 桌面安装器 | Windows SmartScreen 在无受信数字签名时可能拦截新生成的 `.exe` 安装包 | 提供/采购正式 Windows EV/OV 代码签名证书或指引用户加入信任 |
| **BLK-02** | macOS 桌面版本 | macOS Gatekeeper 要求 App 必须经 Apple 官方公证（Notarization）并由受信开发者证书签名 | 提供 Apple Developer 开发者账号与 App-Specific 密码用于 `notarytool` 签名公证 |
| **BLK-03** | 动态 DEX 补丁验证 | 生产热补丁必须使用正式私钥签名，当前客户端已严格校验公钥指纹 | 生产发版时配置私钥签名生成 `patch.zip` |
