# Collecter v4.3.7

- 发布日期：2026-09-01
- Android：versionCode 44 / versionName 4.3.7
- 桌面：4.3.7 Universal JVM JAR

## 本次更新

- 修复 Android WebDAV 连接探测，使用受支持的 HEAD；保留 PUT/GET 完整备份与 ETag 条件覆盖保护。
- 动态 DEX 在安装和启动加载前执行 SHA256withRSA 强验签；缺少公钥、签名或内容被篡改时拒绝加载。
- 补丁 ZIP 拒绝目录穿越、绝对路径、Windows 盘符/ADS，并限制条目数及解压总体积。
- 整合远端 4.3.6 与本地工作台、完整备份、家庭访问控制及桌面功能。
- 兼容两条分支使用的收藏馆存储键和字段别名，保留未知字段及饮品小数余量。

## 发布说明

- Android applicationId 保持 `com.kfaino.diapertracker`，使用与 v4.3.6 相同的原签名，可覆盖升级。
- 动态 DEX 默认仍为关闭状态；本版本不包含 DEX 热补丁。
- 桌面附件是需要 Java 17 的 Universal JVM JAR，不是 Windows Native 安装器。
- 本轮设备验证使用 Android API 34 模拟器；未声称物理设备或所有远端新增实验模块均完成端到端验收。

完整验证记录见 `docs/releases/v4.3.7-report.md`。
