package com.kfaino.diapertracker

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kfaino.diapertracker.databinding.DialogKitChecklistBinding
import com.kfaino.diapertracker.databinding.DialogKitManagerBinding
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

/**
 * 🎒 场景化装备/出行套装与沉浸式打包核对管理器 (Kit & Bundle Manager)
 */
object KitManager {

    data class Kit(
        val id: String = UUID.randomUUID().toString(),
        val name: String,
        val icon: String = "🎒",
        val desc: String = "",
        val itemIds: List<String> = emptyList(),
        val createdAt: Long = System.currentTimeMillis()
    )

    private const val PREFS_NAME = "collector_kits_prefs"
    private const val KEY_KITS_JSON = "custom_kits_v1"

    fun getAllKits(context: Context): List<Kit> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_KITS_JSON, null)
        if (raw.isNullOrBlank()) {
            val defaults = createDefaultKits()
            saveKits(context, defaults)
            return defaults
        }
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<Kit>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val itemsArr = o.optJSONArray("items") ?: JSONArray()
                val items = mutableListOf<String>()
                for (j in 0 until itemsArr.length()) {
                    items.add(itemsArr.getString(j))
                }
                list.add(
                    Kit(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        name = o.optString("name", "装备套装"),
                        icon = o.optString("icon", "🎒"),
                        desc = o.optString("desc", ""),
                        itemIds = items,
                        createdAt = o.optLong("created_at", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (e: Exception) {
            createDefaultKits()
        }
    }

    fun saveKits(context: Context, kits: List<Kit>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        for (k in kits) {
            val o = JSONObject()
            o.put("id", k.id)
            o.put("name", k.name)
            o.put("icon", k.icon)
            o.put("desc", k.desc)
            o.put("created_at", k.createdAt)
            val itemsArr = JSONArray()
            k.itemIds.forEach { itemsArr.put(it) }
            o.put("items", itemsArr)
            arr.put(o)
        }
        prefs.edit().putString(KEY_KITS_JSON, arr.toString()).apply()
    }

    private fun createDefaultKits(): List<Kit> {
        return listOf(
            Kit(name = "摄影外拍套装", icon = "📸", desc = "相机机身、大光圈镜头、备用电池、存储卡与三脚架"),
            Kit(name = "3天商务出差包", icon = "💼", desc = "笔记本电脑、便携拓展坞、剃须刀、洗漱分装瓶与降噪耳机"),
            Kit(name = "周末双人露营包", icon = "🏕️", desc = "双人帐篷、折叠蛋卷桌、营地氛围灯、便携卡式炉与防潮垫"),
            Kit(name = "应急医疗急救包", icon = "🩹", desc = "碘伏消毒棉签、创可贴、体温计、退烧贴与医用纱布")
        )
    }

    /** 弹出场景套装列表管理弹窗 */
    fun showKitListDialog(activity: Activity, store: DataStore) {
        val binding = DialogKitManagerBinding.inflate(LayoutInflater.from(activity))
        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(binding.root)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation

        binding.btnCloseKits.applyPressScaleAnimation(0.92f)
        binding.btnCloseKits.setOnClickListener { dialog.dismiss() }

        fun renderKits() {
            binding.layoutKitsContainer.removeAllViews()
            val kits = getAllKits(activity)
            val allEntries = store.loadAll()

            for (kit in kits) {
                val matchedCount = allEntries.count { it.id in kit.itemIds }

                val card = LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    background = ContextCompat.getDrawable(activity, R.drawable.bg_dialog_card)
                    setPadding(14.dpToPx(activity), 12.dpToPx(activity), 14.dpToPx(activity), 12.dpToPx(activity))
                    val lp = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = 10.dpToPx(activity)
                    }
                    layoutParams = lp

                    // 顶栏：图标 + 名称 + 物品数
                    val header = LinearLayout(activity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = android.view.Gravity.CENTER_VERTICAL
                    }

                    val tvIcon = TextView(activity).apply {
                        text = kit.icon
                        textSize = 20f
                        setPadding(0, 0, 8.dpToPx(activity), 0)
                    }

                    val infoLayout = LinearLayout(activity).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    }

                    val tvName = TextView(activity).apply {
                        text = kit.name
                        textSize = 14f
                        setTextColor(ContextCompat.getColor(activity, R.color.text_primary))
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                    }

                    val tvDesc = TextView(activity).apply {
                        text = if (kit.desc.isNotBlank()) kit.desc else "包含 ${kit.itemIds.size} 件装备"
                        textSize = 11f
                        setTextColor(ContextCompat.getColor(activity, R.color.text_secondary))
                    }

                    infoLayout.addView(tvName)
                    infoLayout.addView(tvDesc)

                    val btnChecklist = TextView(activity).apply {
                        text = "🚀 打包核对"
                        textSize = 12f
                        setTextColor(ContextCompat.getColor(activity, R.color.primary))
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                        background = ContextCompat.getDrawable(activity, R.drawable.bg_chip_inactive)
                        setPadding(10.dpToPx(activity), 6.dpToPx(activity), 10.dpToPx(activity), 6.dpToPx(activity))
                        applyPressScaleAnimation(0.92f)
                        setOnClickListener {
                            dialog.dismiss()
                            showKitChecklistDialog(activity, store, kit)
                        }
                    }

                    header.addView(tvIcon)
                    header.addView(infoLayout)
                    header.addView(btnChecklist)

                    addView(header)

                    // 点击卡片绑定/编辑物品
                    setOnClickListener {
                        showEditKitItemsDialog(activity, store, kit) {
                            renderKits()
                        }
                    }
                }
                binding.layoutKitsContainer.addView(card)
            }
        }

        binding.btnCreateKit.applyPressScaleAnimation(0.92f)
        binding.btnCreateKit.setOnClickListener {
            ModernDialogHelper.showInputDialog(
                context = activity,
                title = "新建装备套装",
                subtitle = "例如: 露营装备包、滑雪装备包、健身随身包",
                hint = "输入套装名称",
                emoji = "🎒",
                positiveText = "创建套装"
            ) { name ->
                if (name.isNotBlank()) {
                    val newKit = Kit(name = name, icon = "🎒", desc = "自定义场景装备包")
                    val list = getAllKits(activity).toMutableList()
                    list.add(newKit)
                    saveKits(activity, list)
                    Toast.makeText(activity, "🎉 套装【$name】创建成功！", Toast.LENGTH_SHORT).show()
                    renderKits()
                }
            }
        }

        renderKits()
        dialog.show()
    }

    /** 弹出沉浸式打包核对模式 */
    fun showKitChecklistDialog(activity: Activity, store: DataStore, kit: Kit) {
        val binding = DialogKitChecklistBinding.inflate(LayoutInflater.from(activity))
        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(binding.root)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation

        binding.tvKitChecklistIcon.text = kit.icon
        binding.tvKitChecklistName.text = "${kit.name} · 打包核对"
        binding.btnCloseChecklist.applyPressScaleAnimation(0.92f)
        binding.btnCloseChecklist.setOnClickListener { dialog.dismiss() }

        val allEntries = store.loadAll()
        val kitEntries = allEntries.filter { it.id in kit.itemIds }

        if (kitEntries.isEmpty()) {
            val emptyMsg = TextView(activity).apply {
                text = "💡 该套装尚未关联具体物品。\n点击下方按钮前往物品库快速添加！"
                textSize = 13f
                setTextColor(ContextCompat.getColor(activity, R.color.text_secondary))
                gravity = android.view.Gravity.CENTER
                setPadding(0, 20.dpToPx(activity), 0, 20.dpToPx(activity))
            }
            binding.layoutChecklistItems.addView(emptyMsg)
            binding.btnAllChecked.text = "➕ 去关联物品"
            binding.btnAllChecked.setOnClickListener {
                dialog.dismiss()
                showEditKitItemsDialog(activity, store, kit) {
                    showKitChecklistDialog(activity, store, it)
                }
            }
            dialog.show()
            return
        }

        val checkedStates = mutableMapOf<String, Boolean>()
        kitEntries.forEach { checkedStates[it.id] = false }

        fun updateProgress() {
            val checkedCount = checkedStates.values.count { it }
            val total = kitEntries.size
            val percent = if (total > 0) (checkedCount * 100 / total) else 0
            binding.pbKitChecklist.progress = percent
            binding.tvKitChecklistProgressDesc.text = "装箱进度：$checkedCount/$total 项 ($percent%)"

            if (checkedCount == total) {
                binding.btnAllChecked.text = "🎉 全套核对完成！点击打卡"
                binding.btnAllChecked.backgroundTintList = ContextCompat.getColorStateList(activity, R.color.primary)
            } else {
                binding.btnAllChecked.text = "完成装箱打卡 ($checkedCount/$total)"
            }
        }

        for (entry in kitEntries) {
            val row = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                background = ContextCompat.getDrawable(activity, R.drawable.bg_icon_circle_soft)
                setPadding(10.dpToPx(activity), 8.dpToPx(activity), 10.dpToPx(activity), 8.dpToPx(activity))
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 6.dpToPx(activity)
                }
                layoutParams = lp
            }

            val cb = CheckBox(activity).apply {
                isChecked = false
                setOnCheckedChangeListener { _, isChecked ->
                    checkedStates[entry.id] = isChecked
                    updateProgress()
                }
            }

            val tvItemInfo = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val tvTitle = TextView(activity).apply {
                text = entry.brand
                textSize = 13f
                setTextColor(ContextCompat.getColor(activity, R.color.text_primary))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }

            val tvLoc = TextView(activity).apply {
                text = "📍 存放于: ${entry.location.ifBlank { "默认空间" }} (数量: ${entry.qty} ${entry.unit})"
                textSize = 11f
                setTextColor(ContextCompat.getColor(activity, R.color.text_secondary))
            }

            tvItemInfo.addView(tvTitle)
            tvItemInfo.addView(tvLoc)

            row.addView(cb)
            row.addView(tvItemInfo)
            row.setOnClickListener {
                cb.isChecked = !cb.isChecked
            }

            binding.layoutChecklistItems.addView(row)
        }

        binding.btnAllChecked.applyPressScaleAnimation(0.92f)
        binding.btnAllChecked.setOnClickListener {
            val checkedCount = checkedStates.values.count { it }
            val total = kitEntries.size
            if (checkedCount == total) {
                Toast.makeText(activity, "🎉 棒极了！【${kit.name}】全套装备已全部清点装箱完毕！", Toast.LENGTH_LONG).show()
                dialog.dismiss()
            } else {
                ModernDialogHelper.showConfirmDialog(
                    context = activity,
                    title = "仍有未核对物品",
                    message = "当前还有 ${total - checkedCount} 件物品尚未装箱，确定现在结束核对吗？",
                    emoji = "⚠️",
                    positiveText = "结束核对",
                    negativeText = "继续清点"
                ) {
                    dialog.dismiss()
                }
            }
        }

        updateProgress()
        dialog.show()
    }

    /** 关联/编辑套装包含的物品 */
    private fun showEditKitItemsDialog(
        activity: Activity,
        store: DataStore,
        kit: Kit,
        onSaved: (Kit) -> Unit
    ) {
        val allEntries = store.loadAll()
        val itemsArray = allEntries.map { "📦 ${it.brand} (${it.location.ifBlank { "未分配" }})" }.toTypedArray()
        val checkedItems = BooleanArray(allEntries.size) { idx ->
            allEntries[idx].id in kit.itemIds
        }

        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle("🎒 选择【${kit.name}】包含的物品")
            .setMultiChoiceItems(itemsArray, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
            .setPositiveButton("保存关联") { _, _ ->
                val selectedIds = mutableListOf<String>()
                for (i in checkedItems.indices) {
                    if (checkedItems[i]) {
                        selectedIds.add(allEntries[i].id)
                    }
                }
                val updatedKit = kit.copy(itemIds = selectedIds)
                val allKits = getAllKits(activity).toMutableList()
                val idx = allKits.indexOfFirst { it.id == kit.id }
                if (idx >= 0) {
                    allKits[idx] = updatedKit
                } else {
                    allKits.add(updatedKit)
                }
                saveKits(activity, allKits)
                Toast.makeText(activity, "🎉 已更新【${kit.name}】关联物品 (${selectedIds.size} 件)", Toast.LENGTH_SHORT).show()
                onSaved(updatedKit)
            }
            .setNegativeButton("取消", null)
            .create()

        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation
        dialog.show()
    }

    private fun Int.dpToPx(context: Context): Int = (this * context.resources.displayMetrics.density).toInt()
}
