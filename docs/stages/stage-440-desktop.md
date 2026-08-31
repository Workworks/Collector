# Stage 440: 桌面单机版架构立项与文档规范重构 Spec

- **状态**：`IN_PROGRESS`
- **目标版本**：`v4.4.0`
- **推进责任**：Agent

---

## 1. 目标

1. **立项桌面单机版（Collecter Standalone）**：引入 `capital-agent-system` 的单机版成熟架构，确立 Native WebView2 宿主 + 内嵌引擎 + 独立数据目录的标准体系；
2. **全量重构 `docs/` 文档约定**：对齐 `capital-agent-system` 的规范目录与分类管理；
3. **重构 `AGENTS.md`、`CLAUDE.md` 与 `GEMINI.md`**：形成全统一的 Agent 启动与执行协议。

---

## 2. 影响范围

- 规范与协议文件：`AGENTS.md`, `CLAUDE.md`, `GEMINI.md`
- 文档系统：重构 `docs/` 目录，建立 `design/`, `development/`, `stages/`, `manuals/`, `bug/`, `aq/`
- 设计方案：`docs/design/05-desktop-standalone-architecture.md`

---

## 3. 工作包 (Work Packages)

- **WP-440-1**：编写 `docs/design/05-desktop-standalone-architecture.md` 桌面端立项方案
- **WP-440-2**：重构 `docs/` 文档体系（`codex-skills.md`, `stage-current.md`, `TODO.md`, `BLOCKERS.md`, `bugList.md`, `aq.md`, `user-guide.md`）
- **WP-440-3**：重构 `AGENTS.md`、`CLAUDE.md`，参考重构 `GEMINI.md`
- **WP-440-4**：自检与回归验证

---

## 4. 完成标准 (Acceptance Criteria)

- [x] **AC-440-01**: `docs/design/05-desktop-standalone-architecture.md` 包含单实例、WebView2、嵌入式运行、数据目录隔离与 NSIS 打包完整设计；
- [x] **AC-440-02**: `docs/` 目录结构与 `capital-agent-system` 100% 对齐，具备 `codex-skills.md`、`stage-current.md`、`TODO.md`、`bug/`、`aq/`、`design/`、`development/`、`stages/`、`manuals/`；
- [x] **AC-440-03**: `AGENTS.md` 确立 6 步启动协议与红线，`CLAUDE.md` 确立长任务自治执行 loop 与 Completion Gate，`GEMINI.md` 保持 5 条铁律并融合治理规范；
- [x] **AC-440-04**: 运行 `pwsh .\tools\selfcheck.ps1` 验证门禁与文档覆盖率无破坏。
