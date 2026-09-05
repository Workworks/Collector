# Stage 463：受限网络更新下载修复

日期：2026-09-01。

## 目标

当手机无法连接 `github.com:443` 时，应用内更新仍可通过 GitHub API 资产入口或内置镜像下载新 APK，并在校验通过后交给系统安装器。

## 边界

- 范围内：Android Release 元数据解析、下载候选顺序、镜像兜底、错误来源标签、回归测试与补丁发布。
- 范围外：不修改 `applicationId`、用户数据、WebDAV、热补丁验签和桌面业务功能。
- 安全不变量：GitHub API 与 GitHub 官方资源始终优先；镜像只能兜底；所有 APK 必须通过 Release 声明的大小和 SHA-256，否则删除并拒绝安装。

## 工作包

- WP-01：保留 Release asset API URL，按 API、官方网页资源、镜像顺序下载。
- WP-02：内置两个经当前发布资产验证可用的镜像，并清晰标记失败来源。
- WP-03：补充候选顺序、域名识别、Release 解析与实际网络回归。
- WP-04：构建、原签名覆盖升级、发布补丁并提供旧版可访问的局域网安装入口。

## 完成标准

- [x] AC-01：候选列表严格为 GitHub API、GitHub 官方、镜像，且不接收非官方原始地址。
- [x] AC-02：API 资产请求携带 `Accept: application/octet-stream`，下载后大小和 SHA-256 校验不可绕过。
- [x] AC-03：两个内置镜像可下载当前发布 APK 的有效字节范围，失败信息能区分来源。
- [x] AC-04：Android 单测、Release 构建与 `tools/selfcheck.ps1` 通过。
- [x] AC-05：以原签名从 v4.3.9 覆盖升级到 v4.3.10，数据探针保留。
- [ ] AC-06（`PARTIAL`）：GitHub Release 三项附件回读哈希一致；官方/API/两个镜像均可用。没有实体手机和常驻局域网分发主机，因此不虚构局域网手机安装验收。

## 验收证据

- v4.3.10 已发布：[发布报告](../releases/v4.3.10-report.md)。
- 下载源当前 Range 实测与 Release API 元数据：[download-sources.json](../releases/evidence-4.3.10/download-sources.json)。
- 模拟器原签名覆盖升级与数据保留：[upgrade-verify.log](../releases/evidence-4.3.10/upgrade-verify.log)。
