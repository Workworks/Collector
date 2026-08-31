# Stage 456：原签名恢复、回归与待办推进

2026-08-31。**原签名阻塞已解除，但全部待办尚未完成；未提高版本号、未公开发布。** 用户明确要求“完成后升级版本”，不能把外部前置或未完成验收删掉后宣布满足条件。当前 Android 4.3.3/40、桌面 JVM 4.3.2 保持不变。

## 已完成与证据

| 工作 | 实际结果 | 文件证据 |
| --- | --- | --- |
| 原签名恢复 | 用户提供的仓库外 keystore 与旧 APK 指纹相同；新 release 不再默认使用本机 debug 密钥 | [签名说明](../releases/signing-456.md)、[最终证书检查](evidence-456/signature-final.log)、[构建配置](../../app/build.gradle.kts) |
| 升级兼容 | 安装原 v4.3.0，注入公开 QA 记录，`install -r` 覆盖，不卸载/清数据；新仓储读到原 marker 和记录 | [安装日志](evidence-456/upgrade-install.log)、[升级前](evidence-456/upgrade-before.xml)、[升级后](evidence-456/upgrade-after.xml)、[设备断言](evidence-456/android-upgrade-family-final.log) |
| 附件失败回滚 | 只删除本次新建副本；既有附件、原文件和业务记录保持不变 | [实现](../../desktop/src/main/kotlin/com/kfaino/collector/desktop/ui/DesktopWorkbench.kt)、[回归](evidence-456/attachment-rollback.xml) |
| 家庭会话 | 过期 token 不再永久占用配额；重新签发不继承旧共享，明确授权后可访问 | [权限实现](../../desktop/src/main/kotlin/com/kfaino/collector/desktop/server/FamilyAccess.kt)、[回归](evidence-456/family-access.xml) |
| Android 家庭连接 | 修复 cleartext 拒绝，真实 Android 客户端访问桌面：viewer 拒写、editor 成功，敏感字段不泄露 | [客户端](../../app/src/main/java/com/kfaino/diapertracker/FamilyClientDialog.kt)、[失败证据](evidence-456/android-upgrade-family.log)、[通过证据](evidence-456/android-upgrade-family-final.log) |
| 带凭据传输边界 | 家庭和双端 WebDAV/历史客户端拒绝公网 HTTP；HTTP 仅私有 IPv4/loopback/localhost，HTTPS 保持系统证书校验；Android WebDAV 禁止跟随重定向 | [地址策略](../../shared/src/main/kotlin/com/kfaino/collecter/core/FamilyEndpoint.kt)、[策略测试](../../shared/src/test/kotlin/com/kfaino/collecter/core/FamilyEndpointTest.kt)、[Android WebDAV](../../app/src/main/java/com/kfaino/diapertracker/WebDavSyncHelper.kt) |
| 固定样本压力 | JVM 解析器接受恰好 64 MiB，拒绝多 1 字节；1000 条记录、100 次批量操作保留未知字段与审计，首次记录 26645 ms | [边界/压力 XML](evidence-456/backup-boundary.xml)；不是 Android 内存或长期压力结论 |
| 公网备份复核 | 临时隧道匿名/错误密码拒绝、PROPFIND/HEAD、PUT/GET、过期条件写 412、实际桌面 Helper 恢复均通过，QA 目录已清理 | [公网日志](evidence-456/public-webdav.log)；未改生产服务或配置 |
| 离线归档 | 手动脚本验证复制前后哈希、磁盘阈值、唯一归档和 manifest；不自动删除历史 | [脚本](../../scripts/Archive-CollecterBackup.ps1)、[手册](../manuals/backup-operations.md)、[成功](evidence-456/archive.log)、[低空间拒绝](evidence-456/archive-low-space.log) |
| 隐私与重试规划 | 完成威胁模型、凭据/业务库迁移顺序及持久队列状态决策；更正“没有应用锁”描述 | [设计结论](../development/privacy-sync-plan-456.md)；不是数据库加密或后台队列实现 |

## 界面与设备验收范围

隔离 API 34 模拟器实际打开原签名 release 工作台，显示升级保留的 Upgrade QA；取消收集后仍为 1 条；输入无匹配词后 0 条、Upgrade 后 1 条；保存搜索后列表出现 Upgrade。证据：[截图](evidence-456/android-workbench.png)、[收集弹窗](evidence-456/android-collect-dialog.xml)、[取消](evidence-456/android-collect-cancel.xml)、[零结果](evidence-456/android-search-empty.xml)、[命中](evidence-456/android-search-hit.xml)、[保存搜索](evidence-456/android-saved-search.xml)。仅 QA 数据，不是物理真机或所有 UI 流程。

