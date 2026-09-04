package com.kfaino.diapertracker

import android.app.Activity
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object BulkImportDialog {
    fun show(activity: Activity, store: DataStore) {
        val options = arrayOf("从相册批量收集", "粘贴 CSV 建立库存", "恢复现有备份", "连续扫码收集")
        MaterialAlertDialogBuilder(activity).setTitle("批量开始")
            .setMessage("新内容只会追加；恢复备份仍会先预览。")
            .setItems(options) { _, index -> when (index) {
                0 -> (activity as? MainActivity)?.collectPhotosToInbox()
                1 -> showCsv(activity, store)
                2 -> (activity as? MainActivity)?.openBackupManager()
                3 -> (activity as? MainActivity)?.startInboxScanner()
            }}.setNegativeButton("取消", null).show()
    }

    private fun showCsv(activity: Activity, store: DataStore) {
        val input = EditText(activity).apply {
            hint = "名称,分类,位置,备注,数量\n纸巾,耗材,储物柜,,3"
            minLines = 7
        }
        MaterialAlertDialogBuilder(activity).setTitle("粘贴 CSV").setView(input)
            .setNegativeButton("取消", null).setPositiveButton("预览并导入") { _, _ ->
                val parsed = SimpleCsvImport.parse(input.text.toString())
                if (parsed.entries.isEmpty()) {
                    Toast.makeText(activity, "没有可导入的有效名称", Toast.LENGTH_LONG).show()
                } else {
                    MaterialAlertDialogBuilder(activity).setTitle("准备导入 ${parsed.entries.size} 条")
                        .setMessage("${parsed.entries.take(5).joinToString("\n") { "• ${it.brand} · ${it.category} · ${it.location}" }}${if (parsed.rejectedRows > 0) "\n另有 ${parsed.rejectedRows} 行缺少名称，将跳过" else ""}")
                        .setNegativeButton("返回", null).setPositiveButton("确认追加") { _, _ ->
                            store.saveAll(store.loadAll() + parsed.entries)
                            Toast.makeText(activity, "已追加 ${parsed.entries.size} 条，未覆盖原数据", Toast.LENGTH_LONG).show()
                            (activity as? MainActivity)?.refreshCurrentFragment()
                        }.show()
                }
            }.show()
    }
}
