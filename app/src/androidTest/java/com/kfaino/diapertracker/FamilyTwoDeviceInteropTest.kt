package com.kfaino.diapertracker

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.net.URL

@RunWith(AndroidJUnit4::class)
class FamilyTwoDeviceInteropTest {
    private val args get() = InstrumentationRegistry.getArguments()

    @Test fun desktopFamilyServerEnforcesAndroidMemberContract() {
        val address = requireNotNull(args.getString("familyAddress"))
        val viewer = requireNotNull(args.getString("viewerToken"))
        val editor = requireNotNull(args.getString("editorToken"))

        val visible = JSONArray(FamilyClientDialog.request(address, viewer, null))
        assertEquals(1, visible.length())
        assertEquals("联调咖啡机", visible.getJSONObject(0).getJSONObject("record").getString("brand"))
        assertFalse(visible.toString().contains("secretField"))
        assertFalse(visible.toString().contains("私人记录"))
        assertFalse(visible.toString().contains("敏感记录"))

        val sharedRef = visible.getJSONObject(0).getString("reference")
        val update = JSONObject().put("op", "batch").put("refs", JSONArray(listOf(sharedRef))).put("patch", JSONObject().put("loc", "客厅"))
        assertEquals(403, raw(address, viewer, update))
        FamilyClientDialog.request(address, editor, update)
        val updated = JSONArray(FamilyClientDialog.request(address, editor, null))
        assertEquals("客厅", updated.getJSONObject(0).getJSONObject("record").getString("loc"))

        val privateUpdate = JSONObject().put("op", "batch").put("refs", JSONArray(listOf("entries:private"))).put("patch", JSONObject().put("loc", "泄漏"))
        assertEquals(403, raw(address, editor, privateUpdate))
        val sensitiveUpdate = JSONObject().put("op", "batch").put("refs", JSONArray(listOf(sharedRef))).put("patch", JSONObject().put("_sensitive", false))
        assertEquals(403, raw(address, editor, sensitiveUpdate))
    }

    @Test fun revokedMemberIsRejectedByDesktop() {
        val address = requireNotNull(args.getString("familyAddress"))
        val viewer = requireNotNull(args.getString("viewerToken"))
        assertEquals(401, rawGet(address, viewer))
    }

    private fun raw(address: String, token: String, command: JSONObject): Int {
        val connection = URL(address).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(command.toString().toByteArray()) }
            connection.responseCode
        } finally { connection.disconnect() }
    }

    private fun rawGet(address: String, token: String): Int {
        val connection = URL(address).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.responseCode
        } finally { connection.disconnect() }
    }
}
