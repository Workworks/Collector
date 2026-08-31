package com.kfaino.diapertracker

import android.app.Activity
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
import com.kfaino.diapertracker.databinding.DialogAddMemoryMomentBinding
import com.kfaino.diapertracker.databinding.DialogLifeCapsuleBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 🎞️ 物品时光胶囊与生活画册回忆录控制器 (Life Capsule Dialog Controller)
 * - 呈现物品陪伴时光里程碑与生活高光时刻时光轴
 * - 支持故事记录、真香评分、照片留存
 * - 支持 Canvas 导出拍立得/杂志风高清长图画册并保存至相册
 */
object LifeCapsuleDialog {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /** 打开指定物品的时光胶囊弹窗 */
    fun showCapsuleDialog(
        activity: Activity,
        store: DataStore,
        targetEntry: Entry,
        onUpdated: () -> Unit
    ) {
        val binding = DialogLifeCapsuleBinding.inflate(activity.layoutInflater)
        var currentEntry = targetEntry

        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(binding.root)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation

        fun refreshUI() {
            // 重新从 DataStore 取出最新数据
            currentEntry = store.loadAll().firstOrNull { it.id == currentEntry.id } ?: currentEntry

            binding.tvCapsuleItemTitle.text = currentEntry.brand
            val typeStr = if (currentEntry.isDigital) "📷 ${currentEntry.getDigitalTypeDisplayName()}" else "🏷️ ${currentEntry.category}"
            binding.tvCapsuleItemSub.text = "$typeStr · 生活画册回忆录"

            binding.tvCapsuleDays.text = "⏳ 陪伴 ${currentEntry.getDaysOwned()} 天"
            binding.tvCapsuleDailyCost.text = "💰 日均成本 ￥${String.format(Locale.getDefault(), "%.2f", currentEntry.getDailyCost())} / 天"

            val avgRating = currentEntry.getAverageRating()
            binding.tvCapsuleRating.text = "⭐ ${String.format(Locale.getDefault(), "%.1f", avgRating)} 真香体验"
            binding.tvCapsuleCount.text = "共 ${currentEntry.memoryMoments.size} 个时光里程碑"

            // 渲染时光轴卡片
            renderTimeline(activity, store, currentEntry, binding.capsuleTimelineContainer) {
                refreshUI()
                onUpdated()
            }
        }

        binding.btnCloseCapsule.applyPressScaleAnimation(0.92f)
        binding.btnCloseCapsule.setOnClickListener { dialog.dismiss() }

        // 记录新瞬间
        binding.btnAddMemoryMoment.applyPressScaleAnimation(0.92f)
        binding.btnAddMemoryMoment.setOnClickListener {
            showAddOrEditMomentDialog(activity, store, currentEntry, editMoment = null) {
                refreshUI()
                onUpdated()
            }
        }

        // 导出长图海报
        binding.btnExportCapsulePoster.applyPressScaleAnimation(0.92f)
        binding.btnExportCapsulePoster.setOnClickListener {
            exportAndSharePoster(activity, currentEntry)
        }

        refreshUI()
        dialog.show()
    }

