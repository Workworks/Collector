# Stage 468：全局弹框视觉统一

日期：2026-09-05。

## 目标

将 Android 全部弹框收敛到一套克制、接近 Apple 系统层级的视觉语言：内容自适应、28dp 连续圆角感、轻量分层、清楚的主次操作，以及短促平滑的淡入缩放动画。

## 设计令牌

- 表面：沿用 Collecter 深浅主题的 `card_elevated`，不引入不兼容的透明模糊。
- 圆角：普通及自定义弹框统一 28dp；内部操作控件维持 12–14dp。
- 排版：标题 20sp / medium，正文 15sp / regular，正文使用次级色。
- 动效：进入 0.965 → 1.0、上移 8dp、240ms；退出 1.0 → 0.985、下移 4dp、160ms。
- 遮罩：约 42%，保持上下文可见但不干扰当前决策。

## 边界

- 不照搬 iOS 控件，不使用品牌不一致的纯白或毛玻璃图片。
- 不修改业务流程、按钮语义、数据和版本号。
- Material 弹框以全局主题覆盖；自定义弹框沿用现有布局但统一外壳和动画。
- 原生日期框和弃用的 `ProgressDialog` 必须退出主路径。

## 完成标准

- [x] AC-01：应用主题统一注入 Material 弹框表面、圆角、文字和窗口动画。
- [x] AC-02：深色和浅色主题均使用各自表面与文字令牌。
- [x] AC-03：自定义基础弹框、输入弹框和日期弹框与全局圆角及动效一致。
- [x] AC-04：业务代码不再使用 `ProgressDialog` 或直接使用 `DatePickerDialog`。
- [x] AC-05：静态审计覆盖所有弹框相关 Kotlin 文件，并列出例外。
- [x] AC-06：代表性弹框通过模拟器截图检查，Release、单测与 selfcheck 通过。

## 验收证据

- 全局静态审计：[evidence-468-dialog-audit.md](evidence-468-dialog-audit.md)。55 个相关 Kotlin 文件、189 个 `MaterialAlertDialogBuilder` 调用均受统一主题覆盖；旧式原生弹框命中为 0。
- 首次引导自定义弹框：[evidence-468-first-dialog.png](evidence-468-first-dialog.png) 与 [UI 层级](evidence-468-first-dialog.xml)。验证大圆角、分层操作和暗色遮罩。
- 标准长列表弹框：[evidence-468-list-dialog.png](evidence-468-list-dialog.png) 与 [UI 层级](evidence-468-list-dialog.xml)。验证标题、列表和次操作在统一外壳内显示。
- 实机动画录屏：[evidence-468-dialog-motion.mp4](evidence-468-dialog-motion.mp4)。记录“更多功能”弹框的进入、停留和退出，验证 240ms 进入与 160ms 退出动效实际生效。
- 模拟器首次启动曾暴露标题样式未继承 Material 布局属性的运行期崩溃；已改为继承 `MaterialAlertDialog.Material3.Title.Text` / `Body.Text`，重新安装、清数据启动及两类弹框复验均无 `AndroidRuntime` 崩溃。
- 完整门禁：`testReleaseUnitTest assembleRelease :desktop:test` 成功；82 个 Android Release 单测 0 失败；`tools/selfcheck.ps1` 六项全部通过，197 个模块 0 个未记录。
- 桌面端本阶段未改 UI：本次范围是 Android `MaterialAlertDialogBuilder` 与 Android 原生日期/进度弹框，`:desktop:test` 已回归通过。
