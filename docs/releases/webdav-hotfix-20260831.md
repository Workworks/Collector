# WebDAV 移动端热修复发布检查

日期：2026-08-31。状态：BLOCKED，未发布。用户已明确授权发布；阻塞不是缺少发布授权。

## 目标、范围与完成标准

目标：让手机获得 WebDAV HEAD 连接修复，再由用户验证公网备份。优先评估现有热更新，不发布不能改变实际行为的资源包。

范围：检查当前热补丁执行路径、线上补丁资产、APK 签名兼容性；保留包名、签名连续性与强验签，保护现有混合工作树。没有修改应用代码、版本、远端 Release、标签或上传资产。

| AC | 验证方式 | 结果 |
| --- | --- | --- |
| 01 原生修复已存在 | 读取 WebDavSyncHelper.kt | PASS：连接探测为 HEAD |
| 02 热补丁能覆盖该调用 | 检查 HotPatchEngine 与 ProfileFragment 调用链 | BLOCKED：没有 WebDAV 补丁分派路径，DexClassLoader 仅创建/保存，未用于加载并执行替代实现 |
| 03 动态补丁可安全签发并被客户端接受 | 核对内置公钥及签名检查 | BLOCKED：当前 PATCH_PUBLIC_KEY_B64 为空；含 dex 补丁按既定安全规则拒绝安装/加载 |
| 04 完整 APK 可兼容覆盖升级 | 实际运行 Verify-AndroidRelease.ps1 | BLOCKED：线上 v4.3.0 与当前候选证书不同，退出码 1 |
| 05 用户现装 APK 身份及端到端验证 | adb devices、手机实测 | NOT RUN：无连接设备，截图不能确认安装版本和签名 |
| 06 发布并回读远端资产 | 门禁通过后才可上传 | NOT RUN：前置未满足，未创建无效发布 |

## 当前检查证据

1. `app/src/main/java/com/kfaino/diapertracker/HotPatchEngine.kt:61`：`PATCH_PUBLIC_KEY_B64 = ""`；239 行起遇空公钥返回 false。现在新建密钥不能让已经安装的旧 APK 自动信任它。
2. 全部源码搜索 `activeDexClassLoader` / `loadClass`：加载器字段只在本文件声明、初始化、清空；`ProfileFragment.kt:692` 直接调用 `WebDavSyncHelper.testConnection`，没有热补丁替换逻辑。仅把新类装进 ZIP 或创建 ClassLoader 并不构成方法热替换。
3. `gh release view --repo Workworks/Collector` 实查 latest 为 v4.3.0，包含 APK 和 611 字节 `hotupdate_v4.3.0.zip`。下载到内存列目录，只有 `manifest.json`（140 字节）、`README.md`（77 字节）、`web/index.html`（135 字节），没有 DEX。旧补丁能分发资源，不证明能修复原生 WebDAV。
4. 实际执行：

```powershell
.\scripts\Verify-AndroidRelease.ps1 -PreviousApk .\dist\release-4.3.3\previous\Collecter-Android-v4.3.0.apk -CandidateApk .\app\build\outputs\apk\release\app-release.apk
```

输出：

```text
Previous SHA256 certificate: 03d73e71eb13c82a820dc50f4ef780630aa698591944693ff6b7c1c3c3bb013a
Candidate SHA256 certificate: 98edb36c3e227cec56d525ae9e3e7aeafbf653ebccdc923804dd2031b8b5097e
SIGNATURE-NOT-COMPATIBLE：禁止发布；必须恢复原签名，不得卸载旧版绕过
```

5. 按文件名扫描指定工作区，未找到 keystore/jks 或补丁私钥文件；本机标准 `.android/debug.keystore` 存在，但产生的是不匹配的当前签名。未扫描无关磁盘、未输出私钥或密码。

## 后续解除条件

- 优先取得构建线上 v4.3.0 时的原 keystore 路径，通过本地安全方式提供签名参数；不要把私钥和口令发到聊天或提交 Git。原签名可能在最初构建电脑或其备份中。
- 若手机安装的不是线上 v4.3.0，先确认其版本与证书，不能把线上包比较结果直接当作手机实装证明。
- 完整 APK 路线：签名匹配后重新构建并运行 Android 测试、selfcheck、覆盖升级和数据保留验证，审查混合工作树后再发布；不能只给当前不兼容包改名上传。
- 真正原生热更新路线需要先发布具有受信公钥及补丁执行入口的基础 APK，再验证签名拒绝、版本匹配、回滚与修复行为；它不是本次给旧版直接上传一个 ZIP 就能完成的步骤。

本轮只做发布前检查与阻塞记录，没有业务代码改动，因此未重复运行完整构建/单元测试/selfcheck；不将以前通过结果当成本轮通过。桌面端未跟进：本次请求是移动端发布，保持其版本不变。
