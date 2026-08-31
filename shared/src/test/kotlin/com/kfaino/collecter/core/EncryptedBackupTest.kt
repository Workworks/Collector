package com.kfaino.collecter.core

import org.junit.Assert.*
import org.junit.Test

class EncryptedBackupTest {
    @Test fun encryptionIsRandomizedAndAuthenticated() {
        val json="""{"entries":[{"id":"private","brand":"中文凭证"}],"saved_searches":[{"id":"query","query":"凭证"}]}"""
        val password="test-only-password-2026".toCharArray()
        val first=EncryptedBackup.encrypt(json,password);val second=EncryptedBackup.encrypt(json,password)
        assertFalse(first.contentEquals(second))
        assertEquals(json,EncryptedBackup.decrypt(first,password))
        assertFalse(String(first).contains("中文凭证"))
        assertThrows(Exception::class.java){EncryptedBackup.decrypt(first,"wrong-password-2026".toCharArray())}
        val tampered=first.clone();tampered[tampered.lastIndex]=(tampered.last().toInt() xor 1).toByte()
        assertThrows(Exception::class.java){EncryptedBackup.decrypt(tampered,password)}
        assertThrows(IllegalArgumentException::class.java){EncryptedBackup.encrypt(json,"short".toCharArray())}
        assertThrows(IllegalArgumentException::class.java){EncryptedBackup.decrypt(first.copyOf(10),password)}
    }
}
