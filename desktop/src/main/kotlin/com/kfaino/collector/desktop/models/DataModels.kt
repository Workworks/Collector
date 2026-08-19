package com.kfaino.collector.desktop.models

import java.util.Calendar
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
