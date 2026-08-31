package com.kfaino.diapertracker

/**
 * 🌬️ 真空密封袋与充氮防氧化气体阻隔衰减推演器 (Vacuum Gas Barrier Degradation)
 */
object VacuumGasBarrierDegradation {

    data class GasBarrierReport(
        val packageType: String,
        val oxygenPpmLevel: Double,
        val isDeoxidizerExhausted: Boolean,
        val maintenanceReminder: String
    )

    fun estimateBarrier(packageType: String, monthsSealed: Int): GasBarrierReport {
        val otr = when {
            packageType.contains("铝箔") -> 0.05 // 铝箔袋阻氧性极佳
            packageType.contains("尼龙") -> 0.45
            else -> 1.5 // 普通 PE 袋
        }

        val o2Ppm = (monthsSealed * otr * 12.0).coerceIn(0.0, 2000.0)
        val exhausted = o2Ppm > 100.0

        val rem = if (exhausted) {
            "⚠️ 脱氧剂已吸附饱和，袋内氧气浓度上升，名贵藏品/雪茄建议重新抽真空并更换脱氧剂包！"
        } else {
            "✨ 充氮/真空密封阻氧状态良好。"
        }

        return GasBarrierReport(packageType, o2Ppm, exhausted, rem)
    }
}