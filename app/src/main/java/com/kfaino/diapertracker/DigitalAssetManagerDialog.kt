package com.kfaino.diapertracker

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kfaino.diapertracker.databinding.DialogAddDigitalAssetBinding
import com.kfaino.diapertracker.databinding.DialogDigitalAssetsBinding
import java.util.UUID

/**
 * 💾 数字相册与电子资产专属展厅控制器 (Digital Asset Vault Controller)
 * - 集中呈现照片回忆相册、正版软件Key、域名与数字课程/文档
 * - 支持容量统计、备份状态追踪、一键复制授权 Key、跳转网盘
 * - 直通物品时光胶囊画册回忆录
 */
object DigitalAssetManagerDialog {

    /** 打开数字资产展厅主弹窗 */
    fun showDigitalVaultDialog(
        activity: Activity,
        store: DataStore,
        onUpdated: () -> Unit
    ) {
        val binding = DialogDigitalAssetsBinding.inflate(activity.layoutInflater)
        var currentFilterType = "all" // "all", "album", "software", "domain", "doc"

        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(binding.root)
            .setCancelable(true)
            .create()
        VaultUiHelper.setupVaultWindow(dialog)

        fun refreshList() {
            val allDigital = store.getDigitalAssets()

            // 统计
            binding.tvDigitalTotalCount.text = "${allDigital.size} 项"
            val totalSynced = allDigital.count { it.backupStatus == "synced" }
            val backupRate = if (allDigital.isNotEmpty()) (totalSynced * 100 / allDigital.size) else 100
            binding.tvDigitalBackupRate.text = "$backupRate%"

            val filtered = when (currentFilterType) {
                "album" -> allDigital.filter { it.digitalType == "album" }
                "software" -> allDigital.filter { it.digitalType == "software" }
                "domain" -> allDigital.filter { it.digitalType == "domain" || it.digitalType == "doc" }
                else -> allDigital
            }

            renderDigitalList(activity, store, filtered, binding.digitalAssetsListContainer) {
                refreshList()
                onUpdated()
            }
        }

        fun updateTabs() {
            val activeBg = R.drawable.bg_chip_active
            val inActiveBg = R.drawable.bg_chip_inactive
            val white = Color.WHITE
            val secColor = ContextCompat.getColor(activity, R.color.text_secondary)

            binding.tabDigitalAll.setBackgroundResource(if (currentFilterType == "all") activeBg else inActiveBg)
            binding.tabDigitalAll.setTextColor(if (currentFilterType == "all") white else secColor)

            binding.tabDigitalAlbum.setBackgroundResource(if (currentFilterType == "album") activeBg else inActiveBg)
            binding.tabDigitalAlbum.setTextColor(if (currentFilterType == "album") white else secColor)

            binding.tabDigitalSoftware.setBackgroundResource(if (currentFilterType == "software") activeBg else inActiveBg)
            binding.tabDigitalSoftware.setTextColor(if (currentFilterType == "software") white else secColor)

            binding.tabDigitalDomain.setBackgroundResource(if (currentFilterType == "domain") activeBg else inActiveBg)
            binding.tabDigitalDomain.setTextColor(if (currentFilterType == "domain") white else secColor)
        }

        binding.tabDigitalAll.setOnClickListener { currentFilterType = "all"; updateTabs(); refreshList() }
        binding.tabDigitalAlbum.setOnClickListener { currentFilterType = "album"; updateTabs(); refreshList() }
        binding.tabDigitalSoftware.setOnClickListener { currentFilterType = "software"; updateTabs(); refreshList() }
        binding.tabDigitalDomain.setOnClickListener { currentFilterType = "domain"; updateTabs(); refreshList() }

        binding.btnCloseDigitalVault.applyPressScaleAnimation(0.92f)
        binding.btnCloseDigitalVault.setOnClickListener { dialog.dismiss() }

        binding.btnAddNewDigitalAsset.applyPressScaleAnimation(0.92f)
        binding.btnAddNewDigitalAsset.setOnClickListener {
            showAddOrEditDigitalDialog(activity, store, editEntry = null) {
                refreshList()
                onUpdated()
            }
        }

        refreshList()
        dialog.show()
    }

