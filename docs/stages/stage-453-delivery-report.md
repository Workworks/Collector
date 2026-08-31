# Stage 453 实施与验收报告

> 2026-08-31 文档刷新：本文保留原验收时间与结果；随后 WebDAV 已增加公网与双端恢复验证，见 [后续报告](../stages/webdav-integration-report.md)。当前状态见 [当前阶段](../stage-current.md)，不得将旧未测项直接当作当前未实现。

日期：2026-08-30。**结论：P0–P2 主要实现和自动化回归完成；整体任务尚未全部验收完成，P3 原生 Windows 交付受阻。** 不发布新版本，不把开发包称为安装器。

## 完成与修改

- **完整备份与安全恢复**：共享 `BackupDocument` 校验 JSON、集合类型、ID、附件哈希和容量；附件按内容寻址还原到私有目录。Android 保存所有专业馆、想法、剪藏、收集箱、关联、提醒、账本与套装；使用三套 SharedPreferences 恢复日志处理失败及中断回滚。桌面原子替换及失败回滚，旧备份缺少的集合保持原样。证据：[BackupDocument](../../shared/src/main/kotlin/com/kfaino/collecter/core/BackupDocument.kt)、[CompleteBackupStore](../../app/src/main/java/com/kfaino/diapertracker/CompleteBackupStore.kt)、[DesktopDataStore](../../desktop/src/main/kotlin/com/kfaino/collector/desktop/storage/DesktopDataStore.kt)。
- **恢复入口**：Android 使用系统文件选择器导出/导入，并在恢复前预览；保留旧粘贴入口。修正 WebDAV Triple 返回值解包，避免把提示文字当作备份。桌面本地文件和 WebDAV 恢复先确认。证据：[ProfileFragment](../../app/src/main/java/com/kfaino/diapertracker/ProfileFragment.kt)、[DesktopBackupActions](../../desktop/src/main/kotlin/com/kfaino/collector/desktop/ui/DesktopBackupActions.kt)、[DesktopWebDavHelper](../../desktop/src/main/kotlin/com/kfaino/collector/desktop/sync/DesktopWebDavHelper.kt)。
- **本地服务安全**：桌面默认只监听回环，`--lan` 才对局域网开放；每次运行生成访问密钥，保护读写 API，检查 Host/Origin；有界读取、HTML 文本转义，关闭释放服务。Android 两个局域网入口增加配对认证，分享页面不再宣传“免密”。HTTP 仍未加密，只用于可信网络。证据：[EmbeddedWebServer](../../desktop/src/main/kotlin/com/kfaino/collector/desktop/server/EmbeddedWebServer.kt)、[LanHttp](../../shared/src/main/kotlin/com/kfaino/collecter/core/LanHttp.kt)、[LanSyncHelper](../../app/src/main/java/com/kfaino/diapertracker/LanSyncHelper.kt)、[LanShareServer](../../app/src/main/java/com/kfaino/diapertracker/LanShareServer.kt)。
- **同步契约**：抽出 `shared` 模块统一字段别名、未知字段保留、编辑时间、删除标记和冲突快照；两端运行时合并均使用共享实现。中文订阅周期可在桌面编辑后保留。清空记录也写删除标记，避免复活。证据：[SnapshotSync](../../shared/src/main/kotlin/com/kfaino/collecter/core/SnapshotSync.kt)、[WireAliases](../../shared/src/main/kotlin/com/kfaino/collecter/core/WireAliases.kt)、[JsonCollectionWriter](../../app/src/main/java/com/kfaino/diapertracker/JsonCollectionWriter.kt)。
- **收集与找回**：Android 系统分享文字、链接和图片先保存原件；“我的”增加收集箱、找回与关联、处理提醒入口。可保留原文后整理标题，图片 OCR 失败可重试；不默认启动全盘截图监听。统一搜索读取各集合，支持资料双向关联及只解除关系。证据：[CollectShareActivity](../../app/src/main/java/com/kfaino/diapertracker/CollectShareActivity.kt)、[CollectionWorkspace](../../app/src/main/java/com/kfaino/diapertracker/CollectionWorkspace.kt)、[CollectionWorkspaceDialog](../../app/src/main/java/com/kfaino/diapertracker/CollectionWorkspaceDialog.kt)、[ScreenshotWatcherHelper](../../app/src/main/java/com/kfaino/diapertracker/ScreenshotWatcherHelper.kt)。
- **提醒闭环**：资产、订阅和已有专业馆聚合提醒采用稳定事项/周期标识；同周期去重，可完成、延后一天、关闭与重启用，状态进入备份和同步。这里的“完成”是提醒处理状态，不会自动更改药品消耗或维保业务记录。证据：[WorkspaceRecords](../../shared/src/main/kotlin/com/kfaino/collecter/core/WorkspaceRecords.kt)、[NotificationHelper](../../app/src/main/java/com/kfaino/diapertracker/NotificationHelper.kt)、[VaultAlertAggregator](../../app/src/main/java/com/kfaino/diapertracker/VaultAlertAggregator.kt)。
- **桌面启动与构建**：主入口支持 `--native`，JVM 管理 Native 子进程及服务退出；WebView2 用户目录独立于安装位置，初始化失败显式失败。脚本必须编译精确版本，缺工具链不得复用旧 EXE 冒充成功。`-SkipNativeBuild` 只生成明确标识的 JVM 开发包。证据：[Main](../../desktop/src/main/kotlin/com/kfaino/collector/desktop/Main.kt)、[CollecterWindow.cpp](../../desktop/src/main/native/webview2/CollecterWindow.cpp)、[打包脚本](../../scripts/Package-CollecterStandalone.ps1)、[原生构建脚本](../../scripts/Build-DesktopWebViewHost.ps1)。C++ 本轮未编译成功，以上仅为源码变更。

