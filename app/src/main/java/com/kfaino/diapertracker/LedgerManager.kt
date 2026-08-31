package com.kfaino.diapertracker

import android.app.Activity
import android.content.Context
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.json.JSONArray
import org.json.JSONObject

/**
 * 多账本独立空间管理体系 (Multi-Ledger Workspace Manager)
 * - 支持「个人私密资产」、「家庭公共仓储」、「办公室耗材」等空间账本无缝切换
 * - 各账本出入库流水、分类与统计完全独立隔离
 */
object LedgerManager {

    private const val PREFS_NAME = "collector_ledgers_v1"
    private const val KEY_CURRENT_LEDGER = "current_ledger_id"
    private const val KEY_LEDGERS_JSON = "ledgers_list_json"

    val DEFAULT_LEDGERS = listOf(
        Ledger(id = "default", name = "个人资产", icon = "🏠", desc = "个人私密物品、数码与日常消费"),
        Ledger(id = "family", name = "家庭仓储", icon = "👨‍👩‍👧", desc = "家庭公共日用品、食品与仓储"),
        Ledger(id = "office", name = "办公物资", icon = "💼", desc = "工作电脑、办公耗材与工具")
    )

    fun getCurrentLedger(context: Context): Ledger {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val curId = prefs.getString(KEY_CURRENT_LEDGER, "default") ?: "default"
        val all = getAllLedgers(context)
        return all.find { it.id == curId } ?: all.firstOrNull() ?: DEFAULT_LEDGERS.first()
    }

    fun setCurrentLedger(context: Context, ledgerId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CURRENT_LEDGER, ledgerId).apply()
    }

    fun getAllLedgers(context: Context): List<Ledger> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_LEDGERS_JSON, null) ?: return DEFAULT_LEDGERS
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<Ledger>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    Ledger(
                        id = o.optString("id"),
                        name = o.optString("name"),
                        icon = o.optString("icon", "📦"),
                        desc = o.optString("desc", ""),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            if (list.isEmpty()) DEFAULT_LEDGERS else list
        } catch (e: Exception) {
            DEFAULT_LEDGERS
        }
    }

    fun saveLedgers(context: Context, list: List<Ledger>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        for (l in list) {
            val o = JSONObject()
            o.put("id", l.id)
            o.put("name", l.name)
            o.put("icon", l.icon)
            o.put("desc", l.desc)
            o.put("createdAt", l.createdAt)
            arr.put(o)
        }
        prefs.edit().putString(KEY_LEDGERS_JSON, arr.toString()).apply()
    }

    /** 弹出账本快速切换与管理对话框 */
    fun showLedgerPicker(activity: Activity, onLedgerChanged: () -> Unit) {
        val all = getAllLedgers(activity)
        val current = getCurrentLedger(activity)
        val curIdx = all.indexOfFirst { it.id == current.id }.coerceAtLeast(0)

        val items = all.map { "${it.icon} ${it.name} (${it.desc})" }.toMutableList()
        items.add("➕ 新建自定义账本...")

        MaterialAlertDialogBuilder(activity)
            .setTitle("📚 切换账本空间")
            .setSingleChoiceItems(items.toTypedArray(), curIdx) { dialog, which ->
                if (which == items.size - 1) {
                    dialog.dismiss()
                    showCreateLedgerDialog(activity, onLedgerChanged)
                } else {
                    val target = all[which]
                    if (target.id != current.id) {
                        setCurrentLedger(activity, target.id)
                        Toast.makeText(activity, "已切换至【${target.icon} ${target.name}】", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        onLedgerChanged()
                    } else {
                        dialog.dismiss()
                    }
                }
            }
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun showCreateLedgerDialog(activity: Activity, onCreated: () -> Unit) {
        val input = EditText(activity).apply {
            hint = "例如: 仓库周转、摄影器材库"
            setPadding(40, 30, 40, 30)
        }

        MaterialAlertDialogBuilder(activity)
            .setTitle("➕ 新建独立账本")
            .setMessage("输入新账本名称：")
            .setView(input)
            .setPositiveButton("创建并切换") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    val newLedger = Ledger(
                        name = name,
                        icon = "📦",
                        desc = "自定义管理账本"
                    )
                    val all = getAllLedgers(activity).toMutableList()
                    all.add(newLedger)
                    saveLedgers(activity, all)
                    setCurrentLedger(activity, newLedger.id)
                    Toast.makeText(activity, "已创建并切换至【${newLedger.name}】", Toast.LENGTH_SHORT).show()
                    onCreated()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
