# DiaperTracker / Collecter 项目诊断基线

> 后续实施更新：2026-08-31 已执行 [Stage 454](stages/stage-454-report.md)，包括历史备份、条件写入及 OCR 并发保护。本文仍保留诊断时证据，不将原基线重写为新实现结论。

> 2026-08-31 文档刷新：本文保留原验收时间与结果；随后 WebDAV 已增加公网与双端恢复验证，见 [后续报告](stages/webdav-integration-report.md)。当前状态见 [当前阶段](stage-current.md)，不得将旧未测项直接当作当前未实现。

> 历史快照：下文描述实施前状态，不代表当前缺陷仍存在。目录链接随用户改名更新为 Collector；代码行号保留诊断时位置，当前实现与验收请见 [Stage 453](stages/stage-453-reliable-collection.md)。

诊断日期：2026-08-30。范围：当前工作区源码、构建配置、工程约定与现有测试；不修改实现，不安装依赖，不运行应用，不导入或导出用户数据。唯一新增文件为本报告 `docs/baseline.md`。

本文“已实现”指源码存在可追踪实现，不等同于构建通过、真机验收或正式发布完成。用户只读诊断要求优先于工程协议中同步修改 TODO/Stage、执行完整构建的常规流程；本次不更新这些文件。工程依据：[docs/codex-skills.md:7](F:/LANShare/AgentWorkSpace/Collector/docs/codex-skills.md:7)、[AGENTS.md:49](F:/LANShare/AgentWorkSpace/Collector/AGENTS.md:49)。

## 1. 总体判断

项目实际产品名为 **Collecter（资产与收纳管家）**，历史工程名为 DiaperTracker，包含 Android 主应用与 JVM 桌面应用两个 Gradle 模块。Android 使用原生 View 界面和本地 JSON 存储；桌面为 Swing/FlatLaf + 内嵌 HTTP 服务，另有 Windows WebView2 C++ 宿主源码。不能仅凭 README 认定桌面已经切换为完整 Native 单机安装版。证据：[AGENTS.md:11](F:/LANShare/AgentWorkSpace/Collector/AGENTS.md:11)、[settings.gradle.kts:20](F:/LANShare/AgentWorkSpace/Collector/settings.gradle.kts:20)、[desktop/build.gradle.kts:9](F:/LANShare/AgentWorkSpace/Collector/desktop/build.gradle.kts:9)、[desktop/src/main/kotlin/com/kfaino/collector/desktop/Main.kt:26](F:/LANShare/AgentWorkSpace/Collector/desktop/src/main/kotlin/com/kfaino/collector/desktop/Main.kt:26)。

**主要质量风险集中在数据完整性和网络访问边界**：Android 备份不包含专业馆、想法和剪藏；桌面导入存在先覆盖后验证问题；桌面同步字段不完整；桌面 API 未鉴权。这些结论的逐项实现证据见第 5 节，不沿用文档中的“全量无损互通”作为验收结果。

## 2. 技术栈

| 层次 | 读取到的事实 | 文件证据 |
| --- | --- | --- |
| 构建 | Gradle Kotlin DSL；Gradle wrapper 8.5；Android Gradle Plugin 8.2.2；Kotlin 插件 1.9.22 | [build.gradle.kts:1](F:/LANShare/AgentWorkSpace/Collector/build.gradle.kts:1)；[gradle/wrapper/gradle-wrapper.properties:3](F:/LANShare/AgentWorkSpace/Collector/gradle/wrapper/gradle-wrapper.properties:3) |
| Android | compile/target SDK 34，min SDK 26；Java/Kotlin 目标 17；applicationId 为 com.kfaino.diapertracker；版本 4.3.2 / code 39 | [app/build.gradle.kts:6](F:/LANShare/AgentWorkSpace/Collector/app/build.gradle.kts:6) |
| Android UI | AppCompat、Material、ConstraintLayout、RecyclerView、ViewBinding；Activity + Fragment + Dialog + XML View | [app/build.gradle.kts:34](F:/LANShare/AgentWorkSpace/Collector/app/build.gradle.kts:34)；[app/src/main/java/com/kfaino/diapertracker/MainActivity.kt:261](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/MainActivity.kt:261) |
| 本地持久化 | Android 使用私有 SharedPreferences，手写 org.json 编解码；桌面使用内存列表 + collector_data.json/config.json，无独立数据库依赖 | [app/src/main/java/com/kfaino/diapertracker/DataStore.kt:12](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/DataStore.kt:12)；[app/src/main/java/com/kfaino/diapertracker/EntryRepository.kt:162](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/EntryRepository.kt:162)；[desktop/src/main/kotlin/com/kfaino/collector/desktop/storage/DesktopDataStore.kt:18](F:/LANShare/AgentWorkSpace/Collector/desktop/src/main/kotlin/com/kfaino/collector/desktop/storage/DesktopDataStore.kt:18)；[desktop/build.gradle.kts:13](F:/LANShare/AgentWorkSpace/Collector/desktop/build.gradle.kts:13) |
| 桌面 | Kotlin/JVM application 插件；Swing + FlatLaf 3.4.1；JDK HttpServer；另有 C++17 WebView2 宿主 | [desktop/build.gradle.kts:1](F:/LANShare/AgentWorkSpace/Collector/desktop/build.gradle.kts:1)；[desktop/src/main/kotlin/com/kfaino/collector/desktop/Main.kt:3](F:/LANShare/AgentWorkSpace/Collector/desktop/src/main/kotlin/com/kfaino/collector/desktop/Main.kt:3)；[desktop/src/main/kotlin/com/kfaino/collector/desktop/server/EmbeddedWebServer.kt:7](F:/LANShare/AgentWorkSpace/Collector/desktop/src/main/kotlin/com/kfaino/collector/desktop/server/EmbeddedWebServer.kt:7)；[scripts/Build-DesktopWebViewHost.ps1:60](F:/LANShare/AgentWorkSpace/Collector/scripts/Build-DesktopWebViewHost.ps1:60) |
| 设备能力 | ZXing 扫码、AndroidX Biometric、ML Kit 中文文本识别；相机/NFC/蓝牙相关权限 | [app/build.gradle.kts:47](F:/LANShare/AgentWorkSpace/Collector/app/build.gradle.kts:47)；[app/src/main/AndroidManifest.xml:11](F:/LANShare/AgentWorkSpace/Collector/app/src/main/AndroidManifest.xml:11) |
| 测试 | JUnit 4.13.2，桌面另用 kotlin.test；Android JVM 单测允许 Android stub 返回默认值，不能代替设备测试 | [app/build.gradle.kts:39](F:/LANShare/AgentWorkSpace/Collector/app/build.gradle.kts:39)；[app/build.gradle.kts:55](F:/LANShare/AgentWorkSpace/Collector/app/build.gradle.kts:55)；[desktop/build.gradle.kts:19](F:/LANShare/AgentWorkSpace/Collector/desktop/build.gradle.kts:19) |