Android `applicationId/namespace=com.kfaino.diapertracker`、版本 `4.3.2 / 39` 不变。用户实际文件夹为 `Collector`，产品名 `Collecter`。未操作真实家庭数据、未上传备份、未更改 Git 信任设置。Git 所有权检查仍阻止状态读取，未提交或丢弃用户已有修改。

## 自动化验证

| 验证 | 结果 | 证据 |
| --- | --- | --- |
| Android 单元测试 | 45 / 45 | app/build/test-results/testReleaseUnitTest |
| 共享契约 | 12 / 12 | shared/build/test-results/test |
| 桌面存储、合并、真实 HTTP | 12 / 12 | desktop/build/test-results/test |
| Android 14 设备存储测试 | 4 / 4 | app/build/outputs/androidTest-results/connected/debug |
| 完整 selfcheck | 六项通过 | [原始输出](evidence-453/selfcheck.log) |
| Android Release 编译 | PASS | selfcheck 中执行真实 assembleRelease |
| JVM 开发包 | PASS | [打包日志](evidence-453/desktop-package.log) |
| Windows 原生编译 | BLOCKED | [原始失败](evidence-453/native-build.log)：MSVC x64 build tools are not installed |

设备测试使用独立 ContextWrapper 偏好前缀与缓存目录，覆盖附件搬迁、无效导入保持原数据、删除不复活、关联解除保留原记录、提醒持久化和中断恢复日志的类型还原。见 [ReliableStorageDeviceTest](../../app/src/androidTest/java/com/kfaino/diapertracker/ReliableStorageDeviceTest.kt)。HTTP 测试真实打开临时端口，覆盖匿名/错误密钥拒绝、授权读取、跨来源拒绝、恶意 HTML 转义、非法恢复及关闭释放端口，见 [ServerSecurityTest](../../desktop/src/test/kotlin/com/kfaino/collector/desktop/server/ServerSecurityTest.kt)。未改弱已有测试断言或 selfcheck。

所有通过数共 **73 项**，无失败。汇总和 SHA-256 见 [verification.json](evidence-453/verification.json)；最终 Gradle 日志见 [gradle-final.log](evidence-453/gradle-final.log)。过程曾发现周期字符串归一化破坏旧值往返，修复实现后重新通过原断言，未靠修改断言通过。

