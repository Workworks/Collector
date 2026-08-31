package com.kfaino.diapertracker

import org.junit.Assert.*
import org.junit.Test

/**
 * 🧪 Collecter 5.0 全维新特性自动化单元测试套件
 */
class Collecter5MasterSuiteTest {

    @Test
    fun BLE距离测算模型准确性验证() {
        val distClose = BleProximityRadarHelper.calculateDistance(-50)
        val distFar = BleProximityRadarHelper.calculateDistance(-85)
        assertTrue("强信号距离应小于弱信号距离", distClose < distFar)
        assertTrue("近场信号计算有效", distClose > 0.0)
    }

    @Test
    fun 全品类非线性残值衰减算法验证() {
        val purchaseTime = System.currentTimeMillis() - 365L * 86400000L // 持有一整年

        // 1. 数码产品第一年折旧应较大
        val digitalVal = DynamicDepreciationEngine.calculateCurrentValuation(10000.0, purchaseTime, "数码电脑")
        assertTrue("数码产品第一年贬值约30%~40%", digitalVal in 5000.0..7500.0)

        // 2. 珠宝奢侈品折旧率应较低
        val luxuryVal = DynamicDepreciationEngine.calculateCurrentValuation(10000.0, purchaseTime, "黄金珠宝")
        assertTrue("奢侈品首年残值应高于85%", luxuryVal > 8500.0)
    }

    @Test
    fun 说明书客服电话与质保期正则提取测试() {
        val text = "感谢购买本设备！本产品全国联保，整机质保2年。如有疑问请拨打全国官方服务热线：400-888-9999。请置于干燥通风处保存。"
        val info = ManualKnowledgeExtractor.extractInfo(text)
        assertEquals("400-888-9999", info.serviceHotline)
        assertEquals(24, info.warrantyMonths)
        assertTrue(info.maintenanceTips.any { it.contains("干燥通风") })
    }

    @Test
    fun E2EE零知识信封加密与解密往返无损验证() {
        val rawJson = "{\"secret\":\"my_offline_private_asset\",\"amount\":99999}"
        val password = "MySuperSecretMasterPassword123!"

        val envelope = E2eeSyncEngine.encryptPayload(rawJson, password)
        assertNotNull("加密信封不应为空", envelope)

        val decrypted = E2eeSyncEngine.decryptPayload(envelope!!, password)
        assertEquals("解密内容必须与原文100%一致", rawJson, decrypted)

        // 错误密码解密应失败并安全拦截
        val badDecrypted = E2eeSyncEngine.decryptPayload(envelope, "WrongPassword")
        assertNull("错误口令解密应返回 null", badDecrypted)
    }
}