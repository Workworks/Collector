package com.kfaino.diapertracker

/**
 * 🤖 自动化物品归位路径规划与 6-DoF 动作求解器 (Restocking Path Planner)
 */
object RestockingPathPlanner {

    data class TrajectoryWaypoint(
        val stepIndex: Int,
        val x: Float,
        val y: Float,
        val z: Float,
        val actionTag: String
    )

    fun planPath(sourceXyz: FloatArray, targetXyz: FloatArray): List<TrajectoryWaypoint> {
        val waypoints = mutableListOf<TrajectoryWaypoint>()
        waypoints.add(TrajectoryWaypoint(1, sourceXyz.getOrElse(0) { 0f }, sourceXyz.getOrElse(1) { 0f }, sourceXyz.getOrElse(2) { 0f }, "PICK_GRASP"))
        waypoints.add(TrajectoryWaypoint(2, sourceXyz.getOrElse(0) { 0f }, sourceXyz.getOrElse(1) { 0f } + 0.3f, sourceXyz.getOrElse(2) { 0f }, "LIFT_CLEARANCE"))
        waypoints.add(TrajectoryWaypoint(3, targetXyz.getOrElse(0) { 1f }, targetXyz.getOrElse(1) { 1f } + 0.2f, targetXyz.getOrElse(2) { 1f }, "NAVIGATE_TRANSIT"))
        waypoints.add(TrajectoryWaypoint(4, targetXyz.getOrElse(0) { 1f }, targetXyz.getOrElse(1) { 1f }, targetXyz.getOrElse(2) { 1f }, "PLACE_RELEASE"))

        return waypoints
    }
}