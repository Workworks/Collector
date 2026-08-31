# 测试计划与持续集成门禁 (Test Plan & Verification)

---

## 1. 测试层级与覆盖范围

```mermaid
graph TD
    T1["1. 单元测试 (JUnit 4/5)<br/>- BackupCodecTest: 往返兼容性<br/>- UpdateSourceTest: 官方源优先级<br/>- PatchArchiveTest: 防 Zip Slip<br/>- AnalyticsQueriesTest: 财务算法"] --> T2["2. 静态检查与架构门禁<br/>- 布局硬编码颜色扫描<br/>- 空 catch 吞异常扫描<br/>- 模块文档覆盖率 100% 校验"]
    T2 --> T3["3. 自动化全量构建<br/>- Android release APK 构建<br/>- 桌面端 Standalone 构建"]
    T3 --> T4["4. 真实环境与端到端验收<br/>- 桌面单机版 WebView2 探测<br/>- Android 真机/模拟器操作流"]
```

---

## 2. 交付前必跑自检指令

```powershell
pwsh -File .\tools\selfcheck.ps1
```

- **验证项**：
  1. `assembleRelease` 编译是否通过；
  2. `testReleaseUnitTest` 单元测试是否 100% 绿灯；
  3. 布局文件硬编码颜色检查（除相机取景框例外外 0 处）；
  4. 源码空 catch 扫描（0 处）；
  5. 版本号多处一致性校验；
  6. 系统核心模块文档记录覆盖率（100% 覆盖）。
- **指纹约束**：自检脚本自带 SHA 指纹防篡改机制，严禁修改测试断言或自检工具来伪造检查通过。
