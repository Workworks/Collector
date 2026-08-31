package com.kfaino.collecter.core

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

/** Lossless operations over stored documents; platforms persist the result as one transaction. */
object CollectionWorkbench {
    private val excluded = setOf("houses", "links", "reminders", "ledgers", "kits", "saved_searches")
    data class Hit(val reference: String, val record: JSONObject)
    fun title(record: JSONObject) = sequenceOf("title", "brand", "name", "original", "content")
        .map { record.optString(it) }.firstOrNull { it.isNotBlank() }?.take(100) ?: "未命名"

    fun records(root: JSONObject): List<Hit> = buildList {
        for (collection in BackupDocument.collections.filterNot { it in excluded }) {
            val array = root.optJSONArray(collection) ?: continue
            for (i in 0 until array.length()) {
                val record = array.getJSONObject(i)
                if (record.optString("id").isNotBlank()) add(Hit("$collection:${record.getString("id")}", record))
            }
        }
        root.optJSONObject("ledger_entries")?.let { ledgers ->
            for (key in ledgers.keys()) {
                val array = ledgers.getJSONArray(key)
                for (i in 0 until array.length()) {
                    val record = array.getJSONObject(i)
                    if (record.optString("id").isNotBlank()) add(Hit("$key:${record.getString("id")}", record))
                }
            }
        }
    }

    fun search(root: JSONObject, query: String = "", location: String = ""): List<Hit> {
        val words = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return records(root).filter { (_, record) ->
            val text = listOf("title","brand","name","original","ocr","notes","content","markdown","loc","location","r_name","roomName","tags","_responsible")
                .joinToString(" ") { record.opt(it)?.toString().orEmpty() }.lowercase()
            val place = listOf("loc","location","r_name","roomName").joinToString(" ") { record.optString(it) }
            words.all(text::contains) && place.contains(location.trim(), ignoreCase = true)
        }.sortedByDescending { it.record.optLong("_lastOpenedAt", it.record.optLong("createdAt",it.record.optLong("ts"))) }
    }

    fun duplicates(root: JSONObject, text: String): List<Hit> {
        val normalized = text.trim().replace(Regex("\\s+"), " ")
        if (normalized.isBlank()) return emptyList()
        return records(root).filter { hit ->
            listOf("original","content","ocr").any { hit.record.optString(it).trim().replace(Regex("\\s+")," ") == normalized }
        }
    }

    fun suggestions(text: String): JSONObject = JSONObject().apply {
        Regex("(?:金额|合计|总计|实付|[¥￥])[:：\\s]*([0-9]+(?:\\.[0-9]{1,2})?)").find(text)?.let { put("amount", it.groupValues[1]) }
        Regex("(20[0-9]{2})[-/年]([0-9]{1,2})[-/月]([0-9]{1,2})日?").find(text)?.let {
            val year=it.groupValues[1].toInt(); val month=it.groupValues[2].toInt(); val day=it.groupValues[3].toInt()
            if (month in 1..12 && day in 1..java.time.YearMonth.of(year,month).lengthOfMonth()) put("date", LocalDate.of(year,month,day).toString())
        }
        Regex("型号[:：\\s]+([A-Za-z0-9][A-Za-z0-9._-]{1,59})").find(text)?.let { put("model", it.groupValues[1]) }
    }

    private fun array(root: JSONObject, name: String): JSONArray = if (name.startsWith("entries_ledger_")) {
        root.getJSONObject("ledger_entries").getJSONArray(name)
    } else root.getJSONArray(name)

