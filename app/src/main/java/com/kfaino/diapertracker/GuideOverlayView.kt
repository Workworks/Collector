package com.kfaino.diapertracker

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.kfaino.diapertracker.databinding.LayoutGuideTooltipBinding

/**
 * 手把手全景引导聚光灯遮罩视图 (Guide Spotlight Overlay)
 * - 采用 Canvas PorterDuff.Mode.CLEAR 进行像素级圆角镂空
 * - 严格触控拦截：仅允许操作高亮镂空目标与引导卡片，屏蔽一切其他界面按钮
 * - 外圈带有脉冲呼吸金色/翡翠绿光晕
 * - 智能计算引导气泡在目标上方或下方的最佳展示位置
 */
class GuideOverlayView(context: Context) : FrameLayout(context) {

    private val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#D9000000") // 85% 半透明暗黑遮罩
    }

    private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(2.5f)
        color = ContextCompat.getColor(context, R.color.primary)
    }

    private val pulsePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(4f)
        color = ContextCompat.getColor(context, R.color.primary)
    }

    private var targetRect = RectF()
    private var targetCornerRadius = dpToPx(16f)
    private var pulseRadiusOffset = 0f
    private var pulseAlpha = 255

    private val tooltipBinding = LayoutGuideTooltipBinding.inflate(LayoutInflater.from(context), this, false)
    private var onTargetClickAction: (() -> Unit)? = null
    private var currentTargetView: View? = null

    private var pulseAnimator: ValueAnimator? = null

    init {
        setWillNotDraw(false)
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        addView(tooltipBinding.root)

        // 呼吸脉冲动画
        pulseAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1400
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { anim ->
                val f = anim.animatedValue as Float
                pulseRadiusOffset = f * dpToPx(12f)
                pulseAlpha = ((1f - f) * 200).toInt().coerceIn(0, 255)
                invalidate()
            }
        }
        pulseAnimator?.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pulseAnimator?.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. 绘制全屏半透明暗色背景
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimPaint)

        // 2. 如果有目标区域，进行圆角镂空
        if (!targetRect.isEmpty) {
            canvas.drawRoundRect(targetRect, targetCornerRadius, targetCornerRadius, clearPaint)

            // 3. 绘制内层高亮发光边框
            canvas.drawRoundRect(targetRect, targetCornerRadius, targetCornerRadius, strokePaint)

            // 4. 绘制外层呼吸光晕
            pulsePaint.alpha = pulseAlpha
            val pulseRect = RectF(
                targetRect.left - pulseRadiusOffset,
                targetRect.top - pulseRadiusOffset,
                targetRect.right + pulseRadiusOffset,
                targetRect.bottom + pulseRadiusOffset
            )
            canvas.drawRoundRect(pulseRect, targetCornerRadius + pulseRadiusOffset, targetCornerRadius + pulseRadiusOffset, pulsePaint)
        }
    }

    /**
     * 严格触控拦截机制：
     * - 允许点击目标区域（触发目标并推进步骤）
     * - 允许点击气泡卡片区域（上一步/下一步/退出）
     * - 拦截并屏蔽全屏其余所有点击，防止误触非目标控件
     */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val x = ev.x
        val y = ev.y

        // 1. 检查是否点击在引导气泡卡片内
        val tooltipRect = Rect()
        tooltipBinding.cardGuideTooltip.getHitRect(tooltipRect)
        if (tooltipRect.contains(x.toInt(), y.toInt())) {
            return super.dispatchTouchEvent(ev)
        }

        // 2. 检查是否点击在高亮目标区域内
        if (!targetRect.isEmpty && targetRect.contains(x, y)) {
            if (ev.action == MotionEvent.ACTION_UP) {
                // 触发目标实际点击
                currentTargetView?.performClick()
                onTargetClickAction?.invoke()
            }
            return true
        }

        // 3. 点击在其他无关区域：严格拦截，并进行防误触抖动提示与轻微震动
        if (ev.action == MotionEvent.ACTION_DOWN) {
            performAppHapticFeedback()
            shakeTooltipCard()
        }
        return true
    }

    private fun shakeTooltipCard() {
        tooltipBinding.cardGuideTooltip.animate()
            .translationXBy(dpToPx(8f))
            .setDuration(50)
            .withEndAction {
                tooltipBinding.cardGuideTooltip.animate()
                    .translationXBy(-dpToPx(16f))
                    .setDuration(50)
                    .withEndAction {
                        tooltipBinding.cardGuideTooltip.animate()
                            .translationX(0f)
                            .setDuration(50)
                            .start()
                    }
                    .start()
            }
            .start()
    }

    /**
     * 更新当前引导步骤
     */
    fun showStep(
        stepIndex: Int,
        totalSteps: Int,
        title: String,
        desc: String,
        actionHint: String,
        targetView: View?,
        onNext: () -> Unit,
        onPrev: (() -> Unit)? = null,
        onExit: () -> Unit,
        onTargetClick: (() -> Unit)? = null
    ) {
        currentTargetView = targetView
        onTargetClickAction = onTargetClick

        // 绑定文案
        tooltipBinding.tvGuideStepBadge.text = "🎯 步骤 $stepIndex / $totalSteps"
        tooltipBinding.tvGuideTitle.text = title
        tooltipBinding.tvGuideDesc.text = desc
        tooltipBinding.tvGuideActionText.text = actionHint

        // 按钮状态
        tooltipBinding.btnGuidePrev.visibility = if (onPrev != null) View.VISIBLE else View.GONE
        tooltipBinding.btnGuidePrev.setOnClickListener {
            performAppHapticFeedback()
            onPrev?.invoke()
        }

        tooltipBinding.btnGuideNext.text = if (stepIndex == totalSteps) "🎉 完成教学" else "我知道了 · 下一步 ➔"
        tooltipBinding.btnGuideNext.setOnClickListener {
            performAppHapticFeedback()
            onNext()
        }

        tooltipBinding.btnExitTour.setOnClickListener {
            performAppHapticFeedback()
            onExit()
        }

        // 计算目标 View 屏幕绝对坐标
        if (targetView != null && targetView.isAttachedToWindow) {
            val loc = IntArray(2)
            targetView.getLocationInWindow(loc)
            val padding = dpToPx(8f)
            targetRect.set(
                loc[0] - padding,
                loc[1] - padding,
                loc[0] + targetView.width + padding,
                loc[1] + targetView.height + padding
            )
        } else {
            targetRect.setEmpty()
        }

        // 智能计算引导气泡布局位置 (如果目标靠下，气泡放在上方；如果目标靠上，气泡放在下方)
        positionTooltip()
        invalidate()
    }

    private fun positionTooltip() {
        val lp = tooltipBinding.cardGuideTooltip.layoutParams as LayoutParams

        if (targetRect.isEmpty) {
            // 居中展示
            lp.gravity = Gravity.CENTER
            lp.topMargin = 0
            lp.bottomMargin = 0
        } else {
            val screenHeight = resources.displayMetrics.heightPixels
            val targetCenterY = targetRect.centerY()

            if (targetCenterY > screenHeight / 2) {
                // 目标在屏幕下半部 -> 气泡放在目标上方
                lp.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                lp.topMargin = (targetRect.top - dpToPx(260f)).toInt().coerceAtLeast(dpToPx(40f).toInt())
                lp.bottomMargin = 0
            } else {
                // 目标在屏幕上半部 -> 气泡放在目标下方
                lp.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                lp.topMargin = (targetRect.bottom + dpToPx(16f)).toInt()
                lp.bottomMargin = 0
            }
        }
        tooltipBinding.cardGuideTooltip.layoutParams = lp
    }

    private fun dpToPx(dp: Float): Float {
        return dp * resources.displayMetrics.density
    }
}
