package com.kfaino.collector.desktop.models

import java.util.UUID

/** 周期订阅资产扣费周期 */
enum class SubCycle(val label: String, val days: Int) {
    MONTHLY("按月", 30),
    QUARTERLY("按季", 90),
    HALF_YEARLY("按半年", 182),
    YEARLY("按年", 365),
    WEEKLY("按周", 7)
}

/** 空间位置轨迹记录 */
data class LocationMovement(
    val location: String = "",
    val houseName: String = "我的家",
    val roomName: String = "",
    val pinX: Float = -1f,
    val pinY: Float = -1f,
    val movedAt: Long = System.currentTimeMillis(),
    val note: String = ""
)

/** 核心资产/出入库条目数据模型 (跨平台与移动端 100% 互通) */
data class Entry(
    val id: String = UUID.randomUUID().toString(),
    val brand: String = "",
    val category: String = "日用品",
    val price: Double = 0.0,
    val qty: Int = 1,
    val unit: String = "件",
    val location: String = "",
    val houseName: String = "我的家",
    val roomName: String = "",
    val pinX: Float = -1f,
    val pinY: Float = -1f,
    val locationHistory: List<LocationMovement> = emptyList(),
    val isIn: Boolean = true,
    val ts: Long = System.currentTimeMillis(),
    val notes: String = "",
    val photoPath: String = "",
    val receiptPath: String = "",
    val barcode: String = "",

    // 物品分类属性
    val isDepreciating: Boolean = true,
    val mfgDate: Long = 0L,
    val expDate: Long = 0L,
    val isDurable: Boolean = false,
    val durableStartDate: Long = 0L,
    val isConsumable: Boolean = false,

    // 折旧与估值
    val originalPrice: Double = price,
    val purchaseDate: Long = ts,
    val currentValuation: Double = price,
    val lastValuationDate: Long = ts,
    val targetResidualRate: Double = 0.1,
    val expectedLifeYears: Double = 3.0,

    // 退役与待办归置
    val isRetired: Boolean = false,
    val retiredDate: Long = 0L,
    val retiredAction: String = "",
    val retiredSoldPrice: Double = 0.0,
    val retiredNote: String = "",

    // 周期订阅资产
    val isSubscription: Boolean = false,
    val subPrice: Double = price,
    val subCycle: SubCycle = SubCycle.MONTHLY,
    val subStartDate: Long = ts,
    val subNextBillingDate: Long = ts + 30L * 24 * 60 * 60 * 1000,
    val subAutoRenew: Boolean = true,

    // 防丢与核对
    val isImportant: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderIntervalDays: Int = 1,
    val lastCheckedDate: Long = ts
) {
    fun getDaysOwned(): Int {
        val now = System.currentTimeMillis()
        val start = if (isSubscription) subStartDate else if (isDurable && durableStartDate > 0) durableStartDate else purchaseDate
        val diff = now - start
        return (diff / (1000L * 60 * 60 * 24)).toInt().coerceAtLeast(0)
    }

    fun getDailyCost(): Double {
        if (isSubscription) {
            val cycleDays = subCycle.days.toDouble().coerceAtLeast(1.0)
            return (subPrice * qty) / cycleDays
        }
        val days = getDaysOwned().coerceAtLeast(1)
        val totalCost = price * qty
        return totalCost / days.toDouble()
    }
}

/** 房间与收纳区域 */
data class Room(
    val name: String,
    val gridX: Int = 0,
    val gridY: Int = 0,
    val gridWidth: Int = 1,
    val gridHeight: Int = 1,
    val color: String = "#059669"
)

/** 房产与空间 */
data class House(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "我的家",
    val rooms: List<Room> = emptyList()
)

// =========================================================================
// 🏛️ 12 大第一性原理专业收纳馆数据模型
// =========================================================================

/** 01. 🎟️ 时效卡券记录模型 */
data class VoucherRecord(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val type: String = "coupon",
    val valueAmount: Double = 0.0,
    val minSpend: Double = 0.0,
    val remainingTimes: Int = 1,
    val totalTimes: Int = 1,
    val startDate: Long = System.currentTimeMillis(),
    val expiryDate: Long = 0L,
    val code: String = "",
    val platform: String = "",
    val photoPath: String = "",
    val notes: String = "",
    val isUsed: Boolean = false,
    val usedAt: Long = 0L
) {
    fun isExpired(): Boolean = !isUsed && expiryDate > 0L && System.currentTimeMillis() > expiryDate
    fun isExpiringSoon(): Boolean = !isUsed && expiryDate > 0L && (expiryDate - System.currentTimeMillis()) in 0..(3L * 24 * 3600 * 1000)
}