## Android 界面冒烟

环境：本次新启动的只读 QA AVD，Android 14/API 34，`emulator-5556`；不使用物理设备与已有用户数据。

1. 安装 APK、启动主页并处理系统通知权限弹窗。
2. 通过 ACTION_SEND 分享合成文字 `QA-453-original-manual`。
3. “我的 → 收集箱”显示 1 条待整理记录；打开可见原文，关联入口可见。见 [列表 XML](evidence-453/android-inbox.xml)、[原文 XML](evidence-453/android-record.xml)、[截图](evidence-453/android-inbox.png)。截图已人工视觉检查，文字和按钮可读。
4. 在相同包名/签名下以 Release APK 覆盖本次 Debug APK；版本仍为 4.3.2/39，重新进入收集箱，原分享记录保留。见 [覆盖安装后的 XML](evidence-453/android-upgrade.xml)。这不是历史生产签名升级验收。
5. 此轮没有观察到 AndroidRuntime 崩溃输出，见 [运行观察](evidence-453/android-smoke-notes.md)。这不等于所有交互都通过。

补充实际操作：
- 通过 Android content URI 分享本次合成截图，复制为私有附件后运行本地 OCR，识别文字实际显示。见 [图片收集](evidence-453/android-photo.xml)、[OCR 结果](evidence-453/android-ocr.xml)。第一次 shell 分享未包含有效 URI 授权被拒绝且旧记录保持；补齐合法授权后成功，没有扩大应用媒体权限。
- 界面中建立图片→文字关联、从关系打开文字，文字侧显示反向关联；解除后原文仍在、关系变为 0。见 [建立](evidence-453/android-link-created.xml)、[反向跳转](evidence-453/android-link-reverse.xml)、[解除](evidence-453/android-unlinked.xml)。
- 系统文件选择器打开合成备份 `{"schemaVersion":2,"inbox":[]}`，显示将替换收集箱的预览；点击取消后两条记录和 OCR 状态仍在。见 [预览](evidence-453/android-restore-preview.xml)、[取消后记录](evidence-453/android-restore-cancel.xml)。
- 桌面 WebDAV 下载使用回环 HTTP fixture 实测：先预览并取消，原数据文件字节不变；再确认，远端样例成功恢复。见 [WebDavPreviewTest](../../desktop/src/test/kotlin/com/kfaino/collector/desktop/sync/WebDavPreviewTest.kt)、[测试日志](evidence-453/webdav-regression.log)，不访问真实云盘。

未执行：第三方照片应用分享兼容矩阵、OCR 损坏图片完整交互、真实 WebDAV 服务验收、跨物理设备全业务字段往返、长时间闹钟调度、桌面 Swing/WebView2 人工交互。不能以本节或单测替代这些验收。

## 逐条验收与剩余任务

| 标准 | 状态 | 说明 |
| --- | --- | --- |
| AC01 无效输入保护 | PASS（回归范围） | 共享、桌面与真实 Android 存储用例覆盖；不是完整安全审计 |
| AC02 全集合/附件恢复 | PASS（固定样例） | 全集合与已知附件字段搬迁；设备测试校验原始字节 |
| AC03 预览与取消 | PASS（指定路径） | Android SAF 预览取消实测；桌面 WebDAV 本地 HTTP fixture 覆盖确认与取消；真实云盘未接入 |
| AC04 认证/Origin/关闭 | PASS（桌面 HTTP、共享解析器） | Android 原始 socket 的压力与断连测试未完成 |
| AC05 转义/大小/版本 | PASS（指定回归） | 真实 HTTP 及共享字节读取测试；未做负载性能测试 |
| AC06 全字段双向往返 | PARTIAL | 共享集合、未知字段、字段别名及桌面周期回归通过；真实两台设备全业务字段验收仍需补齐 |
| AC07 幂等/删除/冲突 | PASS（回归范围） | 单测与 Android 删除测试；冲突保留双版本、摘要提示，不含独立冲突编辑器 |
| AC08 收集入口与原件 | PARTIAL | 文字/授权图片分享与 OCR 成功实际通过；损坏图片重试及第三方应用兼容矩阵待验收 |
| AC09 关联与跳转 | PASS（固定样例） | 持久化回归及建立、反向跳转、解除保留原文的 Android UI 实测通过 |
| AC10 提醒闭环 | PARTIAL | 周期规则与设备持久化通过；实际长时调度、通知交互仍未验收 |
| AC11 桌面构建与生命周期 | PARTIAL | JVM 产物/版本门禁通过；Native 生命周期变更未编译验证 |
| AC12 干净 Windows 安装/升级 | BLOCKED | 缺 MSVC x64 工具链及干净 Windows 验收环境；无原生安装器或签名发布 |