依赖版本均为本地配置事实，本次没有联网评估漏洞公告、依赖新旧或商店合规。

## 3. 入口和启动方式

以下命令在项目根目录执行，**仅记录，不在本次诊断执行**。需要 JDK 17（与编译目标一致）、Android SDK 34 及相应构建工具；首次 Gradle 构建需要依赖缓存或网络。证据：[app/build.gradle.kts:8](F:/LANShare/AgentWorkSpace/Collector/app/build.gradle.kts:8)、[app/build.gradle.kts:28](F:/LANShare/AgentWorkSpace/Collector/app/build.gradle.kts:28)、[settings.gradle.kts:1](F:/LANShare/AgentWorkSpace/Collector/settings.gradle.kts:1)。

### Android

入口是 Manifest 声明的 `.MainActivity`（MAIN/LAUNCHER），初始化后进入 HomeFragment；底部切换首页、时间线、报表、个人设置。生命周期中还接入热补丁初始化、截图监听和生物锁检查。证据：[app/src/main/AndroidManifest.xml:33](F:/LANShare/AgentWorkSpace/Collector/app/src/main/AndroidManifest.xml:33)、[app/src/main/java/com/kfaino/diapertracker/MainActivity.kt:83](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/MainActivity.kt:83)、[app/src/main/java/com/kfaino/diapertracker/MainActivity.kt:113](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/MainActivity.kt:113)、[app/src/main/java/com/kfaino/diapertracker/MainActivity.kt:261](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/MainActivity.kt:261)。

```powershell
.\gradlew.bat :app:assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
adb shell am start -n com.kfaino.diapertracker/.MainActivity
```

前两条对应 README 构建/安装流程；第三条由 Manifest 入口推导，也可点击设备上的 Collecter 图标。证据：[README.md:92](F:/LANShare/AgentWorkSpace/Collector/README.md:92)、[app/src/main/AndroidManifest.xml:33](F:/LANShare/AgentWorkSpace/Collector/app/src/main/AndroidManifest.xml:33)。Release 使用 debug 签名，正式分发风险见 R7。

### JVM 桌面端

```powershell
.\gradlew.bat :desktop:run
# 或先生成包含运行依赖的 JAR，再运行
.\gradlew.bat :desktop:jar
java -jar desktop/build/libs/Collecter-Desktop-4.3.2.jar
```

入口为 `com.kfaino.collector.desktop.MainKt`；先初始化 DesktopDataStore 和 HTTP 服务，再通过 EventQueue 显示 Swing MainWindow。JAR 名和依赖打包由 Gradle jar 任务定义。证据：[desktop/build.gradle.kts:9](F:/LANShare/AgentWorkSpace/Collector/desktop/build.gradle.kts:9)、[desktop/build.gradle.kts:29](F:/LANShare/AgentWorkSpace/Collector/desktop/build.gradle.kts:29)、[desktop/src/main/kotlin/com/kfaino/collector/desktop/Main.kt:26](F:/LANShare/AgentWorkSpace/Collector/desktop/src/main/kotlin/com/kfaino/collector/desktop/Main.kt:26)。

