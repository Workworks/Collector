package com.kfaino.diapertracker

import android.app.Activity
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object ScenarioTemplateDialog {
    data class Template(val title: String, val categories: List<String>)
    val templates = listOf(
        Template("家庭物品", listOf("家电", "数码", "日用品", "清洁耗材")),
        Template("证件保单", listOf("身份证件", "家庭证照", "保险保单", "合同票据")),
        Template("食品药品", listOf("食品", "饮品", "药品", "应急物资")),
        Template("收藏品", listOf("藏书", "纪念品", "艺术收藏", "数字收藏")),
        Template("搬家盘点", listOf("待打包", "已装箱", "贵重随身", "待处理"))
    )

    fun merge(existing: List<String>, template: Template): List<String> =
        (existing + template.categories).map(String::trim).filter(String::isNotEmpty).distinct()

    fun show(activity: Activity, store: DataStore, onSaved: () -> Unit) {
        MaterialAlertDialogBuilder(activity).setTitle("选择一个使用场景")
            .setMessage("只补充需要的分类，不会删除你已有的分类。")
            .setItems(templates.map { "${it.title} · ${it.categories.joinToString("、")}" }.toTypedArray()) { _, index ->
                val selected = templates[index]
                store.saveCategories(merge(store.getCategories(), selected))
                Toast.makeText(activity, "已加入「${selected.title}」分类", Toast.LENGTH_SHORT).show()
                onSaved()
            }.setNegativeButton("取消", null).show()
    }
}
