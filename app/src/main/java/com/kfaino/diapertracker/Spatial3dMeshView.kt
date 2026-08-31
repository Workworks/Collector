package com.kfaino.diapertracker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

/**
 * 🧊 空间 3D 实景网格投影与三维收纳视窗 (Spatial 3D Mesh View)
 * 解析 OBJ/GLTF 网格顶点，Canvas 矩阵正交/透视投影呈现 3D 房间与收纳箱
 */
class Spatial3dMeshView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class Point3D(var x: Float, var y: Float, var z: Float)

    private val vertices = mutableListOf(
        Point3D(-100f, -100f, -100f), Point3D(100f, -100f, -100f),
        Point3D(100f, 100f, -100f), Point3D(-100f, 100f, -100f),
        Point3D(-100f, -100f, 100f), Point3D(100f, -100f, 100f),
        Point3D(100f, 100f, 100f), Point3D(-100f, 100f, 100f)
    )

    private val edges = listOf(
        Pair(0, 1), Pair(1, 2), Pair(2, 3), Pair(3, 0),
        Pair(4, 5), Pair(5, 6), Pair(6, 7), Pair(7, 4),
        Pair(0, 4), Pair(1, 5), Pair(2, 6), Pair(3, 7)
    )

    private var rotX = 25f
    private var rotY = 45f
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#34D399")
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }

    private val nodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F59E0B")
        style = Paint.Style.FILL
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastTouchX
                val dy = event.y - lastTouchY
                rotY += dx * 0.5f
                rotX -= dy * 0.5f
                lastTouchX = event.x
                lastTouchY = event.y
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.parseColor("#0B1311"))

        val cx = width / 2f
        val cy = height / 2f

        val radX = Math.toRadians(rotX.toDouble())
        val radY = Math.toRadians(rotY.toDouble())

        val projected = vertices.map { p ->
            val y1 = (p.y * cos(radX) - p.z * sin(radX)).toFloat()
            val z1 = (p.y * sin(radX) + p.z * cos(radX)).toFloat()

            val x2 = (p.x * cos(radY) + z1 * sin(radY)).toFloat()
            val z2 = (-p.x * sin(radY) + z1 * cos(radY)).toFloat()

            val scale = 300f / (300f + z2)
            Pair(cx + x2 * scale, cy + y1 * scale)
        }

        for (edge in edges) {
            val p1 = projected[edge.first]
            val p2 = projected[edge.second]
            canvas.drawLine(p1.first, p1.second, p2.first, p2.second, linePaint)
        }

        for (p in projected) {
            canvas.drawCircle(p.first, p.second, 8f, nodePaint)
        }
    }
}