桌面 Computer Use 本轮曾列出 QA 窗口，但选择时窗口已消失，未完成交互，不推断操作者或原因；不标记多选/拖放/快捷键通过。其后的家庭测试使用无窗口隔离服务与 adb reverse，仅回环、不开放 LAN。首次服务读取失败来自测试时替换运行中的 JAR，保留 [环境错误日志](evidence-456/qa-jar-rebuilt-error.log)；固定 JAR 副本并重启后原断言通过，没有修改断言。

没有物理设备接入；旧版样本是对隔离沙盒注入，不声称通过旧版 UI 创建业务数据。系统长期通知、锁屏认证、分享/OCR 故障、独立网络和完整文件选择/取消恢复仍待验。

## 门禁与 AC 回填

常规回归 89 项：Android 45、shared 25、desktop 19；87 通过、2 个 opt-in 跳过、0 失败。[计数](evidence-456/test-counts.json)、[最终构建](evidence-456/final-build.log)。桌面公网专项另 1 项通过，不混入常规计数。API 34 的升级/家庭 2 项、已有存储 4 项分两次通过：[升级家庭](evidence-456/android-upgrade-family-final.log)、[存储](evidence-456/android-storage.log)。

| AC | 状态 | 解释 |
| --- | --- | --- |
| AC-01 | PASS | 原签名与旧版相同，release 显式配置 |
| AC-02 | PARTIAL | 隔离覆盖升级通过，物理真机仍未验 |
| AC-03 | PARTIAL | Android 实际网络/部分 UI、HTTP 权限和会话回归通过；完整双端 UI 未验 |
| AC-04 | PARTIAL | JVM 边界与持续批量有数据；移动端极限与长时压力未完成 |
| AC-05 | PASS | 新附件失败回滚及既有文件保护通过 |
| AC-06 | PARTIAL | 归档工具演练及隐私/队列设计完成；长期监控、整体加密和持久队列未实现 |
| AC-07 | BLOCKED | MSVC 未安装且当前非管理员；固定域名/受管隧道配置仍缺；真机不可用 |
| AC-08 | PARTIAL | 构建/回归通过、文档与未完成项回填；自检 5/6，整体交付条件未满足，不升版本冒充完成 |

Native 本轮实际运行构建并失败：[构建日志](evidence-456/native-build.log)、[非管理员与工具链探测](evidence-456/toolchain-environment.log)。没有调用 UAC、修改系统安全设置或复用旧 EXE。当前公网只有临时 Quick Tunnel，本轮再次往返通过也不等于拥有固定域名。

签名配置缺失的负向门禁实际拒绝构建：[拒绝日志](evidence-456/signing-missing-config.log)。这是预期失败，不是绿色构建；之后恢复原签名配置重新生成最终候选：[最终打包](evidence-456/final-signed-package.log)。候选保存在 `dist/stage-456-candidate/`，manifest 明确物理设备未验、未公开发布、全部任务未完成。自建家庭测试进程、adb reverse 和隔离模拟器已关闭，不干预生产 WebDAV 或其他进程。

Android 为可信 LAN 功能显式设置 `usesCleartextTraffic=true`，但带凭据的上述入口额外限制地址；**该 Manifest 开关是全应用设置，不是全应用主机白名单**，其他传输需分别审计。依据 [Android 官方网络安全说明](https://developer.android.com/privacy-and-security/security-config)。已有主界面生物识别入口不能等同全部入口保护或数据库加密，详见隐私设计。

## 未关闭与版本决策

优先补物理设备覆盖升级、完整双端 UI、独立网络/后台与压力验收；再完成 Native 工具链与安装、固定公网和长期运维。规划中的加密迁移/持久队列单独实施，不能用设计文档替代代码。具体账本见 [TODO](../TODO.md)。本轮没有 Git 提交/推送、tag、GitHub Release 或版本号提升；不再将“缺原签名”列为阻塞。

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


桌面端状态：已跟进 JVM 功能与回归，Native 未交付。第 5 项为既有版本分离，适用 TECH_DEBT_AUDIT P0-2；不改验收脚本或提高未验收平台版本伪造通过。

