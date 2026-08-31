package com.kfaino.diapertracker

import org.junit.Assert.*
import org.junit.Test

/**
 * 🧪 Collecter 6.0 究极文明物理世界操作系统自动化单元测试套件
 */
class Collecter60MasterSuiteTest {

    @Test
    fun 声学回声空间尺度与柜体材质推断测试() {
        val signature = AcousticEchoLocatorHelper.analyzeEcho(60L, 3500.0)
        assertEquals("金属/铁艺收纳柜", signature.detectedMaterial)
        assertTrue("空间容积计算合理", signature.estimatedRoomVolumeM3 > 10.0)
    }

    @Test
    fun 高分子材料水解老化风险模型验证() {
        val predictionSafe = PolymerHydrolysisModel.predictLifespan("PEBA 碳板跑鞋", 100, 50.0)
        assertTrue("低湿短时间风险较低", predictionSafe.hydrolysisRiskPercent < 20)

        val predictionHigh = PolymerHydrolysisModel.predictLifespan("PU 登山鞋中底", 800, 80.0)
        assertTrue("高湿长时间属于高危水解期", predictionHigh.hydrolysisRiskPercent > 70)
        assertTrue(predictionHigh.maintenanceRecommendation.contains("密封袋"))
    }

    @Test
    fun BIP39助记词生成与长度校验测试() {
        val words = Bip39AssetMnemonicVault.generate12Words("MyOfflineMasterSecretKey2026")
        assertEquals(12, words.size)
        assertTrue("助记词符合词表验证", Bip39AssetMnemonicVault.verifyMnemonicLength(words))
    }

    @Test
    fun 零知识资产脱敏证明验证() {
        val proof = ZkAssetProofVerifier.generateProof(50000.0, 30000.0, "salt123")
        assertTrue("满足门槛", proof.isValidProof)
        assertNotNull("哈希承诺生成", proof.hashCommitmentHex)

        val proofFail = ZkAssetProofVerifier.generateProof(10000.0, 30000.0, "salt123")
        assertFalse("低于门槛应判定无效", proofFail.isValidProof)
    }

    @Test
    fun 物理常识因果推理测试() {
        val dependency = PhysicalCausalReasoner.inferDependencies("大功率电钻")
        assertNotNull(dependency)
        assertTrue(dependency!!.requiredAccessories.contains("防护眼镜"))
        assertTrue(dependency.safetyNotice.contains("护目镜"))
    }
}