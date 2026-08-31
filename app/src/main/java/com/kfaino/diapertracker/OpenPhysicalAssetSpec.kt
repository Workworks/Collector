package com.kfaino.diapertracker

import org.json.JSONArray
import org.json.JSONObject

/**
 * 🌐 全球离线开放物质代码规范（Open Physical Asset Spec）解析与导出器
 */
object OpenPhysicalAssetSpec {

    fun exportToOpenSpec(store: DataStore): String {
        val entries = store.loadAll().filter { it.isIn && !it.isRetired }
        val root = JSONObject()
        root.put("spec_version", "1.0.0-Universal")
        root.put("schema", "https://collecter.app/spec/open-physical-asset.json")
        root.put("exported_at", System.currentTimeMillis())

        val arr = JSONArray()
        for (e in entries) {
            val item = JSONObject()
            item.put("uuid", e.id)
            item.put("name", e.brand)
            item.put("category", e.category)
            item.put("quantity", e.qty)
            item.put("unit", e.unit)
            item.put("valuation_cny", e.currentValuation.takeIf { it > 0.0 } ?: e.price)
            item.put("location_hierarchy", e.houseName + " > " + e.roomName + " > " + e.location)
            arr.put(item)
        }
        root.put("assets", arr)
        return root.toString(2)
    }
}