package com.kfaino.diapertracker

import android.content.Context
import com.kfaino.collecter.core.CollectionWorkbench
import org.json.JSONArray
import org.json.JSONObject

class WorkbenchRepository(context: Context) {
    private val prefs=context.getSharedPreferences("collector_data",Context.MODE_PRIVATE)
    fun snapshot(): JSONObject = synchronized(CompleteBackupStore.transactionLock) {
        JSONObject(prefs.getString("backup_extra_v2","{}") ?: "{}").apply {
            put("entries",JSONArray(prefs.getString("entries_v4",null) ?: prefs.getString("entries_v3",null) ?: prefs.getString("entries_v2","[]") ?: "[]"))
            for ((name,key) in CompleteBackupStore.collectionKeys) put(name,JSONArray(prefs.getString(key,"[]") ?: "[]"))
            val ledgerEntries=JSONObject()
            for ((key,value) in prefs.all) if (key.startsWith("entries_ledger_") && value is String) ledgerEntries.put(key,JSONArray(value))
            put("ledger_entries",ledgerEntries)
        }
    }
    fun execute(command: JSONObject): Unit = synchronized(CompleteBackupStore.transactionLock) {
        val result=CollectionWorkbench.apply(snapshot(),command)
        val editor=prefs.edit().putString("entries_v4",result.getJSONArray("entries").toString())
        for ((name,key) in CompleteBackupStore.collectionKeys) if(result.has(name)) editor.putString(key,result.getJSONArray(name).toString())
        val ledgers=result.getJSONObject("ledger_entries")
        for(key in ledgers.keys()) editor.putString(key,ledgers.getJSONArray(key).toString())
        val extra=JSONObject(result.toString())
        for(key in CompleteBackupStore.collectionKeys.keys + listOf("entries","ledger_entries")) extra.remove(key)
        editor.putString("backup_extra_v2",extra.toString())
        check(editor.commit()) { "整理结果保存失败" }
    }
}
