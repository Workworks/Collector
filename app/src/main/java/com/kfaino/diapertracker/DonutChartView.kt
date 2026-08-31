package com.kfaino.diapertracker

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import java.util.Locale

/**
 * 资产分类占比交互式环形图 (Interactive Donut Chart)
 * - 原生矢量 Canvas 绘制
 * - 平滑入场展开动画
 * - 触控拾取扇区高亮
 * - 中心显示资产总值与当前选中分类统计
 */
class DonutChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class Slice(
        val name: String,
        val value: Double,
        val color: Int,
        val percentage: Float = 0f
    )

    private var slices: List<Slice> = emptyList()
    private var totalAmount: Double = 0.0
    private var animProgress = 1f
    private var selectedIndex = -1

    var onSliceSelected: ((Slice?) -> Unit)? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val centerTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#94A3B8")
        textSize = 30f
        textAlign = Paint.Align.CENTER
    }

    private val centerValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 42f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    private val oval = RectF()

    fun setData(items: List<Slice>, animate: Boolean = true) {
        totalAmount = items.sumOf { it.value }
        slices = if (totalAmount > 0) {
            items.map { it.copy(percentage = (it.value / totalAmount).toFloat()) }
        } else {
            emptyList()
        }
        selectedIndex = -1

        if (animate) {
            val animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 650
                interpolator = DecelerateInterpolator()
                addUpdateListener {
                    animProgress = it.animatedValue as Float
                    invalidate()
                }
            }
            animator.start()
        } else {
            animProgress = 1f
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val size = Math.min(w, h)
        val strokeW = size * 0.16f
        val radius = (size - strokeW - 24f) / 2f
        val cx = w / 2f
        val cy = h / 2f

        oval.set(cx - radius, cy - radius, cx + radius, cy + radius)
        paint.strokeWidth = strokeW

        if (slices.isEmpty() || totalAmount <= 0.0) {
            // 空数据灰色环
            paint.color = Color.parseColor("#1E293B")
            canvas.drawArc(oval, 0f, 360f, false, paint)
            canvas.drawText("暂无数据", cx, cy + 12f, centerTitlePaint)
            return
        }

        var startAngle = -90f
        for (i in slices.indices) {
            val slice = slices[i]
            val sweep = (slice.percentage * 360f) * animProgress
            paint.color = slice.color

            val isSelected = (i == selectedIndex)
            paint.strokeWidth = if (isSelected) strokeW * 1.25f else strokeW

            // 绘制扇区弧线 (留微小 1.5 度间隙以呈现精致分割线)
            val actualSweep = if (sweep > 3f) sweep - 1.5f else sweep
            canvas.drawArc(oval, startAngle + 0.75f, actualSweep, false, paint)

            startAngle += sweep
        }

        // 中心文字渲染
        if (selectedIndex in slices.indices) {
            val s = slices[selectedIndex]
            centerTitlePaint.textSize = size * 0.065f
            centerTitlePaint.color = s.color
            canvas.drawText("${s.name} (${String.format(Locale.getDefault(), "%.1f%%", s.percentage * 100)})", cx, cy - 14f, centerTitlePaint)

            centerValPaint.textSize = size * 0.085f
            canvas.drawText("¥${String.format(Locale.getDefault(), "%,.0f", s.value)}", cx, cy + 32f, centerValPaint)
        } else {
            centerTitlePaint.textSize = size * 0.065f
            centerTitlePaint.color = Color.parseColor("#94A3B8")
            canvas.drawText("在役资产总值", cx, cy - 14f, centerTitlePaint)

            centerValPaint.textSize = size * 0.085f
            canvas.drawText("¥${String.format(Locale.getDefault(), "%,.0f", totalAmount)}", cx, cy + 32f, centerValPaint)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP && slices.isNotEmpty()) {
            val cx = width / 2f
            val cy = height / 2f
            val dx = event.x - cx
            val dy = event.y - cy
            val dist = Math.sqrt((dx * dx + dy * dy).toDouble())

            val size = Math.min(width, height)
            val strokeW = size * 0.16f
            val radius = (size - strokeW - 24f) / 2f

            if (dist in (radius - strokeW)..(radius + strokeW)) {
                var touchAngle = Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
                if (touchAngle < 0) touchAngle += 360f

                // 转换到 -90 度起始坐标
                val shifted = (touchAngle + 90f) % 360f
                var currentAngle = 0f
                var clickedIdx = -1

                for (i in slices.indices) {
                    val sweep = slices[i].percentage * 360f
                    if (shifted in currentAngle..(currentAngle + sweep)) {
                        clickedIdx = i
                        break
                    }
                    currentAngle += sweep
                }

                selectedIndex = if (selectedIndex == clickedIdx) -1 else clickedIdx
                invalidate()
                onSliceSelected?.invoke(if (selectedIndex != -1) slices[selectedIndex] else null)
            }
        }
        return true
    }
}
