# 缺陷追踪账本 (Bug List)

> **原则**：Bug 唯一入口。已修复标记 `[已修复 YYYY-MM-DD]`，写明现象、根因、修复与验证；每个修复配最小回归测试。历史条目长期保留，不删除。

---

## 缺陷记录

9. **[待处理 2026-08-31] 热更新入口不代表原生 WebDAV 修复可以下发**
   - 现象：用户希望通过热更新修复手机 PROPFIND 报错。
   - 检查：HotPatchEngine 的动态补丁公钥为空，创建的 DexClassLoader 没有接入 WebDAV 调用；线上 v4.3.0 的补丁 ZIP 只有清单及网页资源。
   - 处置：不发布无效 ZIP，不关闭验签；完整 APK 仍受原签名缺失阻塞。没有实现修复，不标记已修复。证据与后续验收见 [专项报告](../releases/webdav-hotfix-20260831.md)。

8. **[已修复 2026-08-31] Android WebDAV 连接探测拒绝 PROPFIND**
   - 模拟器真实调用报 `Expected one of [OPTIONS, GET, HEAD, POST, PUT, DELETE, TRACE, PATCH] but was PROPFIND`，尚未向服务发送请求。
   - 修复：探测备份文件改用 HEAD，保持首次不存在时 404、已有文件 200 成功，401/403 失败；上传下载仍为 PUT/GET。
   - 回归：`WebDavLiveDeviceTest` 在公网执行首次探测、错误密码、上传、下载和全部集合及附件恢复。

7. **[已修复 2026-08-31] 桌面 WebDAV 非标准 HTTP 方法无法发送**
   - JDK HttpURLConnection 拒绝 PROPFIND/MKCOL，连接测试在网络请求前失败；建目录异常被捕获后继续上传。
   - 联调 Spec：`docs/stages/webdav-integration-spec.md`；改用 JDK HttpClient 发送 PROPFIND，移除不再使用的旧远端目录创建。
   - 验证：新增方法契约及真实公网往返测试，保留既有恢复取消断言。

1. **[已修复 2026-08-20] 官方更新源优先级倒置缺陷**
   - **现象**：客户端检查更新时，第三方 CDN 代理节点可能排在官方 `api.github.com` 前面，存在劫持风险。
   - **根因**：候选列表未按安全信任级别排序。
   - **修复**：重构 `UpdateSource.kt`，强制将官方 GitHub API 置于列表第一位，第三方镜像仅作为超时 fallback。
   - **验证**：编写 `UpdateSourceTest` 单元测试，验证官方源始终排首位。

2. **[已修复 2026-08-20] 动态 DEX 补丁无验签加载缺陷**
   - **现象**：早期动态补丁只要文件存在即可通过 `DexClassLoader` 加载。
   - **根因**：缺少数字签名校验机制。
   - **修复**：在 `HotPatchEngine.kt` 中加入 `verifyDexSignature()`，基于 SHA256withRSA 执行强签名校验，验签失败直接 fail-closed 拒绝加载。
   - **验证**：编写 `HotPatchEngineTest`，模拟篡改补丁验证被成功阻断。

3. **[已修复 2026-08-20] 补丁解压路径穿越 (Zip Slip) 缺陷**
   - **现象**：解压恶意构造的 ZIP 补丁可能导致文件写入应用目录之外。
   - **根因**：未校验解压目标路径的规范绝对路径。
   - **修复**：抽出 `PatchArchive.safeUnzip()`，严格校验 `canonicalPath.startsWith(canonicalDestDir)` 并限制最大条目数与体积。
   - **验证**：编写 `PatchArchiveTest` 包含穿越路径用例，变异测试断言立即阻断。

4. **[已修复 2026-08-26] 全局搜索与首页 Banner 编译属性未解析**
   - **现象**：`GlobalSearchDialog.kt` 引用了不存在的 `MedicineRecord.notes`，`HomeFragment.kt` 使用了过时的 `singleLine` 属性导致编译报错。
   - **根因**：字段映射错误（药箱无 notes 字段，应为 targetAudience），Kotlin TextView 属性应为 `isSingleLine`。
   - **修复**：修正 `GlobalSearchDialog.kt` 字段引用与 `HomeFragment.kt` 属性为 `isSingleLine = true`。
   - **验证**：`gradlew testReleaseUnitTest` 与 `assembleRelease` 编译通过。

