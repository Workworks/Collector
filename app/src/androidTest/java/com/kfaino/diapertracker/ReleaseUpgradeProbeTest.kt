package com.kfaino.diapertracker

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test

class ReleaseUpgradeProbeTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    @Test fun seedOnReleased436() {
        assertEquals("4.3.6", context.packageManager.getPackageInfo(context.packageName, 0).versionName)
        assertTrue(context.getSharedPreferences("collector_release_upgrade_probe", 0).edit()
            .putString("sentinel", "保留升级数据-4.3.6-to-4.3.7").commit())
    }
    @Test fun verifyAfterUpgradeTo437AndCleanup() {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        assertEquals("4.3.7", info.versionName)
        assertEquals(44L, if (android.os.Build.VERSION.SDK_INT >= 28) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong())
        val prefs = context.getSharedPreferences("collector_release_upgrade_probe", 0)
        assertEquals("保留升级数据-4.3.6-to-4.3.7", prefs.getString("sentinel", null))
        assertTrue(prefs.edit().clear().commit())
    }
}
