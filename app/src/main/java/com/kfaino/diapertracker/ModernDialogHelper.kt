package com.kfaino.diapertracker

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kfaino.diapertracker.databinding.DialogModernBaseBinding
import com.kfaino.diapertracker.databinding.DialogModernInputBinding

/**
 * 💎 全局统一现代化高定弹窗组件工厂 (Modern UI Dialog Helper)
 * - 统一 26dp 双层质感卡片与圆角微光边缘
 * - 统一平滑入场与出场动画 CustomDialogAnimation
 * - 淘汰粗陋的原生系统 Alert，全量升级为高颜值沉浸式弹窗
 */
object ModernDialogHelper {

    /** 统一现代化确认/取消弹窗 */
    fun showConfirmDialog(
        context: Context,
        title: String,
        message: String,
        emoji: String = "💎",
        positiveText: String = "确认",
        negativeText: String = "取消",
        isDestructive: Boolean = false,
        onConfirm: () -> Unit
    ): AlertDialog {
        val binding = DialogModernBaseBinding.inflate(LayoutInflater.from(context))
        val dialog = MaterialAlertDialogBuilder(context)
            .setView(binding.root)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation

        binding.tvDialogEmoji.text = emoji
        binding.tvDialogTitle.text = title
        binding.tvDialogMessage.text = message
        binding.btnDialogPositive.text = positiveText
        binding.btnDialogNegative.text = negativeText

        if (isDestructive) {
            binding.btnDialogPositive.backgroundTintList =
                ContextCompat.getColorStateList(context, R.color.danger)
        }

        binding.btnDialogPositive.applyPressScaleAnimation(0.92f)
        binding.btnDialogNegative.applyPressScaleAnimation(0.92f)

        binding.btnDialogPositive.setOnClickListener {
            dialog.dismiss()
            onConfirm()
        }

        binding.btnDialogNegative.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        return dialog
    }

    /** 统一现代化信息提示/展示弹窗 (单确认按钮) */
    fun showInfoDialog(
        context: Context,
        title: String,
        message: String,
        emoji: String = "📋",
        buttonText: String = "我知道了",
        onDismiss: (() -> Unit)? = null
    ): AlertDialog {
        val binding = DialogModernBaseBinding.inflate(LayoutInflater.from(context))
        val dialog = MaterialAlertDialogBuilder(context)
            .setView(binding.root)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation

        binding.tvDialogEmoji.text = emoji
        binding.tvDialogTitle.text = title
        binding.tvDialogMessage.text = message
        binding.btnDialogPositive.text = buttonText
        binding.btnDialogNegative.visibility = View.GONE

        binding.btnDialogPositive.applyPressScaleAnimation(0.92f)
        binding.btnDialogPositive.setOnClickListener {
            dialog.dismiss()
            onDismiss?.invoke()
        }

        dialog.show()
        return dialog
    }

    /** 统一现代化单选列表弹窗 (用于筛选/排序/设备选择等) */
    fun showSingleChoiceDialog(
        context: Context,
        title: String,
        options: List<String>,
        selectedIndex: Int = -1,
        emoji: String = "🎯",
        onSelected: (Int, String) -> Unit
    ): AlertDialog {
        val density = context.resources.displayMetrics.density
        fun dp(dp: Int): Int = (dp * density + 0.5f).toInt()

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(context, R.drawable.bg_dialog_card)
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }

