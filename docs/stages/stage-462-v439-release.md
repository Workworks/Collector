# Stage 462：v4.3.9 可靠性交付

## 目标

发布可从 v4.3.8 原签名覆盖升级的 v4.3.9，交付 Stage 459–461 的更新与 UI 修复，并完成当前环境能够真实实施的工作台、隐私锁、备份运维、WebDAV、性能和 Windows Native 任务。

## 安全边界

- `applicationId`、原签名、备份格式与历史存储键不变。
- 不关闭 TLS、DEX 验签或 ZIP 边界检查。
- 不迁移现有明文业务库或 WebDAV 凭据：缺少有锁屏凭据的物理设备，不能冒险执行不可充分恢复验证的数据迁移。
- Quick Tunnel 只记录为临时公网验证，不冒充固定域名生产服务。

## 验收标准

- AC-01：Stage 459–461 UI 与更新反馈进入 v4.3.9。
- AC-02：工作台旋转状态、勾选、拖入和快捷键实现并完成 Android 验收。
- AC-03：应用锁后台超时策略有单元测试，真实设备认证标记真实边界。
- AC-04：每日校验归档任务部署并手动触发成功，保留状态与哈希证据。
- AC-05：WebDAV 公网协议、跨端测试、压力和长时并发门禁通过。
- AC-06：Windows Native Host 与便携包成功构建并执行启动/退出检查。
- AC-07：真实 GitHub v4.3.8 APK → 候选 v4.3.9 原签名覆盖升级、数据保留、冷启动通过。
- AC-08：原 selfcheck、三端测试、产物身份/签名/哈希与 Release 回下载全部通过。
- AC-09：物理设备与固定域名若当前环境不存在，必须标记 `BLOCKED` 并保留探测证据，不写 PASS。

## 验收状态

- AC-01：PASS。
- AC-02：PASS（实现、编译、既有设备工作台与加密往返测试；旋转/快捷键为实现审查，未以物理键盘冒充实测）。
- AC-03：PASS / BLOCKED：后台策略与单测 PASS；物理设备凭据认证 BLOCKED。
- AC-04：PASS。
- AC-05：PASS（本机 JVM/API 34/公网 QA）；物理移动网络长期运行见 AC-09。
- AC-06：PASS（本机 Native smoke）；第二台干净 Windows 机器 BLOCKED。
- AC-07：PASS。
- AC-08：候选门禁 PASS，Release 回下载待发布后补充。
- AC-09：BLOCKED 证据已写入发布报告和 `evidence-4.3.9/external-gates.txt`。
