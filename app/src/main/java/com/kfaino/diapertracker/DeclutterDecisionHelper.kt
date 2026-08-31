package com.kfaino.diapertracker

/**
 * 🧘 智能断舍离（KonMari / 极简主义）回血决策系统 (Declutter Decision Helper)
 * 综合闲置时长、拥有天数、日均成本与二手残值，一键推荐二手回血与环保报废
 */
object DeclutterDecisionHelper {

    enum class DeclutterAction {
        RESALE_IDLE,    // 挂闲鱼/转转回血（残值尚高且闲置）
        DONATE_FRIENDS, // 赠送亲友
        RECYCLE_SCRAP,  // 环保报废（残值归零/损坏）
        KEEP_IN_USE     // 继续保留使用
    }

    data class DeclutterSuggestion(
        val assetId: String,
        val assetBrand: String,
        val suggestedAction: DeclutterAction,
        val estimatedResalePrice: Double,
        val reason: String
    )

    fun evaluateAssets(store: DataStore): List<DeclutterSuggestion> {
        val entries = store.loadAll().filter { it.isIn && !it.isRetired }
        val results = mutableListOf<DeclutterSuggestion>()

        val now = System.currentTimeMillis()
        for (e in entries) {
            val daysOwned = e.getDaysOwned()
            val currentVal = DynamicDepreciationEngine.calculateCurrentValuation(e.price, e.purchaseDate, e.category)
            val residualRate = if (e.price > 0) currentVal / e.price else 0.0

            if (daysOwned > 180 && !e.isImportant) {
                if (currentVal >= 100.0 && residualRate >= 0.25) {
                    results.add(
                        DeclutterSuggestion(
                            e.id, e.brand, DeclutterAction.RESALE_IDLE, currentVal,
                            "已持有  天，目前二手残值约 ¥，建议尽快挂闲鱼回血防贬值"
                        )
                    )
                } else if (residualRate < 0.15 && currentVal < 50.0) {
                    results.add(
                        DeclutterSuggestion(
                            e.id, e.brand, DeclutterAction.RECYCLE_SCRAP, 0.0,
                            "日均成本已充分分摊，残值趋近于零，建议断舍离腾出收纳空间"
                        )
                    )
                }
            }
        }
        return results.sortedByDescending { it.estimatedResalePrice }
    }
}
