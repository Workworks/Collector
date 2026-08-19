package com.kfaino.diapertracker

import java.util.UUID

/** 物品挪动与位置变迁历史记录 */
data class LocationMovement(
    val location: String,                           // 放置地点名称 (如 "主卧衣柜二层")
    val houseName: String = "我的家",                // 所属空间名称 (如 "🏠 自己的家")
    val roomName: String = "主卧",                   // 所属房间名称 (如 "主卧")
    val pinX: Float = -1f,                          // 平面图相对坐标 X (0.0 - 1.0)
    val pinY: Float = -1f,                          // 平面图相对坐标 Y (0.0 - 1.0)
    val movedAt: Long = System.currentTimeMillis(), // 移动时间戳
    val note: String = ""                           // 挪动备注 (如 "从客厅茶几移入")
)

/** 一条出入库/记账记录（集成折旧拥有天数、在役/退役待办归置、订阅型资产体系） */
data class Entry(
    val id: String = UUID.randomUUID().toString(),
    val category: String = "通用",       // 分类: 数码, 日用品, 零食, 耗材等
    val brand: String = "物品",          // 品牌/物品名称
    val qty: Int = 1,           // 数量
    val price: Double = 0.0,    // 购买原价 / 单价（元/单位）
    val currentValuation: Double = 0.0, // 当前二手/折旧估值 (元，若为0则默认按原价)
    val purchaseDate: Long = System.currentTimeMillis(), // 购入日期 (用于计算拥有天数和日均成本)
    val ts: Long = System.currentTimeMillis(),           // 记录时间戳
    val isIn: Boolean = true,   // true=增加(入库/购入), false=减少(出库/消耗)
    val notes: String = "",     // 备注
    val unit: String = "件",    // 数量单位: 件, 台, 片, 包, 个, 箱, 瓶等

    // 空间与位置体系 (选填)
    val location: String = "",                      // 当前放置位置 (如 "主卧衣柜二层")
    val houseId: String = "default_house",          // 所属空间 ID
    val houseName: String = "我的家",                // 所属空间名称
    val roomName: String = "",                      // 所属房间名称 (如 "主卧")
    val pinX: Float = -1f,                          // 平面图标记 X (0.0 ~ 1.0)
    val pinY: Float = -1f,                          // 平面图标记 Y (0.0 ~ 1.0)
    val locationHistory: List<LocationMovement> = emptyList(), // 历史移动轨迹

    // 重要物品防丢与订阅提醒体系
    val isImportant: Boolean = false,               // 是否为重要贵重物品 (钥匙/身份证/房产证/贵重首饰等)
    val reminderEnabled: Boolean = false,           // 是否开启定期核对订阅提醒
    val reminderIntervalDays: Int = 1,              // 提醒间隔天数 (1=每天, 3=每3天, 7=每周, 15=半月, 30=每月)
    val reminderTime: String = "09:00",             // 每日提醒时间
    val lastCheckedAt: Long = 0L,                   // 上次核对位置确认时间戳

    // 3. 物品在役 / 退役与待办归置系统
    val isRetired: Boolean = false,                 // false=在役, true=已退役 (如闲鱼代售、封存)
    val retiredAt: Long = 0L,                       // 退役时间戳
    val retiredAction: String = "",                 // 待办归置渠道 (如 "挂闲鱼代售", "挂转转代售", "赠送亲友", "封箱入库", "环保回收", "报废")
    val retiredSoldPrice: Double = 0.0,             // 二手出掉回血金额 (如有)
    val retiredNote: String = "",                   // 退役归置备注

    // 2. 按期订阅型资产体系 (如 iCloud, ChatGPT, Netflix, 宽带, 健身年卡)
    val isSubscription: Boolean = false,            // 是否为订阅型资产
    val subCycle: String = "按月",                  // 订阅周期: "按月", "按年", "按季", "按周"
    val subNextBillingDate: Long = 0L,              // 下次扣费日期
    val subAutoRenew: Boolean = true,               // 是否自动续费

    // 4. 纠正需求：细分物品类型 (折旧资产, 保质期物品, 长期使用耐用品, 日常消耗品)
    val assetType: String = "consumable",           // "depreciating" (折旧资产), "expiring" (保质期物品), "durable" (长期使用), "consumable" (日常消耗品)
    val manufactureDate: Long = 0L,                 // 生产日期
    val expiryDate: Long = 0L,                      // 到期日期

    // 5. 实物照片与购买发票/保修卡留存 (沙盒私有路径)
    val photoPath: String = "",                     // 物品实物照片文件名/相对路径
    val receiptPath: String = ""                    // 购买发票/凭证/保修卡照片文件名/相对路径
) {
    /** 获取到期状态描述 */
    fun getExpiryStatusText(): String {
        if (expiryDate <= 0L) return ""
        val now = System.currentTimeMillis()
        val diffMs = expiryDate - now
        val days = (diffMs / (24L * 60 * 60 * 1000)).toInt()
        return if (days < 0) {
            "🔴 已过期 ${Math.abs(days)} 天"
        } else {
            "⏳ 剩 $days 天过期"
        }
    }

    /** 获取类型中文描述 */
    fun getAssetTypeDisplayName(): String {
        return when (assetType) {
            "depreciating" -> "折旧资产"
            "expiring" -> "保质期"
            "durable" -> "长期持有"
            else -> "消耗品"
        }
    }
    /** 计算拥有天数 */
    fun getDaysOwned(): Int {
        val endTime = if (isRetired && retiredAt > 0) retiredAt else System.currentTimeMillis()
        val days = ((endTime - purchaseDate) / (24L * 60 * 60 * 1000)).toInt()
        return days.coerceAtLeast(1)
    }

    /** 计算日均使用成本 (元/天) */
    fun getDailyCost(): Double {
        val days = getDaysOwned().toDouble()
        val totalCost = price * qty
        return if (isRetired && retiredSoldPrice > 0) {
            (totalCost - retiredSoldPrice).coerceAtLeast(0.0) / days
        } else if (currentValuation > 0 && currentValuation < totalCost) {
            (totalCost - currentValuation).coerceAtLeast(0.0) / days
        } else {
            totalCost / days
        }
    }
}

