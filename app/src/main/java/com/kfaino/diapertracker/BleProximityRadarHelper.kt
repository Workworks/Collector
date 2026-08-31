package com.kfaino.diapertracker

import android.util.Log
import kotlin.math.pow

/**
 * 📡 蓝牙 BLE 智能寻物标签雷达测距助手 (BLE Proximity Radar Helper)
 * 将 iBeacon / BLE 广播信号强度 (RSSI) 转换为物理测距与雷达波形
 */
object BleProximityRadarHelper {

    private const val TAG = "BleRadarHelper"

    data class BleDeviceSignal(
        val deviceAddress: String,
        val deviceName: String,
        val rssi: Int,
        val distanceMeters: Double,
        val proximityLevel: ProximityLevel
    )

    enum class ProximityLevel {
        IMMEDIATE, // 0~0.5 米（触手可及）
        NEAR,      // 0.5~2 米（同房间内）
        FAR,       // 2~8 米（同房屋跨房间）
        UNKNOWN
    }

    fun calculateDistance(rssi: Int, txPower: Int = -59): Double {
        if (rssi == 0) return -1.0
        val ratio = rssi * 1.0 / txPower
        return if (ratio < 1.0) {
            ratio.pow(10.0)
        } else {
            0.89976 * ratio.pow(7.7095) + 0.111
        }
    }

    fun parseSignal(address: String, name: String, rssi: Int): BleDeviceSignal {
        val dist = calculateDistance(rssi)
        val level = when {
            dist in 0.0..0.6 -> ProximityLevel.IMMEDIATE
            dist in 0.6..2.5 -> ProximityLevel.NEAR
            dist > 2.5 -> ProximityLevel.FAR
            else -> ProximityLevel.UNKNOWN
        }
        return BleDeviceSignal(address, name.ifBlank { "BLE 寻物标签" }, rssi, dist, level)
    }
}
