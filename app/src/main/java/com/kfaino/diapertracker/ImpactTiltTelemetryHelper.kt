package com.kfaino.diapertracker

/**
 * 📐 收纳箱震动倾角遥测与跌落受损报警助手 (Impact & Tilt Telemetry Helper)
 */
object ImpactTiltTelemetryHelper {

    data class TiltImpactState(
        val pitchDegrees: Float,
        val rollDegrees: Float,
        val maxImpactG: Float,
        val isTiltedOver: Boolean,
        val hasSevereImpact: Boolean
    )

    fun evaluateOrientation(pitch: Float, roll: Float, accelG: Float): TiltImpactState {
        val tilted = Math.abs(pitch) > 45f || Math.abs(roll) > 45f
        val severe = accelG > 3.0f // 超过 3G 冲击判定为跌落

        return TiltImpactState(
            pitchDegrees = pitch,
            rollDegrees = roll,
            maxImpactG = accelG,
            isTiltedOver = tilted,
            hasSevereImpact = severe
        )
    }
}