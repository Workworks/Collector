package com.kfaino.diapertracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class UpdateInstallRequestTest {
    @Test
    fun pendingRequestRoundTripsWithoutLosingVerificationData() {
        val pending = UpdateInstallRequest.Pending(
            apkPath = "C:/private/Download/Collecter_4.3.12.apk",
            expectedSize = 48_286_191L,
            expectedSha256 = "sha256:abc123",
            requestedAt = 1_234_567L
        )

        assertEquals(pending, UpdateInstallRequest.decode(UpdateInstallRequest.encode(pending)))
        assertNull(UpdateInstallRequest.decode("not-json"))
    }

    @Test
    fun pendingRequestExpiresAndRejectsFutureTimestamp() {
        val pending = UpdateInstallRequest.Pending("x.apk", 1L, "", 10_000L)

        assertTrue(UpdateInstallRequest.isFresh(pending, 10_000L + UpdateInstallRequest.MAX_AGE_MS))
        assertFalse(UpdateInstallRequest.isFresh(pending, 10_001L + UpdateInstallRequest.MAX_AGE_MS))
        assertFalse(UpdateInstallRequest.isFresh(pending, 9_999L))
    }

    @Test
    fun installerOnlyAcceptsApkInsidePrivateRoots() {
        val root = Files.createTempDirectory("collecter-update-root").toFile()
        val nested = File(root, "nested/Collecter.apk").apply {
            requireNotNull(parentFile).mkdirs()
            writeText("apk")
        }
        val outside = Files.createTempFile("collecter-outside", ".apk").toFile()
        val wrongExtension = File(root, "Collecter.zip").apply { writeText("zip") }

        assertTrue(UpdateInstallRequest.isAllowedApk(nested, listOf(root)))
        assertFalse(UpdateInstallRequest.isAllowedApk(outside, listOf(root)))
        assertFalse(UpdateInstallRequest.isAllowedApk(wrongExtension, listOf(root)))
    }
}
