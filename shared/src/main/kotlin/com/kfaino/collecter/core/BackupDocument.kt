package com.kfaino.collecter.core

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.Base64

/** Versioned, lossless JSON boundary shared by Android and desktop. No model reserialization. */
object BackupDocument {
    const val MAX_BYTES = 64 * 1024 * 1024
    const val MAX_ASSET_BYTES = 16 * 1024 * 1024
    val collections = listOf("entries", "houses", "vouchers", "identity_docs", "medicines", "foods",
        "honors", "wardrobe", "emergency", "tools", "plants", "pets", "books", "beverages",
        "ideas", "clippings", "inbox", "links", "reminders", "ledgers", "kits", "saved_searches")
    private val pathKeys = setOf("img_p", "rec_p", "photo", "photoPath", "receiptPath", "f_photo", "b_photo",
        "frontPhotoPath", "backPhotoPath", "local_path", "localPath", "local_file", "localFilePath", "filePath", "images", "localImagePaths", "voice", "voiceMemoPath", "cover", "coverPath")

    fun parse(text: String): JSONObject {
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "备份超过 64 MiB 上限" }
        val tokener = JSONTokener(text)
        val root = tokener.nextValue() as? JSONObject ?: error("备份必须为 JSON 对象")
        require(tokener.nextClean() == '\u0000') { "备份末尾包含无效内容" }
        require(root.optInt("schemaVersion", 1) in 1..2) { "不支持此备份版本" }
        require(collections.any { root.has(it) }) { "未找到有效数据集合" }
        for (key in collections) {
            if (!root.has(key)) continue
            validateRecords(root.getJSONArray(key), key)
        }
        if (root.has("categories")) {
            val cats = root.getJSONArray("categories")
            for (i in 0 until cats.length()) require(cats.get(i) is String) { "分类必须为文本" }
        }
        if (root.has("ledger_entries")) {
            val ledgers = root.getJSONObject("ledger_entries")
            for (key in ledgers.keys()) {
                require(key.matches(Regex("entries_(v[234]|ledger_[A-Za-z0-9_-]+)"))) { "无效账本键" }
                validateRecords(ledgers.getJSONArray(key), key)
            }
        }
        if (root.has("_tombstones")) {
            val sets = root.getJSONObject("_tombstones")
            for (key in sets.keys()) {
                val deleted = sets.getJSONObject(key)
                for (id in deleted.keys()) require(id.isNotBlank() && deleted.getLong(id) >= 0L) { "删除记录格式错误" }
            }
        }
        if (root.has("_conflicts")) require(root.get("_conflicts") is JSONObject) { "冲突记录格式错误" }
        val assets = root.optJSONObject("assets") ?: JSONObject()
        if (root.has("assets")) require(root.get("assets") is JSONObject) { "附件结构错误" }
        var total = 0L
        for (id in assets.keys()) {
            require(id.matches(Regex("[a-f0-9]{64}"))) { "无效附件标识" }
            val data = Base64.getDecoder().decode(assets.getString(id))
            require(data.size <= MAX_ASSET_BYTES) { "单个附件超过 16 MiB" }
            total += data.size
            require(total <= MAX_BYTES / 2) { "附件总量超过 32 MiB" }
            require(sha256(data) == id) { "附件校验失败" }
        }
        visit(root) { _, value ->
            if (value.startsWith("asset:")) require(assets.has(value.removePrefix("asset:"))) { "缺少附件" }
            value
        }
        return root
    }

    private fun validateRecords(arr: JSONArray, key: String) {
        require(arr.length() <= 100000) { "$key 条目过多" }
        val ids = mutableSetOf<String>()
        for (i in 0 until arr.length()) {
            val record = arr.getJSONObject(i)
            for (field in listOf("tags", "linked_ids", "images", "localImagePaths")) if (record.has(field)) {
                val values = record.getJSONArray(field)
                for (j in 0 until values.length()) require(values.get(j) is String) { "$key.$field 必须为文本数组" }
            }
            if (record.has("loc_hist")) {
                val history = record.getJSONArray("loc_hist")
                for (j in 0 until history.length()) require(history.get(j) is JSONObject) { "$key.loc_hist 必须为对象数组" }
            }
            // Historical records can lack IDs. A present ID must never be empty or duplicated.
            if (record.has("id")) {
                require(record.get("id") is String) { "$key 的 ID 必须是字符串" }
                val id = record.getString("id")
                require(id.isNotBlank() && ids.add(id)) { "$key 中有空或重复 ID" }
            }
        }
    }

    fun preview(text: String): String {
        val root = parse(text)
        val summary = collections.filter { root.has(it) }.joinToString("\n") { "$it：${root.getJSONArray(it).length()}" }
        return "$summary\n附件：${root.optJSONObject("assets")?.length() ?: 0}\n将替换备份包含的集合；未包含的集合保持原样。"
    }

    fun attachFiles(document: JSONObject, allowedRoots: List<File>): JSONObject {
        val copy = JSONObject(document.toString())
        val assets = copy.optJSONObject("assets") ?: JSONObject()
        val roots = allowedRoots.map { it.canonicalFile.toPath() }
        var bytes = 0L
        visit(copy) { key, value ->
            if (key !in pathKeys || value.isBlank() || value.startsWith("asset:")) value
            else {
                val file = File(value)
                if (!file.isAbsolute) value
                else {
                    require(roots.any { file.canonicalFile.toPath().startsWith(it) }) { "附件不在应用目录内：请先导入原件" }
                    require(file.isFile) { "附件不存在，未生成不完整备份" }
                    require(file.length() <= MAX_ASSET_BYTES) { "单个附件超过 16 MiB" }
                    val data = file.readBytes()
                    val id = sha256(data)
                    if (!assets.has(id)) {
                        bytes += data.size
                        require(bytes <= MAX_BYTES / 2) { "附件总量超过 32 MiB" }
                        assets.put(id, Base64.getEncoder().encodeToString(data))
                    }
                    "asset:$id"
                }
            }
        }
        copy.put("schemaVersion", 2).put("assets", assets)
        return parse(copy.toString())
    }

    /** Validate all bytes first; write only content-addressed files under a fixed private directory. */
    fun restoreFiles(document: JSONObject, dataDir: File): JSONObject {
        val copy = parse(document.toString())
        val assets = copy.optJSONObject("assets") ?: JSONObject()
        val folder = File(dataDir, "backup-assets")
        require(!Files.isSymbolicLink(folder.toPath())) { "附件目录不能为符号链接" }
        if (assets.length() > 0) require(folder.isDirectory || folder.mkdirs()) { "无法创建附件目录" }
        for (id in assets.keys()) {
            val file = File(folder, id)
            require(!Files.isSymbolicLink(file.toPath())) { "附件不能为符号链接" }
            val data = Base64.getDecoder().decode(assets.getString(id))
            if (file.exists()) require(sha256(file.readBytes()) == id) { "已有附件损坏" }
            else atomicWrite(file, data)
        }
        visit(copy) { _, value ->
            if (value.startsWith("asset:")) File(folder, value.removePrefix("asset:")).absolutePath else value
        }
        copy.remove("assets")
        return copy
    }

    fun atomicWrite(file: File, data: ByteArray) {
        val parent = file.absoluteFile.parentFile
        require(parent.isDirectory || parent.mkdirs()) { "无法创建数据目录" }
        val temp = File.createTempFile(".collecter-", ".tmp", parent)
        try {
            FileOutputStream(temp).use { it.write(data); it.fd.sync() }
            // Fail closed if this filesystem cannot atomically replace; never fall back to truncating live data.
            Files.move(temp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } finally {
            Files.deleteIfExists(temp.toPath())
        }
    }

    fun sha256(data: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(data)
        .joinToString("") { "%02x".format(it.toInt() and 255) }

    private fun visit(value: Any, transform: (String, String) -> String) {
        when (value) {
            is JSONObject -> for (key in value.keys().asSequence().toList()) {
                if (key == "assets") continue
                val child = value.get(key)
                if (child is String && (key in pathKeys || child.startsWith("asset:"))) value.put(key, transform(key, child))
                else if (child is JSONArray && key in pathKeys) {
                    for (i in 0 until child.length()) child.put(i, transform(key, child.getString(i)))
                }
                else visit(child, transform)
            }
            is JSONArray -> for (i in 0 until value.length()) visit(value.get(i), transform)
        }
    }
}