/** 02. 🪪 家庭多成员证照记录模型 */
data class IdentityDocRecord(
    val id: String = UUID.randomUUID().toString(),
    val nameOnDoc: String = "",
    val member: String = "本人",
    val docType: String = "id_card",
    val certNumber: String = "",
    val issueDate: Long = 0L,
    val expiryDate: Long = 0L,
    val frontPhotoPath: String = "",
    val backPhotoPath: String = "",
    val notes: String = "",
    val hasAnnualAudit: Boolean = false
) {
    fun isExpired(): Boolean = expiryDate > 0L && System.currentTimeMillis() > expiryDate
    fun isExpiringSoon(): Boolean = expiryDate > 0L && (expiryDate - System.currentTimeMillis()) in 0..(90L * 24 * 3600 * 1000)
}

/** 03. 💊 家庭智能健康药箱模型 */
data class MedicineRecord(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val category: String = "fever",
    val form: String = "片剂",
    val qty: Int = 1,
    val unit: String = "盒",
    val location: String = "家庭药箱",
    val dosage: String = "",
    val targetAudience: String = "全家通用",
    val expiryDate: Long = 0L,
    val isOpened: Boolean = false,
    val openedAt: Long = 0L,
    val openedValidityDays: Int = 0,
    val photoPath: String = "",
    val contraindications: String = ""
) {
    fun getEffectiveExpiryDate(): Long {
        if (isOpened && openedValidityDays > 0 && openedAt > 0L) {
            val openedExpire = openedAt + (openedValidityDays.toLong() * 24 * 60 * 60 * 1000)
            return if (expiryDate > 0L) Math.min(expiryDate, openedExpire) else openedExpire
        }
        return expiryDate
    }
    fun isExpired(): Boolean = getEffectiveExpiryDate() > 0L && System.currentTimeMillis() > getEffectiveExpiryDate()
    fun isExpiringSoon(): Boolean = getEffectiveExpiryDate() > 0L && (getEffectiveExpiryDate() - System.currentTimeMillis()) in 0..(30L * 24 * 3600 * 1000)
}

/** 04. 🥦 冰箱冷冻与食材生鲜鲜度模型 */
data class FoodRecord(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val zone: String = "freezer",
    val qty: Int = 1,
    val unit: String = "盒",
    val location: String = "冰箱",
    val mfgDate: Long = 0L,
    val expDate: Long = 0L,
    val isOpened: Boolean = false,
    val openedAt: Long = 0L,
    val photoPath: String = "",
    val notes: String = "",
    val storageMethod: String = "常规冷藏",
    val consumeTargetDate: Long = 0L
) {
    fun isExpired(): Boolean = expDate > 0L && System.currentTimeMillis() > expDate
    fun isExpiringSoon(): Boolean = expDate > 0L && (expDate - System.currentTimeMillis()) in 0..(3L * 24 * 3600 * 1000)
}

/** 05. 🏆 荣誉证书与考级勋章模型 */
data class HonorCredentialRecord(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val member: String = "本人",
    val category: String = "academic",
    val issuer: String = "",
    val certNumber: String = "",
    val certDate: Long = 0L,
    val hasAnnualAudit: Boolean = false,
    val nextAuditDate: Long = 0L,
    val photoPath: String = "",
    val notes: String = ""
)

/** 06. 👗 换季衣橱与服饰单品模型 */
data class WardrobeRecord(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val season: String = "all",
    val category: String = "top",
    val color: String = "",
    val material: String = "",
    val storageLocation: String = "主卧衣柜",
    val price: Double = 0.0,
    val photoPath: String = "",
    val wearCount: Int = 0,
    val lastWornAt: Long = 0L,
    val isSealed: Boolean = false,
    val sealedAt: Long = 0L,
    val notes: String = ""
)

/** 07. 🚨 家庭应急防灾物资模型 */
data class EmergencyItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val kitType: String = "earthquake",
    val category: String = "food",
    val qty: Int = 1,
    val unit: String = "件",
    val location: String = "玄关应急包",
    val expiryDate: Long = 0L,
    val lastTestedAt: Long = 0L,
    val photoPath: String = "",
    val notes: String = "",
    val importanceLevel: String = "must_have"
) {
    fun isExpired(): Boolean = expiryDate > 0L && System.currentTimeMillis() > expiryDate
    fun isExpiringSoon(): Boolean = expiryDate > 0L && (expiryDate - System.currentTimeMillis()) in 0..(30L * 24 * 3600 * 1000)
}

/** 08. 🔧 工具五金与设备维保模型 */
data class ToolMaintenanceRecord(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val spec: String = "",
    val category: String = "electric",
    val qty: Int = 1,
    val unit: String = "件",
    val location: String = "工具箱",
    val maintenanceIntervalDays: Int = 0,
    val lastMaintainedAt: Long = 0L,
    val photoPath: String = "",
    val notes: String = ""
) {
    fun isMaintenanceDue(): Boolean {
        if (maintenanceIntervalDays <= 0 || lastMaintainedAt <= 0L) return false
        val nextDue = lastMaintainedAt + (maintenanceIntervalDays.toLong() * 24 * 3600 * 1000)
        return System.currentTimeMillis() >= nextDue
    }
}

