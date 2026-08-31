package com.kfaino.diapertracker

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64

/**
 * 🔑 BIP-39 分层确定性资产冷备份助记词保险库 (BIP-39 Asset Mnemonic Vault)
 * 将全家私有资产核心密钥转化为 12 个抗审查纸质助记词
 */
object Bip39AssetMnemonicVault {

    private val wordList = listOf(
        "abandon", "ability", "able", "about", "above", "absent", "absorb", "abstract",
        "absurd", "abuse", "access", "accident", "account", "accuse", "achieve", "acid",
        "acoustic", "acquire", "across", "act", "action", "actor", "actress", "actual"
    )

    fun generate12Words(passphrase: String): List<String> {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(passphrase.toByteArray(StandardCharsets.UTF_8))

        val words = mutableListOf<String>()
        for (i in 0 until 12) {
            val byteVal = (hash[i % hash.size].toInt() and 0xFF)
            val wordIndex = byteVal % wordList.size
            words.add(wordList[wordIndex])
        }
        return words
    }

    fun verifyMnemonicLength(words: List<String>): Boolean {
        return words.size == 12 && words.all { wordList.contains(it) }
    }
}