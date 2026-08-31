package com.kfaino.diapertracker

import kotlin.math.exp

/**
 * 💉 医用冷链生物制品与疫苗温区时间积分器 (Cold-Chain Vaccine Integrator)
 */
object ColdChainVaccineIntegrator {

    data class ThermalDamageReport(
        val vaccineName: String,
        val cumulativeDamageScore: Double, // 0.0 ~ 100.0 (超过100失效)
        val isPotencyCompromised: Boolean,
        val safetyVerdict: String
    )

    fun calculateDamage(vaccineName: String, exposureHours: Double, avgTempCelsius: Double): ThermalDamageReport {
        // Arrhenius 反应速率公式：温度每升高10度，化学降解速率翻倍
        val standardTemp = 4.0 // 标准储存 2~8°C
        val deltaT = (avgTempCelsius - standardTemp).coerceAtLeast(0.0)
        val reactionRateMultiplier = exp(0.12 * deltaT)

        val damage = (exposureHours * reactionRateMultiplier * 2.5).coerceIn(0.0, 100.0)
        val compromised = damage >= 80.0

        val verdict = when {
            compromised -> "❌ 累积热损耗过高，生物效价可能严重丧失，严禁接种/使用！"
            damage > 40.0 -> "⚠️ 曾经历短时断电升温，剩余安全有效期缩短 50%，建议优先使用。"
            else -> "✨ 冷链保温合规，生物活性完好。"
        }

        return ThermalDamageReport(vaccineName, damage, compromised, verdict)
    }
}