    /** 动态渲染数字资产卡片列表 */
    private fun renderDigitalList(
        activity: Activity,
        store: DataStore,
        list: List<Entry>,
        container: LinearLayout,
        onRefreshNeeded: () -> Unit
    ) {
        container.removeAllViews()
        val density = activity.resources.displayMetrics.density
        fun dp(dp: Int): Int = (dp * density + 0.5f).toInt()

        if (list.isEmpty()) {
            val emptyTv = TextView(activity).apply {
                text = "💾 暂无数字资产或电子相册\n点击下方按钮立即登记您的第一份数字回忆/授权Key~"
                textSize = 13f
                setTextColor(ContextCompat.getColor(activity, R.color.text_secondary))
                gravity = Gravity.CENTER
                setPadding(dp(20), dp(40), dp(20), dp(40))
            }
            container.addView(emptyTv)
            return
        }

        for (entry in list) {
            val card = MaterialCardView(activity).apply {
                radius = dp(14).toFloat()
                cardElevation = 0f
                strokeWidth = dp(1)
                setStrokeColor(ContextCompat.getColor(activity, R.color.card_border))
                setCardBackgroundColor(ContextCompat.getColor(activity, R.color.card))
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(10) }
                layoutParams = lp
            }

            val root = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12))
            }

            // 1. 顶部行：Emoji + 资产名称 + 备份状态徽章
            val topRow = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val emojiStr = when (entry.digitalType) {
                "album" -> "📷"
                "software" -> "🔑"
                "domain" -> "🌐"
                else -> "📚"
            }

            val emojiTv = TextView(activity).apply {
                text = emojiStr
                textSize = 18f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = dp(8) }
            }

            val nameTv = TextView(activity).apply {
                text = entry.brand
                textSize = 15f
                paint.isFakeBoldText = true
                setTextColor(ContextCompat.getColor(activity, R.color.text_primary))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val backupBadge = TextView(activity).apply {
                val (txt, bg, fg) = when (entry.backupStatus) {
                    "synced" -> Triple("🟢 已备份", R.drawable.bg_chip_active, Color.WHITE)
                    "local" -> Triple("🟡 仅本地", R.drawable.bg_chip_inactive, ContextCompat.getColor(activity, R.color.warning))
                    else -> Triple("🔴 未备份", R.drawable.bg_chip_inactive, ContextCompat.getColor(activity, R.color.danger))
                }
                text = txt
                textSize = 11f
                paint.isFakeBoldText = true
                setTextColor(fg)
                setBackgroundResource(bg)
                setPadding(dp(8), dp(2), dp(8), dp(2))
            }

            topRow.addView(emojiTv)
            topRow.addView(nameTv)
            topRow.addView(backupBadge)
            root.addView(topRow)

            // 2. 次级信息：容量 / 授权Key / 链接
            val infoLayout = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(26), dp(4), 0, dp(6))
            }

            if (entry.digitalSize.isNotBlank()) {
                val sizeTv = TextView(activity).apply {
                    text = "📦 占用容量: ${entry.digitalSize}"
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(activity, R.color.text_secondary))
                }
                infoLayout.addView(sizeTv)
            }

            if (entry.digitalLicenseKey.isNotBlank()) {
                val keyTv = TextView(activity).apply {
                    val masked = if (entry.digitalLicenseKey.length > 8) entry.digitalLicenseKey.take(4) + "****" + entry.digitalLicenseKey.takeLast(4) else "****"
                    text = "🔑 授权Key: $masked (点击复制)"
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(activity, R.color.primary))
                    setOnClickListener {
                        val cm = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("License Key", entry.digitalLicenseKey))
                        Toast.makeText(activity, "📋 授权 Key 已安全复制到剪贴板！", Toast.LENGTH_SHORT).show()
                    }
                }
                infoLayout.addView(keyTv)
            }

            if (entry.digitalUrl.isNotBlank()) {
                val urlTv = TextView(activity).apply {
                    text = "🌐 存储路径/网盘: ${entry.digitalUrl}"
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(activity, R.color.text_hint))
                    maxLines = 1
                }
                infoLayout.addView(urlTv)
            }

            root.addView(infoLayout)

            // 3. 底部快捷操作栏
            val btnRow = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
                setPadding(0, dp(4), 0, 0)
            }

            // 时光胶囊回忆按钮
            val btnCapsule = TextView(activity).apply {
                val mCount = entry.memoryMoments.size
                text = if (mCount > 0) "🎞️ $mCount 段回忆" else "🎞️ 写回忆"
                textSize = 12f
                paint.isFakeBoldText = true
                setTextColor(ContextCompat.getColor(activity, R.color.primary))
                setPadding(dp(10), dp(4), dp(10), dp(4))
                setOnClickListener {
                    LifeCapsuleDialog.showCapsuleDialog(activity, store, entry) {
                        onRefreshNeeded()
                    }
                }
            }

            // 打开云盘/链接
            if (entry.digitalUrl.isNotBlank() && entry.digitalUrl.startsWith("http")) {
                val btnOpen = TextView(activity).apply {
                    text = "🚀 打开网盘"
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(activity, R.color.text_secondary))
                    setPadding(dp(8), dp(4), dp(8), dp(4))
                    setOnClickListener {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(entry.digitalUrl))
                            activity.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(activity, "无法打开链接", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                btnRow.addView(btnOpen)
            }

            val btnEdit = TextView(activity).apply {
                text = "✏️ 编辑"
                textSize = 12f
                setTextColor(ContextCompat.getColor(activity, R.color.text_secondary))
                setPadding(dp(8), dp(4), dp(8), dp(4))
                setOnClickListener {
                    showAddOrEditDigitalDialog(activity, store, editEntry = entry) {
                        onRefreshNeeded()
                    }
                }
            }

            val btnDelete = TextView(activity).apply {
                text = "🗑️ 删除"
                textSize = 12f
                setTextColor(ContextCompat.getColor(activity, R.color.danger))
                setPadding(dp(8), dp(4), dp(8), dp(4))
                setOnClickListener {
                    ModernDialogHelper.showConfirmDialog(
                        context = activity,
                        title = "删除数字资产",
                        message = "确定要从系统中移除【${entry.brand}】吗？",
                        emoji = "🗑️",
                        positiveText = "删除",
                        negativeText = "取消",
                        isDestructive = true
                    ) {
                        val all = store.loadAll().filter { it.id != entry.id }
                        store.saveAll(all)
                        Toast.makeText(activity, "已删除数字资产", Toast.LENGTH_SHORT).show()
                        onRefreshNeeded()
                    }
                }
            }

            btnRow.addView(btnCapsule)
            btnRow.addView(btnEdit)
            btnRow.addView(btnDelete)
            root.addView(btnRow)

            card.addView(root)
            container.addView(card)
        }
    }

    /** 登记或编辑数字资产输入弹窗 */
    fun showAddOrEditDigitalDialog(
        activity: Activity,
        store: DataStore,
        editEntry: Entry?,
        onSaved: () -> Unit
    ) {
        val binding = DialogAddDigitalAssetBinding.inflate(activity.layoutInflater)
        val isEdit = editEntry != null

        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(binding.root)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation

        if (isEdit) {
            binding.tvAddDigitalTitle.text = "编辑数字相册 / 电子资产"
            binding.etDigitalName.setText(editEntry!!.brand)
            binding.etDigitalSize.setText(editEntry.digitalSize)
            binding.etDigitalUrl.setText(editEntry.digitalUrl)
            binding.etDigitalKey.setText(editEntry.digitalLicenseKey)
            if (editEntry.price > 0) binding.etDigitalPrice.setText(editEntry.price.toString())

            when (editEntry.digitalType) {
                "software" -> binding.rbTypeSoftware.isChecked = true
                "domain" -> binding.rbTypeDomain.isChecked = true
                "doc" -> binding.rbTypeDoc.isChecked = true
                else -> binding.rbTypeAlbum.isChecked = true
            }

            when (editEntry.backupStatus) {
                "local" -> binding.rbBakLocal.isChecked = true
                "unbacked" -> binding.rbBakUnbacked.isChecked = true
                else -> binding.rbBakSynced.isChecked = true
            }
        }

        binding.btnCloseAddDigital.applyPressScaleAnimation(0.92f)
        binding.btnCloseAddDigital.setOnClickListener { dialog.dismiss() }

        binding.btnCancelAddDigital.applyPressScaleAnimation(0.92f)
        binding.btnCancelAddDigital.setOnClickListener { dialog.dismiss() }

        binding.btnSaveAddDigital.applyPressScaleAnimation(0.92f)
        binding.btnSaveAddDigital.setOnClickListener {
            val name = binding.etDigitalName.text.toString().trim()
            if (name.isBlank()) {
                Toast.makeText(activity, "请输入数字资产名称", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val size = binding.etDigitalSize.text.toString().trim()
            val url = binding.etDigitalUrl.text.toString().trim()
            val key = binding.etDigitalKey.text.toString().trim()
            val price = binding.etDigitalPrice.text.toString().toDoubleOrNull() ?: 0.0

            val dType = when {
                binding.rbTypeSoftware.isChecked -> "software"
                binding.rbTypeDomain.isChecked -> "domain"
                binding.rbTypeDoc.isChecked -> "doc"
                else -> "album"
            }

            val bStatus = when {
                binding.rbBakLocal.isChecked -> "local"
                binding.rbBakUnbacked.isChecked -> "unbacked"
                else -> "synced"
            }

            val resultEntry = if (isEdit) {
                editEntry!!.copy(
                    brand = name,
                    price = price,
                    isDigital = true,
                    digitalType = dType,
                    digitalSize = size,
                    digitalUrl = url,
                    digitalLicenseKey = key,
                    backupStatus = bStatus
                )
            } else {
                Entry(
                    id = UUID.randomUUID().toString(),
                    category = "数字资产",
                    brand = name,
                    qty = 1,
                    price = price,
                    isDigital = true,
                    digitalType = dType,
                    digitalSize = size,
                    digitalUrl = url,
                    digitalLicenseKey = key,
                    backupStatus = bStatus
                )
            }

            val all = store.loadAll().toMutableList()
            if (isEdit) {
                val idx = all.indexOfFirst { it.id == resultEntry.id }
                if (idx != -1) all[idx] = resultEntry else all.add(0, resultEntry)
            } else {
                all.add(0, resultEntry)
            }
            store.saveAll(all)

            Toast.makeText(activity, "🎉 数字资产【$name】已成功保存！", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            onSaved()
        }

        dialog.show()
    }
}
