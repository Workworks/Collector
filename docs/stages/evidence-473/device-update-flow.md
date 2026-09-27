# Stage 473 API 34 更新权限返回验收

日期：2026-09-27  
设备：Android 14 / API 34 AVD `DiaperPhone`  
签名：原发布证书 SHA-256 `03d73e71eb13c82a820dc50f4ef780630aa698591944693ff6b7c1c3c3bb013a`

## 修复前

1. 安装正式 v4.3.10 / 47，缓存并验证正式 v4.3.11 APK。
2. 在“检查与智能升级”点击“安装包已就绪，立即秒装”。
3. 进入 Android “Install unknown apps”，开启 “Allow from this source” 后返回。
4. `dumpsys window` 当前焦点仍为 `com.kfaino.diapertracker/.MainActivity`，未出现 Package Installer。
5. `dumpsys package` 仍为 `versionName=4.3.10`、`versionCode=47`。

## 修复候选

1. 使用原发布签名构建带修复逻辑的 4.3.10 / 47 验收候选，仅用于让 GitHub 正式 v4.3.11 成为可检测的新版本；候选 APK 未纳入源码或发布附件。
2. 关闭 `REQUEST_INSTALL_PACKAGES` 特殊权限，进入更新页；正式 v4.3.11 缓存包通过大小及 SHA-256 校验。
3. 点击“安装包已就绪，立即秒装”，确认“前往开启”。
4. 在系统设置开启来源权限并返回。
5. `dumpsys window` 当前焦点变为：

```text
com.google.android.packageinstaller/com.android.packageinstaller.PackageInstallerActivity
```

6. UIAutomator 显示：

```text
Collecter
Do you want to update this app?
Cancel
Update
```

结论：授权结果已回传，待安装请求经二次校验后自动进入 Android 系统安装确认页；系统确认仍保留，没有静默安装。
