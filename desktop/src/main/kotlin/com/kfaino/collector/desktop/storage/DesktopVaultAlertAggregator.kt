package com.kfaino.collector.desktop.storage

import com.kfaino.collector.desktop.models.*

/**
 * 桌面端 12 馆综合时效预警聚合器
 */
data class DesktopVaultAlertItem(
    val emoji: String,
    val vaultName: String,
    val label: String,
    val urgencyDays: Int,
    val isExpired: Boolean
)

object DesktopVaultAlertAggregator {

    fun aggregate(store: DesktopDataStore): List<DesktopVaultAlertItem> {
        val alerts = mutableListOf<DesktopVaultAlertItem>()
        val now = System.currentTimeMillis()

        // 1. 🎟️ 卡券
        store.getVouchers().forEach { v ->
            if (!v.isUsed && v.expiryDate > 0L) {
                val diffDays = ((v.expiryDate - now) / (1000L * 3600 * 24)).toInt()
                if (diffDays < 0) {
                    alerts.add(DesktopVaultAlertItem("🎟️", "时效卡券", "${v.title} 已过期", diffDays, true))
                } else if (diffDays <= 3) {
                    alerts.add(DesktopVaultAlertItem("🎟️", "时效卡券", "${v.title} ${diffDays}天后到期", diffDays, false))
                }
            }
        }

        // 2. 💊 药箱
        store.getMedicines().forEach { m ->
            val effExp = m.getEffectiveExpiryDate()
            if (effExp > 0L) {
                val diffDays = ((effExp - now) / (1000L * 3600 * 24)).toInt()
                if (diffDays < 0) {
                    alerts.add(DesktopVaultAlertItem("💊", "家庭药箱", "${m.name} 已过期！严禁服用", diffDays, true))
                } else if (diffDays <= 30) {
                    alerts.add(DesktopVaultAlertItem("💊", "家庭药箱", "${m.name} 还有${diffDays}天到期", diffDays, false))
                }
            }
        }

        // 3. 🥦 食材
        store.getFoods().forEach { f ->
            if (f.expDate > 0L) {
                val diffDays = ((f.expDate - now) / (1000L * 3600 * 24)).toInt()
                if (diffDays < 0) {
                    alerts.add(DesktopVaultAlertItem("🥦", "食材鲜度", "${f.name} 已过保鲜期", diffDays, true))
                } else if (diffDays <= 3) {
                    alerts.add(DesktopVaultAlertItem("🥦", "食材鲜度", "${f.name} 剩${diffDays}天保鲜", diffDays, false))
                }
            }
        }

        // 4. 🚨 应急
        store.getEmergencyItems().forEach { em ->
            if (em.expiryDate > 0L) {
                val diffDays = ((em.expiryDate - now) / (1000L * 3600 * 24)).toInt()
                if (diffDays < 0) {
                    alerts.add(DesktopVaultAlertItem("🚨", "应急防灾", "${em.name} 已过期失效", diffDays, true))
                } else if (diffDays <= 30) {
                    alerts.add(DesktopVaultAlertItem("🚨", "应急防灾", "${em.name} 还有${diffDays}天失效", diffDays, false))
                }
            }
        }

        // 5. 🔧 工具
        store.getToolRecords().forEach { t ->
            if (t.isMaintenanceDue()) {
                alerts.add(DesktopVaultAlertItem("🔧", "工具维保", "${t.name} 维保已逾期", -1, true))
            }
        }

        // 6. 🪴 绿植
        store.getPlantRecords().forEach { p ->
            if (p.isWateringDue()) {
                alerts.add(DesktopVaultAlertItem("🪴", "绿植养护", "${p.name} 需浇水", 0, false))
            }
        }

        // 7. 🐾 萌宠
        store.getPetRecords().forEach { pet ->
            if (pet.isDewormDue()) {
                alerts.add(DesktopVaultAlertItem("🐾", "萌宠健康", "${pet.name} 驱虫排期已到", 0, false))
            }
        }

        // 8. 📚 藏书
        store.getBookRecords().forEach { b ->
            if (b.isLent()) {
                val lentDays = ((now - b.lentDate) / (1000L * 3600 * 24)).toInt()
                if (lentDays > 30) {
                    alerts.add(DesktopVaultAlertItem("📚", "书房藏书", "《${b.title}》已借给${b.borrowerName}${lentDays}天", lentDays, false))
                }
            }
        }

        // 9. 🍷 茶窖
        store.getBeverageTeaRecords().forEach { bv ->
            if (bv.isOpenedPreserveExpired()) {
                alerts.add(DesktopVaultAlertItem("🍷", "茶窖名酿", "${bv.name} 开瓶保鲜期已过", -1, true))
            }
        }

        return alerts.sortedBy { it.urgencyDays }
    }
}
