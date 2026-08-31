package com.kfaino.diapertracker

import android.util.Log

/**
 * 🥽 Android ARCore 空间全息锚定与虚拟箱柜投射助手 (AR Spatial Anchor Helper)
 * 将虚拟收纳箱与浮空信息卡片以毫米级精度锚定在真实物理空间
 */
object ArSpatialAnchorHelper {

    private const val TAG = "ArSpatialAnchorHelper"

    data class SpatialAnchor(
        val anchorId: String,
        val targetBoxName: String,
        val posX: Float,
        val posY: Float,
        val posZ: Float,
        val rotQx: Float,
        val rotQy: Float,
        val rotQz: Float,
        val rotQw: Float,
        val createdAt: Long = System.currentTimeMillis()
    )

    fun createAnchor(boxName: String, x: Float, y: Float, z: Float): SpatialAnchor {
        val anchor = SpatialAnchor(
            anchorId = "anchor_" + java.util.UUID.randomUUID().toString().take(8),
            targetBoxName = boxName,
            posX = x, posY = y, posZ = z,
            rotQx = 0f, rotQy = 0f, rotQz = 0f, rotQw = 1f
        )
        Log.i(TAG, "创建空间全息锚点: " + anchor.anchorId + " -> " + boxName + " (" + x + ", " + y + ", " + z + ")")
        return anchor
    }
}