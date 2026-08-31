# WebDAV 备份使用与排错

适用：当前 Android 4.3.3 候选源码、desktop 4.3.2。修复代码存在不表示你手机上的旧 APK 已包含修复；发布签名仍须按 [发布验收](../releases/v4.3.3-report.md) 处理。

## 配置与操作

1. 在客户端的 WebDAV 设置输入服务端目录 URL、用户名和应用独立密码。两端都使用同一目录中的 `Collecter_Backup.json`，也接受以此文件名结束的 URL。不要在文档、截图或 Git 中保存密码。
2. 先在有数据的设备导出一份本地完整备份，再测试连接并上传。不要用空设备先上传到已有备份路径。
3. 在另一端下载，阅读恢复预览；确认后才替换备份包含的集合，取消不写入。备份中缺少的集合保持原样。下载恢复不是自动双向合并。
4. 需要服务端子目录时先创建；当前桌面客户端不再自动创建旧 `CollectorBackup` 目录。不要复用真实备份目录做 QA。

实现入口：[Android 设置与恢复](../../app/src/main/java/com/kfaino/diapertracker/ProfileFragment.kt)、[桌面设置](../../desktop/src/main/kotlin/com/kfaino/collector/desktop/ui/MainWindow.kt)、[共享恢复边界](../../shared/src/main/kotlin/com/kfaino/collecter/core/BackupDocument.kt)。

## 两端协议差异

| 操作 | Android | Desktop |
| --- | --- | --- |
| 连接检查 | HttpURLConnection HEAD 目标备份文件；200 类和 404 可达视为成功，401/403 失败 | JDK HttpClient PROPFIND 配置 URL，Depth: 0；200 类/207 成功 |
| 上传与下载 | PUT / GET；Basic 认证 | PUT / GET；Basic 认证 |
| 大小保护 | 下载最大 64 MiB；共享附件限制继续生效 | 下载最大 64 MiB；共享附件限制继续生效 |

HEAD 返回 404 仅说明该路径尚无备份，不能独立证明服务支持上传、账号有写权限或整个 WebDAV 配置正确，仍需专用样例 PUT/GET 验证。

证据：[Android Helper](../../app/src/main/java/com/kfaino/diapertracker/WebDavSyncHelper.kt:30)、[Desktop Helper](../../desktop/src/main/kotlin/com/kfaino/collector/desktop/sync/DesktopWebDavHelper.kt:37)、[BackupDocument](../../shared/src/main/kotlin/com/kfaino/collecter/core/BackupDocument.kt:15)。

## 相邻 WebDav 服务与限制

相邻 `F:\LANShare\AgentWorkSpace\WebDav` 是独立可选服务。当前连接地址应在该项目本机 `.runtime/public-url.txt` 查看；凭据由本机受控配置提供，本页不复制它们。已保存的 [公网联调记录](../stages/webdav-integration-report.md) 验证了认证、双端往返和附件恢复，但不代表服务持续在线。

Quick Tunnel 重启可能换地址；记录中曾出现 530 预热失败和连接超时。固定地址需要受管隧道，不能靠旧报告 URL 保证可达。不建议因失败反复用空备份覆盖远端。

相邻 WebDav 服务现已实现备份历史和删除恢复，见 [Stage 454](../stages/stage-454-spec.md)。每路径最多 256 份或 512 MiB 历史，达到上限拒绝覆盖，需要管理员离线归档；不自动删除历史。`GET <备份URL>?history` 列出历史，`?revision=<id>` 下载历史，服务目录的 Restore-Backup.ps1 可条件恢复。

新客户端针对该服务使用 ETag 条件上传：新会话如果远端已有备份，先下载核对；其他设备写入后会拒绝旧版本上传。桌面需完成确认恢复才记住版本；Android 下载成功即记住，取消恢复不改变本地数据。第三方 WebDAV 无能力标头时保留原兼容方式，不能承诺防覆盖。旧客户端仍可无条件写入但保留历史；此功能不等于自动冲突合并。

本轮使用隔离本机服务，未启动生产服务或公网隧道。此前公网测试基于模拟器与本机，不代替物理手机、运营商网络、桌面原生窗口或长期稳定性验收。
