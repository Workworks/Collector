# GEMINI.md — Collecter 项目最高约束与执行协议

> 本文件优先级**高于**单次会话里的临时口头指令（如“快一点”“顺手发个版”）。
> 临时指令可以扩大任务范围，但不能豁免任何一条铁律。冲突时先指出，等用户确认再执行。
>
> 详细工程规范见 [`docs/codex-skills.md`](docs/codex-skills.md)，当前阶段见 [`docs/stage-current.md`](docs/stage-current.md)，待办账本见 [`docs/TODO.md`](docs/TODO.md)。

---

## 1. 项目定位与事实

| 项 | 值 / 事实 |
| :--- | :--- |
| **应用名称** | **Collecter**（资产与收纳管家） |
| **Android 包名** | `com.kfaino.diapertracker` ⚠️ 历史遗留，**绝对禁止修改**（改包名用户升级丢失全部数据） |
| **`rootProject.name`** | `DiaperTracker` ⚠️ 严禁修改 |
| **版本真相源** | `app/build.gradle.kts` 的 `versionName`（其他文档与发布说明均对齐它） |
| **数据持久化** | 100% 本地离线私有沙盒（SharedPreferences + 手写 JSON），无外部数据库、无后端 |
| **桌面单机版架构** | 基于 `capital-agent-system` 体系（Native WebView2 + 嵌入式引擎 + 独立数据目录） |

---

## 2. 五条不可协商的铁律（违反任意一条 = 本次任务失败）

### 🔴 1. 禁止版本号通胀
- 只有用户明确说“发版 / release / 打包发布”时，才允许修改 `versionName` / `versionCode`。**“加个功能” ≠ “发个版”**。
- 单次发版最多 +1 个 patch 位。minor 位需用户明确同意，major 位**永远**需用户批准。

### 🔴 2. 禁止假发版
- 若某模块（如桌面端）未完整实现本次功能，**严禁修改其版本号并假装全平台发版**。
- 必须在发布说明中明确注明「Android 端独占」或对应模块的实际完成度。

### 🔴 3. 安全性不可交换
- **官方源永远第一**：`api.github.com` / `github.com` 必须置顶，第三方 CDN 代理只能作为 fallback。
- **动态 DEX 必须强验签**：`DexClassLoader` 加载任何补丁前必须通过 `SHA256withRSA` 验签，失败即 fail-closed 熔断回滚。
- **解压必须防路径穿越**：解压 zip 必须经过 `PatchArchive.safeUnzip()` 校验规范路径与体积上限。

### 🔴 4. 禁止静默吞异常
- 严禁 `catch (_: Exception) {}` 等空捕获（包含仅有一行 `// Ignore` 的伪处理）。
- 每个 catch 至少记录带完整上下文参数的 `Log.w(TAG, "...", e)`；涉及用户数据读写失败必须给出可见提示。

### 🔴 5. 改动前读 Spec，交付前跑自检，汇报说实话
- 严禁把“编译通过”说成“功能已验证”；
- 验收标准有几条，汇报里就必须对照几条（每条标 ✅ / ❌ / ⬜）；
- **附则：验收工具不可动**。`tools/selfcheck.ps1` 与单元测试是客观证据来源，禁止修改脚本断言伪造绿灯。

---

## 3. 每次任务的执行顺序（6 步协议）

```text
Step 1: 读 docs/codex-skills.md 加载规则与门禁
Step 2: 读 docs/stage-current.md、docs/TODO.md，确认 Spec 目标与 AC
Step 3: git status 检查工作树，读 docs/bug/bugList.md 与 docs/aq/aq.md
Step 4: 按 Spec 实施最小修改与定向验证
Step 5: 运行门禁 (testReleaseUnitTest 与 selfcheck.ps1)，回填 AC 与证据，更新 TODO/Stage
Step 6: 输出标准汇报
```

---

## 4. 交付自检与汇报规范

**交付前必跑：**
```powershell
pwsh -File .\tools\selfcheck.ps1
```

**汇报标准模板：**
```text
完成:
- （完成的工作包与核心成果）

修改:
- （涉及修改与新增的文件列表）

验证:
- （粘贴 selfcheck.ps1 完整输出，包含指纹）
- （测试用例通过情况）

下一步:
- （TODO.md 中下一个待办事项）
```

---

## 5. 长期记忆与平台策略约束 (Long-Term Memory)

1. **📱 Android 端优先演进**：iOS 端相关开发与分发任务已明确全部屏蔽，所有 Stage（460~500）100% 聚焦 Android 原生架构与桌面单机版（Windows/Linux）。
2. **🧠 100% 端侧离线 AI**：视觉识物、意图检索、向量嵌入、说明书解析等全部采用端侧轻量计算，严禁引入中心化第三方 AI 云服务。
3. **🔒 零知识与去中心化**：数据存储于 Android 私有沙盒，局域网直连或 WebDAV 同步，确保用户数据资产主权。
