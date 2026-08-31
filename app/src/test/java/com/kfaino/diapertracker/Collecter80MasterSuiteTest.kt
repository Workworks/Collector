package com.kfaino.diapertracker

import org.junit.Assert.*
import org.junit.Test

/**
 * 🧪 Collecter 8.0 终极宇宙文明元资产操作系统自动化单元测试套件
 */
class Collecter80MasterSuiteTest {

    @Test
    fun 气压高程差分货架层级定位测试() {
        val baseHpa = 1013.25
        val upperLayerHpa = 1013.10 // 气压下降约 0.15 hPa -> ~126 cm
        val layer = BarometricElevationHelper.calculateShelfLayer(baseHpa, upperLayerHpa)
        assertEquals(4, layer.estimatedShelfLayer)
        assertTrue("货架第4层", layer.layerDescription.contains("4"))
    }

    @Test
    fun 极端战备生命维持物资配给算法测试() {
        val plan = CrisisSurvivalRationAllocator.planSurvivalRation(
            totalFoodKcal = 36000.0, // 10 天
            totalCleanWaterLiters = 20.0, // 5 天水 -> 瓶颈
            familyMemberCount = 2
        )
        assertEquals(5, plan.totalDaysSustainable)
        assertTrue("饮用水为瓶颈", plan.emergencyStatusNotice.contains("饮用水"))
    }

    @Test
    fun 疫苗冷链变质时间积分测试() {
        val safeReport = ColdChainVaccineIntegrator.calculateDamage("乙肝疫苗", 2.0, 5.0)
        assertFalse("正常温度短时间未受损", safeReport.isPotencyCompromised)

        val badReport = ColdChainVaccineIntegrator.calculateDamage("狂犬疫苗", 48.0, 32.0)
        assertTrue("高温长时间效价丧失", badReport.isPotencyCompromised)
        assertTrue(badReport.safetyVerdict.contains("严禁接种"))
    }

    @Test
    fun 机器人抓取受力自适应测试() {
        val cupGrasp = RoboticGraspTopologyApi.calculateGrasp("玻璃水杯", 250f)
        assertTrue("易碎品判定", cupGrasp.isFragile)
        assertEquals(4.5f, cupGrasp.maxGripForceNewton, 0.001f)

        val toolGrasp = RoboticGraspTopologyApi.calculateGrasp("五金扳手", 500f)
        assertFalse("非易碎品", toolGrasp.isFragile)
        assertEquals(18.0f, toolGrasp.maxGripForceNewton, 0.001f)
    }

    @Test
    fun 后量子密码学抗量子信封封装测试() {
        val envelope = PostQuantumCryptoVault.sealData("TopSecretAssetVault2026", "MasterKey")
        assertNotNull(envelope.cipherPayloadHex)
        assertNotNull(envelope.pqcSignatureHex)
        assertEquals("Kyber1024-Hybrid", envelope.algorithm)
    }
}