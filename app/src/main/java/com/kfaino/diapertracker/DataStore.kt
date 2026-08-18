package com.kfaino.diapertracker

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import org.json.JSONArray
import org.json.JSONObject

/** 基于 SharedPreferences 的持久化层，管理收纳记账记录、通用分类和主题外观 */
class DataStore(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("collector_data", Context.MODE_PRIVATE)
    private val legacyPrefs = ctx.getSharedPreferences("diaper_data", Context.MODE_PRIVATE)
    private val keyEntries = "entries_v2"
    private val keyCategories = "custom_categories_v2"
    private val keyTheme = "app_theme_mode"

    companion object {
        // 通用默认分类（数码、日用品、零食、耗材）
        val DEFAULT_CATEGORIES = listOf("数码", "日用品", "零食", "耗材")

        // 常用快捷数量单位
        val COMMON_UNITS = listOf("片", "件", "包", "个", "箱", "瓶", "盒", "本")

        const val THEME_SYSTEM = 0
        const val THEME_LIGHT = 1
        const val THEME_DARK = 2

        fun applyThemeMode(mode: Int) {
            when (mode) {
                THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }

    // ==================== 记录存取 ====================

    fun loadAll(): List<Entry> {
        val v2 = loadV2()
        if (v2.isNotEmpty()) return v2
        return loadLegacyMigration()
    }

    fun saveAll(entries: List<Entry>) {
        val arr = JSONArray()
        for (e in entries) {
            arr.put(
                JSONObject()
                    .put("cat", e.category)
                    .put("brand", e.brand)
                    .put("qty", e.qty)
                    .put("price", e.price)
                    .put("ts", e.ts)
                    .put("in", e.isIn)
                    .put("notes", e.notes)
                    .put("unit", e.unit)
            )
        }
        prefs.edit().putString(keyEntries, arr.toString()).apply()
    }

    fun updateEntry(index: Int, newEntry: Entry): Boolean {
        val list = loadAll().toMutableList()
        if (index in 0 until list.size) {
            list[index] = newEntry
            saveAll(list)
            return true
        }
        return false
    }

    fun deleteEntryAt(index: Int): Boolean {
        val list = loadAll().toMutableList()
        if (index in 0 until list.size) {
            list.removeAt(index)
            saveAll(list)
            return true
        }
        return false
    }

    fun clearAllData() {
        prefs.edit().remove(keyEntries).apply()
    }

    fun getLastUsedUnit(): String {
        return prefs.getString("last_used_unit", "片") ?: "片"
    }

    fun setLastUsedUnit(unit: String) {
        val trimmed = unit.trim()
        if (trimmed.isNotEmpty()) {
            prefs.edit().putString("last_used_unit", trimmed).apply()
        }
    }

    /** 导出全部数据为 JSON 字符串 */
    fun exportBackupJson(): String {
        val root = JSONObject()
        root.put("version", 2)
        root.put("timestamp", System.currentTimeMillis())

        val catArr = JSONArray()
        for (c in getCategories()) {
            catArr.put(c)
        }
        root.put("categories", catArr)

        val entryArr = JSONArray()
        for (e in loadAll()) {
            entryArr.put(
                JSONObject()
                    .put("cat", e.category)
                    .put("brand", e.brand)
                    .put("qty", e.qty)
                    .put("price", e.price)
                    .put("ts", e.ts)
                    .put("in", e.isIn)
                    .put("notes", e.notes)
                    .put("unit", e.unit)
            )
        }
        root.put("entries", entryArr)
        return root.toString(2)
    }

    /** 导入 JSON 备份数据 */
    fun importBackupJson(jsonStr: String): Boolean {
        return try {
            val root = JSONObject(jsonStr)
            val catArr = root.optJSONArray("categories")
            if (catArr != null) {
                val cats = mutableListOf<String>()
                for (i in 0 until catArr.length()) {
                    val c = catArr.optString(i).trim()
                    if (c.isNotEmpty() && !cats.contains(c)) {
                        cats.add(c)
                    }
                }
                if (cats.isNotEmpty()) {
                    saveCategories(cats)
                }
            }

            val entryArr = root.optJSONArray("entries")
            if (entryArr != null) {
                val list = mutableListOf<Entry>()
                for (i in 0 until entryArr.length()) {
                    val o = entryArr.getJSONObject(i)
                    list.add(
                        Entry(
                            category = o.optString("cat", "默认分类"),
                            brand = o.optString("brand", "未知"),
                            qty = o.optInt("qty", 1),
                            price = o.optDouble("price", 0.0),
                            ts = o.optLong("ts", System.currentTimeMillis()),
                            isIn = o.optBoolean("in", true),
                            notes = o.optString("notes", ""),
                            unit = o.optString("unit", "片")
                        )
                    )
                }
                saveAll(list)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    /** 加载 v2 格式 */
    private fun loadV2(): List<Entry> {
        val raw = prefs.getString(keyEntries, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val result = mutableListOf<Entry>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                result.add(
                    Entry(
                        category = o.optString("cat", "默认分类"),
                        brand = o.optString("brand"),
                        qty = o.optInt("qty", 1),
                        price = o.optDouble("price", 0.0),
                        ts = o.optLong("ts", 0L),
                        isIn = o.optBoolean("in", true),
                        notes = o.optString("notes", ""),
                        unit = o.optString("unit", "片")
                    )
                )
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** 迁移旧版数据 */
    private fun loadLegacyMigration(): List<Entry> {
        val raw = legacyPrefs.getString("entries_v2", null) ?: legacyPrefs.getString("entries", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val result = mutableListOf<Entry>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                result.add(
                    Entry(
                        category = o.optString("cat", "默认分类"),
                        brand = o.optString("brand"),
                        qty = o.optInt("qty", 1),
                        price = o.optDouble("price", 0.0),
                        ts = o.optLong("ts", 0L),
                        isIn = o.optBoolean("in", true),
                        notes = o.optString("notes", ""),
                        unit = o.optString("unit", "片")
                    )
                )
            }
            saveAll(result)
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ==================== 通用分类分组管理 ====================

    fun getCategories(): List<String> {
        val raw = prefs.getString(keyCategories, null)
        val list = mutableListOf<String>()
        if (raw != null) {
            try {
                val arr = JSONArray(raw)
                for (i in 0 until arr.length()) {
                    val s = arr.optString(i)?.trim()
                    if (!s.isNullOrEmpty() && !list.contains(s)) {
                        list.add(s)
                    }
                }
            } catch (_: Exception) {}
        }

        if (list.isEmpty()) {
            list.addAll(DEFAULT_CATEGORIES)
        }

        val existingEntries = loadAll()
        for (entry in existingEntries) {
            val cat = entry.category.trim()
            if (cat.isNotEmpty() && !list.contains(cat)) {
                list.add(cat)
            }
        }

        return list
    }

    fun saveCategories(categories: List<String>) {
        val arr = JSONArray()
        for (c in categories) {
            val trimmed = c.trim()
            if (trimmed.isNotEmpty()) {
                arr.put(trimmed)
            }
        }
        prefs.edit().putString(keyCategories, arr.toString()).apply()
    }

    fun addCategory(category: String): Boolean {
        val trimmed = category.trim()
        if (trimmed.isEmpty()) return false
        val current = getCategories().toMutableList()
        if (current.any { it.equals(trimmed, ignoreCase = true) }) {
            return false
        }
        current.add(trimmed)
        saveCategories(current)
        return true
    }

    fun deleteCategory(category: String): Boolean {
        val current = getCategories().toMutableList()
        val removed = current.remove(category)
        if (removed) {
            saveCategories(current)
        }
        return removed
    }

    fun resetCategories(): List<String> {
        val defaults = DEFAULT_CATEGORIES.toMutableList()
        for (entry in loadAll()) {
            val cat = entry.category.trim()
            if (cat.isNotEmpty() && !defaults.contains(cat)) {
                defaults.add(cat)
            }
        }
        saveCategories(defaults)
        return defaults
    }

    fun isPresetCategory(category: String): Boolean {
        return DEFAULT_CATEGORIES.contains(category)
    }

    // ==================== 主题设置（深色/浅色/系统） ====================

    fun getThemeMode(): Int {
        return prefs.getInt(keyTheme, THEME_SYSTEM)
    }

    fun setThemeMode(mode: Int) {
        prefs.edit().putInt(keyTheme, mode).apply()
        applyThemeMode(mode)
    }

    // ==================== GitHub 更新仓库设置 ====================

    fun getGithubRepo(): String {
        return prefs.getString("github_repo", "Workworks/Collector") ?: "Workworks/Collector"
    }

    fun setGithubRepo(repo: String) {
        prefs.edit().putString("github_repo", repo.trim()).apply()
    }
}
