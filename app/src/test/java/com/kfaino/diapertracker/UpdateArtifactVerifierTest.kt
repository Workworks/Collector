package com.kfaino.diapertracker

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class UpdateArtifactVerifierTest {
    @get:Rule val temp = TemporaryFolder()
    @Test fun sizeAndGithubDigestMustMatch() {
        val file = temp.newFile("app.apk").apply { writeText("Collecter 4.3.8") }
        assertTrue(UpdateArtifactVerifier.verify(file, 15, "sha256:19bd167ac3f6d6cb442baf7201048e9f9e630f638ae1a7986c35d4b6ccd984a9"))
        assertFalse(UpdateArtifactVerifier.verify(file, 14, ""))
        assertFalse(UpdateArtifactVerifier.verify(file, 15, "sha256:deadbeef"))
        assertTrue(file.delete())
        assertFalse(UpdateArtifactVerifier.verify(file, 0, ""))
    }
}
