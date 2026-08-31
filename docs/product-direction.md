# Collecter：后续需求方向与实施顺序

## 当前进展补充（2026-08-31）

用户再次授权实施七个演化方向后，进入 Stage 455：增加统一整理入口、保存搜索、批量整理、提取建议、生命周期、加密备份/历史恢复和家庭会话权限。保留离线优先，不迁移数据库、不新增专业馆。现有实现、未完成 GUI 与交付前置见 [455 报告](stages/stage-455-report.md)；家庭会话权限不等于永久账号/公网协作，部分专业馆业务联动仍需继续验证。

原建议已部分实现；Android/桌面 WebDAV 公网固定样例和附件恢复已有 [通过证据](stages/webdav-integration-report.md)。2026-08-31 按用户授权自主决定继续聚焦离线收藏、生命周期管理与可靠备份：已实施历史、删除恢复、条件写入和 OCR 并发保护，见 [Stage 454](stages/stage-454-spec.md)。优先保障升级签名、物理设备及长期使用验收；新增专业馆、macOS、复杂悬浮窗、通用 AI 和 UI 重写继续暂缓，不要求本轮重复确认，也不宣称这些方向已交付。执行状态以 [TODO](TODO.md) 为准。


日期：2026-08-30。状态：**原始需求建议；用户随后已授权实施，当前完成情况以 [Stage 453](stages/stage-453-reliable-collection.md) 为准**。依据为当前代码和 [诊断基线](baseline.md)，不是市场调研结论。暂按个人/家庭、离线优先、Android 日常使用、Windows 辅助整理这一现有定位分析；尚无用户使用频率或留存数据，优先级需用真实使用验证。

## 1. 推荐定位

**让个人和家庭的物品、凭证及相关资料，随手收下、随时找到、到期知道，而且换设备不丢。**

