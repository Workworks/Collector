package com.kfaino.diapertracker

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test

/** Run only on the isolated emulator after installing the original APK and seeding its sandbox. */
class Upgrade456DeviceTest {
    @Test fun originalSandboxSurvivesSignedInPlaceUpgrade() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val prefs=context.getSharedPreferences("collector_data",0)
        assertEquals("stage456-old-install",prefs.getString("qa_upgrade_marker",null))
        val entries=WorkbenchRepository(context).snapshot().getJSONArray("entries")
        assertTrue((0 until entries.length()).any { entries.getJSONObject(it).optString("id")=="qa456-preserved" })
        assertEquals("com.kfaino.diapertracker",context.packageName)
    }
    @Test fun realAndroidFamilyClientUsesDesktopPermissions() {
        val args=InstrumentationRegistry.getArguments()
        val url=args.getString("familyUrl") ?: error("Missing isolated familyUrl")
        val viewer=args.getString("viewer") ?: error("Missing viewer")
        val editor=args.getString("editor") ?: error("Missing editor")
        val data=org.json.JSONArray(FamilyClientDialog.request(url,viewer,null))
        assertEquals(1,data.length())
        assertFalse(data.toString().contains("must-not-leak"))
        val update=org.json.JSONObject("""{"op":"batch","refs":["entries:family-qa"],"patch":{"loc":"Android verified"}}""")
        assertThrows(Exception::class.java) { FamilyClientDialog.request(url,viewer,update) }
        FamilyClientDialog.request(url,editor,update)
        assertEquals("Android verified",org.json.JSONArray(FamilyClientDialog.request(url,viewer,null)).getJSONObject(0).getJSONObject("record").getString("loc"))
    }
}