HTTP 使用 TCP 8848，UDP 8849 用于发现应答；浏览器本机入口为 `http://127.0.0.1:8848/`，**实际监听所有 IPv4 网卡**，不是仅本机服务。Windows 默认数据目录为 `%LOCALAPPDATA%\CollecterStandalone\data`，支持 `COLLECTER_DATA_DIR` 指定隔离目录。首次启动会建目录/数据文件，故本次不启动。证据：[desktop/src/main/kotlin/com/kfaino/collector/desktop/server/EmbeddedWebServer.kt:25](F:/LANShare/AgentWorkSpace/Collector/desktop/src/main/kotlin/com/kfaino/collector/desktop/server/EmbeddedWebServer.kt:25)、[desktop/src/main/kotlin/com/kfaino/collector/desktop/server/EmbeddedWebServer.kt:35](F:/LANShare/AgentWorkSpace/Collector/desktop/src/main/kotlin/com/kfaino/collector/desktop/server/EmbeddedWebServer.kt:35)、[desktop/src/main/kotlin/com/kfaino/collector/desktop/server/EmbeddedWebServer.kt:78](F:/LANShare/AgentWorkSpace/Collector/desktop/src/main/kotlin/com/kfaino/collector/desktop/server/EmbeddedWebServer.kt:78)、[desktop/src/main/kotlin/com/kfaino/collector/desktop/storage/DesktopDataDirectory.kt:18](F:/LANShare/AgentWorkSpace/Collector/desktop/src/main/kotlin/com/kfaino/collector/desktop/storage/DesktopDataDirectory.kt:18)、[desktop/src/main/kotlin/com/kfaino/collector/desktop/storage/DesktopDataStore.kt:63](F:/LANShare/AgentWorkSpace/Collector/desktop/src/main/kotlin/com/kfaino/collector/desktop/storage/DesktopDataStore.kt:63)。

### Windows Native 壳与打包

仓库提供 `pwsh -File scripts/Build-DesktopWebViewHost.ps1` 与 `pwsh -File scripts/Package-CollecterStandalone.ps1 -Version 4.3.2`。前者依赖 MSVC x64、WebView2 SDK，生成 `desktop/build/native-window/CollecterWindow.exe`；壳导航到本机 8848，单独打开壳不能据此保证 JVM 引擎已启动。后者构建/测试并输出通用 JAR 和清单，不能把执行末尾的 Complete 当作完整安装包交付证据。证据：[scripts/Build-DesktopWebViewHost.ps1:10](F:/LANShare/AgentWorkSpace/Collector/scripts/Build-DesktopWebViewHost.ps1:10)、[scripts/Build-DesktopWebViewHost.ps1:42](F:/LANShare/AgentWorkSpace/Collector/scripts/Build-DesktopWebViewHost.ps1:42)、[desktop/src/main/native/webview2/CollecterWindow.cpp:24](F:/LANShare/AgentWorkSpace/Collector/desktop/src/main/native/webview2/CollecterWindow.cpp:24)、[scripts/Package-CollecterStandalone.ps1:17](F:/LANShare/AgentWorkSpace/Collector/scripts/Package-CollecterStandalone.ps1:17)、[scripts/Package-CollecterStandalone.ps1:48](F:/LANShare/AgentWorkSpace/Collector/scripts/Package-CollecterStandalone.ps1:48)。

## 4. 主要模块与已有功能