现有项目已经覆盖 12 个专业馆、资产、想法、剪藏和桌面同步。当前短板是这些数据之间的可靠流转，而非缺少更多分类入口。继续横向增加展馆容易增加重复录入、存储协议与测试负担。证据：[仓储实现](../app/src/main/java/com/kfaino/diapertracker/VaultRepositories.kt)、[现有待办](TODO.md)、[基线 R2–R4](baseline.md#5-质量风险按处理优先级)。

最适合继续做深的场景是：**买入一个物品 → 保存发票/说明书 → 指定收纳位置 → 需要时找回 → 到期维护/转卖**。想法和剪藏围绕物品、家庭事务提供关联资料，暂不把产品扩成无边界的通用知识管理平台。

## 2. 先解决什么

| 顺序 | 需求方向 | 为什么现在做 | 可验收结果 |
| --- | --- | --- | --- |
| P0-A | 完整备份与安全恢复 | Android 备份缺少专业馆/想法/剪藏；桌面导入可能覆盖有效数据却返回成功 | 所有集合和附件迁移到空白隔离目录后可读；坏文件导入前后原数据哈希不变；恢复前可预览和取消 |
| P0-B | 本地服务安全 | 桌面 API 启动即监听所有网卡，备份/写入未鉴权；HTML 数据未转义 | 未配对设备不能读写；局域网服务需显式开启；任意名称只显示文本；服务关闭后不可访问 |
| P1-A | 双端数据契约与同步 | 桌面合并丢字段、遗漏证照，同 ID 馆条目更新不完整 | 所有集合/字段往返一致；重复同步不新增重复项；删除不会复活；并发编辑有清晰规则及冲突反馈 |
| P1-B | 统一收集箱 | 已有 OCR、剪藏、想法，但能力分散；截图自动监听仍有权限风险 | 分享文字/链接/照片到同一待整理箱；先保存再补分类；失败时保留原件、可重试，不能静默丢弃 |
| P2-A | 找回与关联 | 已有全局搜索和 452 关联计划 | 搜一个物品能看到位置、发票、说明书、保修与相关笔记；关联可双向跳转，删除关联不删除原记录 |
| P2-B | 提醒与周期维护 | 已有临期、耗材、药品、订阅等模型 | 同一事项不重复轰炸；能完成、延后、关闭；过期处理和提醒状态跨重启一致 |
| P3 | 桌面体验收敛 | Swing、HTTP 看板、Native 壳并存，打包完成不等于可用安装器 | 确定一个主要启动路径；引擎随壳启动/退出；安装升级保留数据；在干净 Windows 环境验证 |

实现证据与风险定位：
- P0-A：[BackupCodec.kt](../app/src/main/java/com/kfaino/diapertracker/BackupCodec.kt)、[DesktopDataStore.kt](../desktop/src/main/kotlin/com/kfaino/collector/desktop/storage/DesktopDataStore.kt)，基线 R2/R3。
- P0-B：[EmbeddedWebServer.kt](../desktop/src/main/kotlin/com/kfaino/collector/desktop/server/EmbeddedWebServer.kt)，基线 R1/R5。
- P1-A：[DesktopSyncMergeEngine.kt](../desktop/src/main/kotlin/com/kfaino/collector/desktop/storage/DesktopSyncMergeEngine.kt)，基线 R4。
- P1-B：[SmartIntakeHelper.kt](../app/src/main/java/com/kfaino/diapertracker/SmartIntakeHelper.kt)、[ScreenshotWatcherHelper.kt](../app/src/main/java/com/kfaino/diapertracker/ScreenshotWatcherHelper.kt)，基线 R8。
- P2：[GlobalSearchDialog.kt](../app/src/main/java/com/kfaino/diapertracker/GlobalSearchDialog.kt)、[VaultRepositories.kt](../app/src/main/java/com/kfaino/diapertracker/VaultRepositories.kt)、[452 当前描述](stage-current.md)。
- P3：[Main.kt](../desktop/src/main/kotlin/com/kfaino/collector/desktop/Main.kt)、[打包脚本](../scripts/Package-CollecterStandalone.ps1)，基线 R7。

P0-A 与 P0-B 均为下一次真实数据使用/交付前的阻断项，不建议等新 UI 完成后再修。

## 3. 对现有 Stage 452 的建议

当前 452 同时包含跨维关联、桌面拖拽/Spotlight、全量同步，实际牵涉不同层次。建议拆分，不再以一个阶段包办：

1. **数据可靠性阶段**：备份清单、附件、导入校验、回滚、网络鉴权。先恢复正确，再谈同步。
2. **同步契约阶段**：统一 ID、schemaVersion、createdAt/updatedAt、删除标记；指定时钟偏差和冲突策略，不直接把创建时间当作编辑版本。删除传播、幂等、字段保留都要用真实双端样例验证。
3. **收集与找回阶段**：一个收集箱、一套跨类型搜索、物品与资料的双向关联。桌面 Spotlight 放到普通搜索确实解决找回需求之后。

以上是建议拆分，不将旧 Stage 的历史“完成”状态自动重写，也不虚构新的完成记录。

## 4. 技术演进选择

- **先统一序列化和契约，再考虑数据库迁移。** 当前 Android 和 desktop 分别手写编解码，已出现字段遗漏；优先抽取可由双方测试的纯 Kotlin 数据契约/编解码层。不能只建立共享模型而继续保留多个独立导出器。
- **是否迁移 SQLite 由实测决定。** 记录数增加后的整文件保存耗时、查询延迟、跨集合事务需求足以说明问题时，再写带旧数据迁移与回滚的 Spec。当前已有的数据损坏风险不能等待数据库重构才修。
- **智能能力优先用于减少录入。** OCR、字段建议、重复项提示先给预览再确认。自动改写原始资料、自动删除、未经确认上传家庭证照不作为默认行为。引入远程模型前明确哪些数据离开设备、如何撤回授权。
- **保持离线核心可用。** 无网络时仍能保存、查看、编辑与搜索；网络恢复后再处理同步队列。云服务不可成为找回家庭资料的前置条件。

依据：[Android 数据门面](../app/src/main/java/com/kfaino/diapertracker/DataStore.kt)、[Android 编解码](../app/src/main/java/com/kfaino/diapertracker/BackupCodec.kt)、[桌面编解码](../desktop/src/main/kotlin/com/kfaino/collector/desktop/storage/DesktopDataStore.kt)、[当前输入规则](../app/src/main/java/com/kfaino/diapertracker/SmartIntakeHelper.kt)。

## 5. 暂缓的需求

暂缓新增更多专业馆、完整 macOS 发行、复杂桌面悬浮窗、通用 AI 聊天和大规模 UI 重写。这些不是永久取消，而是在备份、安全和核心使用闭环稳定前缺少足够优先级。

如果主要使用者是你本人，先围绕最常用的三个场景打磨，保留其他馆但提供隐藏入口；如果准备交给家人使用，优先验证无需解释就能录入和找回。多人共享涉及权限、误删恢复、审计和同步边界，应单独设计，不能默认共用一个 WebDAV 密码就完成家庭协作。

## 6. 如何验证方向是否正确

建议先进行 7–14 天自用记录，不启用远程行为采集：
- 每次想记录时有没有成功存下？失败来自权限、字段太多还是分类难选？
- 一周后能否找回？记录搜索词和找不到的原因。
- 实际用到哪些馆？低频馆先降入口权重，不立即新增同类功能。
- 提醒是否导致了实际处理，还是被直接忽略？

可作为试验目标（**不是目前实测数据**）：普通文字/照片保存 3 步以内；典型找回任务 10 秒内；固定全类型恢复样例字段/附件 100% 一致；无效导入原数据 100% 保留。性能目标应固定设备与数据集后再定，不把 README 的“毫秒级”当作测量。

## 7. 改名记录及边界

- 用户于本次明确要求将 DiaperTracker 改为 Collecter；Gradle rootProject.name 已更新，AGENTS/GEMINI/AQ 同步说明此次授权替代旧命名约定。
- Android applicationId/namespace、包路径、主题资源名、持久化键、版本号、签名配置、远程仓库和更新源不随品牌名批量替换。
- 应用展示名原已是 Collecter；历史诊断基线保留改名前事实。
- 文件夹改名尝试被 Windows 拒绝，错误为“being used by another process”。随后用户已自行改名为 `Collector`；保持这一实际路径，不再次改名。未复制仓库、未删除目录、未强杀占用进程。
- Git 状态仍被 dubious ownership 检查阻断；未修改全局信任配置。此项不是编译失败。
- 本节记录命名与规划阶段；后续已按用户授权实施，见 Stage 453。没有发布新版本。

