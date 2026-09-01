# 当前阶段与验证状态

更新：2026-09-01。**v4.3.8 已正式发布**。移动端更新下载 TLS 故障已修复；构建、原签名、Android 14 官方源联网、v4.3.7 覆盖升级及远端附件回读通过，见 [发布报告](releases/v4.3.8-report.md)。

| 项目 | 当前事实 | 依据 |
| --- | --- | --- |
| Git 基线 | 本地快照 d3c1f4d 与 origin/main b0aafdd 合并；不强制推送 | [整合报告](stages/stage-457-report.md) |
| 产品及版本 | Collecter；Android 4.3.8 / 45，desktop 4.3.8；applicationId 不变 | app/build.gradle.kts、desktop/build.gradle.kts |
| WebDAV | Android HEAD、双端完整备份和条件上传；模拟器、公网、桌面分别记录证据 | [457 证据](stages/evidence-457/) |
| 安全 | DEX 安装/启动均先验签；默认缺钥拒绝；ZIP 边界、数量与体积限制 | HotPatchEngine、DexSignatureVerifier、PatchArchive |
| 数据兼容 | 两分支集合键一次性原子迁移，日期/进度字段兼容，饮品小数不截断 | VaultSchemaMigration、WireAliases |
| 发布 | v4.3.8 Release 已发布并回下载校验；移动端仅使用 GitHub 官方更新源 | [发布报告](releases/v4.3.8-report.md) |
| 后续 | 真机、完整 GUI、Native、固定公网及长期运维仍待验收 | [TODO](TODO.md) |

原签名已找回并核对，不再是“缺少签名”。DEX 公钥未配置及调用未接入，不能用资源 ZIP 修复 WebDAV 原生代码。

[整合前本地状态](stages/archive-before-457-current.md)与[远端历史状态](stages/remote-4.3.6/stage-current.md)保留作追溯，历史完成声明不代替本轮实测。
