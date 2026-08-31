package com.kfaino.diapertracker

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64

class DexSignatureVerifierTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun authenticSignatureAcceptedAndTamperingRejected() {
        val pair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val dex = temp.newFile("patch.dex").apply { writeBytes(ByteArray(18000) { (it % 251).toByte() }) }
        val sig = temp.newFile("patch.dex.sig")
        val signer = Signature.getInstance("SHA256withRSA").apply {
            initSign(pair.private); update(dex.readBytes())
        }
        sig.writeText(Base64.getEncoder().encodeToString(signer.sign()))
        val key = Base64.getEncoder().encodeToString(pair.public.encoded)
        assertTrue(DexSignatureVerifier.verify(dex, sig, key))
        assertFalse(DexSignatureVerifier.verify(dex, sig, ""))
        assertFalse(DexSignatureVerifier.verify(dex, sig, "invalid"))
        val other = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        assertFalse(DexSignatureVerifier.verify(dex, sig, Base64.getEncoder().encodeToString(other.public.encoded)))
        dex.appendText("tampered")
        assertFalse(DexSignatureVerifier.verify(dex, sig, key))
        sig.writeText("invalid signature")
        assertFalse(DexSignatureVerifier.verify(dex, sig, key))
        assertTrue(sig.delete())
        assertFalse(DexSignatureVerifier.verify(dex, sig, key))
    }
}
