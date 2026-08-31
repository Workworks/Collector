package com.kfaino.diapertracker

/**
 * 📦 智能空间容积率与 3D Bin Packing 装箱推演器 (Bin Packing 3D Optimizer)
 */
object BinPacking3dOptimizer {

    data class BoxDimensions(val lengthCm: Double, val widthCm: Double, val heightCm: Double) {
        val volumeCm3: Double get() = lengthCm * widthCm * heightCm
    }

    data class PackingAnalysis(
        val boxVolumeLiters: Double,
        val usedVolumeLiters: Double,
        val remainingCapacityPercent: Int,
        val isOverloaded: Boolean,
        val suggestion: String
    )

    fun analyzeBox(boxDim: BoxDimensions, currentItemCount: Int, avgItemVolumeLiters: Double = 2.5): PackingAnalysis {
        val totalBoxLiters = boxDim.volumeCm3 / 1000.0
        val usedLiters = currentItemCount * avgItemVolumeLiters
        val remainLiters = (totalBoxLiters - usedLiters).coerceAtLeast(0.0)
        val remainPct = ((remainLiters / totalBoxLiters.coerceAtLeast(1.0)) * 100).toInt()
        val overloaded = usedLiters > totalBoxLiters

        val sugg = when {
            overloaded -> "⚠️ 该收纳箱容积已超载，建议分流至其他备用箱！"
            remainPct < 20 -> "📦 空间已接近满载，仅可放入小件扁平物品。"
            else -> "✨ 剩余空间充裕，容积率表现优秀。"
        }

        return PackingAnalysis(totalBoxLiters, usedLiters, remainPct, overloaded, sugg)
    }
}