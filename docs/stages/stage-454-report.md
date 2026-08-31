# Stage 454 执行报告

日期：2026-08-31。用户授权自主决策与实施，规格见 [Spec](stage-454-spec.md)。不是全部交付完成：本轮完成当前环境可验证的备份与并发修复，外部发布和设备验收仍阻塞。

## 完成与方向

继续聚焦离线收藏、资产生命周期和可靠备份。暂缓新增专业馆、macOS、复杂悬浮窗、通用 AI 与整体 UI 重写；不删已有功能，也不将暂缓方向计作完成。

1. 相邻 WebDav 的 Collecter_Backup.json 在覆盖和删除前持久保存历史；认证查询、历史下载及条件恢复命令已实现。历史在 DAV 挂载之外，每路径 256 份/512 MiB 上限，超限或归档失败拒绝覆盖，不静默淘汰。单进程限定，无跨实例锁、历史清理 UI 或自动业务合并。
2. Android/desktop 使用 shared/WebDavRevisionGuard：针对支持能力标头的服务，首次创建用 If-None-Match，已观察版本用 If-Match。新会话有远端备份时先下载核对；冲突停止写入。第三方服务及旧客户端兼容不等于防覆盖保证。
3. OCR 回调改为请求编号匹配后的现存记录字段补丁：不复活删除记录、不覆盖同期标题、不接受旧请求结果。补充失败保留原件与重试、跨年/延后边界提醒规则回归；不把规则测试称为长期系统调度验收。
4. scripts/Verify-AndroidRelease.ps1 实际检查新旧 APK 证书，差异时拒绝继续。scripts/Test-LocalWebDav.ps1 自动创建和清理隔离服务，执行真实桌面仓储往返，不使用生产凭据或数据。

实现文件：相邻 WebDav 的 backup-history.js、server.js、Restore-Backup.ps1；shared 的 WebDavRevisionGuard.kt、WorkspaceRecords.kt；双端 WebDav Helper；Android CollectionWorkspace.kt 与 CollectionWorkspaceDialog.kt。未修改 applicationId、版本号或验收脚本，未提交/推送混合用户工作树。

## 验证

| 验证 | 实际结果 | 证据 |
| --- | --- | --- |
| 最终测试 | Android 45、shared 16、desktop 15；共 76 项，74 通过，2 个 opt-in live 跳过，0 失败 | [计数](evidence-454/test-counts.json)、[日志](evidence-454/final-tests.log) |
| Android release 编译 | BUILD SUCCESSFUL；只是本地候选，非可发布证明 | [构建](evidence-454/final-gradle.log) |
| WebDav 服务 | 12/12；含 8 MiB 与 64 MiB 往返、超限拒绝、同 ETag 并发、归档失败、断线、容量门禁、路径绕过与实际 PowerShell 恢复 | [日志](evidence-454/webdav.log) |
| 真实桌面 Helper | 单独 1 项本机 HTTP 往返、预览取消及确认恢复通过，不与完整套件合并计数 | [日志](evidence-454/desktop-local-webdav.log)、[XML](evidence-454/desktop-local-webdav.xml) |
| selfcheck | 5/6；版本一致项仍失败，Android/README 4.3.3、desktop 4.3.2，既有 P0-2 防假发版约束，不提版本换绿灯 | [原始输出](evidence-454/selfcheck.log) |
| 发布签名 | 旧证书 03d73e…bb013a、新证书 98edb3…b5097e，不兼容，门禁按预期拒绝 | [日志](evidence-454/signature.log) |
| 原生构建 | 缺 MSVC x64，Native host was not built | [日志](evidence-454/native-build.log) |

## AC 回填

- AC-01 / AC-02 / AC-03：PASS，隔离服务与恢复命令测试覆盖；旧客户端无条件覆盖仍保留历史，这是兼容边界。
- AC-04：PARTIAL，编译及测试通过，selfcheck 版本一致项未通过。
- AC-05：BLOCKED，原签名缺失；没有创建 tag、GitHub Release 或发布新 APK。
- AC-06：BLOCKED，缺 MSVC，未做干净安装/升级；无固定域名/受管隧道凭据，未提供稳定公网服务。
- AC-07：BLOCKED / NOT RUN，adb 设备列表为空。物理手机、独立网络、第三方分享矩阵、损坏图片实际 UI 重试、长时后台提醒、客户端 64 MiB 极限与 UI 卡顿、长时并发压力未验收。服务大小边界测试与跨年规则测试不能替代这些项目。

## 运行与数据边界

本轮开始检查时生产 WebDav 未运行；本轮只启动隔离 QA 服务并清理，没有启动生产隧道。最终复查发现服务已由本轮操作之外的进程启动（PID 21276，14:51:48，状态含临时 Quick Tunnel）；不停止或改动该进程，也不把其运行状态算作本轮生产验收。固定域名仍缺失。APK 在 app/build/outputs/apk/release，未复制到可发布目录。

最终 git diff --check 指出既有 DataModels.kt、VaultRepositories.kt、AssetExpiryAndThresholdTest.kt、desktop/models/DataModels.kt、docs/debt/P2-2.md 的 EOF 空行；这些文件不属于本轮修改，不为消除提示改动用户工作。新增报告/Spec/TODO 的相对链接检查无缺失；selfcheck 指纹仍为 3556D7C69A03。

下一步按 [TODO](../TODO.md) 收尾。缺少私钥、硬件和平台前置是实际阻塞，不因自主授权视为已解除。