    fun apply(document: JSONObject, command: JSONObject, actor: String = "owner", now: Long = System.currentTimeMillis()): JSONObject {
        require(actor.isNotBlank() && actor.length <= 100)
        val root = WireAliases.convert(document)
        when (command.getString("op")) {
            "collect" -> {
                val record = WorkspaceRecords.inbox(command.getString("text"), command.optString("photo"), now)
                val inbox = root.optJSONArray("inbox") ?: JSONArray()
                require(inbox.length() < 100000)
                root.put("inbox", inbox.put(record))
            }
            "save-search" -> {
                val query=command.optString("query").trim(); val location=command.optString("location").trim()
                require(query.isNotEmpty() || location.isNotEmpty()) { "不能保存空搜索" }
                require(query.length <= 500 && location.length <= 200)
                val searches=root.optJSONArray("saved_searches") ?: JSONArray()
                val duplicate=(0 until searches.length()).any { searches.getJSONObject(it).let { s -> s.optString("query")==query && s.optString("location")==location } }
                if (!duplicate) {
                    require(searches.length() < 100) { "最多保存 100 个搜索" }
                    searches.put(JSONObject().put("id",UUID.randomUUID().toString()).put("query",query).put("location",location))
                }
                root.put("saved_searches",searches)
            }
            "link" -> {
                val left=command.getString("left"); val right=command.getString("right")
                val refs=records(root).map { it.reference }.toSet()
                require(left in refs && right in refs) { "关联目标已不存在" }
                root.put("links",WorkspaceRecords.link(root.optJSONArray("links") ?: JSONArray(),left,right))
            }
            "batch", "life", "open", "suggest" -> {
                val op=command.getString("op")
                val references=command.getJSONArray("refs").let { (0 until it.length()).map(it::getString).distinct() }
                require(references.size in 1..500) { "每次处理 1–500 条记录" }
                val indexed=records(root).associateBy { it.reference }
                require(references.all(indexed::containsKey)) { "部分记录已删除，请刷新后重试" }
                for (ref in references) {
                    val current=indexed.getValue(ref).record
                    val next=JSONObject(current.toString())
                    when (op) {
                        "open" -> next.put("_lastOpenedAt",now)
                        "batch" -> {
                            val patch=command.getJSONObject("patch")
                            require(patch.keys().asSequence().all { it in setOf("title","loc","tags","notes","_responsible","_sensitive","_sharedWith","status") }) { "不允许修改该字段" }
                            for (field in patch.keys()) {
                                val value=patch.get(field)
                                when (field) {
                                    "tags","_sharedWith" -> {
                                        require(value is JSONArray && value.length() <= 50)
                                        for (i in 0 until value.length()) require(value.get(i) is String && value.getString(i).length in 1..100)
                                    }
                                    "_sensitive" -> require(value is Boolean)
                                    else -> require(value is String && value.length <= 5000)
                                }
                                next.put(field,value)
                            }
                            if (patch.has("loc") && ref.startsWith("books:") || patch.has("loc") && ref.startsWith("beverages:")) next.put("location",patch.getString("loc"))
                        }
                        "suggest" -> next.put("_extracted", suggestions(listOf("original","ocr","content").joinToString("\n") { current.optString(it) }))
                        "life" -> lifecycle(next, command, actor, now)
                    }
                    if (op != "open") {
                        val audit=next.optJSONArray("_audit") ?: JSONArray()
                        require(audit.length() < 10000) { "操作历史过长，请先归档" }
                        audit.put(JSONObject().put("id",UUID.randomUUID().toString()).put("actor",actor).put("op",op).put("at",now))
                        next.put("_audit",audit)
                    }
                    val values=array(root,ref.substringBefore(':'))
                    next.put("_updatedAt",maxOf(now,current.optLong("_updatedAt")+1))
                    val index=(0 until values.length()).first { values.getJSONObject(it).optString("id")==current.getString("id") }
                    values.put(index,next)
                }
            }
            else -> error("未知工作台操作")
        }
        return SnapshotSync.recordChanges(document,root,now)
    }

    private fun lifecycle(record: JSONObject, command: JSONObject, actor: String, now: Long) {
        val action=command.getString("action")
        val state=record.optString("_lifeState","active")
        require(state !in setOf("sold","retired")) { "已转卖或报废的记录不能继续操作" }
        require(action in setOf("purchase","maintenance","lend","return","sell","retire"))
        require(action != "return" || state == "lent") { "没有借出记录，不能归还" }
        require(state != "lent" || action == "return") { "请先记录归还" }
        val person=command.optString("person").trim(); val note=command.optString("note").trim()
        require(person.length <= 100 && note.length <= 5000)
        require(action != "lend" || person.isNotBlank()) { "借出必须填写借用人" }
        val events=record.optJSONArray("_lifeEvents") ?: JSONArray()
        require(events.length() < 10000)
        events.put(JSONObject().put("id",UUID.randomUUID().toString()).put("action",action).put("at",now).put("person",person).put("note",note).put("actor",actor))
        record.put("_lifeEvents",events).put("_lifeState",when(action) { "lend"->"lent";"sell"->"sold";"retire"->"retired";else->"active" })
        if (person.isNotBlank()) record.put("_responsible",person)
        if (action in setOf("sell","retire")) record.put("is_ret",true).put("ret_at",now).put("ret_act",if(action=="sell") "转卖" else "报废").put("_nextActionAt",0)
        if (action == "purchase") record.put("p_date",now)
        if (action == "maintenance") {
            val due=command.optLong("nextAt",0)
            require(due == 0L || due > now) { "下次维护日期必须在本次操作之后" }
            record.put("_nextActionAt",due).put("last_maint_d",now)
        }
    }
}
