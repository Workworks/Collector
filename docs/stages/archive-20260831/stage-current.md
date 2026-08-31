# 文档刷新前原始快照

保存于 2026-08-31；以下按原文保留，链接按原位置 docs/ 解释，仅供追溯，不是当前执行依据。

````markdown
# 当前阶段 (Stage Context Snapshot)

## 2026-08-31 WebDAV 专项联调

相邻 WebDav 服务已开放经认证的临时公网 HTTPS，Android 模拟器、桌面真实 Helper 和 Android → 桌面备份恢复通过。详见 [专项验收报告](stages/webdav-integration-report.md)。未修改版本/签名，未发布客户端，原生 GUI/物理手机未验收；selfcheck 版本一致项仍因既有独立发布范围不一致而失败。临时域名重启会变，不标为固定公网服务。

**最后更新：2026-08-28。** 本文件是启动协议 Step 2 的读取目标，只描述“现在做什么”。
历史与全量排期见 [stage-roadmap.md](stages/stage-roadmap.md)，阻塞详情见 [BLOCKERS.md](BLOCKERS.md)。
**可执行任务账本见 [TODO.md](TODO.md)**——本文件保留当前上下文与重点快照，TODO 按优先级、推进责任和下一动作维护；每次任务必须同时读取，状态仍以 Stage 报告为准。

状态取值只有 `NOT_STARTED` / `IN_PROGRESS` / `COMPLETED` / `BLOCKED` 四种。

---

## 1. 最近交付的阶段

| Stage | 名称 | 状态 | 交付内容与证据 |
| ---: | :--- | :---: | :--- |
| **441** | 桌面单机版独立数据目录与 12 馆数据层全量对齐 | `COMPLETED` | `%LOCALAPPDATA%\CollecterStandalone\data` 数据隔离、12 馆模型对齐、桌面端单元测试 100% 绿灯 |
| **442** | 桌面单机版 Native WebView2 壳与打包流水线 | `COMPLETED` | `CollecterWindow.cpp` (单实例/托盘/启动页探测)、`EmbeddedWebServer.kt` (Port 8848)、`Package-CollecterStandalone.ps1` 自动化打包验证通过 |
| **443** | 局域网双机自发现与 P2P 增量对撞合并 | `COMPLETED` | `LanPeerDiscovery.kt` (UDP 8849 自发现)、`LanSyncMergeEngine.kt` (Last-Write-Wins 增量对撞与 12 馆合并)、单测 100% 绿灯 |
| **450** | 灵感想法舱与智能剪藏知识库底层基建 | `COMPLETED` | `IdeaRecord` / `ClippingRecord` 契约定义、双端持久化仓储、`IdeaVaultDialog` / `ClippingVaultDialog` UI 落地、`GlobalSearchDialog` 全维穿透检索，单测全绿 |
| **451** | 极速智能采集通道与系统截图无感监听器 | `COMPLETED` | `ScreenshotWatcherHelper` 截图无感捕获、`ScreenshotOcrProcessor` ML Kit 离线 OCR 全文提取打标、`WebClipperEngine` 纯净 Markdown 剪藏，单测 45/45 全绿 |

---

## 2. 当前重点任务快照

