# 当前阶段与验证状态

更新：2026-09-05。**正式版仍为 v4.3.9；Stage 468 全局弹框视觉统一已完成，尚未发布。** 55 个弹框相关 Kotlin 文件和 189 个 Material 弹框调用已通过全局主题覆盖；首次引导与标准长列表弹框完成模拟器截图复验，旧式日期/进度弹框已退出业务主路径，见 [Stage 468](stages/stage-468-unified-dialog-design.md)。

Stage 459–462 已进入 v4.3.9。Stage 463–468 已在源码或 QA 流程完成，未创建新版本。两台物理 Android 设备、固定公网域名、物理设备凭据加密迁移和第二台干净 Windows 主机仍受外部环境阻塞，不列为已完成。

| 项目 | 当前事实 | 依据 |
| --- | --- | --- |
| Git 基线 | 本地快照 d3c1f4d 与 origin/main b0aafdd 合并；不强制推送 | [整合报告](stages/stage-457-report.md) |
| 产品及版本 | Collecter；Android 4.3.9 / 46，desktop 4.3.9；Stage 463–468 源码未发布；applicationId 不变 | app/build.gradle.kts、desktop/build.gradle.kts |
| WebDAV | Android HEAD、双端完整备份和条件上传；模拟器、公网、桌面分别记录证据 | [457 证据](stages/evidence-457/) |
| 安全 | DEX 安装/启动均先验签；默认缺钥拒绝；ZIP 边界、数量与体积限制 | HotPatchEngine、DexSignatureVerifier、PatchArchive |
| 数据兼容 | 两分支集合键一次性原子迁移，日期/进度字段兼容，饮品小数不截断 | VaultSchemaMigration、WireAliases |
| 发布 | v4.3.9 三项产物已发布并回下载校验；移动端仅使用 GitHub 官方更新源 | [发布报告](releases/v4.3.9-report.md) |
| UI | 全局 Material 弹框采用 28dp 圆角、统一排版、42% 遮罩和克制的淡入缩放动效；原生旧式弹框为 0 | [Stage 468](stages/stage-468-unified-dialog-design.md) |
| 后续 | 除 Stage 463–468 待发布外，只剩需要外部硬件、域名或恢复前置的阻塞项 | [TODO](TODO.md) |

原签名已找回并核对，不再是“缺少签名”。DEX 公钥未配置及调用未接入，不能用资源 ZIP 修复 WebDAV 原生代码。

[整合前本地状态](stages/archive-before-457-current.md)与[远端历史状态](stages/remote-4.3.6/stage-current.md)保留作追溯，历史完成声明不代替本轮实测。
