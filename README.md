# 📦 Collecter (资产与收纳管家)

<p align="center">
  <b>轻量 · 极简 · 100% 离线私有 · 全生命周期个人资产与空间收纳管家</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android_8.0+-3DDC84?logo=android&logoColor=white" alt="Platform" />
  <img src="https://img.shields.io/badge/Language-Kotlin_1.9-7F52FF?logo=kotlin&logoColor=white" alt="Language" />
  <img src="https://img.shields.io/badge/Version-v2.6.0-10B981" alt="Version" />
  <img src="https://img.shields.io/badge/License-MIT-blue" alt="License" />
</p>

---

## 🌟 核心特性一览

- 📊 **资产看板 & 日均折旧成本**：支持「物品」与「周期订阅」双资产池，实时计算每一件数码/日用资产的**日均使用消费成本（元/天）**与二手回血收益。
- 📸 **实物照片与发票/保修卡留存**：100% 本地沙盒私有存储，自动纠偏压缩，沉浸式高清大图查看与系统分享。
- 🗺️ **空间平面图与寻物高亮穿梭**：可视化家庭空间多房间 Canvas 平面图，支持**由物找空间（呼吸脉冲高亮打点）**与**由空间看物品（房间在库资产抽屉）**双向穿梭。
- ⏳ **物品管理 4 维分类**：折旧资产 / 保质期物品（临期倒计时） / 长期耐用品 / 日常消耗品。
- 🔄 **周期订阅资产监控**：统一纳管 iCloud、ChatGPT Plus、宽带、年卡订阅，月均与年化支出一览无余，支持扣费前通知预警。
- 📦 **物品退役与待办归置**：支持闲鱼代售、转转二手、赠送亲友、封箱收藏与报废归置，记录二手回血收益。
- 📊 **多格式数据备份与导出**：一键生成带 UTF-8 BOM 的 **Excel 兼容 CSV 资产总表与流水表**，支持完整 JSON 备份与剪贴板极速恢复。
- 📳 **触感微动效 & 深浅模式**：全局线性马达微震反馈，完美适配深色暗黑与浅色明亮主题。
- 🚀 **GitHub Releases 在线热更新**：多镜像加速检测下载，后台静默预缓存与 0 秒秒级安装。

---

## 📚 项目维护与开发文档库 (`/docs`)

为了便于项目的长期维护与协作，所有技术与操作文档均归档在 [`/docs`](./docs) 目录下：

- 📖 **[用户使用手册 (docs/USER_MANUAL.md)](./docs/USER_MANUAL.md)**：完整功能使用指南与常见操作。
- 🏗️ **[系统架构与模块设计 (docs/ARCHITECTURE.md)](./docs/ARCHITECTURE.md)**：项目总体架构、核心模块分工与交互流程。
- 🗄️ **[数据模型与存储协议 (docs/DATA_MODELS.md)](./docs/DATA_MODELS.md)**：核心数据模型字段说明与 JSON 序列化规范。
- 🛠️ **[开发与维护指南 (docs/DEVELOPMENT_GUIDE.md)](./docs/DEVELOPMENT_GUIDE.md)**：开发环境、Gradle 构建、代码规范与排错指南。
- 🚀 **[版本发布与热更新手册 (docs/RELEASE_GUIDE.md)](./docs/RELEASE_GUIDE.md)**：GitHub Releases 发版规范与操作流程。
- 📋 **[版本更新日志 (docs/CHANGELOG.md)](./docs/CHANGELOG.md)**：全量版本演进历史与更新记录。

---

## 🚀 快速开始与编译

```powershell
# 编译 Release APK
.\gradlew.bat assembleRelease

# 安装到连接的设备/模拟器
adb install -r app/build/outputs/apk/release/app-release.apk
```

---

## 📄 开源许可

本项目采用 [MIT License](./LICENSE) 许可证。
