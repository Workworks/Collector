package com.kfaino.diapertracker

/** 一条记录：某分类（尺码）下某品牌，增加/减少若干件，记录单价和备注 */
data class Entry(
    val category: String,   // 尺码: NB, S, M, L, XL, XXL, XXXL 等
    val brand: String,      // 品牌: 好奇小森林, 帮宝适...
    val qty: Int,           // 数量
    val price: Double,      // 单价（元/件）
    val ts: Long,           // 时间戳
    val isIn: Boolean,      // true=增加, false=减少
    val notes: String = ""  // 备注
)

/** 某分类下的品牌汇总 */
data class BrandSummary(
    val name: String,
    val count: Int,       // 当前库存（增加-减少）
    val amount: Double,   // 累计花费（仅增加）
    val avgPrice: Double  // 平均单价
)

/** 分类汇总（尺码 → 品牌列表） */
data class CategoryGroup(
    val name: String,           // 尺码名: NB, S, M...
    val brands: List<BrandSummary>,
    val totalCount: Int,
    val totalAmount: Double
)

/** 月度统计 */
data class MonthStat(
    val year: Int,
    val month: Int,      // 1-12
    val addCount: Int,   // 增加件数
    val addAmount: Double, // 增加金额
    val reduceCount: Int,  // 减少件数
    val entryCount: Int    // 总笔数
)
