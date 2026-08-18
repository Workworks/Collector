package com.kfaino.diapertracker

/** 一条出入库/记账记录 */
data class Entry(
    val category: String,       // 分类: 数码, 日用品, 纸尿裤, 耗材等
    val brand: String,          // 品牌/物品名称
    val qty: Int,               // 数量
    val price: Double,          // 单价（元/单位）
    val ts: Long,               // 时间戳
    val isIn: Boolean = true,   // true=增加(入库/购入), false=减少(出库/消耗)
    val notes: String = "",     // 备注
    val unit: String = "片"      // 数量单位: 片, 件, 包, 个, 箱, 瓶等
)

/** 某分类下的品牌/物品汇总 */
data class BrandSummary(
    val name: String,
    val count: Int,             // 当前库存（增加-减少）
    val amount: Double,         // 累计花费（仅增加）
    val avgPrice: Double,       // 平均单价
    val unit: String = "片"      // 常用单位
)

/** 分类汇总 */
data class CategoryGroup(
    val name: String,           // 分类名
    val brands: List<BrandSummary>,
    val totalCount: Int,
    val totalAmount: Double,
    val unit: String = "片"
)

/** 月度统计 */
data class MonthStat(
    val year: Int,
    val month: Int,            // 1-12
    val addCount: Int,         // 增加数量
    val addAmount: Double,     // 增加金额
    val reduceCount: Int,      // 减少数量
    val entryCount: Int        // 总笔数
)
