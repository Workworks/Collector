package com.kfaino.diapertracker

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kfaino.diapertracker.databinding.DialogKitChecklistBinding
import com.kfaino.diapertracker.databinding.DialogKitManagerBinding
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

/**
 * 🎒 场景化装备/出行套装与沉浸式打包归巢舱 (Kit & Packing Return-Home Studio)
 * 1. 商务出差、露营、海岛、摄影、应急医疗、通勤场景化套装
 * 2. 🚀 去程装箱打卡与 🏨 返程离店防漏双向清点
 * 3. 🏡 一键物归原位指引 (Return-Home Auto Reposition)：到家后全景收纳路线图与一键轨迹打卡
 * 4. 1080P 高清出行装箱海报与蓝牙热敏清单导出
 */
object KitManager {

    private const val TAG = "KitManager"

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
            android.util.Log.w(TAG, "解析场景套装 JSON 失败，重置为默认套装", e)
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
            Kit(name = "3天商务出差包", icon = "💼", desc = "笔记本电脑、便携拓展坞、剃须刀、洗漱分装瓶与降噪耳机"),
            Kit(name = "周末双人露营包", icon = "🏕️", desc = "双人帐篷、折叠蛋卷桌、营地氛围灯、便携卡式炉与防潮垫"),
            Kit(name = "亲子海岛度假包", icon = "🏖️", desc = "儿童防晒霜、泳衣泳镜、沙滩玩具、便携常备药与护照证件"),
            Kit(name = "摄影外拍大师套装", icon = "📸", desc = "相机机身、大光圈镜头、备用电池、存储卡与三脚架"),
            Kit(name = "家庭应急医疗急救包", icon = "🩹", desc = "碘伏消毒棉签、创可贴、体温计、退烧贴与医用纱布"),
            Kit(name = "极简日常通勤EDC", icon = "🎒", desc = "随身工卡、机械钥匙、折叠伞、充电宝与纸巾")
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
                        gravity = Gravity.CENTER_VERTICAL
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

    /** 弹出场景套装装箱核对弹窗 (支持出发装箱与返程离店清点双向模式) */
    fun showKitChecklistDialog(activity: Activity, store: DataStore, kit: Kit) {
        val binding = DialogKitChecklistBinding.inflate(LayoutInflater.from(activity))
        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(binding.root)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation

        var isDepartMode = true // true = 出发装箱, false = 返程清点
        binding.tvKitChecklistIcon.text = kit.icon
        binding.tvKitChecklistName.text = "${kit.name} · 出发装箱"
        binding.toggleGroupChecklistMode.check(R.id.btn_mode_depart)
        binding.btnCloseChecklist.applyPressScaleAnimation(0.92f)
        binding.btnCloseChecklist.setOnClickListener { dialog.dismiss() }

        val allEntries = store.loadAll()
        val kitEntries = allEntries.filter { it.id in kit.itemIds }

        val checkedStates = mutableMapOf<String, Boolean>()
        kitEntries.forEach { checkedStates[it.id] = false }

        fun updateProgress() {
            val checkedCount = checkedStates.values.count { it }
            val total = kitEntries.size
            val percent = if (total > 0) (checkedCount * 100 / total) else 0
            binding.pbKitChecklist.progress = percent
            val modeName = if (isDepartMode) "装箱进度" else "返程清点"
            binding.tvKitChecklistProgressDesc.text = "$modeName：$checkedCount/$total 项 ($percent%)"

            if (checkedCount == total) {
                binding.btnAllChecked.text = if (isDepartMode) "🎉 全部装箱就绪！点击打卡" else "🏡 离店清点无遗漏 · 一键归巢归位"
                binding.btnAllChecked.backgroundTintList = ContextCompat.getColorStateList(activity, R.color.primary)
            } else {
                binding.btnAllChecked.text = if (isDepartMode) "完成装箱打卡 ($checkedCount/$total)" else "完成清点打卡 ($checkedCount/$total)"
                binding.btnAllChecked.backgroundTintList = ContextCompat.getColorStateList(activity, R.color.primary)
            }
        }

        fun renderItems() {
            binding.layoutChecklistItems.removeAllViews()
            for (entry in kitEntries) {
                val row = LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
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
                    isChecked = checkedStates[entry.id] == true
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
                    text = "📍 原位: ${entry.houseName} · 【${entry.roomName.ifBlank { "默认" }}】 ${entry.location} (${entry.qty}${entry.unit})"
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
        }

        binding.toggleGroupChecklistMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                isDepartMode = (checkedId == R.id.btn_mode_depart)
                binding.tvKitChecklistName.text = if (isDepartMode) "${kit.name} · 出发装箱" else "${kit.name} · 返程清点"
                // 模式切换时重置勾选
                kitEntries.forEach { checkedStates[it.id] = false }
                renderItems()
                updateProgress()
            }
        }

