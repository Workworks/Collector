# 当前阶段与验证状态

更新：2026-09-01。**v4.3.9 已正式发布**。移动端更新反馈、工作台入口/收集箱 UI、旋转恢复/快捷键、后台隐私重锁、每日校验归档及 Windows Native 便携包已交付；原签名覆盖升级和远端三附件回读通过，见 [发布报告](releases/v4.3.9-report.md)。

Stage 459–462 已进入 v4.3.9。物理 Android 设备、固定公网域名、物理设备凭据加密迁移和第二台干净 Windows 主机仍受外部环境阻塞，不列为已完成。

| 项目 | 当前事实 | 依据 |
| --- | --- | --- |
| Git 基线 | 本地快照 d3c1f4d 与 origin/main b0aafdd 合并；不强制推送 | [整合报告](stages/stage-457-report.md) |
| 产品及版本 | Collecter；Android 4.3.9 / 46，desktop 4.3.9；applicationId 不变 | app/build.gradle.kts、desktop/build.gradle.kts |
| WebDAV | Android HEAD、双端完整备份和条件上传；模拟器、公网、桌面分别记录证据 | [457 证据](stages/evidence-457/) |
| 安全 | DEX 安装/启动均先验签；默认缺钥拒绝；ZIP 边界、数量与体积限制 | HotPatchEngine、DexSignatureVerifier、PatchArchive |
| 数据兼容 | 两分支集合键一次性原子迁移，日期/进度字段兼容，饮品小数不截断 | VaultSchemaMigration、WireAliases |
| 发布 | v4.3.9 三项产物已发布并回下载校验；移动端仅使用 GitHub 官方更新源 | [发布报告](releases/v4.3.9-report.md) |
| 后续 | 只剩需要外部硬件、域名或恢复前置的阻塞项 | [TODO](TODO.md) |

原签名已找回并核对，不再是“缺少签名”。DEX 公钥未配置及调用未接入，不能用资源 ZIP 修复 WebDAV 原生代码。

[整合前本地状态](stages/archive-before-457-current.md)与[远端历史状态](stages/remote-4.3.6/stage-current.md)保留作追溯，历史完成声明不代替本轮实测。
