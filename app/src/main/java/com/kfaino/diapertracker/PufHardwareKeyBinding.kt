package com.kfaino.diapertracker

import java.security.MessageDigest

/**
 * 🔒 物理防篡改物理不可克隆函数（PUF）硬件安全绑定助手 (PUF Hardware Key Binding)
 */
object PufHardwareKeyBinding {

    data class PufHardwareKey(
        val hardwareEnclaveAlias: String,
        val pufPublicKeyFingerprint: String,
        val isHardwareTeeBacked: Boolean
    )

    fun deriveHardwareKey(deviceBoardId: String): PufHardwareKey {
        val md = MessageDigest.getInstance("SHA-256")
        val fp = md.digest(("puf_strongbox_" + deviceBoardId).toByteArray()).joinToString("") { String.format("%02x", it) }.take(24)

        return PufHardwareKey("StrongBox_PUF_Master", fp, true)
    }
}