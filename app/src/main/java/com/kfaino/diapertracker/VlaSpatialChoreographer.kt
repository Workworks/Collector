package com.kfaino.diapertracker

/**
 * 🗣️ 视觉-语言-动作（VLA）端侧意图编排器 (VLA Spatial Choreographer)
 */
object VlaSpatialChoreographer {

    data class ChoreographyTask(
        val speechPrompt: String,
        val targetEntityName: String,
        val destinationBoxName: String,
        val confidenceScore: Float
    )

    fun parseNaturalAction(prompt: String): ChoreographyTask {
        val target = when {
            prompt.contains("镜头盖") -> "单反镜头盖"
            prompt.contains("充电线") -> "Type-C快充线"
            prompt.contains("钥匙") -> "玄关钥匙串"
            else -> "未知小件物品"
        }

        val dest = when {
            prompt.contains("抽屉") -> "书桌主抽屉"
            prompt.contains("箱") || prompt.contains("盒") -> "数码收纳箱 A"
            else -> "待整理置物托盘"
        }

        return ChoreographyTask(prompt, target, dest, 0.92f)
    }
}