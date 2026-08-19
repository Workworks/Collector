# 🛠️ 开发者与维护开发指南 (DEVELOPMENT_GUIDE.md)

本文档为后续参与维护 **Collecter** 的开发者提供环境配置、代码规范、架构约定及常见构建调试说明。

---

## 1. 开发环境要求

- **JDK**: Java Development Kit 17 (推荐 OpenJDK 17 或 Microsoft JDK 17)
- **Android SDK**:
  - `compileSdk`: 34 (Android 14)
  - `minSdk`: 26 (Android 8.0 Oreo)
  - `targetSdk`: 34
  - `buildTools`: 34.0.0+
- **Gradle**: 8.2+ (Gradle Wrapper 已内置)
- **IDE**: Android Studio Hedgehog / Iguana / Jellyfish 或 VS Code / JetBrains Fleet / Antigravity

---

## 2. 快速拉取与构建

```powershell
# 1. 克隆代码仓库
git clone https://github.com/Workworks/Collector.git
cd Collector

# 2. 检查本地环境并编译 Debug 版本
.\gradlew.bat assembleDebug

# 3. 编译正式 Release 版本（已配置便捷签名，可直接安装测试）
.\gradlew.bat assembleRelease

# 4. 一键安装到连接的手机/模拟器
adb install -r app\build\outputs\apk\release\app-release.apk
```

---

## 3. 核心设计与代码规范

### 3.1 零外部侵入式依赖原则
- 本应用坚持极简离线设计，**不引入**冗余的 RxJava、大型 ORM、大型网络库等；
- 网络请求仅用于版本更新（`HttpURLConnection` 实现，无 OkHttp 依赖）；
- 图片采用私有沙盒安全存储与 `BitmapFactory` + `LruCache` 自研缓存引擎；
- 空间平面图采用自定义 `FloorPlanView` 直接在 Android 原生 Canvas 上进行矢量绘制。

### 3.2 UI 与微动效规范
- **禁止使用未样式化的原生 AlertDialog 文本列表**：所有弹窗均需使用 ViewBinding 创建独立卡片布局，使用 `dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))` 与 `R.style.CustomDialogAnimation`。
- **全局触感震动**：所有可点击控件、药丸选择器、关键操作按钮均需使用 `applyPressScaleAnimation(0.92f)`，并调用 `ViewExt.performAppHapticFeedback()` 提供线性马达反馈。
- **深浅模式语义色**：XML 布局中禁止硬编码 `#FFFFFF` 或 `#0F172A`，必须统一引用 `@color/background`、`@color/card`、`@color/input_bg`、`@color/text_primary` 等语义色。

### 3.3 数据向前兼容性规范
- `DataStore` 负责统一的 JSON 序列化与反序列化，新字段添加时必须提供安全的默认值（如 `optString("img_p", "")`），严禁破坏旧版本备份 JSON 的还原兼容性。
- 导出的 CSV 文件必须添加 `\uFEFF` UTF-8 BOM 头，确保 Microsoft Excel 打开时不产生乱码。

---

## 4. 常见问题排查

### 4.1 `assembleRelease` 找不到 Android SDK 路径？
- 确保根目录下或用户主目录下存在 `local.properties` 文件，并包含：
  ```properties
  sdk.dir=C\:\\Users\\YourUsername\\AppData\\Local\\Android\\Sdk
  ```

### 4.2 提示内存溢出 (OOM)？
- 图片处理已由 `ImageVaultHelper` 统一管控（最大边 1600px 压缩，列表使用 120x120 采样与 LruCache），请勿直接在主线程加载原始大图。
