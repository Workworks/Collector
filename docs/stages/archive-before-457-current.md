> Stage 457 整合前本地历史状态，非当前结论。

# 当前阶段与验证状态

**发布检查更正（2026-08-31）：远端最新正式版为 v4.3.6 / 43，桌面发布 JAR 为 4.3.6；本地工作树仍为 Android 4.3.3、desktop 4.3.2。** 已下载验证 v4.3.6 APK 的哈希/版本/原签名一致，但 tag 源码存在 DEX 未验签、ZIP 路径越界防护缺失及 Android PROPFIND 问题，因此未发布 4.3.7。见 [当前检查报告](releases/v4.3.7-check.md)。下文版本数字仅为当时本地状态，不代表最新线上版本。

**最新：Stage 456（2026-08-31）**。原签名已恢复，最终 APK 验签一致；隔离覆盖升级、Android→桌面家庭协议、附件失败回滚和过期会话回归通过。常规 89 项中 87 通过、2 跳过，自检 5/6。完整 UI、物理设备、Native、固定公网和长期运行仍未关闭；按“完成后升级版本”，本轮没有提高版本号或公开发布。以 [456 报告](stages/stage-456-report.md)、[TODO](TODO.md) 和 [当前签名说明](releases/signing-456.md) 为准。**下文“缺原签名/签名拒绝”等是历史事实，已被本次实测取代。**

更新：2026-08-31，当前阶段为 **Stage 455**。七个方向的最小实现、逐项验收与限制见 [最新报告](stages/stage-455-report.md)，入口见 [工作台手册](manuals/workbench.md)。以下 Stage 454/WebDAV 内容保留历史运行范围；最新状态以 455 报告和 [TODO](TODO.md) 为准。

本轮增加双端整理/搜索/生命周期工作台、加密备份与历史恢复入口、家庭会话权限。常规自动化 84 项中 82 通过、2 跳过；隔离 Android 加密及跨端往返通过。selfcheck 5/6，既有版本分离未消除；Android 签名门禁拒绝发布。GUI 验收被用户 Esc 中止后未恢复桌面操作，因此未全部关闭。本轮未发布、未改版本，未修改相邻 WebDav 生产服务。桌面本轮为 JVM 入口，Native 安装未交付。

2026-08-31 移动端 WebDAV 修复发布补充：用户已授权发布，实际检查发现当前热更新不能替换 WebDAV 原生调用，公钥为空；完整 APK 与线上 v4.3.0 签名仍不兼容。本次未发布，详见 [发布检查](releases/webdav-hotfix-20260831.md)。下文 Stage 454 结果保留其原运行范围。

更新：2026-08-31。当前为 [Stage 454 自主执行](stages/stage-454-report.md)：已修改备份保护、双端条件上传及 OCR 并发处理；未发布、未启动生产公网服务。下文旧联调保留历史语境，最新计数、实现和未关闭验收以 454 报告与 TODO 为准。旧快照在 [归档](stages/archive-20260831/stage-current.md)。

## 当前事实

| 项目 | 当前状态 | 依据 |
| --- | --- | --- |
| 工程与目录 | 产品/Gradle 名 Collecter，文件夹 Collector；包含 app、desktop、shared | [settings.gradle.kts](../settings.gradle.kts:24) |
| Android | 4.3.3 / 40；包名 com.kfaino.diapertracker，release 仍使用 debug 签名 | [app/build.gradle.kts](../app/build.gradle.kts:10) |
| 桌面 | JVM 开发版本 4.3.2；不等同于原生安装器交付 | [desktop/build.gradle.kts](../desktop/build.gradle.kts:7)、[发布验收](releases/v4.3.3-report.md) |
| WebDAV 专项 | 公网认证、模拟器与真实桌面 Helper、Android→桌面恢复已有通过记录 | [专项报告](stages/webdav-integration-report.md)、[原始 XML](stages/evidence/webdav-20260831/desktop-live.xml) |
| Stage 453 | 主要实现落地，剩余验收未全部关闭 | [Stage 453 验收](stages/stage-453-delivery-report.md) |
| Android 发布 | 最近发布验收为 BLOCKED：旧签名缺失；本轮没有重新发布或验证远端 Release | [签名报告](releases/v4.3.3-report.md) |

## 本次新增变化

Android WebDAV 连接检查改用 HEAD；桌面连接检查改用 JDK HttpClient 的 PROPFIND；两端仍使用 PUT/GET 和 Collecter_Backup.json。桌面不再自动创建旧 CollectorBackup 目录。见 [Android Helper](../app/src/main/java/com/kfaino/diapertracker/WebDavSyncHelper.kt:30)、[桌面 Helper](../desktop/src/main/kotlin/com/kfaino/collector/desktop/sync/DesktopWebDavHelper.kt:37)。

相邻 WebDav 项目是可选备份服务，非 Collector 的新 Gradle 模块。公网 Quick Tunnel 是临时地址，报告中的 URL 仅代表当时探测；本轮没有访问配置密码，也未验证服务当前在线。

## 验证解释

WebDAV 专项保存了 Android 45、shared 12、desktop 15 项通过记录，以及 Android 设备专项 1 项通过。它与此前 Stage 453 的 73 项、发版候选的 69 项来自不同运行，不能相加或作为同一轮结果。

最新文档刷新验证见 [重新加载报告](stages/docs-refresh-20260831.md)。selfcheck 的版本一致项仍应真实反映 Android/README 4.3.3 与 desktop 4.3.2 的分离，不为绿灯升桌面版本或改验收脚本。

下一步只以 [TODO](TODO.md) 为执行账本；历史 441/442 的 COMPLETED 不能代替当前原生安装验收，452 不再重复列出不同状态。
