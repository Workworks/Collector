package com.kfaino.diapertracker

/**
 * 🦾 通用机器人抓取姿态与碰撞体积拓扑接口 (Robotic Grasp Topology API)
 */
object RoboticGraspTopologyApi {

    data class GraspAffordance(
        val itemId: String,
        val centerOfMassXyz: FloatArray,
        val graspWidthMm: Float,
        val maxGripForceNewton: Float,
        val isFragile: Boolean
    )

    fun calculateGrasp(itemName: String, weightGrams: Float): GraspAffordance {
        val fragile = itemName.contains("杯") || itemName.contains("镜头") || itemName.contains("手办")
        val force = if (fragile) 4.5f else 18.0f // 易碎品微力抓取

        return GraspAffordance(
            itemId = "grasp_" + itemName.hashCode(),
            centerOfMassXyz = floatArrayOf(0f, 0f, 0f),
            graspWidthMm = 65.0f,
            maxGripForceNewton = force,
            isFragile = fragile
        )
    }
}