5. **[处理中 2026-08-30] 备份恢复与局域网数据完整性风险（Stage 453）**
   - 基线 R1–R5：匿名 HTTP 读写、桌面导入先覆盖后解析、Android 备份遗漏集合/附件、桌面合并丢字段/遗漏证照、看板 HTML 注入。
   - 修复与独立回归测试按 `docs/stages/stage-453-reliable-collection.md` 分项验证，未通过前不标记已修复。
6. **[修复验证中 2026-08-30] 双端订阅周期与恢复入口兼容**
   - Android 使用中文周期值，桌面枚举直接解析会退回按月并在保存时覆盖。需要显式双向映射及真实桌面仓储往返测试。
   - Android WebDAV 下载返回 Triple(ok, message, json)，原入口误把 message 当备份；现按第三项预览，取消不执行恢复。
   - 清空资产原来删除偏好键，缺少 tombstone；现保存空集合并记录删除，已有设备回归覆盖。

Stage 453 验证回填：上述 #5/#6 主要修复已实现；73 项自动化用例及六项 selfcheck 通过。新发现的订阅周期问题由 `SafeRestoreTest.androidYearlyCycleSurvivesDesktopEdit` 回归；WebDAV 预览取消由 `WebDavPreviewTest` 回归。原生交付与完整业务验收仍未关闭，详见 [阶段报告](../stages/stage-453-delivery-report.md)。

7. **[已修复 2026-08-31] OCR 回调覆盖并发整理或复活已删除记录**：CollectionWorkspaceDialog 将旧 JSONObject 整体 upsert。Stage 454 改为请求标识校验和现存记录字段补丁；DeferredWorkRegressionTest 验证删除不复活、旧请求不覆盖、标题保留、失败与重试。实际损坏图片 UI 操作仍待设备验收，见 [报告](../stages/stage-454-report.md)。

8. **[已修复 2026-08-31] Stage 455 桌面编译：位置输入框与 Swing Component.location 同名**：JPanel.apply 内解析成 Point，导致 add/text 类型错误。改为明确的 locationField，不改测试或门禁；最终双端构建通过，见 [验证记录](../stages/stage-455-report.md)。

9. **[已修复 2026-08-31] 导入失败残留新附件**：桌面批量导入逐个复制后才提交记录，后续文件失败会留下未引用副本。仅回滚本次新建文件，保留所有既有文件，新增失败路径回归。
10. **[已修复 2026-08-31] 过期家庭会话占用成员配额**：签发检查包含过期 token，持续使用可能无法再签发。签发前清理过期项，加入可控时钟及过期/重新授权回归。
11. **[待验证 2026-08-31] 隐私锁覆盖范围与文档不一致**：工程已有 MainActivity 生物识别入口，但不是完整数据加密；先核实可绕过的入口、无法认证行为和后台重锁，不能沿用“没有应用锁”的笼统描述。
12. **[已修复 2026-08-31] Android 家庭 HTTP 入口被系统默认策略阻断**：真实 API 34 测试返回 Cleartext HTTP traffic not permitted。启用局域网所需明文传输，同时家庭客户端仅接受私有 IPv4/loopback 明文地址，公网必须 HTTPS；保留失败日志和跨端回归。

Stage 456：#9/#10 仓储与会话回归通过；#12 实际 Android 客户端对桌面 HTTP 测试通过；#11 仅核实并完成威胁模型，完整锁保护仍待实施验收。见 [报告](../stages/stage-456-report.md)。


13. **[发布阻塞 2026-08-31] v4.3.6 tag 缺失本地已实现的安全与 WebDAV 修复**：HotPatchEngine 直接加载 DEX、解压缺少路径边界；Android 连接测试仍使用 PROPFIND。本地工作树与线上分叉，不能以本地安全测试结果代表已发布代码。原签名已验证一致；需隔离整合并回归后再发 4.3.7。见 [检查报告](../releases/v4.3.7-check.md)。
