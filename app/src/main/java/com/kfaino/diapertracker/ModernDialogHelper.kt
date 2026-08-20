package com.kfaino.diapertracker

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kfaino.diapertracker.databinding.DialogModernBaseBinding
import com.kfaino.diapertracker.databinding.DialogModernInputBinding

/**
 * 全局统一现代化弹窗组件工厂 (Modern UI Dialog Helper)
 * - 统一 26dp 双层质感卡片与圆角微光边缘
 * - 统一平滑入场与出场动画 CustomDialogAnimation
 * - 淘汰粗陋的原生系统 Alert，全量升级为高颜值沉浸式弹窗
 */
object ModernDialogHelper {

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
                androidx.core.content.ContextCompat.getColorStateList(context, R.color.danger)
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