/** 空间房间模型 */
data class HouseRoom(
    val id: String = UUID.randomUUID().toString(),
    val name: String,             // 房间名称 (如 "客厅", "主卧", "玄关", "储物间")
    val icon: String = "🛋️",
    val colorHex: String = "#10B981",
    val xPct: Float = 0.1f,       // 房间左上角 X (0.0 ~ 1.0)
    val yPct: Float = 0.1f,       // 房间左上角 Y (0.0 ~ 1.0)
    val widthPct: Float = 0.35f,  // 房间宽 (0.0 ~ 1.0)
    val heightPct: Float = 0.35f  // 房间高 (0.0 ~ 1.0)
)

/** 家庭/空间模型 */
data class HouseSpace(
    val id: String = UUID.randomUUID().toString(),
    val name: String,             // 空间名称 (如 "🏠 自己的家", "🏡 父母家", "🏢 办公室", "🚗 汽车")
    val type: String = "住宅",     // 空间类型
    val rooms: List<HouseRoom> = defaultRooms(),
    val isDefault: Boolean = false
) {
    companion object {
        fun defaultRooms(): List<HouseRoom> = listOf(
            HouseRoom(name = "玄关", icon = "🚪", colorHex = "#3B82F6", xPct = 0.05f, yPct = 0.05f, widthPct = 0.28f, heightPct = 0.25f),
            HouseRoom(name = "客厅", icon = "🛋️", colorHex = "#10B981", xPct = 0.36f, yPct = 0.05f, widthPct = 0.58f, heightPct = 0.42f),
            HouseRoom(name = "厨房", icon = "🍳", colorHex = "#F59E0B", xPct = 0.05f, yPct = 0.33f, widthPct = 0.28f, heightPct = 0.30f),
            HouseRoom(name = "主卧", icon = "🛏️", colorHex = "#8B5CF6", xPct = 0.05f, yPct = 0.66f, widthPct = 0.43f, heightPct = 0.29f),
            HouseRoom(name = "次卧", icon = "🛌", colorHex = "#EC4899", xPct = 0.51f, yPct = 0.50f, widthPct = 0.43f, heightPct = 0.22f),
            HouseRoom(name = "储物间", icon = "📦", colorHex = "#06B6D4", xPct = 0.51f, yPct = 0.75f, widthPct = 0.43f, heightPct = 0.20f)
        )
    }
}

/** 某分类下的品牌/物品汇总 */
data class BrandSummary(
    val name: String,
    val count: Int,             // 当前库存（增加-减少）
    val amount: Double,         // 累计花费（仅增加）
    val avgPrice: Double,       // 平均单价
    val unit: String = "件",    // 常用单位
    val location: String = "",  // 当前最新放置位置
    val houseName: String = "", // 所属空间
    val isImportant: Boolean = false,
    val isRetired: Boolean = false,
    val retiredAction: String = "",
    val daysOwned: Int = 1,
    val dailyCost: Double = 0.0,
    val isSubscription: Boolean = false,
    val subCycle: String = "按月",
    val subNextBillingDate: Long = 0L,
    val currentValuation: Double = 0.0
)

/** 分类汇总 */
data class CategoryGroup(
    val name: String,           // 分类名
    val brands: List<BrandSummary>,
    val totalCount: Int,
    val totalAmount: Double,
    val unit: String = "件"
)

/** 月度统计模型（账本报表） */
data class MonthStat(
    val year: Int,
    val month: Int,            // 1-12
    val addCount: Int,         // 增加数量
    val addAmount: Double,     // 增加金额 (本月总投入)
    val reduceCount: Int,      // 减少数量 (本月总消耗)
    val entryCount: Int,       // 总笔数
    val topItems: List<String> = emptyList() // 当月主要记账项目
)
