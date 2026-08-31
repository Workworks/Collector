package com.kfaino.diapertracker

import java.util.UUID

/**
 * 🌲 Wi-Fi Direct / BLE Mesh 自组网野外极端无网联络协议 (Off-Grid Mesh Net Protocol)
 */
object OffGridMeshNetProtocol {

    data class MeshMessage(
        val messageId: String,
        val senderNodeAlias: String,
        val hopCount: Int,
        val emergencySupplyPayload: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    fun createMeshBroadcast(sender: String, payload: String): MeshMessage {
        return MeshMessage(
            messageId = "mesh_" + UUID.randomUUID().toString().take(8),
            senderNodeAlias = sender,
            hopCount = 0,
            emergencySupplyPayload = payload
        )
    }

    fun relayMessage(original: MeshMessage): MeshMessage? {
        if (original.hopCount >= 5) return null // 防洪泛 TTL
        return original.copy(hopCount = original.hopCount + 1)
    }
}