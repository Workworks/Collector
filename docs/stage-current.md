# 当前阶段与验证状态

更新：2026-09-27。**正式版为 v4.3.12 / Android 49；Stage 473 应用内更新授权续装修复已经发布。** 原签名覆盖升级、权限返回续装、桌面兼容产物、GitHub Release 和三项附件回下载哈希均已验证，见 [发布报告](releases/v4.3.12-report.md)。

Stage 459–462 已进入 v4.3.9，Stage 463–468 已进入 v4.3.10，Stage 471 已进入 v4.3.11，Stage 473 已进入 v4.3.12。两台物理 Android 设备、固定公网域名、物理设备凭据加密迁移和第二台干净 Windows 主机仍受外部环境阻塞，不列为已完成。

| 项目 | 当前事实 | 依据 |
| --- | --- | --- |
| Git 基线 | 本地快照 d3c1f4d 与 origin/main b0aafdd 合并；不强制推送 | [整合报告](stages/stage-457-report.md) |
| 产品及版本 | Collecter；Android 4.3.12 / 49，desktop 4.3.12；applicationId 不变 | app/build.gradle.kts、desktop/build.gradle.kts |
| WebDAV | Android HEAD、双端完整备份和条件上传；模拟器、公网、桌面分别记录证据 | [457 证据](stages/evidence-457/) |
| 安全 | DEX 安装/启动均先验签；默认缺钥拒绝；ZIP 边界、数量与体积限制 | HotPatchEngine、DexSignatureVerifier、PatchArchive |
| 数据兼容 | 两分支集合键一次性原子迁移，日期/进度字段兼容，饮品小数不截断 | VaultSchemaMigration、WireAliases |
| 发布 | v4.3.12 三项产物已发布并回下载校验；GitHub API、官方资源和两个镜像 Range 请求通过 | [发布报告](releases/v4.3.12-report.md) |
| 应用内更新 | 首次来源安装授权返回后自动续装；待安装请求二次验证时效、私有目录、大小和 SHA-256 | [Stage 473](stages/stage-473-update-install-permission-resume.md) |
| UI | 全局 Material 弹框采用 28dp 圆角、统一排版、42% 遮罩和克制的淡入缩放动效；原生旧式弹框为 0 | [Stage 468](stages/stage-468-unified-dialog-design.md) |
| 账单 | 底部第三入口、手动记账、快速文本、月度汇总及完整备份已随 v4.3.11 发布；API 34/360dp 通过，实体手机阻塞 | [Stage 471](stages/stage-471-billing-ledger.md) |
| 后续 | Stage 469 待真实设备和使用者到位后开展 7 天复验；Stage 470 等待外部硬件、域名或恢复前置 | [TODO](TODO.md) |

原签名已找回并核对，不再是“缺少签名”。DEX 公钥未配置及调用未接入，不能用资源 ZIP 修复 WebDAV 原生代码。

[整合前本地状态](stages/archive-before-457-current.md)与[远端历史状态](stages/remote-4.3.6/stage-current.md)保留作追溯，历史完成声明不代替本轮实测。
