# Stage 455：工作台与安全协作交付记录

2026-08-31。七个演化方向的最小实现已落地，自动化主要通过；完整界面、真实多设备与发布验收尚未关闭。本轮未改包名、版本、签名配置，未发布、未提交 Git，未修改相邻 WebDav 服务或用户生产数据。

## 实现与文件证据

| 方向 | 本轮结果 | 实现证据 |
| --- | --- | --- |
| 收集整理 | 快速收集、原文重复提示、金额/日期/型号建议预览、批量整理；不自动删重或覆盖原文 | [CollectionWorkbench](../../shared/src/main/kotlin/com/kfaino/collecter/core/CollectionWorkbench.kt:44)、[Android 工作台](../../app/src/main/java/com/kfaino/diapertracker/WorkbenchActivity.kt:87) |
| 搜索关联 | 跨集合关键词与位置筛选、保存查询、最近打开排序、关联资料；账本条目引用包含账本 ID | [共享查询](../../shared/src/main/kotlin/com/kfaino/collecter/core/CollectionWorkbench.kt:15)、[桌面入口](../../desktop/src/main/kotlin/com/kfaino/collector/desktop/ui/DesktopWorkbench.kt) |
| 生命周期 | 购买/维护/借还/转卖/报废、责任人、历史、下一次维护；禁止无效转换；维护提醒完成会记录事件 | [状态规则](../../shared/src/main/kotlin/com/kfaino/collecter/core/CollectionWorkbench.kt:141)、[通知](../../app/src/main/java/com/kfaino/diapertracker/NotificationHelper.kt)、[提醒完成](../../app/src/main/java/com/kfaino/diapertracker/CollectionWorkspaceDialog.kt) |
| 备份可理解性 | 双端本地加密导出/恢复、状态与错误、历史下载预览后恢复到本机、冲突只读；不自动覆盖远端 | [加密协议](../../shared/src/main/kotlin/com/kfaino/collecter/core/EncryptedBackup.kt)、[历史客户端](../../shared/src/main/kotlin/com/kfaino/collecter/core/WebDavHistoryClient.kt)、[桌面备份入口](../../desktop/src/main/kotlin/com/kfaino/collector/desktop/ui/DesktopBackupActions.kt) |
| 桌面操作 | JVM 整理窗口、批量编辑、Ctrl+F、文件选择/拖入确认与私有副本 | [DesktopWorkbench](../../desktop/src/main/kotlin/com/kfaino/collector/desktop/ui/DesktopWorkbench.kt)、[启动菜单](../../desktop/src/main/kotlin/com/kfaino/collector/desktop/Main.kt) |
| 家庭协作 | 显式成员授权、viewer/editor、12 小时会话、撤销、敏感与证照排除、写入审计；Android 客户端入口 | [权限](../../desktop/src/main/kotlin/com/kfaino/collector/desktop/server/FamilyAccess.kt)、[HTTP 路由](../../desktop/src/main/kotlin/com/kfaino/collector/desktop/server/EmbeddedWebServer.kt)、[Android 客户端](../../app/src/main/java/com/kfaino/diapertracker/FamilyClientDialog.kt) |
| 隐私与规模 | 加密备份互通、家庭字段白名单、固定数据集测量；暂不迁移数据库 | [加密回归](../../shared/src/test/kotlin/com/kfaino/collecter/core/EncryptedBackupTest.kt)、[工作流回归](../../shared/src/test/kotlin/com/kfaino/collecter/core/CollectionWorkbenchTest.kt) |

Android 使用 [WorkbenchRepository](../../app/src/main/java/com/kfaino/diapertracker/WorkbenchRepository.kt) 在原备份事务锁内提交；桌面通过 [DesktopDataStore](../../desktop/src/main/kotlin/com/kfaino/collector/desktop/storage/DesktopDataStore.kt) 的现有导入路径保存。新增 saved_searches 已进入 BackupDocument 与 Android 完整备份映射。操作说明见 [使用手册](../manuals/workbench.md)。

## 实测与 AC 回填

| AC | 状态 | 证据与边界 |
| --- | --- | --- |
| AC-01 | 自动化通过；入口交互待补 | 共享原文/未知字段保留、重复/建议、批量操作回归；双端仓储接入，非完整 UI 验收 |
| AC-02 | PARTIAL | 搜索/查询保存/关联自动化通过；实际 JVM 窗口显示 2 条隔离记录及关联数量；搜索快捷键和完整操作未验完 |
| AC-03 | PASS（自动化） | 非法转换不改数据、生命周期及备份持久化通过；系统长期提醒另列待办 |
| AC-04 | PARTIAL | JVM→Android→JVM 实际加密往返通过；错误密码/篡改、仓储保护通过；双端完整文件选择器与取消 UI 流程未验 |
| AC-05 | PARTIAL | 实际附件复制/备份恢复仓储测试通过，窗口已打开；拖拽、多选、取消交互未验完 |
| AC-06 | PASS（HTTP 自动化） | 实际 HTTP 覆盖 viewer 拒写、editor 允许、私人/敏感过滤、禁止改权限、禁止全量备份、审计与撤销；非真实家庭多设备验收 |
| AC-07 | PASS（固定样本） | 20,000 条查询返回 200 条，本机单次 403 ms；不是 Android 卡顿、长期压力或数据库容量结论 |
| AC-08 | PARTIAL | Gradle 编译与回归通过；selfcheck 5/6，签名门禁拒绝；真机、Native 安装与发布仍未通过 |

