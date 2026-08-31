package com.kfaino.diapertracker

/**
 * 🍎 生鲜果蔬乙烯释放与催熟阻隔矩阵 (Ethylene Ripening Matrix)
 */
object EthyleneRipeningMatrix {

    enum class EthyleneRole {
        HIGH_EMITTER,   // 高乙烯释放者（苹果、香蕉、猕猴桃、芒果）
        HIGH_SENSITIVE, // 极易被催熟腐烂（叶菜、西兰花、西瓜、浆果）
        NEUTRAL
    }

    data class ConflictWarning(
        val emitterName: String,
        val sensitiveName: String,
        val warningMessage: String
    )

    fun checkConflict(itemsInZone: List<String>): List<ConflictWarning> {
        val warnings = mutableListOf<ConflictWarning>()

        val emitters = itemsInZone.filter { it.contains("苹果") || it.contains("香蕉") || it.contains("猕猴桃") || it.contains("芒果") }
        val sensitives = itemsInZone.filter { it.contains("叶菜") || it.contains("西蓝花") || it.contains("西瓜") || it.contains("草莓") || it.contains("菠菜") }

        for (e in emitters) {
            for (s in sensitives) {
                warnings.add(
                    ConflictWarning(
                        e, s,
                        "【" + e + "】会释放大量乙烯气体，与【" + s + "】同箱存放将导致后者加速发黄腐烂，建议物理隔离！"
                    )
                )
            }
        }
        return warnings
    }
}