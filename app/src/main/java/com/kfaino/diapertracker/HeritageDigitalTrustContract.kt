package com.kfaino.diapertracker

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 📜 家庭遗产与世代传承资产契约数字信托协议 (Heritage Digital Trust Contract)
 */
object HeritageDigitalTrustContract {

    data class TrustContract(
        val contractId: String,
        val assetName: String,
        val designatedBeneficiary: String,
        val lockUntilTimestamp: Long,
        val formattedContractText: String
    )

    fun createContract(assetName: String, beneficiary: String, yearsLock: Int): TrustContract {
        val unlockMs = System.currentTimeMillis() + yearsLock * 365L * 86400000L
        val unlockDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(unlockMs))
        val cid = "trust_" + java.util.UUID.randomUUID().toString().take(8)

        val text = "【家庭世代传承数字信托协议】\n" +
                "• 信托标的资产：" + assetName + "\n" +
                "• 指定继承/保管人：" + beneficiary + "\n" +
                "• 约定解锁期：" + unlockDateStr + " (" + yearsLock + " 年后)\n" +
                "• 契约存证：100% 离线私有沙盒加密，抗审查零泄露。"

        return TrustContract(cid, assetName, beneficiary, unlockMs, text)
    }
}