        binding.btnExportPackingPoster.applyPressScaleAnimation(0.92f)
        binding.btnExportPackingPoster.setOnClickListener {
            exportPackingPoster(activity, kit, kitEntries, checkedStates, isDepartMode)
        }

        binding.btnAllChecked.applyPressScaleAnimation(0.92f)
        binding.btnAllChecked.setOnClickListener {
            val checkedCount = checkedStates.values.count { it }
            val total = kitEntries.size
            if (checkedCount == total) {
                if (isDepartMode) {
                    Toast.makeText(activity, "🎉 太棒了！【${kit.name}】出行装备已全部装箱完毕，祝您旅途愉快！", Toast.LENGTH_LONG).show()
                    dialog.dismiss()
                } else {
                    dialog.dismiss()
                    showReturnHomeDialog(activity, store, kit, kitEntries)
                }
            } else {
                val remain = total - checkedCount
                ModernDialogHelper.showConfirmDialog(
                    context = activity,
                    title = "仍有物品未打卡",
                    message = "当前还有 $remain 件物品尚未打卡核对，确定现在结束核对吗？",
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

    /** 弹出「🏡 出行归巢·物归原位指引与一键归位」全景弹窗 */
    private fun showReturnHomeDialog(activity: Activity, store: DataStore, kit: Kit, entries: List<Entry>) {
        val dialogView = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_dialog_card)
            setPadding(16.dpToPx(activity), 16.dpToPx(activity), 16.dpToPx(activity), 16.dpToPx(activity))
        }

        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation

        // 1. 顶栏标题
        val topRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 10.dpToPx(activity))
        }

