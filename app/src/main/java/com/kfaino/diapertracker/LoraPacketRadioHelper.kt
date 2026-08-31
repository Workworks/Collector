package com.kfaino.diapertracker

import java.util.UUID

/**
 * 📻 LoRa / 业余无线电超远距离无网资产广播助手 (LoRa Packet Radio Helper)
 */
object LoraPacketRadioHelper {

    data class LoraPacket(
        val packetId: String,
        val frequencyMhz: Double,
        val spreadingFactor: Int, // SF7 ~ SF12
        val payloadRawHex: String,
        val signalSnrDb: Double
    )

    fun encodePacket(assetPayloadJson: String, freqMhz: Double = 433.175): LoraPacket {
        val hex = assetPayloadJson.toByteArray().joinToString("") { String.format("%02x", it) }
        return LoraPacket(
            packetId = "lora_" + UUID.randomUUID().toString().take(8),
            frequencyMhz = freqMhz,
            spreadingFactor = 10,
            payloadRawHex = hex,
            signalSnrDb = 8.5
        )
    }
}