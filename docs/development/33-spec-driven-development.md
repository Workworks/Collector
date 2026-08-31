# Spec 驱动开发指南 (Spec-Driven Development)

> 凡新增功能、跨模块修复、界面重构、版本发布或数据结构变化，**实现前必须确认目标 Stage 报告或验收规格已经写清目标、边界、验证方式和完成标准**。
> 缺任一项先补 Spec，再写代码。

---

## 1. Spec 的核心要素

每个 Spec 必须包含以下四项：

### 1.1 目标 (Goal)
清晰描述用户可感知的端到端行为或系统能力，不使用“重构代码”、“增加工具类”等实现细节代替业务价值。

### 1.2 边界 (Scope & Non-Goals)
- **范围内**：明确本次改动涉及的模块与文件。
- **范围外**：明确本次不做的工作，防止范围蔓延。
- **安全与兼容不变量**：写明数据兼容、生物隐私、官方源等硬性约束。

### 1.3 验证方式 (Verification Plan)
- 单元测试命令与用例清单；
- 编译构建命令（`assembleRelease` / `:desktop:classes`）；
- 自动化门禁脚本（`pwsh .\tools\selfcheck.ps1`）；
- 真机/桌面端手工操作路径。

### 1.4 完成标准 (Acceptance Criteria, AC)
使用独立编号的可判定条目（如 `AC-01`、`AC-02`）：
- 每条完成标准必须支持明确的 `PASS / FAIL / BLOCKED` 判定；
- 必须有实际的代码、测试与日志证据对应；
- 未执行验证的条目严禁标记为 PASS。

---

## 2. 标准 Spec 模板

```markdown
# [Stage 编号]: [功能/特性名称] Spec

## 1. 目标
一句话描述用户可感知的端到端结果。

## 2. 影响范围
- Android 端：`app/...`
- 桌面端：`desktop/...`
- 数据层：是否变更 JSON / SharedPreferences
- 文档与教程：`TutorialDialog.kt` / `SingleFeatureTours.kt`

## 3. 工作包 (Work Packages)
- WP-01: [模块1实现]
- WP-02: [模块2实现]
- WP-03: [测试用例与教程同步]

## 4. 完成标准 (Acceptance Criteria)
- [ ] AC-01: [判定标准 1]
- [ ] AC-02: [判定标准 2]
- [ ] AC-03: [单元测试与门禁全绿]

## 5. 验收证据
（执行测试与 selfcheck 后回填输出）
```
