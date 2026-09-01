# 备份归档与容量检查

2026-09-01。单次归档仍由 `Archive-CollecterBackup.ps1` 执行；日常维护由 `Run-CollecterBackupMaintenance.ps1` 调用相同校验流程，并在校验 manifest 后按保留策略清理。

1. 在客户端导出加密 `.collecter` 备份，密码另处保管。归档正在写入的数据库文件不能替代应用导出。
2. 在另一块磁盘或离线介质执行：

```powershell
pwsh -File .\scripts\Archive-CollecterBackup.ps1 -Source '<已导出的备份绝对路径>' -ArchiveDirectory '<离线归档目录>'
```

脚本检查源文件与目标目录、大小和磁盘余量；默认至少保留 1 GiB 空间。复制前、复制后及副本 SHA-256 必须一致，才把临时文件改为正式归档文件并写入 manifest。低于容量阈值退出报错，不自动删旧备份腾空间。密码、服务器配置不进入 manifest。

3. 归档完成后断开离线介质；定期在隔离客户端选择该副本，输入密码、预览内容并确认恢复，核对记录、附件与生命周期。脚本的字节校验不等于应用恢复已成功。
4. 检查服务 `data/`、`data.history/` 与归档介质的空间，历史容量达到上限时先离线归档并验证，再按明确保留策略人工处理。不要直接清空历史目录。

当前主机已为 `F:\LANShare\AgentWorkSpace\WebDav\data\Collecter_Backup.json` 部署 `Collecter Daily Verified Backup` 计划任务，每日 03:15 归档到 `F:\LANShare\Backups\Collecter`。`maintenance-status.json` 与 `maintenance.log` 是运维状态源；手动触发返回 0。默认保留至少 7 份，且仅清理超过 30 天、文件名严格匹配、manifest 名称与 SHA-256 均通过的归档。该任务不包含远程通知渠道；计划任务失败需由主机监控读取状态文件。
