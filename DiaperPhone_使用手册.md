# DiaperPhone 模拟器使用手册

> 纸尿裤记账（DiaperTracker）Android 模拟器环境使用指南

---

## 目录

1. [环境概览](#1-环境概览)
2. [快速启动](#2-快速启动)
3. [常用 ADB 命令](#3-常用-adb-命令)
4. [应用管理](#4-应用管理)
5. [截图与录屏](#5-截图与录屏)
6. [模拟器控制](#6-模拟器控制)
7. [故障排除](#7-故障排除)
8. [关键路径参考](#8-关键路径参考)

---

## 1. 环境概览

| 项目 | 详情 |
|------|------|
| AVD 名称 | DiaperPhone |
| 设备 | Pixel 6（1080×2400） |
| 系统 | Android 14（API 34），含 Google APIs |
| 架构 | x86_64 |
| 硬件加速 | WHPX（Windows Hypervisor Platform） |
| 模拟器版本 | 37.1.11.0 |
| 已安装应用 | 纸尿裤记账（`com.kfaino.diapertracker`） |
| SDK 路径 | `C:\Users\kfaino\AppData\Local\Android\Sdk` |

---

## 2. 快速启动

### 方式一：桌面快捷方式（推荐）

双击桌面上的 **DiaperPhone Emulator** 快捷方式即可启动模拟器。

### 方式二：命令行启动

```bash
C:\Users\kfaino\AppData\Local\Android\Sdk\emulator\emulator.exe -avd DiaperPhone
```

### 方式三：PowerShell 启动（指定 adb 路径后可直接用 adb）

```powershell
$env:PATH += ";C:\Users\kfaino\AppData\Local\Android\Sdk\platform-tools"
C:\Users\kfaino\AppData\Local\Android\Sdk\emulator\emulator.exe -avd DiaperPhone
```

> **提示：** 模拟器启动需要 30-60 秒，首次启动可能更长。

---

## 3. 常用 ADB 命令

以下命令均需先将 `platform-tools` 加入 PATH，或使用完整路径：

```
C:\Users\kfaino\AppData\Local\Android\Sdk\platform-tools\adb.exe <命令>
```

### 连接与状态

```bash
# 查看已连接设备
adb devices

# 查看模拟器详细信息
adb shell getprop ro.build.display.id

# 查看 Android 版本
adb shell getprop ro.build.version.release
```

### 安装与卸载应用

```bash
# 安装 APK
adb install "D:\test\DiaperTracker\Collecter-v2.0.apk"

# 覆盖安装（保留数据）
adb install -r "D:\test\DiaperTracker\Collecter-v1.1.apk"

# 卸载应用
adb uninstall com.kfaino.diapertracker
```

### 启动应用

```bash
# 启动纸尿裤记账主界面
adb shell am start -n com.kfaino.diapertracker/.MainActivity

# 强制停止应用
adb shell am force-stop com.kfaino.diapertracker

# 清除应用数据
adb shell pm clear com.kfaino.diapertracker
```

### 文件传输

```bash
# 推送文件到模拟器
adb push 本地路径 /sdcard/目标路径

# 从模拟器拉取文件
adb pull /sdcard/文件路径 本地路径
```

### 查看当前界面

```bash
# 查看当前前台 Activity
adb shell dumpsys window | findstr mCurrentFocus

# 查看当前焦点应用
adb shell dumpsys window | findstr mFocusedApp
```

---

## 4. 应用管理

### 查看已安装应用

```bash
adb shell pm list packages | findstr diapertracker
```

### 查看应用信息

```bash
adb shell dumpsys package com.kfaino.diapertracker
```

### 查看应用日志

```bash
# 实时查看日志（按 Ctrl+C 停止）
adb logcat | findstr diapertracker

# 清除日志缓冲区
adb logcat -c
```

---

## 5. 截图与录屏

### 截图

```bash
# 截图并保存到本地
adb exec-out screencap -p > "D:\test\screenshot.png"

# 截图保存到模拟器
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png "D:\test\screenshot.png"
```

### 录屏

```bash
# 开始录屏（最长 180 秒）
adb shell screenrecord /sdcard/recording.mp4

# 停止录屏：在另一个终端执行
adb shell kill -2 $(adb shell pgrep -f screenrecord)

# 拉取录屏文件
adb pull /sdcard/recording.mp4 "D:\test\recording.mp4"
```

---

## 6. 模拟器控制

### 模拟按键

```bash
# 返回键
adb shell input keyevent KEYCODE_BACK

# Home 键
adb shell input keyevent KEYCODE_HOME

# 音量+/- 
adb shell input keyevent KEYCODE_VOLUME_UP
adb shell input keyevent KEYCODE_VOLUME_DOWN

# 电源键（开关屏幕）
adb shell input keyevent KEYCODE_POWER
```

### 模拟触摸与输入

```bash
# 点击屏幕坐标 (540, 1200)
adb shell input tap 540 1200

# 滑动（从 x1,y1 到 x2,y2，持续 300ms）
adb shell input swipe 540 1800 540 600 300

# 输入文字
adb shell input text "Hello"

# 输入中文（需借助剪贴板）
adb shell am broadcast -a clipper.set -e text "中文内容"
```

### 模拟电话与短信

```bash
# 模拟来电
adb shell am start -a android.intent.action.CALL -d tel:10086

# 模拟收到短信
adb shell am broadcast -a android.intent.action.SENDTO -d sms:10086 --es sms_body "测试内容"
```

### 网络控制

```bash
# 查看网络状态
adb shell ifconfig

# 打开/关闭飞行模式
adb shell settings put global airplane_mode_on 1
adb shell am broadcast -a android.intent.action.AIRPLANE_MODE --ez state true
```

---

## 7. 故障排除

### 模拟器无法启动

| 问题 | 解决方案 |
|------|----------|
| "No accelerator found" | 确认 WHPX 已启用：运行 `emulator -accel-check` |
| 启动黑屏 | 尝试切换图形后端：`emulator -avd DiaperPhone -gpu swiftshader_indirect` |
| 内存不足 | 关闭其他虚拟机，确保系统至少有 4GB 可用内存 |

### ADB 连接问题

```bash
# 重启 ADB 服务
adb kill-server
adb start-server

# 确认设备已连接
adb devices
```

### 应用安装失败

```bash
# 检查 APK 完整性
# 重新构建 APK：在 DiaperTracker 项目目录下执行
# gradlew assembleDebug

# 确认 minSdk 兼容（模拟器 API 34，应用 minSdk 26）
adb shell getprop ro.build.version.sdk
```

### 性能优化

```bash
# 启动时分配更多内存（默认 2GB）
emulator -avd DiaperPhone -memory 4096

# 使用冷启动（不加载快照）
emulator -avd DiaperPhone -no-snapshot-load
```

---

## 8. 关键路径参考

| 项目 | 路径 |
|------|------|
| Android SDK | `C:\Users\kfaino\AppData\Local\Android\Sdk` |
| 模拟器 | `C:\Users\kfaino\AppData\Local\Android\Sdk\emulator\emulator.exe` |
| ADB | `C:\Users\kfaino\AppData\Local\Android\Sdk\platform-tools\adb.exe` |
| AVD 配置 | `C:\Users\kfaino\.android\avd\DiaperPhone.avd\` |
| 系统镜像 | `C:\Users\kfaino\AppData\Local\Android\Sdk\system-images\android-34\google_apis\x86_64\` |
| 项目源码 | `D:\test\DiaperTracker\` |
| APK 文件 | `D:\test\DiaperTracker\纸尿裤记账-debug.apk` |
| 桌面快捷方式 | `C:\Users\kfaino\Desktop\DiaperPhone Emulator.lnk` |

---

## 快速参考卡片

```bash
# ★ 一键启动并安装应用
C:\Users\kfaino\AppData\Local\Android\Sdk\emulator\emulator.exe -avd DiaperPhone &
timeout 60
C:\Users\kfaino\AppData\Local\Android\Sdk\platform-tools\adb.exe install -r "D:\test\DiaperTracker\纸尿裤记账-debug.apk"
C:\Users\kfaino\AppData\Local\Android\Sdk\platform-tools\adb.exe shell am start -n com.kfaino.diapertracker/.MainActivity

# ★ 快捷截图
C:\Users\kfaino\AppData\Local\Android\Sdk\platform-tools\adb.exe exec-out screencap -p > "D:\test\screenshot.png"
```

---

*手册版本：v1.0 | 创建日期：2026-08-18 | 适用于 DiaperPhone AVD（API 34）*
