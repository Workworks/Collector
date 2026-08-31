# Collector WebDAV 公网备份联调

## 目标和边界
让 Android 和桌面现有备份入口连接相邻 WebDav 项目，完成真实 HTTPS 上传、下载和隔离恢复。
保留 `Collecter_Backup.json`、JSON 契约、包名、签名和版本号；不覆盖真实用户备份，不发布 APK，不修改用户工作树中的无关内容。
允许启动本项目服务及 Cloudflare 临时隧道。无固定域名/账户配置时不承诺固定地址、开机启动或 SLA。

## 工作包
1. 修复 WebDav 请求解析、认证、网页转义及精准启停；凭据不进入日志，服务仅监听回环。
2. 按实际联调修复 Collector WebDAV 客户端；新测试不修改既有断言。
3. 在专用随机远端测试目录、桌面临时存储及 Android 测试上下文中验证，不触碰真实备份。
4. 交付连接说明和实际证据，公网 URL 与密码只存本机运行文件，不提交。

## 完成标准
- AC-01：WebDav 安全回归和 HTTP 协议往返通过。
- AC-02：公网 HTTPS 无凭据/错误凭据被拒绝，有凭据 PUT/GET 内容一致。
- AC-03：Collector 桌面真实 Helper 连接、上传、取消恢复和确认恢复通过。
- AC-04：Android 模拟器真实 Helper 上传下载及隔离数据恢复通过；物理手机独立标注。
- AC-05：相关模块测试、构建及 selfcheck 通过，运行服务/连接说明可复用。

## 验证记录
AC-01/02/03/04 PASS；AC-05 部分通过，既有 Android/desktop 版本分离导致 selfcheck 版本一致项 FAIL，未伪造全绿。结果及失败原文见 [验收报告](webdav-integration-report.md)。
