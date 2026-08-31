# 原签名恢复与升级门禁

2026-08-31。用户提供 `F:\LANShare\debug.keystore`，仅在仓库外使用，不复制到源码或分发目录。

证书 SHA-256：`03d73e71eb13c82a820dc50f4ef780630aa698591944693ff6b7c1c3c3bb013a`，与原 v4.3.0 APK 一致。[实测日志](../stages/evidence-456/signature-compatible.log)。这解除签名不兼容阻塞，不自动代表真机升级或公开发布通过。

`app/build.gradle.kts` 的 release 构建要求以下进程环境变量：`COLLECTER_KEYSTORE`、`COLLECTER_STORE_PASSWORD`、`COLLECTER_KEY_ALIAS`、`COLLECTER_KEY_PASSWORD`。由本机安全提供口令，不在仓库保存；缺少任一项，packageRelease 拒绝构建，不退回本机默认 debug 密钥。普通 debug 构建保留原开发签名。

```powershell
.\gradlew.bat :app:assembleRelease
pwsh -File .\scripts\Verify-AndroidRelease.ps1 -PreviousApk '<原发布 APK>' -CandidateApk '.\app\build\outputs\apk\release\app-release.apk'
```

本轮升级实验使用隔离 API 34 模拟器：安装原 v4.3.0 APK，在旧沙盒注入公开测试记录，再 `adb install -r` 覆盖；没有卸载、没有清数据。安装结果与前后样本在 evidence-456；应用仓储读取断言另行验证。此样本验证安装与存储标识延续，不冒充完整旧版 UI 创建业务数据或真实用户设备升级。

用户要求“完成后升级版本”。只有可交付范围与遗留验收明确后才调整相应平台版本，不能为 selfcheck 绿灯把未完成的 Native 交付升级。原私钥现已找到，账本不得继续写“原私钥缺失”。
