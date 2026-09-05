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

    @Test fun seedOnReleased437() {
        assertEquals("4.3.7", context.packageManager.getPackageInfo(context.packageName, 0).versionName)
        assertTrue(context.getSharedPreferences("collector_release_upgrade_probe", 0).edit()
            .putString("sentinel", "保留升级数据-4.3.7-to-4.3.8").commit())
    }

    @Test fun verifyAfterUpgradeTo438AndCleanup() {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        assertEquals("4.3.8", info.versionName)
        assertEquals(45L, if (android.os.Build.VERSION.SDK_INT >= 28) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong())
        val prefs = context.getSharedPreferences("collector_release_upgrade_probe", 0)
        assertEquals("保留升级数据-4.3.7-to-4.3.8", prefs.getString("sentinel", null))
        assertTrue(prefs.edit().clear().commit())
    }

    @Test fun seedOnReleased438() {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        assertEquals("4.3.8", info.versionName)
        assertTrue(context.getSharedPreferences("release_upgrade_probe", 0).edit()
            .putString("sentinel", "保留升级数据-4.3.8-to-4.3.9").commit())
    }

    @Test fun verifyAfterUpgradeTo439AndCleanup() {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        assertEquals("4.3.9", info.versionName)
        @Suppress("DEPRECATION") val code = info.versionCode
        assertEquals(46, code)
        val prefs = context.getSharedPreferences("release_upgrade_probe", 0)
        assertEquals("保留升级数据-4.3.8-to-4.3.9", prefs.getString("sentinel", null))
        prefs.edit().clear().commit()
    }

    @Test fun seedOnReleased439() {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        assertEquals("4.3.9", info.versionName)
        assertTrue(context.getSharedPreferences("release_upgrade_probe", 0).edit()
            .putString("sentinel", "保留升级数据-4.3.9-to-4.3.10").commit())
    }

    @Test fun verifyAfterUpgradeTo4310AndCleanup() {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        assertEquals("4.3.10", info.versionName)
        @Suppress("DEPRECATION") val code = info.versionCode
        assertEquals(47, code)
        val prefs = context.getSharedPreferences("release_upgrade_probe", 0)
        assertEquals("保留升级数据-4.3.9-to-4.3.10", prefs.getString("sentinel", null))
        prefs.edit().clear().commit()
    }

}
