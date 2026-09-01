package com.kfaino.diapertracker

import org.junit.Assert.*
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL

class UpdateTlsLiveTest {
    @Test fun officialTlsChainWorksOnAndroid() {
        val official = UpdateSource.latestReleaseApi("Workworks/Collector")
        val candidates = UpdateSource.candidates(official)
        assertEquals(listOf("GitHub 官方"), candidates.map(UpdateSource::label))
        for (candidate in candidates) {
            val connection = URL(candidate).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.instanceFollowRedirects = true
                connection.setRequestProperty("Accept", "application/vnd.github+json")
                connection.setRequestProperty("User-Agent", "Collecter-TLS-QA")
                assertTrue("${UpdateSource.label(candidate)} HTTP ${connection.responseCode}", connection.responseCode in 200..299)
                assertTrue(connection.inputStream.bufferedReader().use { it.readText() }.contains("\"tag_name\""))
            } finally { connection.disconnect() }
        }
    }
}
