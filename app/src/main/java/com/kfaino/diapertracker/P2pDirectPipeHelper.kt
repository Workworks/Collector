package com.kfaino.diapertracker

import android.util.Log

/**
 * ⚡ 跨端即时双向剪贴板与大文件局域网直连管道 (P2P Direct Pipe)
 */
object P2pDirectPipeHelper {

    private const val TAG = "P2pDirectPipeHelper"

    data class DirectPipePacket(
        val packetType: String,
        val payload: String,
        val senderDevice: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    fun wrapClipboardPacket(text: String, deviceName: String): DirectPipePacket {
        return DirectPipePacket(
            packetType = "clipboard_text",
            payload = text,
            senderDevice = deviceName
        )
    }

    fun wrapFileMetadataPacket(filename: String, fileSize: Long, deviceName: String): DirectPipePacket {
        return DirectPipePacket(
            packetType = "file_metadata",
            payload = filename + ":" + fileSize,
            senderDevice = deviceName
        )
    }
}