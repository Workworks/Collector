package com.kfaino.diapertracker

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import org.json.JSONArray
import org.json.JSONObject

/** 基于 SharedPreferences 的持久化层，管理记账记录、自定义分组和主题外观 */
class DataStore(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("diaper_data", Context.MODE_PRIVATE)
    private val keyEntries = "entries_v2"
    private val keyCategories = "custom_categories_v1"
    private val keyTheme = "app_theme_mode"

    companion object {
        val DEFAULT_CATEGORIES = listOf("NB", "S", "M", "L", "XL", "XXL", "XXXL")

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
        return loadV1Migration()
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
            )
        }
        prefs.edit().putString(keyEntries, arr.toString()).apply()
    }

    /** 加载 v2 格式（含 category） */
    private fun loadV2(): List<Entry> {
        val raw = prefs.getString(keyEntries, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val result = mutableListOf<Entry>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                result.add(
                    Entry(
                        category = o.optString("cat", "S"),
                        brand = o.optString("brand"),
                        qty = o.optInt("qty", 1),
                        price = o.optDouble("price", 0.0),
                        ts = o.optLong("ts", 0L),
                        isIn = o.optBoolean("in", true),
                        notes = o.optString("notes", "")
                    )
                )
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** 迁移旧版 v1 数据（无 category），默认归类为 S */
    private fun loadV1Migration(): List<Entry> {
        val raw = prefs.getString("entries", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val result = mutableListOf<Entry>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                result.add(
                    Entry(
                        category = "S",
                        brand = o.optString("brand"),
                        qty = o.optInt("qty", 1),
                        price = o.optDouble("price", 0.0),
                        ts = o.optLong("ts", 0L),
                        isIn = o.optBoolean("in", true),
                        notes = ""
                    )
                )
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ==================== 尺码/分类分组管理 ====================

    /** 获取所有分类（按保存的自定义顺序，并自动合并已存在记录中的分类） */
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

        // 自动合并记录中可能存在但未在列表中的分类
        val existingEntries = loadAll()
        for (entry in existingEntries) {
            val cat = entry.category.trim()
            if (cat.isNotEmpty() && !list.contains(cat)) {
                list.add(cat)
            }
        }

        return list
    }

    /** 保存分类列表（保证自定义顺序） */
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

    /** 新增分类，若已存在则返回 false */
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

    /** 删除分类 */
    fun deleteCategory(category: String): Boolean {
        val current = getCategories().toMutableList()
        val removed = current.remove(category)
        if (removed) {
            saveCategories(current)
        }
        return removed
    }

    /** 恢复默认预设分类 */
    fun resetCategories(): List<String> {
        val defaults = DEFAULT_CATEGORIES.toMutableList()
        // 保留已有记录中使用的分类
        for (entry in loadAll()) {
            val cat = entry.category.trim()
            if (cat.isNotEmpty() && !defaults.contains(cat)) {
                defaults.add(cat)
            }
        }
        saveCategories(defaults)
        return defaults
    }

    /** 判断是否为系统预设分类 */
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
        return prefs.getString("github_repo", "Workworks/DiaperTracker") ?: "Workworks/DiaperTracker"
    }

    fun setGithubRepo(repo: String) {
        prefs.edit().putString("github_repo", repo.trim()).apply()
    }
}

