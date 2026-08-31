package com.kfaino.diapertracker

/**
 * 💰 家庭重资产净值沉没成本与未来现金流推演模拟器 (Asset Cashflow Simulator)
 */
object AssetCashflowSimulator {

    data class YearlyNetWorthProjection(
        val yearOffset: Int,
        val projectedTotalNetWorth: Double,
        val depreciationLossThisYear: Double
    )

    fun simulateFuture5Years(store: DataStore): List<YearlyNetWorthProjection> {
        val entries = store.loadAll().filter { it.isIn && !it.isRetired }
        val currentTotal = entries.sumOf { it.price * it.qty }

        val list = mutableListOf<YearlyNetWorthProjection>()
        var prevWorth = currentTotal

        for (year in 1..5) {
            var yearWorth = 0.0
            for (e in entries) {
                val decayRate = DynamicDepreciationEngine.getCategoryCurve(e.category).annualDecayRate
                val decayedPrice = e.price * Math.pow(1.0 - decayRate * 0.7, year.toDouble()).coerceAtLeast(0.1)
                yearWorth += decayedPrice * e.qty
            }
            val loss = prevWorth - yearWorth
            list.add(YearlyNetWorthProjection(year, yearWorth, loss.coerceAtLeast(0.0)))
            prevWorth = yearWorth
        }

        return list
    }
}