| 模块 | 源码确认的功能与边界 | 文件证据 |
| --- | --- | --- |
| Android 导航/UI | 首页、流水时间线、报表、个人设置；并有独立扫码 Activity | [app/src/main/java/com/kfaino/diapertracker/MainActivity.kt:261](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/MainActivity.kt:261)；[app/src/main/AndroidManifest.xml:53](F:/LANShare/AgentWorkSpace/Collector/app/src/main/AndroidManifest.xml:53) |
| 数据门面与资产仓储 | DataStore 委托 EntryRepository、空间/分类/设置仓储；资产保存、更新、删除、退役、借出归还、回忆记录；不应把界面与持久化视为统一跨端领域层 | [app/src/main/java/com/kfaino/diapertracker/DataStore.kt:12](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/DataStore.kt:12)；[app/src/main/java/com/kfaino/diapertracker/EntryRepository.kt:274](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/EntryRepository.kt:274)；[app/src/main/java/com/kfaino/diapertracker/EntryRepository.kt:359](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/EntryRepository.kt:359)；[app/src/main/java/com/kfaino/diapertracker/EntryRepository.kt:403](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/EntryRepository.kt:403) |
| 12 个专业馆 | 卡券、证照、药品、食材、荣誉、衣橱、应急、工具、绿植、宠物、藏书、茶酒；有对应仓储和打卡/消耗操作 | [app/src/main/java/com/kfaino/diapertracker/VaultRepositories.kt:23](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/VaultRepositories.kt:23)；[app/src/main/java/com/kfaino/diapertracker/VaultRepositories.kt:141](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/VaultRepositories.kt:141)；[app/src/main/java/com/kfaino/diapertracker/VaultRepositories.kt:221](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/VaultRepositories.kt:221)；[app/src/main/java/com/kfaino/diapertracker/VaultRepositories.kt:322](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/VaultRepositories.kt:322)；[app/src/main/java/com/kfaino/diapertracker/VaultRepositories.kt:436](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/VaultRepositories.kt:436)；[app/src/main/java/com/kfaino/diapertracker/VaultRepositories.kt:515](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/VaultRepositories.kt:515)；[app/src/main/java/com/kfaino/diapertracker/VaultRepositories.kt:628](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/VaultRepositories.kt:628)；[app/src/main/java/com/kfaino/diapertracker/VaultRepositories.kt:716](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/VaultRepositories.kt:716)；[app/src/main/java/com/kfaino/diapertracker/VaultRepositories.kt:802](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/VaultRepositories.kt:802)；[app/src/main/java/com/kfaino/diapertracker/VaultRepositories.kt:898](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/VaultRepositories.kt:898)；[app/src/main/java/com/kfaino/diapertracker/VaultRepositories.kt:998](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/VaultRepositories.kt:998)；[app/src/main/java/com/kfaino/diapertracker/VaultRepositories.kt:1118](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/VaultRepositories.kt:1118) |
| 想法、剪藏与搜索 | 想法/剪藏独立仓储；全局搜索；网页标题提取与 HTML 转 Markdown；截图 OCR 与文本分类。已存在代码，不代表系统截图监听已通过权限/后台场景验收 | [app/src/main/java/com/kfaino/diapertracker/VaultRepositories.kt:1221](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/VaultRepositories.kt:1221)；[app/src/main/java/com/kfaino/diapertracker/VaultRepositories.kt:1320](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/VaultRepositories.kt:1320)；[app/src/main/java/com/kfaino/diapertracker/GlobalSearchDialog.kt:57](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/GlobalSearchDialog.kt:57)；[app/src/main/java/com/kfaino/diapertracker/WebClipperEngine.kt:23](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/WebClipperEngine.kt:23)；[app/src/main/java/com/kfaino/diapertracker/ScreenshotOcrProcessor.kt:31](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/ScreenshotOcrProcessor.kt:31) |
| 智能录入 | ML Kit 图片 OCR + 文本规则解析，含价格/数量/类别猜测；此入口自然语言处理是本地规则，不据此宣称接入通用大模型 | [app/src/main/java/com/kfaino/diapertracker/SmartIntakeHelper.kt:37](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/SmartIntakeHelper.kt:37)；[app/src/main/java/com/kfaino/diapertracker/SmartIntakeHelper.kt:60](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/SmartIntakeHelper.kt:60)；[app/src/main/java/com/kfaino/diapertracker/SmartIntakeHelper.kt:136](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/SmartIntakeHelper.kt:136) |
| 空间与分析 | Canvas 空间绘制/触摸交互；二手资产分析、低库存和补货文本；订阅筛选 | [app/src/main/java/com/kfaino/diapertracker/FloorPlanView.kt:125](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/FloorPlanView.kt:125)；[app/src/main/java/com/kfaino/diapertracker/FloorPlanView.kt:208](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/FloorPlanView.kt:208)；[app/src/main/java/com/kfaino/diapertracker/AnalyticsQueries.kt:22](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/AnalyticsQueries.kt:22)；[app/src/main/java/com/kfaino/diapertracker/DataStore.kt:90](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/DataStore.kt:90) |
| 备份与云同步 | JSON 导入导出、WebDAV 测连接/上传/下载；桌面 CSV 带 UTF-8 BOM。Android 备份范围不等于整个应用数据，见 R3 | [app/src/main/java/com/kfaino/diapertracker/BackupCodec.kt:17](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/BackupCodec.kt:17)；[app/src/main/java/com/kfaino/diapertracker/WebDavSyncHelper.kt:31](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/WebDavSyncHelper.kt:31)；[app/src/main/java/com/kfaino/diapertracker/WebDavSyncHelper.kt:62](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/WebDavSyncHelper.kt:62)；[app/src/main/java/com/kfaino/diapertracker/WebDavSyncHelper.kt:110](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/WebDavSyncHelper.kt:110)；[desktop/src/main/kotlin/com/kfaino/collector/desktop/storage/DesktopDataStore.kt:890](F:/LANShare/AgentWorkSpace/Collector/desktop/src/main/kotlin/com/kfaino/collector/desktop/storage/DesktopDataStore.kt:890) |
| 局域网互通 | Android 发现、HTTP 同步和 P2P 合并；桌面资产/预警/备份/合并 API。两端合并入口已调用 ideas/clippings，但备份输出链路仍可能根本不携带这些字段 | [app/src/main/java/com/kfaino/diapertracker/LanPeerDiscovery.kt:36](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/LanPeerDiscovery.kt:36)；[app/src/main/java/com/kfaino/diapertracker/LanSyncHelper.kt:66](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/LanSyncHelper.kt:66)；[app/src/main/java/com/kfaino/diapertracker/LanSyncHelper.kt:448](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/LanSyncHelper.kt:448)；[app/src/main/java/com/kfaino/diapertracker/LanSyncMergeEngine.kt:152](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/LanSyncMergeEngine.kt:152)；[desktop/src/main/kotlin/com/kfaino/collector/desktop/storage/DesktopSyncMergeEngine.kt:115](F:/LANShare/AgentWorkSpace/Collector/desktop/src/main/kotlin/com/kfaino/collector/desktop/storage/DesktopSyncMergeEngine.kt:115)；[desktop/src/main/kotlin/com/kfaino/collector/desktop/server/EmbeddedWebServer.kt:38](F:/LANShare/AgentWorkSpace/Collector/desktop/src/main/kotlin/com/kfaino/collector/desktop/server/EmbeddedWebServer.kt:38) |
| 安全与升级 | 生物锁、提醒/桌面小组件；官方更新源优先；DEX 验签在加载前执行；补丁解压检查 canonicalPath | [app/src/main/java/com/kfaino/diapertracker/MainActivity.kt:180](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/MainActivity.kt:180)；[app/src/main/AndroidManifest.xml:59](F:/LANShare/AgentWorkSpace/Collector/app/src/main/AndroidManifest.xml:59)；[app/src/main/java/com/kfaino/diapertracker/UpdateSource.kt:35](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/UpdateSource.kt:35)；[app/src/main/java/com/kfaino/diapertracker/HotPatchEngine.kt:98](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/HotPatchEngine.kt:98)；[app/src/main/java/com/kfaino/diapertracker/PatchArchive.kt:35](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/PatchArchive.kt:35) |
| 桌面持久化/UI | 独立目录、专业馆/想法/剪藏内存数据与 JSON；Swing 窗口及 HTML 看板并存。数据模型存在不等同于所有 Android 操作均有桌面 UI | [desktop/src/main/kotlin/com/kfaino/collector/desktop/storage/DesktopDataDirectory.kt:30](F:/LANShare/AgentWorkSpace/Collector/desktop/src/main/kotlin/com/kfaino/collector/desktop/storage/DesktopDataDirectory.kt:30)；[desktop/src/main/kotlin/com/kfaino/collector/desktop/storage/DesktopDataStore.kt:29](F:/LANShare/AgentWorkSpace/Collector/desktop/src/main/kotlin/com/kfaino/collector/desktop/storage/DesktopDataStore.kt:29)；[desktop/src/main/kotlin/com/kfaino/collector/desktop/Main.kt:32](F:/LANShare/AgentWorkSpace/Collector/desktop/src/main/kotlin/com/kfaino/collector/desktop/Main.kt:32)；[desktop/src/main/kotlin/com/kfaino/collector/desktop/server/EmbeddedWebServer.kt:227](F:/LANShare/AgentWorkSpace/Collector/desktop/src/main/kotlin/com/kfaino/collector/desktop/server/EmbeddedWebServer.kt:227) |