- 最终构建：[final-delivery.log](evidence-455/final-delivery.log)，命令 `gradlew :shared:test :desktop:test :app:testReleaseUnitTest :app:assembleRelease :desktop:jar`。
- 常规用例共 84 项：Android 45、shared 22、desktop 17；82 通过、2 个 opt-in 跳过、0 失败。[计数](evidence-455/test-counts.json)、[工作流 XML](evidence-455/workbench-tests.xml)、[家庭 HTTP XML](evidence-455/family-http-tests.xml)。跳过用例不计通过。
- 隔离 API 34 模拟器：加密/偏好存储专项 1 项通过，93.442 秒；已有存储回归 4 项通过；跨平台加密专项另 1 项通过，92.648 秒。这是三个不同运行，不冒充一个测试套件。[Android 加密](evidence-455/android-device.log)、[存储](evidence-455/android-storage-regression.log)、[跨端设备](evidence-455/android-cross-crypto.log)、[桌面回读](evidence-455/cross-platform-crypto.log)。KDF 在模拟器耗时明显，已放到后台并显示进度，真机延迟待测。
- 跨端样本仅含公开 QA 数据及测试密码；[生成/回读程序](evidence-455/BackupInterop.java) 与测试 fixture 可复现，不包含用户数据。
- 自检 [selfcheck-final.log](evidence-455/selfcheck-final.log)：5/6；唯一失败为 Android/README 4.3.3、desktop 4.3.2 的既有版本分离（TECH_DEBT_AUDIT P0-2）。未改门禁、断言或升版本伪造通过。桌面功能已跟进 JVM，Native 未交付。
- [签名门禁](evidence-455/signature-gate-final.log)：旧证书 `03d73e71…bb013a` 与候选 `98edb36c…b5097e` 不匹配，明确禁止发布。没有通过卸载绕过覆盖升级。
- 首轮桌面编译的 Swing location 同名问题已修复；[初始失败](evidence-455/compile-initial.log) 保留。设备首轮在启动未完成时失败，等待开机后原测试通过；[启动未就绪日志](evidence-455/android-device-boot-not-ready.log) 保留。
- 实际 GUI 仅观察到主窗口及整理窗口。用户按 Esc 停止 Computer Use 后未再发送桌面输入；随后“继续”只用于非 GUI 验证。自建 JVM 进程、隔离模拟器已关闭，QA 数据保留在 build 目录，不影响用户数据。
- `git diff --check` 仍列出既有 DataModels、VaultRepositories、AssetExpiryAndThresholdTest、desktop DataModels、docs/debt/P2-2 的 EOF 空行；本轮没有为格式检查改写这些已有修改。

## 已知限制与后续顺序

1. 优先补足新入口的真机与桌面交互验收，尤其取消恢复、文件拖放、旋转/后台、重复点击与慢速加密。当前不能宣称所有任务闭环。
2. 原签名恢复、物理设备覆盖升级仍是 Android 发布前提；Native MSVC 工具链和干净安装是桌面交付前提。本轮未重新运行 Native 构建，不把 JVM 构建当替代。
3. 家庭协作是会话共享：退出/过期失效，重新签发会生成新成员 ID，需要重新授权；没有长期账号/跨公网 TLS 服务。HTTP 仅用于可信局域网，默认不开放 LAN。
4. 加密只覆盖主动导出的备份，不加密既有本地数据库、WebDAV 配置或原有自动上传。没有应用锁、密码恢复后门或自动冲突合并；失败仅显式重试，无持久后台队列。
5. 桌面附件导入若中途失败，记录不提交，但已复制的附件可能成为未引用副本；后续清理必须先比对引用，不能盲删目录。
6. 当前 JSON 全量扫描与读写适合先以固定样本观察；64 MiB 客户端边界、移动端内存、长时压力和长期通知尚未验证。固定域名、运维监控及真实家庭业务验收继续保留在 [TODO](../TODO.md)。

## 自检原始输出

#### §4 交付前自检结果

<sub>selfcheck 指纹 `3556D7C69A03` · 若与上次汇报不同，说明验收脚本被改动过，必须在汇报中说明原因</sub>

| # | 检查项 | 实测结果 | 判定 |
| :-: | :--- | :--- | :--- |
| 1 | assembleRelease 编译 | BUILD SUCCESSFUL | ✅ 通过 |
| 2 | 单元测试 testReleaseUnitTest | 45 个用例，0 个失败 | ✅ 通过 |
| 3 | 布局硬编码颜色 | 共 8 处（scanner 相机遮罩 8 处属合理例外，其余 0 处） | ✅ 通过 |
| 4 | 空 catch（含仅注释） | 0 处 | ✅ 通过 |
| 5 | 版本号三处一致 | app=4.3.3 / README=4.3.3 / desktop=4.3.2 | ❌ 未通过 |
| 6 | 模块文档覆盖 | 112 个模块，0 个未记录 | ✅ 通过 |

> ⚠️ 有 **1** 项未通过。按铁律 5，必须在汇报里逐项说明：是本次改动引入的，还是既有欠账（附 TECH_DEBT_AUDIT 编号）。


<!-- ↑ 以上为脚本输出，属客观证据，一个字都不要改 -->


桌面端状态：已跟进 JVM 工作台、备份和家庭服务；Native 安装未跟进交付，当前缺工具链及实际安装验收。第 5 项是既有版本分离，适用 TECH_DEBT_AUDIT P0-2，不通过升版本伪造同步交付。

