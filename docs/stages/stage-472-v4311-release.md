# Stage 472：v4.3.11 账单模块发布

状态：`COMPLETED`。日期：2026-09-27。

## 1. 目标

把 Stage 471 已完成的 Android 账单模块发布为 Collecter v4.3.11，并提供可从正式 v4.3.10 原签名覆盖升级的 APK。同步构建桌面兼容产物，确保桌面端在没有账单编辑界面的前提下无损保留 `bills` 集合和未来字段。

## 2. 发布边界

- Android：`versionName` 从 4.3.10 升至 4.3.11，`versionCode` 从 47 升至 48。
- Desktop：构建号对齐 4.3.11，交付 Universal JAR 和 Windows Native 便携包；本版不新增桌面账单编辑界面，发布说明必须明确。
- `applicationId`、namespace、存储键、备份协议、原签名、DEX 验签和 ZIP 路径边界保持不变。
- 不把模拟器描述为实体手机，不把本机 Native smoke 描述为第二台干净 Windows 验收。
- 不创建热补丁 ZIP；账单涉及 Kotlin/布局与数据集合，必须通过完整 APK 覆盖升级。

## 3. 工作包

- WP-01：更新 Android、Desktop、README 与当前阶段版本真相。
- WP-02：运行共享、Android、Desktop 测试和原 selfcheck。
- WP-03：使用 `F:\LANShare\debug.keystore` 构建签名 APK，验证 v4.3.10 → v4.3.11 签名、包名、版本、数据哨兵和冷启动。
- WP-04：构建 Desktop Universal JAR 与 Windows Native ZIP，并通过 `java -jar Collecter-Desktop.jar --native --smoke-test`。
- WP-05：生成候选哈希、发布报告和证据，提交并推送源码。
- WP-06：创建注释 tag 与 GitHub Release，上传三项产物后从 Release 回下载并核对大小、SHA-256 和 APK 签名。

## 4. 验收标准

- [x] AC-01：Android 为 4.3.11 / 48，Desktop 与 README 为 4.3.11，三处一致。
- [x] AC-02：Stage 471 账单解析、存储、备份、桌面保真和定向设备测试通过。
- [x] AC-03：共享、Android、Desktop、Release 构建及 `tools/selfcheck.ps1` 全部通过。
- [x] AC-04：正式 v4.3.10 APK 与候选 APK 包名一致、证书 SHA-256 一致，`adb install -r` 保留升级哨兵并冷启动无 fatal。
- [x] AC-05：Desktop JAR 与 Windows Native ZIP 构建成功，完整后端 Native smoke 退出码为 0；干净机和受信代码签名标记 `BLOCKED`。
- [x] AC-06：发布提交、注释 tag 和 GitHub Release 指向提交 `5153a8ee9d46ebe9edab0ec25737945361ed0717`；Release 非草稿、非预发布。
- [x] AC-07：三项附件经 GitHub 回下载后字节数与 SHA-256 和本地候选一致，APK 再次验签通过。
- [x] AC-08：实体手机、移动网络、第二台干净 Windows、固定公网域名等外部门禁继续如实标记，不阻断已经具备证据的补丁发布。

## 5. 验收证据

候选与发布证据已写入 `docs/releases/evidence-4.3.11/` 与 `docs/releases/v4.3.11-report.md`。正式 Release：https://github.com/Workworks/Collector/releases/tag/v4.3.11 。