## 5. 质量风险（按处理优先级）

P1：优先阻断数据丢失、未授权访问；P2：功能可靠性、交付及维护问题。以下为静态代码诊断，不伪称已进行攻击或运行复现。

### R1 · P1 · 桌面 HTTP API 缺少访问认证，启动即对局域网开放

`Main.kt` 无条件启动服务；服务绑定 `0.0.0.0`，注册备份导出、覆盖导入和合并接口，处理器中直接读写 store，未见 token/配对或认证过滤。可达该端口的其他设备存在读取完整备份和修改数据的路径；实际网络可达性仍受防火墙影响，不能推定公网暴露。证据：[desktop/src/main/kotlin/com/kfaino/collector/desktop/Main.kt:28](F:/LANShare/AgentWorkSpace/Collector/desktop/src/main/kotlin/com/kfaino/collector/desktop/Main.kt:28)、[desktop/src/main/kotlin/com/kfaino/collector/desktop/server/EmbeddedWebServer.kt:35](F:/LANShare/AgentWorkSpace/Collector/desktop/src/main/kotlin/com/kfaino/collector/desktop/server/EmbeddedWebServer.kt:35)、[desktop/src/main/kotlin/com/kfaino/collector/desktop/server/EmbeddedWebServer.kt:123](F:/LANShare/AgentWorkSpace/Collector/desktop/src/main/kotlin/com/kfaino/collector/desktop/server/EmbeddedWebServer.kt:123)、[desktop/src/main/kotlin/com/kfaino/collector/desktop/server/EmbeddedWebServer.kt:204](F:/LANShare/AgentWorkSpace/Collector/desktop/src/main/kotlin/com/kfaino/collector/desktop/server/EmbeddedWebServer.kt:204)、[desktop/src/main/kotlin/com/kfaino/collector/desktop/server/EmbeddedWebServer.kt:215](F:/LANShare/AgentWorkSpace/Collector/desktop/src/main/kotlin/com/kfaino/collector/desktop/server/EmbeddedWebServer.kt:215)。

后续应优先明确默认监听范围、显式开启局域网服务及配对鉴权；本次不更改。

### R2 · P1 · 桌面导入会先覆盖原文件，解析失败仍可能返回成功

`importJson` 先 `dataFile.writeText(jsonStr)`，再调用 `loadAll()` 并返回 true；但 loadAll 自己捕获解析异常并返回内存旧值，不向调用方报告失败。因而格式错误的输入也可能覆盖有效备份，并向 HTTP 调用方返回成功。普通保存同样直接覆盖文件，没有临时文件原子替换。证据：[desktop/src/main/kotlin/com/kfaino/collector/desktop/storage/DesktopDataStore.kt:879](F:/LANShare/AgentWorkSpace/Collector/desktop/src/main/kotlin/com/kfaino/collector/desktop/storage/DesktopDataStore.kt:879)、[desktop/src/main/kotlin/com/kfaino/collector/desktop/storage/DesktopDataStore.kt:453](F:/LANShare/AgentWorkSpace/Collector/desktop/src/main/kotlin/com/kfaino/collector/desktop/storage/DesktopDataStore.kt:453)、[desktop/src/main/kotlin/com/kfaino/collector/desktop/storage/DesktopDataStore.kt:698](F:/LANShare/AgentWorkSpace/Collector/desktop/src/main/kotlin/com/kfaino/collector/desktop/storage/DesktopDataStore.kt:698)、[desktop/src/main/kotlin/com/kfaino/collector/desktop/server/EmbeddedWebServer.kt:221](F:/LANShare/AgentWorkSpace/Collector/desktop/src/main/kotlin/com/kfaino/collector/desktop/server/EmbeddedWebServer.kt:221)。

后续需要先完整解析/校验、原子替换及失败保留原数据的回归用例。

### R3 · P1 · Android“备份”范围不完整，WebDAV 同步沿用同一输出

