# 单机桌面版（Collecter Standalone）架构设计

> **立项背景**：为实现资产与收纳管家全平台战略，彻底解决既有 Swing 轻量客户端与 Android 端功能体验脱节的问题，本项目全面采纳 `capital-agent-system` 的单机版成熟架构体系，立项构建新一代 **Collecter Standalone 桌面单机版**。

---

## 1. 设计目标与核心事实

### 1.1 核心目标
- **一个 EXE，装完即用**：不依赖外部 Docker、不配复杂环境、无繁琐前置依赖。
- **全平台 100% 数据互通**：使用与移动端完全一致的数据模型与 JSON 备份结构，支持两端数据无缝迁移互传。
- **现代化视觉与原生性能**：基于 Native WebView2 框架与沉浸式 UI，彻底告别老旧 Swing 渲染卡顿。
- **绝对数据安全**：本地离线持久化，数据与安装目录物理隔离。

### 1.2 架构决策矩阵（对齐 capital-agent-system）

| 架构维度 | 传统桌面模式 | Collecter Standalone（采纳方案） | 收益与依据 |
| :--- | :--- | :--- | :--- |
| **外壳形态** | Swing / JavaFX | **Native C++ (WebView2) 宿主 + 嵌入式引擎** | 内存占用低、渲染颜值高、支持系统托盘与原生毛玻璃效果 |
| **进程模型** | 多进程/外部服务 | **单体嵌入式进程（`127.0.0.1` 高位端口）** | 极简分发，随 App 启停，无端口外网暴露风险 |
| **数据格式** | SQLite / H2 双方言 | **统一 JSON / JSONL + 私有沙盒图片存储** | 与移动端零方言分裂，一套序列化逻辑两端通用 |
| **数据目录** | 安装目录同级 | **`%LOCALAPPDATA%\CollecterStandalone\data`** | 卸载程序绝不触碰用户数据，覆盖升级零风险 |
| **安装打包** | 简单 zip 压缩包 | **`jpackage` JRE 裁剪 + NSIS 一键安装包** | 内置极简 JRE，用户机器无需预装 Java 环境 |

---

## 2. 形态与进程模型

```
Collecter-Standalone-Setup.exe   （NSIS 一键安装包）
└── 安装至 %PROGRAMFILES%\CollecterStandalone
    ├── CollecterWindow.exe      （Native C++ 宿主外壳）
    ├── runtime/                 （jpackage 裁剪后的 JRE 17/21 运行时）
    ├── app/
    │   ├── collecter-server.jar （嵌入式核心引擎 + Web 静态资产）
    │   └── branding/            （高清应用图标、托盘徽标与资源）
    └── updater/                 （增量热更新与自升级工具）
```

### 2.1 运行时数据目录隔离
数据目录**严格独立于安装目录**：
- Windows: `%LOCALAPPDATA%\CollecterStandalone\data`
- Linux: `~/.local/share/collecter-standalone/data`
- macOS: `~/Library/Application Support/CollecterStandalone/data`

```
%LOCALAPPDATA%\CollecterStandalone\data/
├── collecter_data.json         （核心资产与 12 馆主库数据）
├── item_vault/                 （实物照片与票据私有沙盒）
├── backups/                    （自动定时循环备份包）
└── logs/                       （运行日志）
```

> ⚠️ **硬性铁律**：NSIS 卸载脚本只删除安装目录，**严禁触碰数据目录**，并在卸载完成页面明确提示数据保留位置。

---

## 3. Native WebView2 宿主设计 (`CollecterWindow.cpp`)

参考 `capital-agent-system/capital-standalone/src/main/native/webview2/CapitalAgentWindow.cpp`：

1. **单实例互斥（Single-Instance Mutex）**：使用 `CreateMutexW(..., L"CollecterStandaloneMainWindowMutex")` 防止重复拉起。重复启动时自动将已有窗口唤起并置顶。
2. **系统托盘驻留（System Tray）**：
   - 支持关闭窗口最小化至系统托盘；
   - 托盘右键菜单：`打开资产看板`、`局域网互传大屏`、`今日待办预警`、`检查更新`、`退出应用`；
   - 托盘徽标动态感知时效预警（有临期待办时显示橙黄色小徽标）。
3. **启动页探测与平滑加载（Splash Loading & Probe）**：
   - 启动初期展示品牌 Loading 页面，通过 WinHTTP 轮询后台探测端点 `http://127.0.0.1:<PORT>/api/v1/health`；
   - 探测成功后无缝切换加载主 UI 界面，避免出现浏览器原生 404/连接拒绝白屏。

---

## 4. 核心功能与 12 大收纳馆全面覆盖

桌面端单机版与移动端共享 **第一性原理 12 大收纳馆**：
1. 🎟️ 时效权益与卡券票据馆
2. 🪪 家庭证照与契约安全夹
3. 💊 家庭智能健康药箱
4. 🥦 冰箱冷冻与生鲜食材鲜度库
5. 🏆 成长履历与荣誉勋章馆
6. 👗 四季换季衣橱与穿搭舱
7. 🚨 家庭应急防灾与生命线舱
8. 🔧 工具五金与设备维保日历舱
9. 🪴 绿植花卉与水肥养护舱
10. 🐾 萌宠档案与健康耗材舱
11. 📚 书房藏书与阅读收纳舱
12. 🍷 茶窖名酿与适饮时效舱

---

## 5. 打包流水线与分发规范

通过 PowerShell 构建脚本 `scripts/Package-CollecterStandalone.ps1` 自动化执行：
1. 编译前端生产静态资源并打包至 jar 资源目录；
2. 编译 Native WebView2 C++ 宿主 `CollecterWindow.exe`；
3. 执行 `jpackage` 输出免安装 app-image；
4. 调用 NSIS 生成独立安装程序 `Collecter-Desktop-Setup-<version>.exe`；
5. 计算 SHA-256 校验和并生成发布清单。
