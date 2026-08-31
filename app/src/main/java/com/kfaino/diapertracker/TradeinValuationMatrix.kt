package com.kfaino.diapertracker

/**
 * 📱 电子产品官方 Trade-in 以旧换新行情比价矩阵 (Trade-in Valuation Matrix)
 */
object TradeinValuationMatrix {

    data class TradeinQuote(
        val deviceName: String,
        val officialTradeinPrice: Double,
        val secondHandMarketPrice: Double,
        val recommendedChannel: String // "官方以旧换新" vs "闲鱼个人转卖"
    )

    fun estimateBestChannel(productName: String, currentMarketValue: Double): TradeinQuote {
        // 官方以旧换新通常约为市场价的 75%~85%，但无扯皮风险
        val officialPrice = currentMarketValue * 0.80
        val channel = if (currentMarketValue - officialPrice > 400.0) "闲鱼个人转卖(溢价更高)" else "官方以旧换新(省心免扯皮)"

        return TradeinQuote(productName, officialPrice, currentMarketValue, channel)
    }
}