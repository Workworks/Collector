# Collecter (纸尿裤记账) - 版本发布与热更新操作手册

本手册详细介绍了如何为 **Collecter** 应用打包、发布新版本以及通过 GitHub Releases 实现客户端一键在线热更新。

---

## 📌 项目与仓库信息

| 项目属性 | 对应信息 |
| :--- | :--- |
| **GitHub 仓库** | [https://github.com/Workworks/Collector](https://github.com/Workworks/Collector) |
| **Releases 页面** | [https://github.com/Workworks/Collector/releases](https://github.com/Workworks/Collector/releases) |
| **应用包名** | `com.kfaino.diapertracker` |
| **默认更新源** | `Workworks/Collector` |

---

## 🚀 极简发布流程（CLI 一键发布）

日常发布只需以下 3 步：

```powershell
# 1. 编译打包生成 Release APK
.\gradlew.bat assembleRelease

# 2. 提交代码变更
git add .
git commit -m "chore: bump version to v2.2"
git push origin main

# 3. 使用 GitHub CLI 一键发布 Release 并上传 APK
$env:Path = "C:\Program Files\GitHub CLI;$env:Path"
gh release create v2.2 "app\build\outputs\apk\release\app-release.apk#Collecter-v2.2.apk" --title "Collecter v2.2 发布" --notes "### 🌟 更新说明`n- 优化了记账体验`n- 修复了已知问题"
```

---

## 📖 完整发布步骤详解

### 第一步：修改应用版本号

打开 [`app/build.gradle.kts`](file:///D:/test/DiaperTracker/app/build.gradle.kts)，找到 `defaultConfig` 代码块修改版本：

```kotlin
defaultConfig {
    applicationId = "com.kfaino.diapertracker"
    minSdk = 26
    targetSdk = 34
    versionCode = 3          // 递增整数（如：2 -> 3）
    versionName = "2.2"      // 用户可见版本号（如："2.1" -> "2.2"）
}
```

> **提示**：更新检测引擎采用语义化版本对比（如 `2.2` > `2.1`），Release Tag 必须高于手机端当前已安装版本。

---

### 第二步：编译打包 APK

在项目根目录下运行 Gradle 编译命令：

```powershell
.\gradlew.bat assembleRelease
```

编译完成后，产物将生成在：
`app/build/outputs/apk/release/app-release.apk`

---

### 第三步：发布 Release

#### 方式 A：通过 GitHub CLI 发布（推荐，最快）

打开 PowerShell 执行以下命令：

```powershell
# 引入 GitHub CLI 环境变量
$env:Path = "C:\Program Files\GitHub CLI;$env:Path"

# 发布 Release 并挂载 APK 附件
gh release create v2.2 "app\build\outputs\apk\release\app-release.apk#Collecter-v2.2.apk" `
  --title "Collecter v2.2 发布" `
  --notes "### 🌟 Collecter v2.2 更新说明`n- 🚀 新增功能描述`n- 🛠️ 体验优化与 Bug 修复"
```

#### 方式 B：通过 GitHub 网页端发布

1. 打开浏览器访问：[https://github.com/Workworks/Collector/releases/new](https://github.com/Workworks/Collector/releases/new)
2. **Choose a tag**：输入新版本 Tag（例如：`v2.2`），点击 *Create new tag*；
3. **Target**：选择 `main` 分支；
4. **Release title**：填写标题，如 `Collecter v2.2 发布`；
5. **Description**：填写更新日志（Markdown 格式）；
6. **Attach binaries**：将打包好的 `app-release.apk` 拖入附件上传区域，建议重命名为 `Collecter-v2.2.apk`；
7. 点击底部 **【Publish release】** 完成发布。

---

## 📱 客户端在线更新机制说明

1. **检查更新入口**：
   - 打开 App -> 切换到底栏【我的】-> 点击【检查新版本 (GitHub)】。
2. **版本号比对规则**：
   - 自动获取 Release 中的 `tag_name`，剔除 `v`/`V` 前缀后与手机当前运行的 `versionName` 逐位对比。
3. **镜像加速下载**：
   - 内置直连与镜像备用通道（`ghfast.top`、`ghproxy.net`），保障在不同网络环境下均可高速下载。
4. **自动安装与权限**：
   - 下载完成后通过 Android `FileProvider` 自动拉起系统安装器；
   - Android 8.0+ 首次安装会引导开启“允许安装未知应用”权限。
5. **切换/修改目标仓库**：
   - 在“我的”页面**长按**【检查新版本】栏目，可直接弹出仓库设置弹窗，自定义目标 GitHub 仓库。

---

## ❓ 常见问题排查 (FAQ)

### 1. 点击“检查更新”提示“当前已是最新版本”？
- **原因**：GitHub 上的 Release Tag 版本号小于或等于当前手机已安装版本。
- **解决**：确保发布的 Tag 为更高版本（如当前为 `2.1`，发布 `v2.2` 或更高）。

### 2. 提示“未包含可供安装的 .apk 附件”？
- **原因**：发布 Release 时未上传文件名以 `.apk` 结尾的文件。
- **解决**：编辑该 Release，将编译出的 `.apk` 文件重新拖拽上传为附件。

### 3. GitHub CLI 权限报错？
- **解决**：运行 `gh auth login` 重新授权，或运行 `gh auth status` 确认当前登录账号为 `Workworks`。