        // 头部
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(16) }
            layoutParams = lp
        }

        val emojiTv = TextView(context).apply {
            text = emoji
            textSize = 24f
            gravity = Gravity.CENTER
            background = ContextCompat.getDrawable(context, R.drawable.bg_icon_circle_soft)
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(48)).apply {
                bottomMargin = dp(10)
            }
        }

        val titleTv = TextView(context).apply {
            text = title
            textSize = 17f
            paint.isFakeBoldText = true
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            gravity = Gravity.CENTER
        }

        header.addView(emojiTv)
        header.addView(titleTv)
        root.addView(header)

        // 选项列表滚动容器
        val scroll = ScrollView(context).apply {
            overScrollMode = View.OVER_SCROLL_NEVER
            isScrollbarFadingEnabled = true
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply {
                // 最大高度限制
            }
            layoutParams = lp
        }

        val listContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        val dialog = MaterialAlertDialogBuilder(context)
            .setView(root)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation

        for ((idx, opt) in options.withIndex()) {
            val isSelected = (idx == selectedIndex)
            val card = MaterialCardView(context).apply {
                radius = dp(12).toFloat()
                cardElevation = 0f
                strokeWidth = dp(1)
                setStrokeColor(
                    if (isSelected) ContextCompat.getColor(context, R.color.primary)
                    else ContextCompat.getColor(context, R.color.card_border)
                )
                setCardBackgroundColor(
                    if (isSelected) ContextCompat.getColor(context, R.color.card)
                    else ContextCompat.getColor(context, R.color.input_bg)
                )
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(46)
                ).apply {
                    bottomMargin = dp(8)
                }
                layoutParams = lp
            }

            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(14), 0, dp(14), 0)
            }

            val label = TextView(context).apply {
                text = opt
                textSize = 14f
                if (isSelected) {
                    paint.isFakeBoldText = true
                    setTextColor(ContextCompat.getColor(context, R.color.primary))
                } else {
                    setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                }
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            row.addView(label)

            if (isSelected) {
                val check = TextView(context).apply {
                    text = "✓"
                    textSize = 15f
                    paint.isFakeBoldText = true
                    setTextColor(ContextCompat.getColor(context, R.color.primary))
                }
                row.addView(check)
            }

            card.addView(row)
            card.applyPressScaleAnimation(0.95f)
            card.setOnClickListener {
                dialog.dismiss()
                onSelected(idx, opt)
            }

            listContainer.addView(card)
        }

        scroll.addView(listContainer)
        root.addView(scroll)

        // 底部取消按钮
        val btnCancel = Button(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "取消"
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(44)
            ).apply {
                topMargin = dp(12)
            }
            layoutParams = lp
            applyPressScaleAnimation(0.92f)
            setOnClickListener { dialog.dismiss() }
        }
        root.addView(btnCancel)

        dialog.show()
        return dialog
    }

    /** 统一现代化文字输入弹窗 */
    fun showInputDialog(
        context: Context,
        title: String,
        subtitle: String = "",
        hint: String = "",
        defaultValue: String = "",
        emoji: String = "✏️",
        positiveText: String = "确认",
        negativeText: String = "取消",
        isMultiLine: Boolean = false,
        onConfirm: (String) -> Unit
    ): AlertDialog {
        val binding = DialogModernInputBinding.inflate(LayoutInflater.from(context))
        val dialog = MaterialAlertDialogBuilder(context)
            .setView(binding.root)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation

        binding.tvInputEmoji.text = emoji
        binding.tvInputTitle.text = title
        if (subtitle.isNotBlank()) {
            binding.tvInputSubtitle.text = subtitle
            binding.tvInputSubtitle.visibility = View.VISIBLE
        } else {
            binding.tvInputSubtitle.visibility = View.GONE
        }

        binding.etDialogInput.hint = hint
        if (defaultValue.isNotBlank()) {
            binding.etDialogInput.setText(defaultValue)
            binding.etDialogInput.setSelection(defaultValue.length)
        }

        if (isMultiLine) {
            binding.etDialogInput.minLines = 3
            binding.etDialogInput.maxLines = 6
        }

        binding.btnInputPositive.text = positiveText
        binding.btnInputNegative.text = negativeText

        binding.btnInputPositive.applyPressScaleAnimation(0.92f)
        binding.btnInputNegative.applyPressScaleAnimation(0.92f)

        binding.btnInputPositive.setOnClickListener {
            val text = binding.etDialogInput.text.toString().trim()
            dialog.dismiss()
            onConfirm(text)
        }

        binding.btnInputNegative.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        return dialog
    }
}
