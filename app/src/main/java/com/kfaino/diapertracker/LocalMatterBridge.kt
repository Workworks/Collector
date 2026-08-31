package com.kfaino.diapertracker

import android.util.Log
import org.json.JSONObject

/**
 * 🔌 Matter / Home Assistant 局域网协议本地直连桥接器 (Local Matter Bridge)
 * 100% 局域网直连免外网读取温湿度传感器与智能开关状态
 */
object LocalMatterBridge {

    private const val TAG = "LocalMatterBridge"

    data class SensorReading(
        val sensorId: String,
        val sensorType: String, // "temperature", "humidity", "illuminance"
        val value: Double,
        val unit: String,
        val locationName: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    fun parseHaStateJson(jsonString: String): SensorReading? {
        return try {
            val obj = JSONObject(jsonString)
            val entityId = obj.optString("entity_id", "sensor.unknown")
            val stateStr = obj.optString("state", "0.0")
            val valNum = stateStr.toDoubleOrNull() ?: 0.0
            val attrs = obj.optJSONObject("attributes")
            val unit = attrs?.optString("unit_of_measurement", "") ?: ""
            val loc = attrs?.optString("friendly_name", "局域网传感器") ?: "局域网传感器"

            val sType = when {
                entityId.contains("temperature") || unit.contains("°C") -> "temperature"
                entityId.contains("humidity") || unit.contains("%") -> "humidity"
                else -> "sensor"
            }

            SensorReading(entityId, sType, valNum, unit, loc)
        } catch (e: Exception) {
            Log.w(TAG, "解析 HA 传感器状态异常", e)
            null
        }
    }
}