package com.kfaino.diapertracker

import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray

/**
 * 🏷️ 资产分类与预设分组持久化仓储 (Category Repository)
 * 封装物品分类增删改、预设分类与已有物品隐式分类的动态合并计算。
 * 作为 DataStore 门面下沉的专用仓储，严格保持 SharedPreferences Key 完全不变。
 */
class CategoryRepository(private val prefs: SharedPreferences) {

    companion object {
        private const val TAG = "CategoryRepository"
        const val KEY_CATEGORIES = "custom_categories_v2"
    }

    fun getCategories(entriesSupplier: () -> List<Entry>): List<String> {
        val raw = prefs.getString(KEY_CATEGORIES, null)
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
            } catch (e: Exception) {
                Log.w(TAG, "解析自定义分类 JSON 失败: $raw", e)
            }
        }

        if (list.isEmpty()) {
            list.addAll(DataStore.DEFAULT_CATEGORIES)
        }

        val existingEntries = entriesSupplier()
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
        prefs.edit().putString(KEY_CATEGORIES, arr.toString()).apply()
    }

    fun addCategory(category: String, entriesSupplier: () -> List<Entry>): Boolean {
        val trimmed = category.trim()
        if (trimmed.isEmpty()) return false
        val current = getCategories(entriesSupplier).toMutableList()
        if (current.any { it.equals(trimmed, ignoreCase = true) }) {
            return false
        }
        current.add(trimmed)
        saveCategories(current)
        return true
    }

    fun deleteCategory(category: String, entriesSupplier: () -> List<Entry>): Boolean {
        val current = getCategories(entriesSupplier).toMutableList()
        val removed = current.remove(category)
        if (removed) {
            saveCategories(current)
        }
        return removed
    }

    fun resetCategories(entriesSupplier: () -> List<Entry>): List<String> {
        val defaults = DataStore.DEFAULT_CATEGORIES.toMutableList()
        for (entry in entriesSupplier()) {
            val cat = entry.category.trim()
            if (cat.isNotEmpty() && !defaults.contains(cat)) {
                defaults.add(cat)
            }
        }
        saveCategories(defaults)
        return defaults
    }

    fun isPresetCategory(category: String): Boolean {
        return DataStore.DEFAULT_CATEGORIES.contains(category)
    }
}