`DataStore.exportBackupJson()` 仅传 categories 与 entries；BackupCodec 顶层只写 version、timestamp、categories、entries，未包含 12 馆、ideas、clippings，也只记录图片路径而非媒体文件。ProfileFragment 的 WebDAV 上传直接使用这一 JSON。因此不能用于承诺全应用灾难恢复或全量跨端迁移；专业馆和知识库不会随此备份恢复到新设备。证据：[app/src/main/java/com/kfaino/diapertracker/DataStore.kt:156](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/DataStore.kt:156)、[app/src/main/java/com/kfaino/diapertracker/BackupCodec.kt:17](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/BackupCodec.kt:17)、[app/src/main/java/com/kfaino/diapertracker/BackupCodec.kt:79](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/BackupCodec.kt:79)、[app/src/main/java/com/kfaino/diapertracker/BackupCodec.kt:83](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/BackupCodec.kt:83)、[app/src/main/java/com/kfaino/diapertracker/ProfileFragment.kt:631](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/ProfileFragment.kt:631)。实际存在这些独立仓储的证据：[app/src/main/java/com/kfaino/diapertracker/VaultRepositories.kt:141](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/VaultRepositories.kt:141)、[app/src/main/java/com/kfaino/diapertracker/VaultRepositories.kt:1221](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/VaultRepositories.kt:1221)、[app/src/main/java/com/kfaino/diapertracker/VaultRepositories.kt:1320](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/VaultRepositories.kt:1320)。

后续应建立包含所有集合与媒体的备份清单、跨端契约和新设备恢复验收。

### R4 · P1 · 桌面增量合并丢字段、遗漏证照馆，不能称为全量无损

桌面从 incoming JSON 重建 Entry 时仅赋少数基础字段，较新时间戳会用这个对象替换整个旧对象；模型中订阅、空间、折旧等其他字段因此使用默认值而非输入值。主合并调用列表没有证照馆合并；卡券示例仅添加不存在的 ID，同 ID 的后续修改不更新。证据：[desktop/src/main/kotlin/com/kfaino/collector/desktop/storage/DesktopSyncMergeEngine.kt:68](F:/LANShare/AgentWorkSpace/Collector/desktop/src/main/kotlin/com/kfaino/collector/desktop/storage/DesktopSyncMergeEngine.kt:68)、[desktop/src/main/kotlin/com/kfaino/collector/desktop/storage/DesktopSyncMergeEngine.kt:88](F:/LANShare/AgentWorkSpace/Collector/desktop/src/main/kotlin/com/kfaino/collector/desktop/storage/DesktopSyncMergeEngine.kt:88)、[desktop/src/main/kotlin/com/kfaino/collector/desktop/storage/DesktopSyncMergeEngine.kt:103](F:/LANShare/AgentWorkSpace/Collector/desktop/src/main/kotlin/com/kfaino/collector/desktop/storage/DesktopSyncMergeEngine.kt:103)、[desktop/src/main/kotlin/com/kfaino/collector/desktop/storage/DesktopSyncMergeEngine.kt:132](F:/LANShare/AgentWorkSpace/Collector/desktop/src/main/kotlin/com/kfaino/collector/desktop/storage/DesktopSyncMergeEngine.kt:132)；完整 Entry/证照数据能力对照：[desktop/src/main/kotlin/com/kfaino/collector/desktop/models/DataModels.kt:26](F:/LANShare/AgentWorkSpace/Collector/desktop/src/main/kotlin/com/kfaino/collector/desktop/models/DataModels.kt:26)、[desktop/src/main/kotlin/com/kfaino/collector/desktop/storage/DesktopDataStore.kt:717](F:/LANShare/AgentWorkSpace/Collector/desktop/src/main/kotlin/com/kfaino/collector/desktop/storage/DesktopDataStore.kt:717)。

已有桌面同步测试只校验基础名称/价格、条数和少数馆条目，未覆盖上述字段完整性，不能用它们证明无损。证据：[desktop/src/test/kotlin/com/kfaino/collector/desktop/storage/DesktopSyncMergeEngineTest.kt:27](F:/LANShare/AgentWorkSpace/Collector/desktop/src/test/kotlin/com/kfaino/collector/desktop/storage/DesktopSyncMergeEngineTest.kt:27)、[desktop/src/test/kotlin/com/kfaino/collector/desktop/storage/DesktopSyncMergeEngineTest.kt:71](F:/LANShare/AgentWorkSpace/Collector/desktop/src/test/kotlin/com/kfaino/collector/desktop/storage/DesktopSyncMergeEngineTest.kt:71)。

### R5 · P1 · 桌面看板直接拼接用户数据到 HTML

品牌、分类、单位、位置和预警标签未经 HTML 转义就进入页面。结合资产写入/导入 API，可形成持久化 HTML/脚本注入风险；本次未投放载荷或运行复现。证据：[desktop/src/main/kotlin/com/kfaino/collector/desktop/server/EmbeddedWebServer.kt:164](F:/LANShare/AgentWorkSpace/Collector/desktop/src/main/kotlin/com/kfaino/collector/desktop/server/EmbeddedWebServer.kt:164)、[desktop/src/main/kotlin/com/kfaino/collector/desktop/server/EmbeddedWebServer.kt:240](F:/LANShare/AgentWorkSpace/Collector/desktop/src/main/kotlin/com/kfaino/collector/desktop/server/EmbeddedWebServer.kt:240)、[desktop/src/main/kotlin/com/kfaino/collector/desktop/server/EmbeddedWebServer.kt:246](F:/LANShare/AgentWorkSpace/Collector/desktop/src/main/kotlin/com/kfaino/collector/desktop/server/EmbeddedWebServer.kt:246)。

