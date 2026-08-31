# Collector 公网 WebDAV 联调报告

完成时间：2026-08-31（Asia/Shanghai）。范围按 [联调 Spec](webdav-integration-spec.md)。

## 结果

服务已在本机后台运行并通过 Cloudflare HTTPS 对外访问。Android 模拟器、桌面真实 Helper/仓储、公网协议验证通过；没有覆盖用户备份，测试目录已经删除。物理手机和桌面原生窗口未进行本轮 GUI 验收。

连接地址从 `F:\LANShare\AgentWorkSpace\WebDav\.runtime\public-url.txt` 获取，密码保存在 WebDav 的本机 `config.json`；本次完整连接快照在 `.runtime/Collector连接信息.txt`，不在报告中记录凭据。

## 修复

- Android `HttpURLConnection` 拒绝 PROPFIND，连接测试改为 HEAD。上传 PUT、下载 GET、备份文件名和格式不变。
- 桌面使用 JDK HttpClient 发送 PROPFIND；移除旧的、不再使用的 CollectorBackup 目录创建请求。
- 服务端统一 Basic 解析，支持冒号密码和 UTF-8；强制认证，拒绝畸形 URL 和符号链接，统一别名与 MOVE 目标路径。
- 网页文件名按上下文转义，下载强制 attachment。服务改为仅监听回环，并更换随机强密码，不打印到日志。
- 上传先写临时文件、落盘再替换，上限 64 MiB；中断和超限上传保留旧备份。
- 精准启停本项目 PID 与隧道子进程，不再全局 taskkill。未设置开机自启，未改防火墙。

## 验收矩阵

| 标准 | 结果 | 证据 |
| --- | --- | --- |
| AC-01 服务协议与安全 | PASS，5 项回归 | [server-tests.txt](evidence/webdav-20260831/server-tests.txt) |
| AC-02 公网 HTTPS 认证与传输 | PASS，无/错误凭据 401，正确 PROPFIND 207；双端 PUT/GET 往返通过 | [公网最终探测](evidence/webdav-20260831/public-final.json)、双端结果 |
| AC-03 桌面真实 Helper | PASS，连接、上传、取消恢复不改字节、确认恢复；桌面全套 15 项通过 | [desktop-live.xml](evidence/webdav-20260831/desktop-live.xml)、[构建日志](evidence/webdav-20260831/desktop-final.log) |
| AC-04 Android 真实运行时 | PASS，模拟器上传下载字节一致，全部 CompleteBackupStore.collectionKeys 集合的未知字段和附件恢复；Android 备份进一步在桌面恢复成功 | [Android结果](evidence/webdav-20260831/android-live-result.txt)、desktop-live.xml |
| AC-05 构建及完整自检 | 部分通过：Android release 构建、45 项单测、shared 12 项、desktop 15 项通过；selfcheck 5/6，版本一致项 FAIL | [完整原始自检](evidence/webdav-20260831/selfcheck.txt) |

自检版本失败为既有 Android 独立发布状态：Android/README 4.3.3，desktop 4.3.2。与既有约束 **TECH_DEBT_AUDIT P0-2（停止假发版）** 对应，[发布 Spec](../releases/v4.3.3-spec.md) 已明确不得升桌面版本伪装交付。本轮未修改版本、签名、selfcheck 脚本或既有测试断言，不能把完整门禁说成全绿。

## 失败记录与限制

- [Android 修复前失败](evidence/webdav-20260831/android-before-fix.txt)：系统拒绝 PROPFIND；修复后通过，没有放宽测试断言。
- [隧道预热失败](evidence/webdav-20260831/android-tunnel-warming.txt)：地址刚生成时返回 530，稍后真实重试通过。
- [桌面瞬时超时](evidence/webdav-20260831/desktop-transient-timeout.xml)：曾出现 8 秒 HTTP connect timed out；随后原测试重跑全部通过，不能据此承诺稳定性。
- Quick Tunnel 重启后域名变化，没有 SLA。固定公网地址需要用户自己的 Cloudflare 账户、域名和受管隧道配置，未部署。
- 实测为本机和 Android 模拟器经公网域名/Cloudflare 路径访问；没有独立运营商蜂窝网络或物理手机验收。
- 未发布新 APK 或原生桌面安装包，旧手机需要包含本次修复的客户端；原发布签名兼容阻塞保持原状，不卸载用户旧应用。
- 当前服务仅保留最新备份，没有版本历史、回收站或多设备写入冲突合并；不要将空设备备份覆盖有数据设备的备份。

## 数据保护和工作树

公网测试仅使用随机 `qa-desktop-*`、`qa-android-*` 目录；均已通过受认证 DELETE 清理。用户根备份未创建或改写，`data/` 保留原 README.txt。Android 使用随机偏好前缀和缓存文件目录、桌面使用 TemporaryFolder；Android 测试凭据文件已清除。

本轮开始时 Collector 已有大量未提交修改，均保留。整体 `git diff --check` 报告的若干既有 EOF 空行未顺手修改。真实凭据在 WebDav 忽略的配置/运行目录中，不进入 Collector 文档及测试源码。

## 原始 selfcheck 输出

见下方原样记录。桌面端状态：已跟进，真实 Helper 及跨端仓储测试通过；原生 GUI 和正式安装包未验收。

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
| 5 | 版本号三处一致 | app=4.3.3 / README=4.3.3 / desktop=4.3.2 | ❌ 未通过 |
| 6 | 模块文档覆盖 | 109 个模块，0 个未记录 | ✅ 通过 |

> ⚠️ 有 **1** 项未通过。按铁律 5，必须在汇报里逐项说明：是本次改动引入的，还是既有欠账（附 TECH_DEBT_AUDIT 编号）。


<!-- ↑ 以上为脚本输出，属客观证据，一个字都不要改 -->

#### 桌面端状态（脚本无法判定，由你在汇报中另起一节填写）

> 脚本不知道你这次改了什么，所以这一节**不在上面的证据块里**，需要你自己写：
> 「已跟进」或「未跟进 + 具体原因」。禁止以修改版本号冒充全平台交付（铁律 2）。

