# 核心开发与编码规范指南 (Development Guide)

---

## 1. 架构与分层原则

1. **门面统一访问**：所有针对资产、空间、分类、12 馆数据的读写，必须通过 `DataStore` 门面发起，禁止直接越权修改私有存储文件。
2. **禁止静默吞异常**：
   - 严禁 `catch (_: Exception) {}` 或仅注释 `// Ignore` 的空捕获；
   - 捕获异常必须打印结构化日志 `android.util.Log.w(TAG, "做某操作失败 [参数上下文]", e)`；
   - 涉及用户数据读写的失败必须通过 Toast 或 MaterialAlertDialog 给用户明确可见反馈。
3. **UI 规范与复用**：
   - 弹窗必须复用 `ModernDialogHelper`，统一圆角微光卡片风格与平滑出入场动画；
   - 按压动效统一调用 `.applyPressScaleAnimation(0.92f)`；
   - 颜色必须引用 `@color/` 语义色板，禁止硬编码十六进制颜色值。

---

## 2. 教程同步四大铁律 (Rule 6)

新增任何用户可见功能，必须同步更新：
1. `TutorialDialog.kt`：新增 `TutorialItem` 图文教程；
2. `SingleFeatureTours.kt`：新增交互式跟手演练分支，支持随时退出；
3. `TourSandbox.kt`：注入的示例数据必须在退出时 100% 自动回滚清理；
4. `docs/manuals/20-user-guide.md`：同步补齐操作手册章节。