| 优先级 | Stage | 状态 | 当前下一步 | 推进方 | 交付物/证据 |
| :--- | ---: | :---: | :--- | :--- | :--- |
| **P2** | **452** | `IN_PROGRESS` | **跨维双向锚定与全平台深度联动**：<br/>1. 实体物品 × 想法 × 剪藏文章双向锚定穿透视图<br/>2. 桌面端拖拽解析与 Spotlight 全局搜索悬浮窗<br/>3. P2P 局域网对撞与 WebDAV 同步全面覆盖想法与剪藏 | Agent | 跨维穿透关联测试、桌面端全局检索证据 |
| **P2** | **452** | `NOT_STARTED` | **跨维双向锚定与全平台深度联动**：<br/>1. 实体物品 × 想法 × 剪藏文章双向锚定穿透视图<br/>2. 桌面端拖拽解析与 Spotlight 全局搜索悬浮窗<br/>3. P2P 局域网对撞与 WebDAV 同步全面覆盖想法与剪藏 | Agent | 跨维穿透关联测试、桌面端全局检索证据 |
| **P2** | **452** | `NOT_STARTED` | **跨维双向锚定与全平台深度联动**：<br/>1. 实体物品 × 想法 × 剪藏文章双向锚定穿透视图<br/>2. 桌面端拖拽解析与 Spotlight 全局搜索悬浮窗<br/>3. P2P 局域网对撞与 WebDAV 同步全面覆盖想法与剪藏 | Agent | 跨维穿透关联测试、桌面端全局检索证据 |
| **P1** | **441** | `NOT_STARTED` | **桌面单机版核心引擎与统一数据持久化**：<br/>1. 实现 `%LOCALAPPDATA%\CollecterStandalone\data` 安全数据目录隔离<br/>2. 桌面端对齐全量 12 馆数据模型与双向 JSON 备份互通 | Agent | `:desktop` 存储模块、数据迁移与往返测试 |
| **P2** | **442** | `NOT_STARTED` | **桌面单机版 Native WebView2 壳与系统托盘**：<br/>1. 实现 `CollecterWindow.cpp` 托盘控制、启动探测与单实例互斥<br/>2. 完成 Windows app-image 与 NSIS 一键安装包构建流水线 | Agent | Native C++ 源码、PowerShell 打包脚本、Windows 安装器 |

---

## 3. 约束与不变量

1. **桌面端与移动端数据 100% 互通**：共享一套 JSON 备份规范，绝不产生数据库方言分裂。
2. **数据目录独立于安装目录**：`%LOCALAPPDATA%\CollecterStandalone\data`，卸载程序严禁触碰数据目录。
3. **禁止假发版**：桌面单机版未完成全量真机验收前，不得更改其发布版本号。


## 2026-08-30 命名与规划补充

2026-08-30 命名更新：Gradle 工程名已改为 Collecter，Android 安装和存储标识不变；当时目录改名被占用阻止，后由用户完成为 `Collector`。后续需求分析见 [product-direction.md](product-direction.md)，当前仅为建议，不改写既有 Stage 的验收状态。

## 2026-08-30 执行计划（优先级覆盖旧快照）

实际目录为 `Collector`，用户已完成目录改名；产品/Gradle 工程名为 `Collecter`。
用户已授权实施非暂缓需求，唯一验收 Spec 为 [Stage 453](stages/stage-453-reliable-collection.md)。

| 顺序 | 工作包 | 状态 |
| --- | --- | --- |
| P0 | 完整备份、安全恢复、本地服务安全 | 实现及自动化回归通过；交互/外部服务验收范围见 453 |
| P1 | 完整双端同步、统一收集箱 | 已实现；跨物理设备与图片分享完整验收待补 |
| P2 | 关联检索、提醒闭环 | 已实现；存储回归通过，长期系统提醒待实用验证 |
| P3 | 桌面启动与交付验收 | BLOCKED：缺 MSVC；不能宣称原生安装/升级验收通过 |

旧表中 441/442/452 的冲突状态不作为本轮验收依据；历史完成记录保留，按 453 的实际测试结果收敛。
本轮交付与验收明细：[Stage 453 实施报告](stages/stage-453-delivery-report.md)。73 项测试、selfcheck 六项通过；Android 分享/OCR/关联/取消恢复已实测。整体任务未全部验收，不把原生交付阻塞改为完成。

## Android v4.3.3 发布任务（2026-08-30）

用户已授权发布；候选版本 4.3.3 / 40，桌面仍 4.3.2。签名与 GitHub 旧 APK 不一致，发布状态 BLOCKED，未公开推送。详见 [发布 Spec](releases/v4.3.3-spec.md)。

````
