# 缺陷追踪账本 (Bug List)

> **原则**：Bug 唯一入口。已修复标记 `[已修复 YYYY-MM-DD]`，写明现象、根因、修复与验证；每个修复配最小回归测试。历史条目长期保留，不删除。

---

## 缺陷记录

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
