package com.kfaino.diapertracker

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.wifi.WifiManager
import android.os.Build
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.Collections
import java.util.concurrent.Executors

/**
 * 局域网免配置极速互传引擎 (LAN P2P WiFi Sync)
 * - 手机端内置轻量 HTTP 微服务 (Port 8848)
 * - 支持电脑/另一台手机通过局域网 IP 免配置极速双向同步与数据合并
 */
object LanSyncHelper {

    const val DEFAULT_PORT = 8848
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private val executor = Executors.newCachedThreadPool()

    fun getLocalIpAddress(context: Context): String {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (wifiManager != null && wifiManager.isWifiEnabled) {
                val ipInt = wifiManager.connectionInfo.ipAddress
                if (ipInt != 0) {
                    return Formatter.formatIpAddress(ipInt)
                }
            }

            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr.hostAddress.indexOf(':') < 0) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "127.0.0.1"
    }

    @Synchronized
    fun startServer(context: Context, onDataReceived: (String) -> Unit): Int {
        if (isRunning && serverSocket != null) return DEFAULT_PORT

        try {
            serverSocket = ServerSocket(DEFAULT_PORT)
            isRunning = true

            executor.execute {
                while (isRunning && serverSocket != null) {
                    try {
                        val client = serverSocket!!.accept()
                        handleClient(client, context, onDataReceived)
                    } catch (e: Exception) {
                        if (!isRunning) break
                    }
                }
            }
            return DEFAULT_PORT
        } catch (e: Exception) {
            e.printStackTrace()
            return -1
        }
    }

    @Synchronized
    fun stopServer() {
        isRunning = false
        try {
            serverSocket?.close()
            serverSocket = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleClient(client: Socket, context: Context, onDataReceived: (String) -> Unit) {
        executor.execute {
            try {
                val reader = BufferedReader(InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8))
                val out: OutputStream = client.getOutputStream()

                val requestLine = reader.readLine() ?: return@execute
                val parts = requestLine.split(" ")
                if (parts.size < 2) return@execute
                val method = parts[0]
                val path = parts[1]

                // 读取 Header
                var contentLength = 0
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (line!!.isEmpty()) break
                    if (line!!.lowercase().startsWith("content-length:")) {
                        contentLength = line!!.substring(15).trim().toIntOrNull() ?: 0
                    }
                }

                val store = DataStore(context)

                when {
                    method == "GET" && (path == "/api/pull" || path == "/backup") -> {
                        // 导出完整备份
                        val json = store.exportBackupJson()
                        val bytes = json.toByteArray(StandardCharsets.UTF_8)
                        val header = "HTTP/1.1 200 OK\r\nContent-Type: application/json; charset=utf-8\r\nContent-Length: ${bytes.size}\r\nAccess-Control-Allow-Origin: *\r\n\r\n"
                        out.write(header.toByteArray(StandardCharsets.UTF_8))
                        out.write(bytes)
                        out.flush()
                    }

                    method == "POST" && (path == "/api/push" || path == "/sync") -> {
                        // 接收远程推送的数据
                        val buffer = CharArray(contentLength)
                        var read = 0
                        while (read < contentLength) {
                            val r = reader.read(buffer, read, contentLength - read)
                            if (r == -1) break
                            read += r
                        }
                        val receivedJson = String(buffer)
                        (context as? Activity)?.runOnUiThread {
                            onDataReceived(receivedJson)
                        }

                        val resp = "{\"status\":\"ok\",\"message\":\"数据已接收并成功合并\"}".toByteArray(StandardCharsets.UTF_8)
                        val header = "HTTP/1.1 200 OK\r\nContent-Type: application/json; charset=utf-8\r\nContent-Length: ${resp.size}\r\nAccess-Control-Allow-Origin: *\r\n\r\n"
                        out.write(header.toByteArray(StandardCharsets.UTF_8))
                        out.write(resp)
                        out.flush()
                    }

                    else -> {
                        val status = JSONObject().apply {
                            put("app", "Collecter")
                            put("device", Build.MODEL)
                            put("version", "2.8.0")
                            put("totalEntries", store.loadAll().size)
                        }.toString().toByteArray(StandardCharsets.UTF_8)

                        val header = "HTTP/1.1 200 OK\r\nContent-Type: application/json; charset=utf-8\r\nContent-Length: ${status.size}\r\nAccess-Control-Allow-Origin: *\r\n\r\n"
                        out.write(header.toByteArray(StandardCharsets.UTF_8))
                        out.write(status)
                        out.flush()
                    }
                }

                client.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /** 弹出局域网互传面板与配对二维码 */
    fun showLanSyncDialog(activity: Activity, store: DataStore, onSyncCompleted: () -> Unit) {
        val ip = getLocalIpAddress(activity)
        startServer(activity) { receivedJson ->
            val count = store.importBackupJson(receivedJson)
            Toast.makeText(activity, "⚡ 局域网极速同步成功！已合并 $count 条资产记录", Toast.LENGTH_LONG).show()
            onSyncCompleted()
        }

        val url = "http://$ip:$DEFAULT_PORT/api/pull"
        val qrBitmap = generateQrCode(url, 480)

        val dialogView = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 40, 48, 40)
            gravity = android.view.Gravity.CENTER_HORIZONTAL
        }

        val titleTv = TextView(activity).apply {
            text = "⚡ 局域网免密极速互传"
            textSize = 18f
            setTextColor(Color.WHITE)
            paint.isFakeBoldText = true
        }

        val descTv = TextView(activity).apply {
            text = "确保电脑或另一台设备与手机连接至同一 Wi-Fi：\n直接在电脑浏览器打开或通过桌面端扫描同步！"
            textSize = 12f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, 12, 0, 16)
        }

        val qrIv = ImageView(activity).apply {
            layoutParams = LinearLayout.LayoutParams(400, 400)
            setImageBitmap(qrBitmap)
            setBackgroundColor(Color.WHITE)
            setPadding(12, 12, 12, 12)
        }

        val ipTv = TextView(activity).apply {
            text = "服务地址: $url"
            textSize = 13f
            setTextColor(Color.parseColor("#10B981"))
            paint.isFakeBoldText = true
            setPadding(0, 16, 0, 8)
        }

        val btnCopy = Button(activity).apply {
            text = "📋 复制链接发给电脑"
            setOnClickListener {
                val cm = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Collecter LAN Sync", url))
                Toast.makeText(activity, "已复制局域网同步地址到剪贴板！", Toast.LENGTH_SHORT).show()
            }
        }

        val btnPullFromOther = Button(activity).apply {
            text = "📥 从其他设备 IP 拉取数据"
            setOnClickListener {
                showPullFromOtherDialog(activity, store, onSyncCompleted)
            }
        }

        dialogView.addView(titleTv)
        dialogView.addView(descTv)
        dialogView.addView(qrIv)
        dialogView.addView(ipTv)
        dialogView.addView(btnCopy)
        dialogView.addView(btnPullFromOther)

        MaterialAlertDialogBuilder(activity)
            .setView(dialogView)
            .setPositiveButton("完成", null)
            .setOnDismissListener { stopServer() }
            .show()
    }

    private fun showPullFromOtherDialog(activity: Activity, store: DataStore, onSyncCompleted: () -> Unit) {
        val input = EditText(activity).apply {
            hint = "例如: 192.168.1.120:8848"
            setPadding(36, 28, 36, 28)
        }

        MaterialAlertDialogBuilder(activity)
            .setTitle("📥 从目标设备拉取数据")
            .setMessage("输入另一台开启互传的手机或电脑 IP 地址：")
            .setView(input)
            .setPositiveButton("开始拉取") { _, _ ->
                val target = input.text.toString().trim()
                if (target.isNotEmpty()) {
                    val fullUrl = if (!target.startsWith("http")) "http://$target/api/pull" else target
                    executor.execute {
                        try {
                            val conn = URI.create(fullUrl).toURL().openConnection() as HttpURLConnection
                            conn.connectTimeout = 6000
                            conn.readTimeout = 6000
                            val json = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                            activity.runOnUiThread {
                                val count = store.importBackupJson(json)
                                Toast.makeText(activity, "拉取成功！已同步 $count 条记录", Toast.LENGTH_SHORT).show()
                                onSyncCompleted()
                            }
                        } catch (e: Exception) {
                            activity.runOnUiThread {
                                Toast.makeText(activity, "拉取失败: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun generateQrCode(text: String, size: Int): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bmp
    }
}
