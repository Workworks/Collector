# Collecter — Agent 启动协议

本仓库所有 Coding Agent 的统一入口协议。**详细工程规范在 [docs/codex-skills.md](docs/codex-skills.md)，本文件不重复其内容。** Claude Code 另有专属自治规则见 [CLAUDE.md](CLAUDE.md)，Gemini / Antigravity 见 [GEMINI.md](GEMINI.md)。

不得依赖上一次会话的记忆代替实际读取。用户新增长期要求时同步维护本文件或 `codex-skills.md`。

---

## 1. 项目定位

**Collecter**（资产与收纳管家）是面向家庭与个人的全维度离线资产、生命周期与收纳管理系统。
- **Android 包名**：`com.kfaino.diapertracker` ⚠️ 历史遗留，**绝对禁止修改**（修改会导致用户覆盖升级丢失全部数据与私有沙盒）；
- **`rootProject.name`**：`Collecter`（2026-08-30 按用户明确要求由 DiaperTracker 改名）；Android applicationId、namespace 与存储标识保持不变，禁止随品牌改名替换。
- **存储模式**：100% 本地离线私有沙盒（SharedPreferences + 手写 JSON），无外部数据库、无中心化后端；
- **桌面单机版架构**：基于 `capital-agent-system` 架构体系（Native WebView2 + 嵌入式引擎 + 独立数据目录）。

---

## 2. 每次任务的执行顺序

### Step 1 加载规则
读 [docs/codex-skills.md](docs/codex-skills.md)，确认 Token 策略、测试门禁与 Git 策略。

### Step 2 检查当前阶段、任务账本与 Spec
依次读取 [docs/stage-current.md](docs/stage-current.md) 与 [docs/TODO.md](docs/TODO.md)：
- 前者给出当前 Stage 上下文和重点快照，后者给出按优先级组织的待办账本；
- 读 [Spec 驱动开发指南](docs/development/33-spec-driven-development.md)：凡新增功能、跨模块修复、界面重构、版本发布或数据变更，**实现前必须确认目标 Stage 报告或 Spec 已经写清目标、边界、验证方式和完成标准**。缺任一项先补 Spec，再写代码。

### Step 3 检查已知问题与 Git
- 执行 `git status --short`，识别用户已有修改与未跟踪文件——**严禁覆盖或丢弃不属于本任务的工作**；
- 读 [docs/bug/bugList.md](docs/bug/bugList.md) 与 [docs/aq/aq.md](docs/aq/aq.md)，存在相关未完成项时优先闭环。

### Step 4 执行任务
按 Spec 编号工作包实施：最小修改、最少读取、定向验证；发现需要扩大边界时先更新 Spec。

### Step 5 门禁与完成检查
- 运行门禁检查（`.\gradlew.bat testReleaseUnitTest` 与 `pwsh -File .\tools\selfcheck.ps1`）；
- 逐条回填 Spec 的完成标准（AC）与客观证据；未执行、未通过的条目不得勾选；
- 同步维护 `docs/TODO.md` 与 `docs/stage-current.md`。

### Step 6 输出报告
遵循标准报告格式：
```text
完成:
修改:
验证:
下一步:
```

---

## 3. 不可违反的红线

1. **禁止版本号通胀**：用户未明确说“发版/release/打包发布”，严禁修改 `versionName`/`versionCode`。
2. **禁止假发版**：模块未真实实现本次功能（如桌面端未对齐），严禁修改其版本号冒充全平台交付。
3. **安全性不可交换**：
   - 官方源 `api.github.com` 永远排在更新源第一位；
   - 动态加载 dex 必须强验签（SHA256withRSA，fail-closed）；
   - 解压必须防路径穿越（Zip Slip）。
4. **禁止静默吞异常**：严禁 `catch(_: Exception){}` 等空捕获，每个 catch 必须带上下文日志；涉及数据必须给用户可见反馈。
5. **验收工具不可动**：`tools/selfcheck.ps1` 与测试是客观证据来源，严禁修改脚本或测试断言伪造绿灯。
6. **禁止破坏用户工作树**：严禁 `git reset --hard` 或私自删除用户未跟踪修改。
