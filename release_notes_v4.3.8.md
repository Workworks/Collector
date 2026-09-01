# Collecter v4.3.8 下载修复

- 发布日期：2026-09-01
- Android：versionCode 45 / versionName 4.3.8
- Desktop：4.3.8 Universal JVM JAR

## 修复

- 移除证书已过期或握手失败的更新代理，保持 GitHub 官方源第一。
- 应用内下载仅使用 GitHub 官方源并显示具体失败原因，不再被失效代理的 `Chain Validation failed` 覆盖。
- TLS 证书链失败时提示检查手机自动日期与时间，并提供系统浏览器打开 GitHub 官方下载。
- 解析并校验 GitHub Release 的 SHA-256 digest；文件大小或摘要不一致时立即删除并拒绝安装，缓存同样复核。

本版本没有关闭 TLS 验证、没有导入不受信证书，也没有降低 Android 原签名覆盖升级要求。
