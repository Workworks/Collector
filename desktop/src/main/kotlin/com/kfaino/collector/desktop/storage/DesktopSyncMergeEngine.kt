package com.kfaino.collector.desktop.storage

import com.kfaino.collector.desktop.models.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class DesktopMergeReport(
    val insertedEntries: Int = 0,
    val updatedEntries: Int = 0,
    val preservedEntries: Int = 0,
    val mergedCategories: Int = 0,
    val mergedVaultItems: Int = 0,
    val success: Boolean = true,
    val message: String = "合并成功"
) {
    fun summary(): String {
        return "桌面端增量对撞合并完成：新增资产 ${insertedEntries} 件，更新资产 ${updatedEntries} 件，保留 ${preservedEntries} 件，合并分类 ${mergedCategories} 个，合并馆藏/资料 ${mergedVaultItems} 项。$message"
    }
}

/**
 * 桌面端 P2P 增量对撞合并核心引擎
 */
object DesktopSyncMergeEngine {

    fun merge(store: DesktopDataStore, incomingJsonStr: String): DesktopMergeReport {
        return try {
            val merged = com.kfaino.collecter.core.SnapshotSync.merge(store.exportJson(), incomingJsonStr)
            check(store.importJson(merged.document.toString())) { "合并结果保存失败" }
            DesktopMergeReport(merged.inserted, merged.updated, merged.preserved, 0, merged.vaultChanges,
                message = "冲突 ${merged.conflicts} 项（保留双方副本），删除 ${merged.deleted} 项")
        } catch (e: Exception) {
            System.err.println("完整快照合并失败: ${e.message}")
            DesktopMergeReport(success = false, message = e.message ?: "合并失败")
        }
    }
}
