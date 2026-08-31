# 全景教程与互动导览开发指南 (Tutorial & Interactive Tour Guide)

---

## 1. 教程同步四大铁律

新增任何用户可见特性时，必须同步：
1. **`TutorialDialog.kt`**：在 `TUTORIAL_LIST` 中追加图文条目；
2. **`SingleFeatureTours.kt`**：在 `startSingleFeatureTour(key)` 中追加聚光灯引导与步骤交互；
3. **`TourSandbox.kt`**：演练如需注入示例数据，退出时必须调用 `cleanup()` 原样全量清空；
4. **`docs/manuals/20-user-guide.md`**：同步追加用户使用手册章节。

---

## 2. 演练开发示例规范

```kotlin
// SingleFeatureTours.kt
"my_feature_key" -> {
    guideTour.startCustomTour(listOf(
        TourStep(
            targetViewId = R.id.btn_target,
            title = "✨ 特性标题",
            description = "清晰明了的跟手操作说明，引导用户完成操作。",
            gravity = TourGravity.BOTTOM
        )
    ), onTourEnd = {
        TourSandbox.cleanupDemonstrationData(requireContext())
    })
}
```
