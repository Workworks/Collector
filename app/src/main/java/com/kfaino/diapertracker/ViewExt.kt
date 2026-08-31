package com.kfaino.diapertracker

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View

/**
 * 触发系统级微震动触感反馈 (Linear Motor Haptic Vibration Feedback)
 */
fun View.performAppHapticFeedback() {
    try {
        val store = DataStore(context)
        if (!store.isHapticFeedbackEnabled()) return

        // 1. 优先调用 View 自带的触觉反馈系统 (FLAG_IGNORE_GLOBAL_SETTING 确保系统级响应)
        val success = performHapticFeedback(
            HapticFeedbackConstants.KEYBOARD_TAP,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )

        // 2. 如果系统触感反馈未触发或设备定制受限，采用 Vibrator 精确短震动双重兜底 (12ms 轻触)
        if (!success) {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            vibrator?.let {
                if (it.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        it.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                    } else {
                        @Suppress("DEPRECATION")
                        it.vibrate(12L)
                    }
                }
            }
        }
    } catch (_: Exception) {
        // 忽略无法震动的非关键异常
    }
}

/**
 * 为任意 View 注入灵动轻触微动效与触感震动 (Touch & Scale Spring Micro-Animations + Haptics)
 */
@SuppressLint("ClickableViewAccessibility")
fun View.applyPressScaleAnimation(scale: Float = 0.94f) {
    setOnTouchListener { v, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                v.performAppHapticFeedback()
                v.animate()
                    .scaleX(scale)
                    .scaleY(scale)
                    .setDuration(90)
                    .start()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                v.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(120)
                    .start()
            }
        }
        false
    }
}
