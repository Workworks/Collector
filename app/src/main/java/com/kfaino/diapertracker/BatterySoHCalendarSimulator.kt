package com.kfaino.diapertracker

/**
 * 🔋 闲置锂电池自放电与健康度（SoH）日历寿命模拟器 (Battery SoH Simulator)
 */
object BatterySoHCalendarSimulator {

    data class BatteryHealthStatus(
        val deviceName: String,
        val currentEstimatedSocPercent: Int,
        val healthSoHPercent: Int,
        val needsChargeMaintenance: Boolean,
        val advice: String
    )

    fun simulateBattery(name: String, initialSoc: Int, monthsIdle: Int): BatteryHealthStatus {
        // 锂电池常温月自放电率约为 2%~3%
        val remainingSoc = (initialSoc - monthsIdle * 2.5).toInt().coerceAtLeast(0)
        val soh = (100 - monthsIdle * 0.4).toInt().coerceIn(60, 100)
        val needCharge = remainingSoc < 25

        val adv = if (needCharge) {
            "⚠️ 剩余电量约 " + remainingSoc + "%，即将进入深度亏电过放危险区，请立即充至 50%~60% 保养电量！"
        } else {
            "✨ 电池储存状态良好，剩余电量充足。"
        }

        return BatteryHealthStatus(name, remainingSoc, soh, needCharge, adv)
    }
}