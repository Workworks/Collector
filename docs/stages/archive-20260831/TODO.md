# 文档刷新前原始快照

保存于 2026-08-31；以下按原文保留，链接按原位置 docs/ 解释，仅供追溯，不是当前执行依据。

````markdown
# 待办执行账本 (Task Execution Ledger)

## WebDAV 联调后续（2026-08-31）

- 固定公网域名：BLOCKED，需用户的 Cloudflare 账户、域名及受管隧道配置；临时隧道已实测，不承诺 SLA。
- 物理手机与正式客户端更新：未执行；保持原签名兼容发布门禁，不安装不兼容 APK 覆盖旧应用。
- 本轮代码、真实公网往返及失败证据见 [验收报告](stages/webdav-integration-report.md)，已完成项不重复列入待办。

**最后更新：2026-08-28。**

本文件只维护**尚未完成且可以继续执行的事项**，按优先级和推进责任排序。历史交付不在这里重复，查看 [Stage 路线图](stages/stage-roadmap.md) 与各 Stage 报告；缺陷查看 [bugList](bug/bugList.md)；问答查看 [AQ](aq/aq.md)；外部阻塞的详细解除条件查看 [BLOCKERS](BLOCKERS.md)。

状态以 Stage 报告为准。本账本不得把 `BLOCKED` 写成“进行中”，不得保留已经完成的长篇实施记录。

---

## 1. 当前重点（按执行顺序）

| 优先级 | Stage | 状态 | 下一动作 | 推进方 | 完成证据 |
| :--- | ---: | :---: | :--- | :--- | :--- |
| **P0** | **440** | `COMPLETED` | 桌面单机版架构立项与 docs 文档重构全部完成 | Agent | `docs/design/05-desktop-standalone-architecture.md`, `AGENTS.md` |
| **P1** | **441** | `COMPLETED` | 桌面单机版 `%LOCALAPPDATA%` 数据隔离、12 馆全量模型对齐与单元测试已全绿 | Agent | `DesktopDataDirectory.kt`, `DesktopDataStoreTest.kt` (全通过) |
| **P2** | **442** | `COMPLETED` | 桌面单机版 Native WebView2 壳源码、嵌入式引擎与自动化打包流水线已全部就绪 | Agent | `CollecterWindow.cpp`, `EmbeddedWebServer.kt`, `Package-CollecterStandalone.ps1` |
| **P2** | **443** | `COMPLETED` | 局域网 UDP 广播秒级自发现与双机 P2P 增量对撞合并引擎落地，双端单测全绿 | Agent | `LanPeerDiscovery.kt`, `LanSyncMergeEngine.kt`, `LanSyncMergeEngineTest.kt` |
| **P1** | **451** | `COMPLETED` | 极速智能采集通道与系统截图无感监听器落地，ML Kit 离线 OCR 与网页 Markdown 深度剪藏通过验证 | Agent | `docs/stages/stage-451-screenshot-watcher-clipper.md`, `ScreenshotClipperTest.kt` (全通过) |
| **P2** | **452** | `IN_PROGRESS` | **跨维双向锚定与全平台深度联动**：<br/>1. 实体物品 × 想法 × 剪藏文章双向锚定穿透视图<br/>2. 桌面端拖拽解析与 Spotlight 全局搜索悬浮窗<br/>3. P2P 局域网对撞与 WebDAV 同步全面覆盖想法与剪藏 | Agent | 跨维穿透关联测试、桌面端全局检索证据 |

---

## 2. 外部阻塞项

| 优先级 | Stage | 缺少条件 |
| :--- | ---: | :--- |
| **P2** | **442** | Windows 代码签名证书（当前使用内测自签名，正式分发需受信代码签名证书） |
| **P3** | **445** | macOS Apple 开发者账号与公证凭据（macOS 桌面安装包分发所需） |

---

## 3. 维护规则

1. Agent 启动时读取本文件，并把第 1 节的当前重点快照同步到 [stage-current.md](stage-current.md)。
2. 开始新 Stage 前先在 Stage 报告写清目标、边界、验证方式和完成标准，再把事项移入第 1 节。
3. 完成事项后从执行账本删除，不保留大段历史；真实结论写入对应 Stage 报告和证据目录。
4. 发现 Bug 或用户疑问分别进入 `bugList.md`、`aq.md`，不在本文件复制详情。
5. 优先级变化必须同时更新本文件与 `stage-current.md`。


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