### R6 · P2 · WebDAV 密码明文持久化

Android 将密码直接写普通 SharedPreferences；桌面将 webdav_pass 写入 config.json。应用私有目录和生物锁并不等同于凭据加密；读取到配置的进程、备份或设备访问者可取得凭据。证据：[app/src/main/java/com/kfaino/diapertracker/DataStore.kt:13](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/DataStore.kt:13)、[app/src/main/java/com/kfaino/diapertracker/SettingsStore.kt:114](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/SettingsStore.kt:114)、[desktop/src/main/kotlin/com/kfaino/collector/desktop/storage/DesktopDataStore.kt:866](F:/LANShare/AgentWorkSpace/Collector/desktop/src/main/kotlin/com/kfaino/collector/desktop/storage/DesktopDataStore.kt:866)。本次没有读取用户实际密码或配置文件。

### R7 · P2 · Release 签名与桌面打包完成判定不适合直接认定正式交付

Android Release 使用 debug signingConfig 且不混淆。桌面打包脚本默认版本仍为 4.3.1，找不到精确 JAR 时取第一个 JAR，Native 构建异常降为警告，末尾仍输出 Complete；Gradle 桌面版本已为 4.3.2，HTTP health 却仍写 4.3.1。存在签名管理、旧包混入、清单/运行版本不一致、缺 Native 产物仍“成功”的风险。证据：[app/build.gradle.kts:19](F:/LANShare/AgentWorkSpace/Collector/app/build.gradle.kts:19)、[scripts/Package-CollecterStandalone.ps1:3](F:/LANShare/AgentWorkSpace/Collector/scripts/Package-CollecterStandalone.ps1:3)、[scripts/Package-CollecterStandalone.ps1:27](F:/LANShare/AgentWorkSpace/Collector/scripts/Package-CollecterStandalone.ps1:27)、[scripts/Package-CollecterStandalone.ps1:37](F:/LANShare/AgentWorkSpace/Collector/scripts/Package-CollecterStandalone.ps1:37)、[scripts/Package-CollecterStandalone.ps1:70](F:/LANShare/AgentWorkSpace/Collector/scripts/Package-CollecterStandalone.ps1:70)、[desktop/build.gradle.kts:7](F:/LANShare/AgentWorkSpace/Collector/desktop/build.gradle.kts:7)、[desktop/src/main/kotlin/com/kfaino/collector/desktop/server/EmbeddedWebServer.kt:116](F:/LANShare/AgentWorkSpace/Collector/desktop/src/main/kotlin/com/kfaino/collector/desktop/server/EmbeddedWebServer.kt:116)。

### R8 · P2 · 截图自动采集的设备权限链路待验收

监听器查询外部 MediaStore，并读取图片 DATA 路径；Manifest 权限列表未声明 READ_EXTERNAL_STORAGE/READ_MEDIA_IMAGES。系统生成的截图不是应用自行创建的媒体，因此读取覆盖范围和 URI 授权需要按设备版本验证。现有 ScreenshotClipperTest 测的是文本分析、HTML 清洗和截图名称判断，未证明 ContentObserver 能在目标设备上读取截图。证据：[app/src/main/java/com/kfaino/diapertracker/ScreenshotWatcherHelper.kt:41](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/ScreenshotWatcherHelper.kt:41)、[app/src/main/java/com/kfaino/diapertracker/ScreenshotWatcherHelper.kt:65](F:/LANShare/AgentWorkSpace/Collector/app/src/main/java/com/kfaino/diapertracker/ScreenshotWatcherHelper.kt:65)、[app/src/main/AndroidManifest.xml:4](F:/LANShare/AgentWorkSpace/Collector/app/src/main/AndroidManifest.xml:4)、[app/src/test/java/com/kfaino/diapertracker/ScreenshotClipperTest.kt:11](F:/LANShare/AgentWorkSpace/Collector/app/src/test/java/com/kfaino/diapertracker/ScreenshotClipperTest.kt:11)。这是静态识别的权限缺口和验收风险，不是本次真机复现结论。

### R9 · P2 · 状态文档与验证工具的覆盖边界不一致

stage-current 同时把 441/442 写为 COMPLETED 和 NOT_STARTED，452 出现多条冲突状态；TODO 声称只保存未完成项却包含多个 COMPLETED 项。静态 selfcheck 的版本检查只比较 app/README/desktop，漏掉打包默认版本和 health 版本；空 catch 正则也不能识别所有吞异常路径，例如 UDP catch 运行期间不记录异常。证据：[docs/stage-current.md:15](F:/LANShare/AgentWorkSpace/Collector/docs/stage-current.md:15)、[docs/stage-current.md:29](F:/LANShare/AgentWorkSpace/Collector/docs/stage-current.md:29)、[docs/TODO.md:5](F:/LANShare/AgentWorkSpace/Collector/docs/TODO.md:5)、[docs/TODO.md:16](F:/LANShare/AgentWorkSpace/Collector/docs/TODO.md:16)、[tools/selfcheck.ps1:82](F:/LANShare/AgentWorkSpace/Collector/tools/selfcheck.ps1:82)、[tools/selfcheck.ps1:96](F:/LANShare/AgentWorkSpace/Collector/tools/selfcheck.ps1:96)、[desktop/src/main/kotlin/com/kfaino/collector/desktop/server/EmbeddedWebServer.kt:92](F:/LANShare/AgentWorkSpace/Collector/desktop/src/main/kotlin/com/kfaino/collector/desktop/server/EmbeddedWebServer.kt:92)。

