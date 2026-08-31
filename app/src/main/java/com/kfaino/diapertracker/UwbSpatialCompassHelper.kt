package com.kfaino.diapertracker

import kotlin.math.atan2

/**
 * 🧭 超宽带 UWB 厘米级空间精准测距与罗盘指向引擎 (UWB Spatial Compass Helper)
 */
object UwbSpatialCompassHelper {

    data class UwbVector(
        val distanceCentimeters: Float,
        val azimuthDegrees: Float,
        val elevationDegrees: Float,
        val isLineOfSight: Boolean
    )

    fun calculateVector(dx: Float, dy: Float, dz: Float): UwbVector {
        val dist = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz) * 100f
        val azimuth = Math.toDegrees(atan2(dx.toDouble(), dz.toDouble())).toFloat()
        val elevation = Math.toDegrees(atan2(dy.toDouble(), kotlin.math.sqrt((dx * dx + dz * dz).toDouble()))).toFloat()

        return UwbVector(
            distanceCentimeters = dist,
            azimuthDegrees = (azimuth + 360f) % 360f,
            elevationDegrees = elevation,
            isLineOfSight = true
        )
    }
}