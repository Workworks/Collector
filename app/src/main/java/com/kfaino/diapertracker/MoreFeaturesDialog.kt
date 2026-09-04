package com.kfaino.diapertracker

import android.app.Activity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object MoreFeaturesDialog {
    fun show(activity: Activity, store: DataStore, onRefresh: () -> Unit) {
        val labels = arrayOf(
            "分类与场景模板", "多账本", "空间地图", "扫码查找", "盘点巡检",
            "生活流", "统计报表", "全部专业馆", "家庭协作", "数据导入与备份"
        )
        MaterialAlertDialogBuilder(activity).setTitle("更多功能").setItems(labels) { _, index ->
            when (index) {
                0 -> ScenarioTemplateDialog.show(activity, store, onRefresh)
                1 -> LedgerManager.showLedgerPicker(activity, onRefresh)
                2 -> FloorPlanDialog.show(activity, store, isSelectMode = false)
                3 -> (activity as? MainActivity)?.startQrScanner()
                4 -> InventoryAuditDialog.startAudit(activity, store, onRefresh)
                5 -> (activity as? MainActivity)?.navigateToLegacyTab(1)
                6 -> (activity as? MainActivity)?.navigateToLegacyTab(2)
                7 -> UniversalVaultCenterDialog.show(activity, store, onRefresh)
                8 -> FamilyClientDialog.show(activity)
                9 -> BulkImportDialog.show(activity, store)
            }
        }.setNegativeButton("关闭", null).show()
    }
}