/** 09. 🪴 绿植花卉水肥养护模型 */
data class PlantCareRecord(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val species: String = "",
    val lightDemand: String = "scattered",
    val location: String = "客厅阳台",
    val waterIntervalDays: Int = 7,
    val lastWateredAt: Long = 0L,
    val fertilizeIntervalDays: Int = 30,
    val lastFertilizedAt: Long = 0L,
    val photoPath: String = "",
    val careTips: String = "",
    val plantedAt: Long = System.currentTimeMillis()
) {
    fun isWateringDue(): Boolean {
        if (waterIntervalDays <= 0 || lastWateredAt <= 0L) return false
        val nextDue = lastWateredAt + (waterIntervalDays.toLong() * 24 * 3600 * 1000)
        return System.currentTimeMillis() >= nextDue
    }
}

/** 10. 🐾 萌宠生活与健康档案模型 */
data class PetCareRecord(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val species: String = "cat",
    val breed: String = "",
    val weightKg: Double = 0.0,
    val chipNumber: String = "",
    val foodBrand: String = "",
    val foodStorageLocation: String = "",
    val dewormIntervalDays: Int = 30,
    val lastDewormedAt: Long = 0L,
    val vaccineIntervalDays: Int = 365,
    val lastVaccinatedAt: Long = 0L,
    val photoPath: String = "",
    val notes: String = ""
) {
    fun isDewormDue(): Boolean {
        if (dewormIntervalDays <= 0 || lastDewormedAt <= 0L) return false
        val nextDue = lastDewormedAt + (dewormIntervalDays.toLong() * 24 * 3600 * 1000)
        return System.currentTimeMillis() >= nextDue
    }
    fun isVaccineDue(): Boolean {
        if (vaccineIntervalDays <= 0 || lastVaccinatedAt <= 0L) return false
        val nextDue = lastVaccinatedAt + (vaccineIntervalDays.toLong() * 24 * 3600 * 1000)
        return System.currentTimeMillis() >= nextDue
    }
}

/** 11. 📚 书房藏书与阅读流转模型 */
data class BookRecord(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val author: String = "",
    val category: String = "general",
    val totalPages: Int = 0,
    val currentPages: Int = 0,
    val bookshelfLocation: String = "书房书架",
    val rating: Int = 5,
    val borrowerName: String = "",
    val lentDate: Long = 0L,
    val notes: String = "",
    val coverPhotoPath: String = ""
) {
    fun isLent(): Boolean = borrowerName.isNotBlank() && lentDate > 0L
    fun isReadFinished(): Boolean = totalPages > 0 && currentPages >= totalPages
}

/** 12. 🍷 家庭茶窖与名酿珍藏模型 */
data class BeverageTeaRecord(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val category: String = "liquor",
    val vintageYear: Int = 0,
    val optimalAgingYear: Int = 0,
    val qty: Int = 1,
    val unit: String = "瓶",
    val storageLocation: String = "恒温酒柜",
    val originRegion: String = "",
    val isOpened: Boolean = false,
    val openedAt: Long = 0L,
    val openedPreserveDays: Int = 0,
    val photoPath: String = "",
    val notes: String = ""
) {
    fun isOpenedPreserveExpired(): Boolean {
        if (isOpened && openedPreserveDays > 0 && openedAt > 0L) {
            val expire = openedAt + (openedPreserveDays.toLong() * 24 * 3600 * 1000)
            return System.currentTimeMillis() > expire
        }
        return false
    }
}

/** 13. 💡 灵感想法收纳舱模型 */
data class IdeaRecord(
    val id: String = UUID.randomUUID().toString(),
    val content: String = "",
    val tags: List<String> = emptyList(),
    val moodEmoji: String = "💡",
    val isPinned: Boolean = false,
    val colorHex: String = "#10B981",
    val linkedAssetIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getPreview(maxLen: Int = 40): String {
        val clean = content.replace("\n", " ").trim()
        return if (clean.length > maxLen) "${clean.take(maxLen)}..." else clean
    }
}

/** 14. 📰 智能剪藏与知识库模型 */
data class ClippingRecord(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val originalUrl: String = "",
    val sourcePlatform: String = "web",
    val fullMarkdown: String = "",
    val ocrRawText: String = "",
    val localImagePaths: List<String> = emptyList(),
    val summary: String = "",
    val tags: List<String> = emptyList(),
    val linkedAssetIds: List<String> = emptyList(),
    val capturedAt: Long = System.currentTimeMillis()
) {
    fun getSearchableContent(): String = "$title $summary $ocrRawText $fullMarkdown ${tags.joinToString(" ")}"
}
