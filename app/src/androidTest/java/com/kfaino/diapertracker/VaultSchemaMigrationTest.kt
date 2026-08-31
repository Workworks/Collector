package com.kfaino.diapertracker

import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class VaultSchemaMigrationTest {
    @Test fun release436KeysMigrateWithoutLosingLocalOrUnknownFields() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "migration457-${UUID.randomUUID()}"
        val prefs = context.getSharedPreferences(name, 0)
        try {
            assertTrue(prefs.edit()
                .putString("vault_tools_v1", """[{"id":"old","m_days":30,"m_at":123,"future":"kept"}]""")
                .putString("vault_tools_maintenance_v1", """[{"id":"local","interval_d":7}]""")
                .putString("vault_plants_v1", """[{"id":"plant","w_days":8,"w_at":123,"f_days":20,"f_at":456}]""")
                .putString("vault_pets_v1", """[{"id":"pet","chip":"xyz","dw_days":10,"vac_days":365}]""")
                .putString("vault_beverage_v1", """[{"id":"drink","qty":0.5,"open_at":123,"p_days":7,"aging":2030}]""")
                .putString("vault_clippings_v1", """[{"id":"clip","cap_at":123,"prog":75}]""")
                .commit())
            VaultSchemaMigration.migrate(prefs)
            val tools = JSONArray(prefs.getString("vault_tools_maintenance_v1", "[]"))
            assertEquals(2, tools.length())
            assertEquals("kept", tools.getJSONObject(0).getString("future"))
            assertEquals(30, ToolMaintenanceVaultRepository(prefs).getToolRecords().first().maintenanceIntervalDays)
            assertEquals(8, PlantCareVaultRepository(prefs).getPlantRecords().single().waterIntervalDays)
            assertEquals("xyz", PetCareVaultRepository(prefs).getPetRecords().single().microchipId)
            val beverages = BeverageTeaVaultRepository(prefs)
            val drink = beverages.getBeverageRecords().single()
            assertEquals(0.5, drink.qty, 0.0)
            assertEquals(123L, drink.openedAt)
            beverages.saveBeverageRecords(listOf(drink))
            assertEquals(0.5, beverages.getBeverageRecords().single().qty, 0.0)
            assertEquals(0.75f, ClippingVaultRepository(prefs).getClippings().single().readingProgress, 0.0f)
            assertTrue(prefs.contains("vault_tools_v1"))
            prefs.edit().putString("vault_tools_maintenance_v1", "[]").commit()
            VaultSchemaMigration.migrate(prefs)
            assertEquals(0, JSONArray(prefs.getString("vault_tools_maintenance_v1", "[]")).length())
        } finally { context.deleteSharedPreferences(name) }
    }
}
