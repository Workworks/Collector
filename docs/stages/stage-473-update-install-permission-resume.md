# Stage 473：应用内更新授权后续装与 v4.3.12 热修复 Spec

## 1. 目标

当用户首次使用应用内更新且尚未授予“安装未知应用”权限时，Collecter 打开系统授权页；用户授权并返回后，应用必须继续拉起 Android 系统安装确认页，不再停留在“我的”页且没有反馈。

## 2. 影响范围与边界

- Android 更新入口：`app/src/main/java/com/kfaino/diapertracker/UpdateManager.kt`。
- Android Activity 返回协调：`app/src/main/java/com/kfaino/diapertracker/MainActivity.kt`。
- 自动化回归：`app/src/test/java/com/kfaino/diapertracker/UpdateInstallRequestTest.kt`。
- 发布版本：Android `4.3.12 / 49`；桌面端仅同步发布版本标识，不增加桌面功能。
- 文档：缺陷账本、当前阶段、TODO、README 和发布报告。

范围外：不静默安装 APK，不绕过 Android 系统确认页，不降低 TLS、文件大小或 SHA-256 校验，不更换 GitHub 官方源优先级，不修改 `applicationId`、namespace、存储键或发布签名。

安全与兼容不变量：待安装请求只接受应用私有缓存/下载目录内的 APK；授权返回后再次验证请求时效、文件边界、大小与摘要；覆盖升级继续使用原签名 `03d73e71eb13c82a820dc50f4ef780630aa698591944693ff6b7c1c3c3bb013a`。

## 3. 工作包

- WP-01：记录 v4.3.10 → v4.3.11 首次授权后未续装的 API 34 复现证据。
- WP-02：持久保存经过校验的待安装请求，并通过 Activity Result 接收系统授权页返回。
- WP-03：授权返回后重新校验 APK 并拉起系统安装器；拒绝授权、请求过期、路径越界或摘要不符时清理请求并给出可见提示。
- WP-04：新增请求序列化、时效和路径边界的最小回归测试。
- WP-05：执行 Android 单测、Release 构建、整体 selfcheck、API 34 授权返回验收和原签名覆盖升级。
- WP-06：发布 v4.3.12，回下载附件并复核哈希、签名和版本元数据。

## 4. 验证方式

1. `./gradlew.bat testReleaseUnitTest`：全部 Android JVM 回归通过。
2. `./gradlew.bat assembleRelease`：使用原签名构建 v4.3.12 Release APK。
3. `pwsh -File ./tools/selfcheck.ps1`：六项仓库门禁全部通过，输出原样归档。
4. API 34 AVD：关闭 Collecter 的未知来源权限，触发安装，授权后返回；预期自动进入系统安装确认页。
5. API 34 AVD：从 v4.3.11 覆盖安装 v4.3.12，版本变为 49 且应用私有数据哨兵保留。
6. GitHub Release：下载三项附件，哈希与本地交付物一致；APK 证书摘要和版本信息符合本 Spec。

## 5. 完成标准

- [x] AC-01：授权并返回后自动进入系统安装确认页，且不会直接静默安装。
- [x] AC-02：拒绝授权时给出明确提示，用户可再次从更新入口重试。
- [x] AC-03：过期、越界、缺失或摘要不符的待安装请求不会进入安装器。
- [x] AC-04：Android 单测、Release 构建和整体 selfcheck 全绿。
- [x] AC-05：v4.3.11 → v4.3.12 原签名覆盖升级和数据保留验证通过。
- [ ] AC-06：v4.3.12 Release 发布并完成远端附件回读验证。

## 6. 当前复现证据

- `evidence-473/pre-fix-after-grant.png`：v4.3.10 在系统授权页开启权限并返回后仍停留“我的”页。
- `evidence-473/pre-fix-after-grant.xml`：当前焦点为 `MainActivity`，未出现 Android Package Installer。
- `adb shell dumpsys package com.kfaino.diapertracker`：返回后版本仍为 `4.3.10 / 47`。

## 7. 验收结果

- AC-01 `PASS`：API 34 AVD 授权返回后焦点进入 `PackageInstallerActivity`，系统仍显示 Update/Cancel，见 `evidence-473/device-update-flow.md`。
- AC-02 `PASS`：Activity Result 的未授权分支清理待安装请求并显示“未开启安装权限，可再次点击更新重试”；授权请求可以从更新入口重新创建。
- AC-03 `PASS`：`UpdateInstallRequestTest` 覆盖十分钟时效、未来时间、私有目录、越界路径和非 APK 扩展名；进入安装器前再次执行大小与 SHA-256 校验。
- AC-04 `PASS`：Android 89 项、Desktop 24 项均 0 失败；签名 Release 构建成功；selfcheck 6/6 通过。
- AC-05 `PASS`：正式 v4.3.11 → 候选 v4.3.12 `adb install -r` 成功，中文数据哨兵保留，冷启动 AndroidRuntime fatal 为 0。
- AC-06 `PENDING`：候选及三项附件已生成，等待创建 GitHub Release 并回下载核验。
