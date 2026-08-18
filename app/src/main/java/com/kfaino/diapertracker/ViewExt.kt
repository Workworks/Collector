package com.kfaino.diapertracker

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View

/**
 * 为任意 View 注入灵动轻触跟手微动效 (Touch & Scale Spring Micro-Animations)
 */
@SuppressLint("ClickableViewAccessibility")
fun View.applyPressScaleAnimation(scale: Float = 0.94f) {
    setOnTouchListener { v, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
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
