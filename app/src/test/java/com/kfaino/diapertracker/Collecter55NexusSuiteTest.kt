package com.kfaino.diapertracker

import org.junit.Assert.*
import org.junit.Test

/**
 * 🧪 Collecter 5.5 Nexus 究极全息元操作系统自动化单元测试套件
 */
class Collecter55NexusSuiteTest {

    @Test
    fun `3D装箱空间容积率计算准确性测试`() {
        val dim = BinPacking3dOptimizer.BoxDimensions(50.0, 40.0, 30.0) // 60L
        assertEquals(60.0, dim.volumeCm3 / 1000.0, 0.001)

        val analysis = BinPacking3dOptimizer.analyzeBox(dim, 10, 3.0) // 使用 30L
        assertEquals(60.0, analysis.boxVolumeLiters, 0.001)
        assertEquals(30.0, analysis.usedVolumeLiters, 0.001)
        assertEquals(50, analysis.remainingCapacityPercent)
        assertFalse("30L 未超载", analysis.isOverloaded)
    }

    @Test
    fun 光学镜头高湿防霉预警测试() {
        val alertNormal = AmbientDegradationMonitor.checkRisk("相机镜头", 22.0, 45.0)
        assertEquals(AmbientDegradationMonitor.RiskLevel.SAFE, alertNormal.riskLevel)

        val alertDanger = AmbientDegradationMonitor.checkRisk("微单镜头", 25.0, 72.0)
        assertEquals(AmbientDegradationMonitor.RiskLevel.DANGER, alertDanger.riskLevel)
        assertTrue(alertDanger.warningMessage.contains("霉菌"))
    }

    @Test
    fun 多币种离线折算与格式化准确性测试() {
        val usdAmount = 100.0
        val cny = MultiCurrencyConverter.convertToCny(usdAmount, MultiCurrencyConverter.Currency.USD)
        assertEquals(725.0, cny, 0.001)

        val formatted = MultiCurrencyConverter.formatWithCurrency(123.45, MultiCurrencyConverter.Currency.JPY)
        assertTrue("日元符号格式化", formatted.startsWith("円"))
    }

    @Test
    fun 耗材流速耗尽周期预测测试() {
        val prediction = ConsumptionVelocityPredictor.predictExhaustion("空气净化器滤芯", 5.0, 1.0)
        assertEquals(5, prediction.daysUntilExhausted)
        assertTrue("小于7天触发补货提醒", prediction.needsRestockPrompt)
    }
}