下一步按优先级：先补 AC06/08/10 尚缺的端到端验收（全部用合成数据，不接入真实云盘）；再在安装 MSVC x64 与 Windows SDK 的构建环境验证原生壳，制作含运行时的安装交付并进行干净 Windows 安装、升级和退出验证。当前 JVM 包要求 Java 17，不是无需环境依赖的正式产品安装器。未实现“所有任务全部完成”的验收状态。

已知限制：整份 JSON 与附件仍有内存开销，Android 部分预览/导入在主线程执行，大备份需测卡顿；并发和请求洪泛未做压力测试；同步时钟偏差仍采用确定性选胜并保留冲突副本，不能声称自动解决业务冲突；不同账本中人工使用相同资产 ID 可能令关联引用歧义。暂缓的新专业馆、通用 AI、macOS 与大规模界面重写未开展。

## 产物

- Android 自用签名 APK：[app-release.apk](../../app/build/outputs/apk/release/app-release.apk)。沿用项目既有调试签名，不是新生产签名发行。
- 桌面 JVM 开发包：`dist/desktop/4.3.2-jvm-development-20260830125444`；启动 `Start-Collecter.cmd`，需要 Java 17，默认只监听本机。
- Native 原生包：未生成；未使用旧 EXE 顶替。

## 自检原始输出

```text

=== GEMINI.md §4 交付前自检 ===

[1/6] 编译 assembleRelease ...
[2/6] 单元测试 testReleaseUnitTest ...
[3/6] 检查布局硬编码颜色 ...
[4/6] 检查空 catch ...
[5/6] 检查版本号一致性 ...
[6/6] 检查架构文档覆盖率 ...

--- 以下内容整段复制进汇报，不要改写、不要只贴通过项 ---

#### §4 交付前自检结果

<sub>selfcheck 指纹 `3556D7C69A03` · 若与上次汇报不同，说明验收脚本被改动过，必须在汇报中说明原因</sub>

| # | 检查项 | 实测结果 | 判定 |
| :-: | :--- | :--- | :--- |
| 1 | assembleRelease 编译 | BUILD SUCCESSFUL | ✅ 通过 |
| 2 | 单元测试 testReleaseUnitTest | 45 个用例，0 个失败 | ✅ 通过 |
| 3 | 布局硬编码颜色 | 共 8 处（scanner 相机遮罩 8 处属合理例外，其余 0 处） | ✅ 通过 |
| 4 | 空 catch（含仅注释） | 0 处 | ✅ 通过 |
| 5 | 版本号三处一致 | app=4.3.2 / README=4.3.2 / desktop=4.3.2 | ✅ 通过 |
| 6 | 模块文档覆盖 | 109 个模块，0 个未记录 | ✅ 通过 |

> ✅ 六项全部通过。


<!-- ↑ 以上为脚本输出，属客观证据，一个字都不要改 -->

#### 桌面端状态（脚本无法判定，由你在汇报中另起一节填写）

> 脚本不知道你这次改了什么，所以这一节**不在上面的证据块里**，需要你自己写：
> 「已跟进」或「未跟进 + 具体原因」。禁止以修改版本号冒充全平台交付（铁律 2）。

```