        val tvTitle = TextView(activity).apply {
            text = "🏡 【${kit.name}】物归原位路线图"
            textSize = 16f
            paint.isFakeBoldText = true
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val btnClose = TextView(activity).apply {
            text = "✕"
            textSize = 18f
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            setPadding(8.dpToPx(activity), 4.dpToPx(activity), 8.dpToPx(activity), 4.dpToPx(activity))
            setOnClickListener { dialog.dismiss() }
        }

        topRow.addView(tvTitle)
        topRow.addView(btnClose)
        dialogView.addView(topRow)

        // 2. 提示卡片
        val hintCard = MaterialCardView(activity).apply {
            radius = 12.dpToPx(activity).toFloat()
            cardElevation = 0f
            strokeWidth = 1.dpToPx(activity)
            setStrokeColor(ContextCompat.getColor(context, R.color.card_border))
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.primary_surface))
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = 10.dpToPx(activity)
            layoutParams = lp
        }

        val hintTv = TextView(activity).apply {
            text = "✨ 离店清点零遗漏！请对照以下全屋原位指引将装备放回原处，点击底部按钮即可一键批量打卡确认归位并写入变迁轨迹："
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.primary_dark))
            setPadding(12.dpToPx(activity), 10.dpToPx(activity), 12.dpToPx(activity), 10.dpToPx(activity))
        }
        hintCard.addView(hintTv)
        dialogView.addView(hintCard)

        // 3. 物品归位路线列表
        val scroll = ScrollView(activity).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 260.dpToPx(activity))
            isFillViewport = true
        }

        val listLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
        }

        for (e in entries) {
            val itemRow = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundResource(R.drawable.bg_input_box)
                setPadding(12.dpToPx(activity), 8.dpToPx(activity), 12.dpToPx(activity), 8.dpToPx(activity))
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = 6.dpToPx(activity)
                }
                layoutParams = lp
            }

            val iconTv = TextView(activity).apply {
                text = "📦"
                textSize = 16f
                setPadding(0, 0, 8.dpToPx(activity), 0)
            }

            val infoLayout = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val nameTv = TextView(activity).apply {
                text = e.brand
                textSize = 13f
                paint.isFakeBoldText = true
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            }

            val locTv = TextView(activity).apply {
                text = "➔ 归位至: ${e.houseName} · 【${e.roomName.ifBlank { "默认" }}】 ${e.location}"
                textSize = 11f
                setTextColor(ContextCompat.getColor(context, R.color.primary))
                paint.isFakeBoldText = true
            }

            infoLayout.addView(nameTv)
            infoLayout.addView(locTv)

            itemRow.addView(iconTv)
            itemRow.addView(infoLayout)
            listLayout.addView(itemRow)
        }

        scroll.addView(listLayout)
        dialogView.addView(scroll)

        // 4. 一键归位确认打卡按钮
        val btnBatchReposition = TextView(activity).apply {
            text = "🏡 确认全量物归原位并打卡"
            textSize = 13f
            paint.isFakeBoldText = true
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(context, android.R.color.white))
            setBackgroundResource(R.drawable.bg_btn_primary)
            setPadding(0, 12.dpToPx(activity), 0, 12.dpToPx(activity))
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 12.dpToPx(activity)
            }
            layoutParams = lp
            applyPressScaleAnimation(0.92f)
            setOnClickListener {
                val all = store.loadAll().toMutableList()
                val now = System.currentTimeMillis()
                var updatedCount = 0

                for (i in 0 until all.size) {
                    val item = all[i]
                    if (entries.any { it.id == item.id }) {
                        val newHist = item.locationHistory.toMutableList()
                        newHist.add(
                            0,
                            LocationMovement(
                                location = item.location,
                                houseName = item.houseName,
                                roomName = item.roomName,
                                movedAt = now,
                                note = "🏡 【${kit.name}】出行归来·物归原位"
                            )
                        )
                        all[i] = item.copy(
                            lastCheckedAt = now,
                            locationHistory = newHist
                        )
                        updatedCount++
                    }
                }

                store.saveAll(all)
                Toast.makeText(activity, "🎉 完美！已成功将 $updatedCount 件装备全部物归原位，健康分已刷新！", Toast.LENGTH_LONG).show()
                dialog.dismiss()
            }
        }

        dialogView.addView(btnBatchReposition)
        dialog.show()
    }

    /** 导出 1080P 出行装备装箱清单高清海报 */
    private fun exportPackingPoster(
        activity: Activity,
        kit: Kit,
        entries: List<Entry>,
        checkedStates: Map<String, Boolean>,
        isDepartMode: Boolean
    ) {
        val width = 1080
        val headerHeight = 340
        val itemHeight = 110
        val footerHeight = 160
        val height = headerHeight + entries.size * itemHeight + footerHeight

        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)

        // 背景 (深色黑曜石质感)
        val bgPaint = android.graphics.Paint().apply {
            color = Color.parseColor("#0F172A")
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 标题与副标题
        val titlePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 50f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        val subPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#10B981")
            textSize = 28f
        }
        val metaPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 24f
        }

        val modeTitle = if (isDepartMode) "🛫 出发装箱清单" else "🏨 离店返程清点表"
        canvas.drawText("${kit.icon} ${kit.name} · $modeTitle", 80f, 130f, titlePaint)
        val checkedCount = checkedStates.values.count { it }
        val percent = if (entries.isNotEmpty()) checkedCount * 100 / entries.size else 0
        canvas.drawText("PACKING CHECKLIST · 核对进度: $checkedCount/${entries.size} ($percent%)", 80f, 185f, subPaint)

        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        canvas.drawText("生成时间: ${sdf.format(Date())}  ·  共收录 ${entries.size} 件专属装备", 80f, 240f, metaPaint)

        val linePaint = android.graphics.Paint().apply {
            color = Color.parseColor("#334155")
            strokeWidth = 3f
        }
        canvas.drawLine(80f, 285f, (width - 80).toFloat(), 285f, linePaint)

        // 绘制条目
        var currentY = headerHeight.toFloat()
        val cardPaint = android.graphics.Paint().apply {
            color = Color.parseColor("#1E293B")
            style = android.graphics.Paint.Style.FILL
        }
        val itemTitlePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 32f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        val itemLocPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 22f
        }
        val checkTagPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 26f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }

        for (e in entries) {
            val isChecked = checkedStates[e.id] == true
            val cardRect = android.graphics.RectF(80f, currentY, (width - 80).toFloat(), currentY + 90f)
            canvas.drawRoundRect(cardRect, 14f, 14f, cardPaint)

            if (isChecked) {
                checkTagPaint.color = Color.parseColor("#10B981")
                canvas.drawText("✅ 已装箱", 110f, currentY + 56f, checkTagPaint)
            } else {
                checkTagPaint.color = Color.parseColor("#F59E0B")
                canvas.drawText("⬜ 待装箱", 110f, currentY + 56f, checkTagPaint)
            }

            canvas.drawText(e.brand, 270f, currentY + 44f, itemTitlePaint)
            canvas.drawText("📍 存放: ${e.location.ifBlank { "默认储物区" }} (${e.qty} ${e.unit})", 270f, currentY + 76f, itemLocPaint)

            currentY += itemHeight
        }

        // 底部水印
        val footerPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 22f
            textAlign = android.graphics.Paint.Align.CENTER
        }
        canvas.drawText("🎒 由 Collecter 场景化装备与智能收纳宇宙生成", (width / 2).toFloat(), (height - 50).toFloat(), footerPaint)

        try {
            val dir = activity.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES) ?: activity.cacheDir
            val file = java.io.File(dir, "Collecter_Packing_${kit.name}_${System.currentTimeMillis()}.png")
            java.io.FileOutputStream(file).use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
            }
            ModernDialogHelper.showInfoDialog(
                context = activity,
                title = "🎉 出行清单海报已生成！",
                emoji = "📜",
                message = "1080P 出行装备清单已导出至：\n${file.absolutePath}\n\n已记录 ${entries.size} 件出行装备，可微信发送给同行亲友核对！",
                buttonText = "收到"
            )
        } catch (e: Exception) {
            android.util.Log.w(TAG, "导出出行装箱海报失败", e)
            Toast.makeText(activity, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
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
