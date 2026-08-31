package com.kfaino.collector.desktop.server

import com.kfaino.collecter.core.*
import com.kfaino.collector.desktop.storage.DesktopDataStore
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Per-session grants: credentials never enter the shared backup or application records. */
class FamilyAccess(private val store: DesktopDataStore, private val clock:()->Long = System::currentTimeMillis) {
    data class Member(val id:String,val name:String,val role:String,val expires:Long)
    data class Grant(val member:Member,val token:String)
    private val tokens=ConcurrentHashMap<String,Member>()
    @Synchronized fun issue(name:String,role:String): Grant {
        require(name.trim().length in 1..80 && role in setOf("viewer","editor"))
        tokens.entries.removeIf { it.value.expires <= clock() }
        require(tokens.size < 100) { "本会话成员过多，请先撤销" }
        val token=Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32).also { SecureRandom().nextBytes(it) })
        val member=Member(UUID.randomUUID().toString(),name.trim(),role,clock()+12L*60*60*1000)
        tokens[BackupDocument.sha256(token.toByteArray())]=member
        return Grant(member,token)
    }
    fun identify(token:String):Member? = tokens[BackupDocument.sha256(token.toByteArray())]?.takeIf { it.expires>clock() }
    fun members()=tokens.values.filter { it.expires>clock() }.toList()
    @Synchronized fun revoke(id:String) { tokens.entries.removeIf { it.value.id==id } }
    @Synchronized fun clear()=tokens.clear()
    private fun visible(hit:CollectionWorkbench.Hit,member:Member):Boolean {
        if(hit.reference.startsWith("identity_docs:") || hit.record.optBoolean("_sensitive")) return false
        val shared=hit.record.optJSONArray("_sharedWith") ?: return false
        return (0 until shared.length()).any { shared.optString(it)==member.id }
    }
    @Synchronized fun read(token:String):JSONArray {
        val member=identify(token) ?: throw SecurityException("成员密钥无效、已撤销或过期")
        val safeFields=setOf("id","title","brand","name","loc","location","tags","notes","_responsible","_lifeState","_lifeEvents","_nextActionAt")
        return JSONArray(CollectionWorkbench.records(store.workbenchSnapshot()).filter { visible(it,member) }.map { hit ->
            JSONObject().put("reference",hit.reference).put("record",JSONObject().apply {
                for(field in safeFields) if(hit.record.has(field)) put(field,hit.record.get(field))
            })
        })
    }
    @Synchronized fun write(token:String,command:JSONObject) {
        val member=identify(token) ?: throw SecurityException("成员密钥无效、已撤销或过期")
        if(member.role!="editor") throw SecurityException("查看者不能修改资料")
        synchronized(store) {
            val allowed=CollectionWorkbench.records(store.workbenchSnapshot()).filter { visible(it,member) }.map { it.reference }.toSet()
            val refs=command.getJSONArray("refs")
            if((0 until refs.length()).any { refs.getString(it) !in allowed }) throw SecurityException("不能访问未共享或敏感记录")
            when(command.getString("op")) {
                "batch" -> if(command.getJSONObject("patch").keys().asSequence().any { it !in setOf("loc","tags","notes","_responsible") }) throw SecurityException("成员不能改变共享权限或敏感标记")
                "life" -> if(command.optString("action") !in setOf("maintenance","lend","return")) throw SecurityException("成员仅可记录维护和借还")
                else -> throw SecurityException("成员不能执行该操作")
            }
            store.executeWorkbench(command,member.id)
        }
    }
}
