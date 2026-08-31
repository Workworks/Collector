package com.kfaino.diapertracker

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException

/**
 * 📡 局域网 HTTP 互传服务器 (Port 8848)
 * - 同网段设备通过浏览器访问 http://<本机IP>:8848 即可查看资产大屏
 * - GET /       → HTML 资产大屏概览
 * - GET /backup → 下载完整备份 JSON（Collecter_Backup.json）
 * - GET /ping   → 连通性测试（返回 {"status":"ok"}）
 * - 零第三方依赖，基于 Java 原生 ServerSocket 实现
 * - 数据 100% 本地离线，仅局域网私有访问
 */
class LanShareServer(private val context: Context, private val store: DataStore) {

    val accessToken = com.kfaino.collecter.core.LanHttp.token()
    private val tag = "LanShareServer"
    private val port = 8848

    private var serverSocket: ServerSocket? = null
    @Volatile private var running = false
    private var serverThread: Thread? = null

    /**
     * 启动局域网 HTTP 服务器
     * @return true=启动成功，false=端口占用或权限不足
     */
    fun start(): Boolean {
        if (running) return true
        return try {
            serverSocket = ServerSocket(port)
            running = true
            serverThread = Thread(::acceptLoop, "LanShareServer-Thread").also {
                it.isDaemon = true
                it.start()
            }
            Log.i(tag, "局域网服务器已启动，监听端口 $port")
            true
        } catch (e: Exception) {
            Log.w(tag, "启动局域网服务器失败", e)
            false
        }
    }

    /** 停止局域网 HTTP 服务器 */
    fun stop() {
        running = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            // finally 中关闭 ServerSocket，SocketException 属正常终止，可安全忽略
            Log.w(tag, "关闭 ServerSocket 时异常（可忽略）", e)
        }
        serverSocket = null
        serverThread = null
        Log.i(tag, "局域网服务器已停止")
    }

    fun isRunning(): Boolean = running

    private fun acceptLoop() {
        while (running) {
            try {
                val socket = serverSocket?.accept() ?: break
                Thread { handleClient(socket) }.also { it.isDaemon = true }.start()
            } catch (e: SocketException) {
                if (!running) break  // 正常停止触发的 SocketException，不是错误
                Log.w(tag, "接受客户端连接时发生 SocketException", e)
            } catch (e: Exception) {
                Log.w(tag, "acceptLoop 异常", e)
            }
        }
    }

    private fun handleClient(socket: Socket) {
        try {
            socket.soTimeout = 5000
            val request = com.kfaino.collecter.core.LanHttp.readHeaders(socket.getInputStream())
            val writer = PrintWriter(socket.getOutputStream(), true, Charsets.UTF_8)
            if (request.method != "GET" || !com.kfaino.collecter.core.LanHttp.authorize(request, accessToken)) {
                writer.print(com.kfaino.collecter.core.LanHttp.UNAUTHORIZED)
                writer.flush()
                return
            }
            val path = request.path
            when (path) {
                "/ping" -> {
                    val body = "{\"status\":\"ok\"}"
                    writer.print(buildHttpResponse("application/json", body))
                    writer.flush()
                }
                "/backup" -> {
                    val json = store.exportBackupJson()
                    val extraHeaders = "Content-Disposition: attachment; filename=\"Collecter_Backup.json\"\r\n"
                    writer.print(buildHttpResponse("application/json; charset=utf-8", json, extraHeaders))
                    writer.flush()
                }
                "/", "/index.html" -> {
                    val html = buildHtmlPage()
                    writer.print(buildHttpResponse("text/html; charset=utf-8", html))
                    writer.flush()
                }
                else -> {
                    writer.print("HTTP/1.1 404 Not Found\r\nContent-Length: 9\r\nConnection: close\r\n\r\nNot Found")
                    writer.flush()
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "处理 HTTP 客户端请求失败", e)
        } finally {
            try {
                socket.close()
            } catch (e: Exception) {
                // finally 中关闭 socket，SocketException 属正常操作，可安全忽略
                Log.w(tag, "关闭客户端 socket 时异常（可忽略）", e)
            }
        }
    }

    private fun buildHttpResponse(contentType: String, body: String, extraHeaders: String = ""): String {
        val bodyBytes = body.toByteArray(Charsets.UTF_8)
        return buildString {
            append("HTTP/1.1 200 OK\r\n")
            append("Content-Type: $contentType\r\n")
            append("Content-Length: ${bodyBytes.size}\r\n")
            append("Cache-Control: no-store\r\nX-Content-Type-Options: nosniff\r\n")
            if (extraHeaders.isNotBlank()) append(extraHeaders)
            append("Connection: close\r\n")
            append("\r\n")
            append(body)
        }
    }

    private fun buildHtmlPage(): String {
        val entries = try {
            store.loadAll()
        } catch (e: Exception) {
            Log.w(tag, "构建大屏页时加载资产失败", e)
            emptyList()
        }
        val totalCount = entries.filter { !it.isRetired }.size
        val totalValue = entries.filter { !it.isRetired }.sumOf { it.qty * it.price }
        return """
            <!DOCTYPE html>
            <html lang="zh-CN">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width,initial-scale=1">
              <title>Collecter 局域网大屏</title>
              <style>
                body { font-family: -apple-system, sans-serif; background: #059669; color: #fff; padding: 20px; margin: 0; }
                .card { background: rgba(255,255,255,0.15); border-radius: 16px; padding: 20px; margin: 12px 0; }
                h1 { margin: 0 0 4px; font-size: 24px; }
                .sub { opacity: 0.8; font-size: 13px; margin-bottom: 20px; }
                .stat { font-size: 28px; font-weight: bold; }
                .label { font-size: 12px; opacity: 0.8; margin-top: 4px; }
                a { color: #A7F3D0; text-decoration: none; border-bottom: 1px solid rgba(167,243,208,0.5); }
                footer { margin-top: 24px; font-size: 11px; opacity: 0.6; text-align: center; }
              </style>
            </head>
            <body>
              <h1>📦 Collecter 局域网大屏</h1>
              <p class="sub">同 Wi-Fi 访问 · 数据 100% 本地离线</p>
              <div class="card">
                <div class="stat">${totalCount} 件</div>
                <div class="label">在库资产总数</div>
              </div>
              <div class="card">
                <div class="stat">¥${String.format("%.0f", totalValue)}</div>
                <div class="label">资产总价值估算</div>
              </div>
              <div class="card">
                <b>⬇️ 数据下载</b><br><br>
                <a href="/backup">📥 下载完整备份 JSON（Collecter_Backup.json）</a>
              </div>
              <footer>Collecter · 局域网服务器 · Port $port · 关闭 App 中的互传面板即停止服务</footer>
            </body>
            </html>
        """.trimIndent()
    }
}
