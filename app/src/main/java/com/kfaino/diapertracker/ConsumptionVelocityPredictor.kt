package com.kfaino.diapertracker

import kotlin.math.max

/**
 * ⏳ 智能耗材用量流速预测与自动补货助手 (Consumption Velocity Predictor)
 * 根据家庭成员消耗速率动态推演滤芯、猫粮、纸巾、咖啡豆耗尽日期
 */
object ConsumptionVelocityPredictor {

    data class ConsumableVelocity(
        val itemName: String,
        val currentRemainingQty: Double,
        val dailyConsumptionRate: Double,
        val daysUntilExhausted: Int,
        val needsRestockPrompt: Boolean
    )

    fun predictExhaustion(itemName: String, currentQty: Double, dailyRate: Double): ConsumableVelocity {
        val safeRate = max(0.01, dailyRate)
        val daysLeft = (currentQty / safeRate).toInt()
        val needPrompt = daysLeft <= 7 // 少于 7 天提醒补货

        return ConsumableVelocity(
            itemName = itemName,
            currentRemainingQty = currentQty,
            dailyConsumptionRate = dailyRate,
            daysUntilExhausted = daysLeft,
            needsRestockPrompt = needPrompt
        )
    }
}