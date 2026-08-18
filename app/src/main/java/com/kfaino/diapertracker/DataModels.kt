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

/** 一条出入库/记账记录（集成空间位置、移动历史、重要订阅提醒） */
data class Entry(
    val id: String = UUID.randomUUID().toString(),
    val category: String,       // 分类: 数码, 日用品, 零食, 耗材等
    val brand: String,          // 品牌/物品名称
    val qty: Int,               // 数量
    val price: Double = 0.0,    // 单价（元/单位）
    val ts: Long = System.currentTimeMillis(), // 时间戳
    val isIn: Boolean = true,   // true=增加(入库/购入), false=减少(出库/消耗)
    val notes: String = "",     // 备注
    val unit: String = "片",    // 数量单位: 片, 件, 包, 个, 箱, 瓶等

    // 空间与位置体系 (可填可不填)
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
    val lastCheckedAt: Long = 0L                    // 上次核对位置确认时间戳
)

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

/** 家庭/空间模型 (支持多个家与平面图) */
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
    val unit: String = "片",    // 常用单位
    val location: String = "",  // 当前最新放置位置
    val houseName: String = "", // 所属空间
    val isImportant: Boolean = false
)

/** 分类汇总 */
data class CategoryGroup(
    val name: String,           // 分类名
    val brands: List<BrandSummary>,
    val totalCount: Int,
    val totalAmount: Double,
    val unit: String = "片"
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
