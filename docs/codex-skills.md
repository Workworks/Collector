# 工程规范（Agent 长期约束）

所有 Coding Agent 的详细执行规则。启动流程见根目录 [AGENTS.md](../AGENTS.md)，当前阶段见 [stage-current.md](stage-current.md)，可执行任务账本见 [TODO.md](TODO.md)。本文件是规范正文，入口文件不重复内容。

---

## 1. 事实源顺序

冲突时不得静默选择，按此优先级并在报告中说明取舍：

1. **用户当前明确指令**
2. **本文件与 [AGENTS.md](../AGENTS.md) / [GEMINI.md](../GEMINI.md)**
3. **[路线图](stages/stage-roadmap.md) 与当前 Stage 规格报告（[stage-current.md](stage-current.md)）**
4. **[bugList](bug/bugList.md) 与 [aq](aq/aq.md) 中的未完成项**
5. **[系统架构](design/02-system-architecture.md) · [数据模型](design/03-data-models-design.md) · [桌面单机架构](design/05-desktop-standalone-architecture.md)**
6. **用户手册与测试开发规范**

`stage-current.md` 是当前上下文与重点快照，`TODO.md` 是按优先级和推进责任组织的执行账本；二者都不是 Stage 状态的独立真相来源。每次任务必须读取两者，发现漂移时以 Stage 报告为准同步修复。

---

## 2. Token 策略

- **最小读取**：只读与当前任务直接相关的文件与章节；不为“了解全貌”盲目通读不相关的代码库。
- **最小修改**：只改任务要求的范围，不顺手重构无关代码，不引入非必要的依赖。
- **最小测试**：按变更范围选择测试层级，但不得用低层测试代替高层闭环。
- **证据存底**：验证结果、哈希指纹与关键日志进入 Stage 报告或证据文档，上下文只保留精炼摘要。

---

## 2a. Spec 驱动开发（强制）

执行细则见 [Spec 驱动开发指南](development/33-spec-driven-development.md)。新增功能、跨模块修复、界面重构、版本发布或数据迁移**必须先确认有可执行 Spec**。

实现前必须同时满足：
1. **目标**：描述用户可观察的实际结果，不用“新增接口/优化代码”代替。
2. **边界**：写清范围内、范围外、外部前置、安全不变量和数据兼容性决策。
3. **验证方式**：逐项写明环境、操作、预期和证据，区分自动化单测与真机/桌面端实际验证。
4. **完成标准**：使用可判定条目并编号（如 `AC-01`、`AC-02`），每条都能映射到实现工作包和验证证据。

实施只能在 Spec 边界内推进。完成时逐条回填 `PASS / FAIL / BLOCKED` 与真实证据，未执行不得写 PASS。

---

## 3. Bug 与问答账本

- **Bug 唯一入口**：[bug/bugList.md](bug/bugList.md)，已修复标记 `[已修复 YYYY-MM-DD]`，写明现象、根因、修复与验证；每个修复必须配最小回归测试。历史条目长期保留，不删除。
- **问答唯一入口**：[aq/aq.md](aq/aq.md)，在原问题后答复并标记 `[已回复 YYYY-MM-DD]`，答案指向真实代码、命令或文档。
- **测试/构建新发现缺陷**：先登记入 `bugList.md` 再修复，严禁私自绕过或掩盖。

---

## 4. 门禁与测试策略

按变更范围执行门禁，未执行的不得写为 PASS：

| 变更范围 | 门禁要求 | 验证命令 |
| :--- | :--- | :--- |
| **Android 端 (:app)** | 单元测试全绿、`assembleRelease` 编译成功、布局硬编码颜色检查、空 catch 检查 | `.\gradlew.bat testReleaseUnitTest`<br/>`.\gradlew.bat assembleRelease` |
| **桌面端 (:desktop)** | 模块编译通过、数据模型往返测试、本地存储初始化与 WebView2 探测通过 | `.\gradlew.bat :desktop:classes`<br/>`.\gradlew.bat :desktop:test` |
| **跨端数据兼容** | JSON 备份导入导出 100% 互通、缺字段安全默认值、CSV 带 UTF-8 BOM | `BackupCodecTest.kt` 全部用例 |
| **安全不变量** | 官方源优先（GitHub API 第一位）、动态 dex 强验签、解压防 Zip Slip | `UpdateSourceTest`、`PatchArchiveTest` |
| **整体自检** | 运行交付前自检脚本，输出原样贴入汇报 | `pwsh -File .\tools\selfcheck.ps1` |

---

## 5. Git 与工作区管理

- **绝不破坏用户工作树**：禁止 `git reset --hard`、丢弃未跟踪修改或强制覆盖用户已有变更。
- **禁止提交敏感凭据**：WebDAV 密码、私钥、Token 严禁硬编码进代码库。
- **修改原子性**：只提交与本次 Spec 明确相关的文件变更。
