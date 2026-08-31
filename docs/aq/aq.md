# 架构问答账本 (Architecture Questions & Answers)

> **原则**：问答唯一入口。在原问题后答复并标记 `[已回复 YYYY-MM-DD]`，答案指向真实代码、命令或文档。

---

## 问答记录

1. **[已回复 2026-08-26] 为什么 Android 包名为 `com.kfaino.diapertracker` 而应用名叫 Collecter？**
   - **答复**：这是 Android 生态的历史遗留包名（`applicationId`）。在 Android 系统中，包名是应用的唯一数字身份凭据，修改包名将导致系统判定为全新应用，造成老用户覆盖升级时丢失全部本地数据与私有沙盒文件。因此 `applicationId` 和 `rootProject.name` 绝对禁止修改，应用展示名由 `strings.xml` 的 `app_name` 控制为 **Collecter**。详见 [`docs/codex-skills.md`](../codex-skills.md) 与 [`GEMINI.md`](../../GEMINI.md)。

2. **[已回复 2026-08-28] 桌面端为什么选择 Native WebView2 宿主 + 嵌入式引擎架构，而不是纯 Swing 或 Electron？**
   - **答复**：参考 `capital-agent-system/docs/design/29-standalone-edition-architecture.md` 的成熟结论：
     1. 纯 Swing 界面老旧、高分屏缩放差、难以复用移动端高定现代 UI 组件；
     2. Electron 需携带整个 Chromium 和 Node 运行时，打包体积通常超过 150MB~200MB，内存开销大；
     3. **Native WebView2 方案**直接利用 Windows 10/11 系统内置的 Evergreen WebView2 运行时，C++ 宿主仅几百 KB，兼顾原生系统托盘、无缝沉浸式毛玻璃视觉、极速冷启动与超低资源占用。详见 [`docs/design/05-desktop-standalone-architecture.md`](../design/05-desktop-standalone-architecture.md)。

3. **[已回复 2026-08-28] 为什么数据目录要放在 `%LOCALAPPDATA%\CollecterStandalone\data` 而不是安装目录？**
   - **答复**：Windows NSIS 卸载程序在卸载时会清空安装根目录。如果数据与程序混放，卸载旧版或重新安装时会导致用户多年的家庭资产、证件照片和流水数据被彻底删除。将数据独立存放在 `%LOCALAPPDATA%`，既符合 Windows 现代应用规范，又能确保卸载和覆盖升级时数据绝对安全。详见 [`docs/design/05-desktop-standalone-architecture.md`](../design/05-desktop-standalone-architecture.md)。
