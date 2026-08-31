package com.kfaino.collector.desktop.server

import com.kfaino.collector.desktop.models.*
import com.kfaino.collector.desktop.storage.DesktopDataStore
import com.kfaino.collector.desktop.storage.DesktopSyncMergeEngine
import com.kfaino.collector.desktop.storage.DesktopVaultAlertAggregator
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import org.json.JSONArray
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

/**
 * 桌面端嵌入式轻量 HTTP 引擎 (Port 8848) & 局域网广播自发现 (Port 8849)
 *
 * 为 Native WebView2 宿主外壳和同局域网设备提供统一的 REST API 与现代化 Web 大屏资产工作台。
 */
class EmbeddedWebServer(
    private val store: DesktopDataStore,
    private val port: Int = 8848
) {
    private var server: HttpServer? = null
    private var udpSocket: DatagramSocket? = null
    private var isAnnouncing = false
    private val executor = Executors.newCachedThreadPool()

    fun start() {
        if (server != null) return
        try {
            server = HttpServer.create(InetSocketAddress("0.0.0.0", port), 0).apply {
                executor = this@EmbeddedWebServer.executor

                // 1. 健康探测端点（供 WebView2 启动页探测）
                createContext("/api/v1/health", HealthHandler())

                // 2. 核心资产 API
                createContext("/api/v1/entries", EntriesHandler(store))

                // 2.1 灵感想法与智能剪藏 API
                createContext("/api/v1/ideas", IdeasHandler(store))
                createContext("/api/v1/clippings", ClippingsHandler(store))

                // 2.2 全维联合搜索 API (Spotlight 检索服务)
                createContext("/api/v1/search", SearchHandler(store))

                // 3. 12 馆时效预警 API
                createContext("/api/v1/vaults/alerts", AlertsHandler(store))

                // 4. 全量备份导入导出与拉取
                createContext("/api/v1/backup/export", BackupExportHandler(store))
                createContext("/api/v1/backup/import", BackupImportHandler(store))
                createContext("/api/pull", BackupExportHandler(store))
                createContext("/backup", BackupExportHandler(store))

                // 5. P2P 增量对撞合并端点
                createContext("/api/v1/sync/merge", SyncMergeHandler(store))
                createContext("/api/merge", SyncMergeHandler(store))

                // 6. 现代化 Web 大屏前端
                createContext("/", DashboardHandler(store))

                start()
            }
            startUdpAnnouncer()
            println("Collecter Embedded Server started on http://127.0.0.1:$port/")
        } catch (e: Exception) {
            System.err.println("启动嵌入式服务失败: ${e.message}")
        }
    }

    fun stop() {
        stopUdpAnnouncer()
        server?.stop(0)
        server = null
    }

    private fun startUdpAnnouncer() {
        if (isAnnouncing) return
        try {
            udpSocket = DatagramSocket(8849).apply { broadcast = true }
            isAnnouncing = true
            executor.execute {
                val buf = ByteArray(512)
                val packet = DatagramPacket(buf, buf.size)
                while (isAnnouncing && udpSocket != null) {
                    try {
                        udpSocket!!.receive(packet)
                        val msg = String(packet.data, 0, packet.length, StandardCharsets.UTF_8).trim()
                        if (msg == "COLLECTER_DISCOVERY_PING") {
                            val pong = "COLLECTER_DISCOVERY_PONG:Collecter Desktop (PC):$port".toByteArray(StandardCharsets.UTF_8)
                            val respPacket = DatagramPacket(pong, pong.size, packet.address, packet.port)
                            udpSocket!!.send(respPacket)
                        }
                    } catch (e: Exception) {
                        if (!isAnnouncing) break
                    }
                }
            }
        } catch (e: Exception) {
            System.err.println("启动局域网 UDP 发现应答失败: ${e.message}")
        }
    }

    private fun stopUdpAnnouncer() {
        isAnnouncing = false
        try {
            udpSocket?.close()
            udpSocket = null
        } catch (e: Exception) {
            System.err.println("关闭 UDP Announcer 失败: ${e.message}")
        }
    }

    private class HealthHandler : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            val resp = JSONObject()
                .put("status", "UP")
                .put("app", "Collecter")
                .put("version", "4.3.6")
                .put("timestamp", System.currentTimeMillis())
                .toString()
            sendJsonResponse(exchange, 200, resp)
        }
    }

    private class SyncMergeHandler(private val store: DesktopDataStore) : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            if (exchange.requestMethod.uppercase() != "POST") {
                sendJsonResponse(exchange, 405, "Method Not Allowed")
                return
            }
            val body = exchange.requestBody.reader(StandardCharsets.UTF_8).readText()
            val report = DesktopSyncMergeEngine.merge(store, body)
            val resp = JSONObject().apply {
                put("status", if (report.success) "ok" else "error")
                put("inserted", report.insertedEntries)
                put("updated", report.updatedEntries)
                put("preserved", report.preservedEntries)
                put("mergedVaults", report.mergedVaultItems)
                put("message", report.summary())
            }.toString()
            sendJsonResponse(exchange, if (report.success) 200 else 400, resp)
        }
    }

    private class EntriesHandler(private val store: DesktopDataStore) : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            when (exchange.requestMethod.uppercase()) {
                "GET" -> {
                    val entries = store.loadAll()
                    val arr = JSONArray()
                    for (e in entries) {
                        arr.put(JSONObject()
                            .put("id", e.id)
                            .put("brand", e.brand)
                            .put("category", e.category)
                            .put("price", e.price)
                            .put("qty", e.qty)
                            .put("unit", e.unit)
                            .put("location", e.location)
                            .put("isIn", e.isIn)
                            .put("dailyCost", e.getDailyCost())
                        )
                    }
                    sendJsonResponse(exchange, 200, arr.toString())
                }
                "POST" -> {
                    val body = exchange.requestBody.reader(StandardCharsets.UTF_8).readText()
                    try {
                        val obj = JSONObject(body)
                        val entry = Entry(
                            brand = obj.optString("brand", ""),
                            category = obj.optString("category", "日用品"),
                            price = obj.optDouble("price", 0.0),
                            qty = obj.optInt("qty", 1),
                            unit = obj.optString("unit", "件"),
                            location = obj.optString("location", "")
                        )
                        store.addEntry(entry)
                        sendJsonResponse(exchange, 201, JSONObject().put("success", true).put("id", entry.id).toString())
                    } catch (e: Exception) {
                        sendJsonResponse(exchange, 400, JSONObject().put("error", e.message).toString())
                    }
                }
                else -> sendJsonResponse(exchange, 405, "Method Not Allowed")
            }
        }
    }

    private class IdeasHandler(private val store: DesktopDataStore) : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            when (exchange.requestMethod.uppercase()) {
                "GET" -> {
                    val ideas = store.getIdeas()
                    val arr = JSONArray()
                    for (i in ideas) {
                        arr.put(JSONObject()
                            .put("id", i.id)
                            .put("content", i.content)
                            .put("moodEmoji", i.moodEmoji)
                            .put("isPinned", i.isPinned)
                            .put("colorHex", i.colorHex)
                            .put("tags", JSONArray(i.tags))
                            .put("linkedAssetIds", JSONArray(i.linkedAssetIds))
                            .put("createdAt", i.createdAt)
                        )
                    }
                    sendJsonResponse(exchange, 200, arr.toString())
                }
                "POST" -> {
                    val body = exchange.requestBody.reader(StandardCharsets.UTF_8).readText()
                    try {
                        val obj = JSONObject(body)
                        val idea = IdeaRecord(
                            content = obj.optString("content", ""),
                            moodEmoji = obj.optString("moodEmoji", "💡"),
                            colorHex = obj.optString("colorHex", "#10B981")
                        )
                        store.addOrUpdateIdea(idea)
                        sendJsonResponse(exchange, 201, JSONObject().put("success", true).put("id", idea.id).toString())
                    } catch (e: Exception) {
                        sendJsonResponse(exchange, 400, JSONObject().put("error", e.message).toString())
                    }
                }
                else -> sendJsonResponse(exchange, 405, "Method Not Allowed")
            }
        }
    }

    private class ClippingsHandler(private val store: DesktopDataStore) : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            when (exchange.requestMethod.uppercase()) {
                "GET" -> {
                    val clippings = store.getClippings()
                    val arr = JSONArray()
                    for (c in clippings) {
                        arr.put(JSONObject()
                            .put("id", c.id)
                            .put("title", c.title)
                            .put("summary", c.summary)
                            .put("sourcePlatform", c.sourcePlatform)
                            .put("originalUrl", c.originalUrl)
                            .put("tags", JSONArray(c.tags))
                            .put("linkedAssetIds", JSONArray(c.linkedAssetIds))
                            .put("capturedAt", c.capturedAt)
                        )
                    }
                    sendJsonResponse(exchange, 200, arr.toString())
                }
                "POST" -> {
                    val body = exchange.requestBody.reader(StandardCharsets.UTF_8).readText()
                    try {
                        val obj = JSONObject(body)
                        val clip = ClippingRecord(
                            title = obj.optString("title", ""),
                            summary = obj.optString("summary", ""),
                            originalUrl = obj.optString("originalUrl", ""),
                            sourcePlatform = obj.optString("sourcePlatform", "web")
                        )
                        store.addOrUpdateClipping(clip)
                        sendJsonResponse(exchange, 201, JSONObject().put("success", true).put("id", clip.id).toString())
                    } catch (e: Exception) {
                        sendJsonResponse(exchange, 400, JSONObject().put("error", e.message).toString())
                    }
                }
                else -> sendJsonResponse(exchange, 405, "Method Not Allowed")
            }
        }
    }

    private class SearchHandler(private val store: DesktopDataStore) : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            val query = exchange.requestURI.query ?: ""
            val q = query.split("&").firstOrNull { it.startsWith("q=") }?.substring(2)?.lowercase()?.trim() ?: ""
            val decodedQ = try { java.net.URLDecoder.decode(q, "UTF-8") } catch (e: Exception) { q }

            val arr = JSONArray()
            if (decodedQ.isNotBlank()) {
                // 1. 实体资产
                store.loadAll().filter {
                    it.brand.lowercase().contains(decodedQ) ||
                    it.category.lowercase().contains(decodedQ) ||
                    it.location.lowercase().contains(decodedQ) ||
                    it.notes.lowercase().contains(decodedQ)
                }.forEach {
                    arr.put(JSONObject()
                        .put("type", "asset")
                        .put("emoji", "📦")
                        .put("id", it.id)
                        .put("title", it.brand)
                        .put("subtitle", "${it.category} · ¥${it.price} · ${it.location}")
                    )
                }

                // 2. 灵感想法
                store.getIdeas().filter {
                    it.content.lowercase().contains(decodedQ) ||
                    it.tags.any { t -> t.lowercase().contains(decodedQ) }
                }.forEach {
                    arr.put(JSONObject()
                        .put("type", "idea")
                        .put("emoji", it.moodEmoji)
                        .put("id", it.id)
                        .put("title", it.getPreview(25))
                        .put("subtitle", "灵感想法 · ${it.tags.joinToString(" ")}")
                    )
                }

                // 3. 智能剪藏
                store.getClippings().filter {
                    it.getSearchableContent().lowercase().contains(decodedQ)
                }.forEach {
                    arr.put(JSONObject()
                        .put("type", "clipping")
                        .put("emoji", "📰")
                        .put("id", it.id)
                        .put("title", it.title.ifBlank { it.summary }.take(25))
                        .put("subtitle", "剪藏文章 · ${it.sourcePlatform}")
                    )
                }
            }

            sendJsonResponse(exchange, 200, arr.toString())
        }
    }

    private class AlertsHandler(private val store: DesktopDataStore) : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            val alerts = DesktopVaultAlertAggregator.aggregate(store)
            val arr = JSONArray()
            for (a in alerts) {
                arr.put(JSONObject()
                    .put("emoji", a.emoji)
                    .put("vaultName", a.vaultName)
                    .put("label", a.label)
                    .put("urgencyDays", a.urgencyDays)
                    .put("isExpired", a.isExpired)
                )
            }
            sendJsonResponse(exchange, 200, arr.toString())
        }
    }

    private class BackupExportHandler(private val store: DesktopDataStore) : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            val json = store.exportJson()
            val bytes = json.toByteArray(StandardCharsets.UTF_8)
            exchange.responseHeaders.set("Content-Type", "application/json; charset=UTF-8")
            exchange.responseHeaders.set("Content-Disposition", "attachment; filename=\"Collecter_Backup.json\"")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
    }

    private class BackupImportHandler(private val store: DesktopDataStore) : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            if (exchange.requestMethod.uppercase() != "POST") {
                sendJsonResponse(exchange, 405, "Method Not Allowed")
                return
            }
            val body = exchange.requestBody.reader(StandardCharsets.UTF_8).readText()
            val ok = store.importJson(body)
            sendJsonResponse(exchange, if (ok) 200 else 400, JSONObject().put("success", ok).toString())
        }
    }

    private class DashboardHandler(private val store: DesktopDataStore) : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            val entries = store.loadAll()
            val alerts = DesktopVaultAlertAggregator.aggregate(store)
            val totalAssets = entries.filter { it.isIn && !it.isRetired }.sumOf { it.price * it.qty }
            val count = entries.filter { it.isIn && !it.isRetired }.size

            val alertItemsHtml = if (alerts.isEmpty()) {
                "<div style='color:#10b981;padding:12px;text-align:center;'>✅ 今日全家 12 馆物资时效 100% 正常，无待处理事项！</div>"
            } else {
                alerts.joinToString("") { a ->
                    val color = if (a.isExpired) "#ef4444" else "#f59e0b"
                    "<div style='display:flex;justify-content:space-between;padding:10px 14px;margin-bottom:8px;background:rgba(255,255,255,0.04);border-radius:10px;border-left:4px solid $color;'>" +
                            "<span>${a.emoji} <strong>${a.vaultName}</strong>: ${a.label}</span>" +
                            "<span style='color:$color;font-weight:600;'>${if (a.isExpired) "已超期" else "${a.urgencyDays}天内"}</span>" +
                            "</div>"
                }
            }

            val tableRows = entries.take(50).joinToString("") { e ->
                "<tr>" +
                        "<td><strong>${e.brand}</strong></td>" +
                        "<td><span style='background:rgba(16,185,129,0.15);color:#34d399;padding:2px 8px;border-radius:6px;'>${e.category}</span></td>" +
                        "<td>¥${String.format("%.2f", e.price)}</td>" +
                        "<td>${e.qty}${e.unit}</td>" +
                        "<td>${e.location}</td>" +
                        "<td>${if (e.isRetired) "<span style='color:#94a3b8;'>已退役</span>" else "<span style='color:#10b981;'>在役中</span>"}</td>" +
                        "</tr>"
            }

            val html = """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Collecter — 资产与收纳管家</title>
                    <style>
                        :root { color-scheme: dark; }
                        body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Microsoft YaHei", sans-serif; background: #0b1311; color: #e2e8f0; margin: 0; padding: 24px; }
                        .container { max-width: 1100px; margin: 0 auto; }
                        .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; padding-bottom: 16px; border-bottom: 1px solid rgba(255,255,255,0.08); }
                        .logo { display: flex; align-items: center; gap: 12px; font-size: 22px; font-weight: 700; color: #10b981; }
                        .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 16px; margin-bottom: 24px; }
                        .stat-card { background: #13221e; border: 1px solid rgba(16,185,129,0.2); border-radius: 14px; padding: 18px; box-shadow: 0 4px 20px rgba(0,0,0,0.3); }
                        .stat-val { font-size: 28px; font-weight: 700; color: #34d399; margin-top: 6px; }
                        .section-title { font-size: 17px; font-weight: 600; margin-bottom: 12px; display: flex; align-items: center; gap: 8px; }
                        table { width: 100%; border-collapse: collapse; background: #13221e; border-radius: 12px; overflow: hidden; }
                        th, td { padding: 12px 16px; text-align: left; border-bottom: 1px solid rgba(255,255,255,0.06); font-size: 14px; }
                        th { background: rgba(0,0,0,0.2); color: #94a3b8; font-weight: 600; }
                        .btn { background: #10b981; color: #022c22; padding: 8px 16px; border-radius: 8px; text-decoration: none; font-weight: 600; display: inline-flex; align-items: center; gap: 6px; border: none; cursor: pointer; }
                        .btn:hover { background: #34d399; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <div class="logo"><span>📦</span> Collecter 资产与收纳管家</div>
                            <div>
                                <a href="/api/v1/backup/export" class="btn">📥 导出备份 JSON</a>
                            </div>
                        </div>

                        <div class="stats-grid">
                            <div class="stat-card">
                                <div style="color:#94a3b8;font-size:13px;">在库资产总估值</div>
                                <div class="stat-val">¥${String.format("%.2f", totalAssets)}</div>
                            </div>
                            <div class="stat-card">
                                <div style="color:#94a3b8;font-size:13px;">在库物资总件数</div>
                                <div class="stat-val">${count} <span style="font-size:14px;font-weight:normal;color:#94a3b8;">件</span></div>
                            </div>
                            <div class="stat-card">
                                <div style="color:#94a3b8;font-size:13px;">12 馆时效待办事项</div>
                                <div class="stat-val" style="color:${if (alerts.isEmpty()) "#10b981" else "#f59e0b"};">${alerts.size} <span style="font-size:14px;font-weight:normal;color:#94a3b8;">项</span></div>
                            </div>
                        </div>

                        <div style="margin-bottom:28px;">
                            <div class="section-title">⚠️ 今日 12 馆时效生命线看板</div>
                            <div style="background:#13221e;border:1px solid rgba(255,255,255,0.08);border-radius:14px;padding:16px;">
                                $alertItemsHtml
                            </div>
                        </div>

                        <div>
                            <div class="section-title">📊 最近物资资产列表</div>
                            <table>
                                <thead>
                                    <tr>
                                        <th>品名/品牌</th>
                                        <th>分类</th>
                                        <th>单价</th>
                                        <th>数量</th>
                                        <th>存放位置</th>
                                        <th>状态</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    $tableRows
                                </tbody>
                            </table>
                        </div>
                    </div>
                </body>
                </html>
            """.trimIndent()

            val bytes = html.toByteArray(StandardCharsets.UTF_8)
            exchange.responseHeaders.set("Content-Type", "text/html; charset=UTF-8")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
    }

    companion object {
        private fun sendJsonResponse(exchange: HttpExchange, statusCode: Int, json: String) {
            val bytes = json.toByteArray(StandardCharsets.UTF_8)
            exchange.responseHeaders.set("Content-Type", "application/json; charset=UTF-8")
            exchange.sendResponseHeaders(statusCode, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
    }
}
