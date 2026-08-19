package com.kfaino.diapertracker

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * 交互式家庭空间平面图画布 (Interactive Floor Plan & Pin Mapper)
 * 支持绘制房间、物品图钉、实时热区触控、房间物品统计以及图钉快速定位
 */
class FloorPlanView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var houseSpace: HouseSpace = HouseSpace(id = "default", name = "我的家")
        set(value) {
            field = value
            invalidate()
        }

    var entries: List<Entry> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    var isPinSelectionMode: Boolean = false
    var selectedPinX: Float = -1f
    var selectedPinY: Float = -1f

    // 高亮聚焦目标（用于从物品卡片一键穿梭定位）
    var highlightedRoomName: String? = null
    var highlightedPinX: Float = -1f
    var highlightedPinY: Float = -1f

    var onRoomClicked: ((HouseRoom, List<Entry>) -> Unit)? = null
    var onPinPlaced: ((Float, Float, String) -> Unit)? = null
    var onPinClicked: ((Entry) -> Unit)? = null

    private val highlightRoomFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#33F59E0B") // 琥珀金半透明高亮
    }

    private val highlightRoomStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = Color.parseColor("#F59E0B") // 琥珀金高亮边框
    }

    private val highlightPulsePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.parseColor("#F59E0B")
    }

    private val highlightPulseFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#44F59E0B")
    }

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#131926")
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1F2A40")
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val roomFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val roomStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 34f
        textAlign = Paint.Align.CENTER
    }

    private val countBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#10B981")
        style = Paint.Style.FILL
    }

    private val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 24f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#EF4444")
        style = Paint.Style.FILL
    }

    private val pinGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#44EF4444")
        style = Paint.Style.FILL
    }

    private val pinSelectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#10B981")
        style = Paint.Style.FILL
    }

    private val roomRect = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        // 1. 绘制平面图底板与微网格
        val outerRect = RectF(8f, 8f, w - 8f, h - 8f)
        canvas.drawRoundRect(outerRect, 24f, 24f, bgPaint)
        canvas.drawRoundRect(outerRect, 24f, 24f, gridPaint)

        // 2. 绘制每个房间
        for (room in houseSpace.rooms) {
            val rx = room.xPct * w
            val ry = room.yPct * h
            val rw = room.widthPct * w
            val rh = room.heightPct * h
            roomRect.set(rx, ry, rx + rw, ry + rh)

            val baseColor = try { Color.parseColor(room.colorHex) } catch (_: Exception) { Color.parseColor("#10B981") }
            val fillAlpha = 0x22
            val fillColor = Color.argb(fillAlpha, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor))

            roomFillPaint.color = fillColor
            roomStrokePaint.color = baseColor

            val isHighlightedRoom = (highlightedRoomName != null && highlightedRoomName == room.name)
            if (isHighlightedRoom) {
                canvas.drawRoundRect(roomRect, 16f, 16f, highlightRoomFillPaint)
                canvas.drawRoundRect(roomRect, 16f, 16f, highlightRoomStrokePaint)
            } else {
                canvas.drawRoundRect(roomRect, 16f, 16f, roomFillPaint)
                canvas.drawRoundRect(roomRect, 16f, 16f, roomStrokePaint)
            }

            // 房间图标与名称
            val centerX = roomRect.centerX()
            val centerY = roomRect.centerY()
            textPaint.textSize = (Math.min(rw, rh) * 0.22f).coerceIn(22f, 38f)
            textPaint.color = if (isHighlightedRoom) Color.parseColor("#F59E0B") else Color.WHITE
            canvas.drawText("${room.icon} ${room.name}", centerX, centerY + (textPaint.textSize / 3), textPaint)
            textPaint.color = Color.WHITE

            // 统计该房间内的物品数量
            val itemsInRoom = entries.filter { it.roomName == room.name || it.location.contains(room.name) }
            if (itemsInRoom.isNotEmpty()) {
                val totalQty = itemsInRoom.sumOf { it.qty }
                val badgeX = roomRect.right - 28f
                val badgeY = roomRect.top + 28f
                canvas.drawCircle(badgeX, badgeY, 20f, countBadgePaint)
                canvas.drawText("$totalQty", badgeX, badgeY + 8f, badgeTextPaint)
            }
        }

        // 3. 绘制已标记图钉的物品 Pins
        for (e in entries) {
            if (e.pinX in 0f..1f && e.pinY in 0f..1f && (e.houseName == houseSpace.name || e.houseId == houseSpace.id)) {
                val px = e.pinX * w
                val py = e.pinY * h
                canvas.drawCircle(px, py, 22f, pinGlowPaint)
                canvas.drawCircle(px, py, 12f, pinPaint)
            }
        }

        // 4. 选点模式下当前选中的位置图钉
        if (selectedPinX in 0f..1f && selectedPinY in 0f..1f) {
            val px = selectedPinX * w
            val py = selectedPinY * h
            canvas.drawCircle(px, py, 28f, pinSelectedPaint)
            canvas.drawCircle(px, py, 14f, pinPaint)
        }

        // 5. 焦点高亮寻物图钉 (Pulsing Focus Pin)
        if (highlightedPinX in 0f..1f && highlightedPinY in 0f..1f) {
            val px = highlightedPinX * w
            val py = highlightedPinY * h
            canvas.drawCircle(px, py, 42f, highlightPulseFill)
            canvas.drawCircle(px, py, 30f, highlightPulsePaint)
            canvas.drawCircle(px, py, 15f, highlightRoomStrokePaint)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val w = width.toFloat()
            val h = height.toFloat()
            if (w <= 0 || h <= 0) return true

            val xPct = (event.x / w).coerceIn(0f, 1f)
            val yPct = (event.y / h).coerceIn(0f, 1f)

            // 找到点击的房间
            var clickedRoom: HouseRoom? = null
            for (room in houseSpace.rooms) {
                if (xPct >= room.xPct && xPct <= (room.xPct + room.widthPct) &&
                    yPct >= room.yPct && yPct <= (room.yPct + room.heightPct)) {
                    clickedRoom = room
                    break
                }
            }

            if (isPinSelectionMode) {
                selectedPinX = xPct
                selectedPinY = yPct
                invalidate()
                val rName = clickedRoom?.name ?: "自定义位置"
                onPinPlaced?.invoke(xPct, yPct, rName)
            } else {
                if (clickedRoom != null) {
                    val itemsInRoom = entries.filter { it.roomName == clickedRoom.name || it.location.contains(clickedRoom.name) }
                    onRoomClicked?.invoke(clickedRoom, itemsInRoom)
                }
            }
        }
        return true
    }
}