    /** 动态渲染时光轴卡片列表 */
    private fun renderTimeline(
        activity: Activity,
        store: DataStore,
        entry: Entry,
        container: LinearLayout,
        onRefreshNeeded: () -> Unit
    ) {
        container.removeAllViews()
        val density = activity.resources.displayMetrics.density
        fun dp(dp: Int): Int = (dp * density + 0.5f).toInt()

        if (entry.memoryMoments.isEmpty()) {
            val emptyCard = TextView(activity).apply {
                text = "✨ 暂无时光故事记录\n点击下方「➕ 记录新瞬间」写下第一段陪伴回忆吧~"
                textSize = 13f
                setTextColor(ContextCompat.getColor(activity, R.color.text_secondary))
                gravity = Gravity.CENTER
                setPadding(dp(20), dp(40), dp(20), dp(40))
            }
            container.addView(emptyCard)
            return
        }

        for (moment in entry.memoryMoments) {
            val card = MaterialCardView(activity).apply {
                radius = dp(12).toFloat()
                cardElevation = 0f
                strokeWidth = dp(1)
                setStrokeColor(ContextCompat.getColor(activity, R.color.card_border))
                setCardBackgroundColor(ContextCompat.getColor(activity, R.color.card))
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(10)
                }
                layoutParams = lp
            }

            val cardLayout = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(12), dp(12), dp(12))
            }

            // 头部：Emoji + 标题 + 日期 + 评分
            val header = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val emojiTv = TextView(activity).apply {
                text = moment.moodEmoji
                textSize = 18f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = dp(6) }
            }

            val titleTv = TextView(activity).apply {
                text = moment.title.ifBlank { "高光瞬间" }
                textSize = 14f
                paint.isFakeBoldText = true
                setTextColor(ContextCompat.getColor(activity, R.color.text_primary))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val starsTv = TextView(activity).apply {
                text = "★".repeat(moment.rating)
                textSize = 12f
                setTextColor(ContextCompat.getColor(activity, R.color.primary))
            }

            header.addView(emojiTv)
            header.addView(titleTv)
            header.addView(starsTv)
            cardLayout.addView(header)

            // 日期
            val dateTv = TextView(activity).apply {
                text = "📅 ${dateFormat.format(Date(moment.date))}"
                textSize = 11f
                setTextColor(ContextCompat.getColor(activity, R.color.text_hint))
                setPadding(dp(24), dp(2), 0, dp(4))
            }
            cardLayout.addView(dateTv)

            // 故事文本
            if (moment.story.isNotBlank()) {
                val storyTv = TextView(activity).apply {
                    text = moment.story
                    textSize = 13f
                    setTextColor(ContextCompat.getColor(activity, R.color.text_secondary))
                    setLineSpacing(dp(3).toFloat(), 1.1f)
                    setPadding(dp(4), dp(4), dp(4), dp(4))
                }
                cardLayout.addView(storyTv)
            }

            // 照片预览缩略图
            if (moment.photoPath.isNotBlank()) {
                val pFile = File(activity.filesDir, "item_photos/${moment.photoPath}")
                val realPFile = if (pFile.exists()) pFile else File(moment.photoPath)
                if (realPFile.exists()) {
                    val iv = ImageView(activity).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            dp(140)
                        ).apply {
                            topMargin = dp(6)
                            bottomMargin = dp(4)
                        }
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        background = ContextCompat.getDrawable(activity, R.drawable.bg_input_box)
                        clipToOutline = true
                        setImageURI(Uri.fromFile(realPFile))
                        setOnClickListener {
                            PhotoPreviewDialog.show(activity, moment.title.ifBlank { "回忆瞬间" }, moment.photoPath)
                        }
                    }
                    cardLayout.addView(iv)
                }
            }

            // 操作行：编辑与删除
            val actions = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
                setPadding(0, dp(6), 0, 0)
            }

            val btnEdit = TextView(activity).apply {
                text = "✏️ 编辑"
                textSize = 12f
                setTextColor(ContextCompat.getColor(activity, R.color.primary))
                setPadding(dp(8), dp(4), dp(8), dp(4))
                setOnClickListener {
                    showAddOrEditMomentDialog(activity, store, entry, editMoment = moment) {
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
                        title = "删除时光回忆",
                        message = "确定要删除这条【${moment.title}】生活回忆瞬间吗？",
                        emoji = "🗑️",
                        positiveText = "删除",
                        negativeText = "取消",
                        isDestructive = true
                    ) {
                        store.deleteMemoryMoment(entry.id, moment.id)
                        Toast.makeText(activity, "已删除该瞬间", Toast.LENGTH_SHORT).show()
                        onRefreshNeeded()
                    }
                }
            }

            actions.addView(btnEdit)
            actions.addView(btnDelete)
            cardLayout.addView(actions)

            card.addView(cardLayout)
            container.addView(card)
        }
    }

    /** 记录或编辑单条时光回忆瞬间弹窗 */
    private fun showAddOrEditMomentDialog(
        activity: Activity,
        store: DataStore,
        entry: Entry,
        editMoment: ItemMemoryMoment?,
        onSaved: () -> Unit
    ) {
        val binding = DialogAddMemoryMomentBinding.inflate(activity.layoutInflater)
        val isEdit = editMoment != null
        var selectedDate = editMoment?.date ?: System.currentTimeMillis()
        var selectedPhotoPath = editMoment?.photoPath ?: ""

        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(binding.root)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation

        if (isEdit) {
            binding.tvMomentDialogTitle.text = "编辑生活高光回忆"
            binding.etMomentEmoji.setText(editMoment!!.moodEmoji)
            binding.etMomentTitle.setText(editMoment.title)
            binding.etMomentStory.setText(editMoment.story)
            when (editMoment.rating) {
                1 -> binding.rbStar1.isChecked = true
                2 -> binding.rbStar2.isChecked = true
                3 -> binding.rbStar3.isChecked = true
                4 -> binding.rbStar4.isChecked = true
                else -> binding.rbStar5.isChecked = true
            }
        }

        binding.tvMomentDatePicker.text = "📅 " + dateFormat.format(Date(selectedDate))
        binding.tvMomentDatePicker.setOnClickListener {
            ModernDatePickerDialog.show(activity, selectedDate, "选择回忆日期") { timeMs ->
                selectedDate = timeMs
                binding.tvMomentDatePicker.text = "📅 " + dateFormat.format(Date(selectedDate))
            }
        }

        // 照片预览
        fun updatePhotoPreview() {
            if (selectedPhotoPath.isNotBlank()) {
                val pFile = File(activity.filesDir, "item_photos/$selectedPhotoPath")
                val realPFile = if (pFile.exists()) pFile else File(selectedPhotoPath)
                if (realPFile.exists()) {
                    binding.ivMomentPhotoPreview.setImageURI(Uri.fromFile(realPFile))
                    binding.ivMomentPhotoPreview.setPadding(0, 0, 0, 0)
                    binding.tvMomentPhotoHint.text = "已选定照片，点击右侧可更换"
                }
            }
        }
        updatePhotoPreview()

        binding.btnChooseMomentPhoto.setOnClickListener {
            // 打开系统相册选择图片并存入沙盒
            val intent = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            try {
                activity.startActivityForResult(intent, 2026) // 2026 for moment photo
                Toast.makeText(activity, "请选择一张生活高光现场照片", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(activity, "无法调用系统相册", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnCloseMomentInput.applyPressScaleAnimation(0.92f)
        binding.btnCloseMomentInput.setOnClickListener { dialog.dismiss() }

        binding.btnCancelMoment.applyPressScaleAnimation(0.92f)
        binding.btnCancelMoment.setOnClickListener { dialog.dismiss() }

        binding.btnSaveMoment.applyPressScaleAnimation(0.92f)
        binding.btnSaveMoment.setOnClickListener {
            val title = binding.etMomentTitle.text.toString().trim()
            val emoji = binding.etMomentEmoji.text.toString().trim().ifBlank { "✨" }
            val story = binding.etMomentStory.text.toString().trim()
            val rating = when {
                binding.rbStar1.isChecked -> 1
                binding.rbStar2.isChecked -> 2
                binding.rbStar3.isChecked -> 3
                binding.rbStar4.isChecked -> 4
                else -> 5
            }

            val newMoment = ItemMemoryMoment(
                id = editMoment?.id ?: UUID.randomUUID().toString(),
                title = title.ifBlank { "高光瞬间" },
                story = story,
                photoPath = selectedPhotoPath,
                date = selectedDate,
                moodEmoji = emoji,
                rating = rating
            )

            if (isEdit) {
                store.updateMemoryMoment(entry.id, newMoment)
                Toast.makeText(activity, "🎉 生活回忆已更新", Toast.LENGTH_SHORT).show()
            } else {
                store.addMemoryMoment(entry.id, newMoment)
                Toast.makeText(activity, "🎉 已成功记录生活高光回忆", Toast.LENGTH_SHORT).show()
            }

            dialog.dismiss()
            onSaved()
        }

        dialog.show()
    }

    /** 导出并保存分享时光画册高清海报 */
    private fun exportAndSharePoster(activity: Activity, entry: Entry) {
        Toast.makeText(activity, "正在渲染 1080P 拍立得时光画册长图...", Toast.LENGTH_SHORT).show()

        try {
            val bitmap = LifeCapsulePosterGenerator.generatePosterBitmap(activity, entry)
            val uri = LifeCapsulePosterGenerator.savePosterToGallery(activity, bitmap, entry.brand)

            if (uri != null) {
                ModernDialogHelper.showInfoDialog(
                    context = activity,
                    title = "🎉 画册长图已保存至相册！",
                    emoji = "🖼️",
                    message = "已成功生成【${entry.brand}】的 1080P 高清时光回忆录长图，并已保存至手机系统相册 (Pictures/Collecter)！\n\n您可以随时在相册查看或分享给亲友。",
                    buttonText = "🌟 查看大图 / 分享"
                ) {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/jpeg"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    activity.startActivity(Intent.createChooser(shareIntent, "分享【${entry.brand}】时光画册海报"))
                }
            } else {
                Toast.makeText(activity, "保存至系统相册失败，请检查存储权限", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(activity, "生成海报出错: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