因此当前阶段、README 的“完整支持”及四项静态绿灯都不应代替实际发布验收。

## 6. 本次验证结果与限制

- 已读取工程约定、阶段/待办/缺陷/问答、两端构建配置、入口、关键存储/同步/API/备份实现，以及现有测试和打包脚本；各结论的文件证据见上文。
- 执行 `git status --short` 被 Git 的 dubious ownership 检查拒绝：仓库归 LANShareUser，当前进程为 kfaino。未修改 safe.directory 或绕过信任检查，故不能提供可靠的 Git 工作树差异或提交基线。此项为本次命令观察，不是业务源码缺陷。
- 执行 `pwsh -NoProfile -File tools/selfcheck.ps1 -SkipBuild`，退出码 1。四项静态检查通过；两项“未通过”来自主动跳过编译/单测，**不是已确认的编译或测试失败**。脚本逻辑证据：[tools/selfcheck.ps1:43](F:/LANShare/AgentWorkSpace/Collector/tools/selfcheck.ps1:43)、[tools/selfcheck.ps1:54](F:/LANShare/AgentWorkSpace/Collector/tools/selfcheck.ps1:54)。
- 未运行 Gradle、Android/JVM 单测、真机/模拟器、Native 壳、网络服务和同步往返；避免生成构建缓存/产物、首次初始化或修改业务数据。未验证编译与测试；历史文档“全绿”不作为本次结果。
- 本次桌面端状态：已完成静态诊断，未修改桌面实现、未运行桌面验收；不是桌面功能交付。
- 无源码、配置、测试、版本号或其他文档修复，仅新增本报告。

### 静态自检原始输出

```text

=== GEMINI.md §4 交付前自检 ===

[3/6] 检查布局硬编码颜色 ...
[4/6] 检查空 catch ...
[5/6] 检查版本号一致性 ...
[6/6] 检查架构文档覆盖率 ...

--- 以下内容整段复制进汇报，不要改写、不要只贴通过项 ---

#### §4 交付前自检结果

<sub>selfcheck 指纹 `3556D7C69A03` · 若与上次汇报不同，说明验收脚本被改动过，必须在汇报中说明原因</sub>

| # | 检查项 | 实测结果 | 判定 |
| :-: | :--- | :--- | :--- |
| 1 | assembleRelease 编译 | 未运行 (-SkipBuild) | ❌ 未通过 |
| 2 | 单元测试 testReleaseUnitTest | 未运行 (-SkipBuild) | ❌ 未通过 |
| 3 | 布局硬编码颜色 | 共 8 处（scanner 相机遮罩 8 处属合理例外，其余 0 处） | ✅ 通过 |
| 4 | 空 catch（含仅注释） | 0 处 | ✅ 通过 |
| 5 | 版本号三处一致 | app=4.3.2 / README=4.3.2 / desktop=4.3.2 | ✅ 通过 |
| 6 | 模块文档覆盖 | 104 个模块，0 个未记录 | ✅ 通过 |

> ⚠️ 有 **2** 项未通过。按铁律 5，必须在汇报里逐项说明：是本次改动引入的，还是既有欠账（附 TECH_DEBT_AUDIT 编号）。


<!-- ↑ 以上为脚本输出，属客观证据，一个字都不要改 -->

#### 桌面端状态（脚本无法判定，由你在汇报中另起一节填写）

> 脚本不知道你这次改了什么，所以这一节**不在上面的证据块里**，需要你自己写：
> 「已跟进」或「未跟进 + 具体原因」。禁止以修改版本号冒充全平台交付（铁律 2）。
```

## 7. 后续验证建议（本次未执行）

优先建立隔离数据目录与脱敏样例，验证恶意/无效备份不会覆盖现有数据、未配对设备不能导出或修改数据、所有集合和字段跨端往返不丢失、HTML 字段仅以文本显示。对应风险和源码证据：R1–R5。

随后按项目已有门禁运行下列命令，再补截图权限、设备能力、WebDAV 恢复、Native 壳/安装器实际验收。命令依据：[docs/codex-skills.md:49](F:/LANShare/AgentWorkSpace/Collector/docs/codex-skills.md:49)、[tools/selfcheck.ps1:47](F:/LANShare/AgentWorkSpace/Collector/tools/selfcheck.ps1:47)。

```powershell
.\gradlew.bat :app:testReleaseUnitTest :app:assembleRelease
.\gradlew.bat :desktop:classes :desktop:test
pwsh -NoProfile -File tools/selfcheck.ps1
```

不要直接使用当前桌面导入或同步路径验证真实个人数据；本报告尚未修复 R1–R4。



## 后续变更记录

2026-08-30：本报告保留改名前的诊断事实；后续按用户要求将 Gradle 工程名改为 Collecter，包名和数据标识不变。目录改名被进程占用阻止，当前证据路径仍有效；详情见 [命名和需求方向记录](product-direction.md)。
