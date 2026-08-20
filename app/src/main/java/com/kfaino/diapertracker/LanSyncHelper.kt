package com.kfaino.diapertracker

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.wifi.WifiManager
import android.os.Build
import android.text.format.Formatter
import android.widget.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.*
import java.nio.charset.StandardCharsets
import java.util.Collections
import java.util.Locale
import java.util.concurrent.Executors

/**
 * 局域网免配置极速互传 & 内置免安装 Web 网页端资产控制台 (Port 8848)
 * - 电脑/iPad 浏览器访问 http://手机IP:8848/ 即可直接打开大屏 Web 控制台
 * - 支持网页端大屏批量改位置、批量删除、快速记一笔与实时同步
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

                var contentLength = 0
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (line!!.isEmpty()) break
                    if (line!!.lowercase().startsWith("content-length:")) {
                        contentLength = line!!.substring(15).trim().toIntOrNull() ?: 0
                    }
                }

                fun readBody(): String {
                    if (contentLength <= 0) return ""
                    val buffer = CharArray(contentLength)
                    var read = 0
                    while (read < contentLength) {
                        val r = reader.read(buffer, read, contentLength - read)
                        if (r == -1) break
                        read += r
                    }
                    return String(buffer)
                }

                val store = DataStore(context)

                when {
                    // 1. Web 网页端主控制台 HTML (优先读取热补丁版本)
                    method == "GET" && (path == "/" || path == "/index.html") -> {
                        val patchedHtml = HotPatchEngine.getActiveWebDashboardHtml(context)
                        val html = patchedHtml ?: buildWebDashboardHtml(store)
                        sendHttpResponse(out, "text/html; charset=utf-8", html.toByteArray(StandardCharsets.UTF_8))
                    }

                    // 2. REST API: 获取所有物品列表
                    method == "GET" && path == "/api/entries" -> {
                        val all = store.loadAll()
                        val arr = JSONArray()
                        for (e in all) {
                            arr.put(JSONObject().apply {
                                put("id", e.id)
                                put("brand", e.brand)
                                put("category", e.category)
                                put("price", e.price)
                                put("qty", e.qty)
                                put("unit", e.unit)
                                put("location", e.location)
                                put("houseName", e.houseName)
                                put("roomName", e.roomName)
                                put("isIn", e.isIn)
                                put("isRetired", e.isRetired)
                                put("daysOwned", e.getDaysOwned())
                                put("dailyCost", e.getDailyCost())
                                put("notes", e.notes)
                            })
                        }
                        sendHttpResponse(out, "application/json; charset=utf-8", arr.toString().toByteArray(StandardCharsets.UTF_8))
                    }

                    // 3. REST API: 网页端添加物品
                    method == "POST" && path == "/api/entry/add" -> {
                        val body = readBody()
                        val json = JSONObject(body)
                        val newEntry = Entry(
                            brand = json.optString("brand", "新物品"),
                            category = json.optString("category", "日用品"),
                            price = json.optDouble("price", 0.0),
                            qty = json.optInt("qty", 1),
                            unit = json.optString("unit", "件"),
                            location = json.optString("location", ""),
                            notes = json.optString("notes", ""),
                            isIn = json.optBoolean("isIn", true)
                        )
                        val all = store.loadAll().toMutableList()
                        all.add(0, newEntry)
                        store.saveAll(all)
                        (context as? Activity)?.runOnUiThread { onDataReceived("") }
                        sendHttpResponse(out, "application/json", "{\"status\":\"ok\",\"id\":\"${newEntry.id}\"}".toByteArray())
                    }

                    // 4. REST API: 网页端批量移动放置位置
                    method == "POST" && path == "/api/entries/batch_move" -> {
                        val body = readBody()
                        val json = JSONObject(body)
                        val idsArr = json.optJSONArray("ids") ?: JSONArray()
                        val targetLoc = json.optString("location", "")
                        val targetRoom = json.optString("roomName", "")
                        val idSet = (0 until idsArr.length()).map { idsArr.getString(it) }.toSet()

                        val all = store.loadAll().map { e ->
                            if (idSet.contains(e.id)) {
                                e.copy(location = targetLoc, roomName = targetRoom)
                            } else e
                        }
                        store.saveAll(all)
                        (context as? Activity)?.runOnUiThread { onDataReceived("") }
                        sendHttpResponse(out, "application/json", "{\"status\":\"ok\",\"count\":${idSet.size}}".toByteArray())
                    }

                    // 5. REST API: 网页端批量删除
                    method == "POST" && path == "/api/entries/batch_delete" -> {
                        val body = readBody()
                        val json = JSONObject(body)
                        val idsArr = json.optJSONArray("ids") ?: JSONArray()
                        val idSet = (0 until idsArr.length()).map { idsArr.getString(it) }.toSet()

                        val all = store.loadAll().filterNot { idSet.contains(it.id) }
                        store.saveAll(all)
                        (context as? Activity)?.runOnUiThread { onDataReceived("") }
                        sendHttpResponse(out, "application/json", "{\"status\":\"ok\",\"deleted\":${idSet.size}}".toByteArray())
                    }

                    // 6. 数据备份导出与互传拉取
                    method == "GET" && (path == "/api/pull" || path == "/backup") -> {
                        val json = store.exportBackupJson()
                        sendHttpResponse(out, "application/json; charset=utf-8", json.toByteArray(StandardCharsets.UTF_8))
                    }

                    // 7. 接收远程推送数据
                    method == "POST" && (path == "/api/push" || path == "/sync") -> {
                        val body = readBody()
                        (context as? Activity)?.runOnUiThread { onDataReceived(body) }
                        val resp = "{\"status\":\"ok\",\"message\":\"数据已接收并成功合并\"}".toByteArray(StandardCharsets.UTF_8)
                        sendHttpResponse(out, "application/json; charset=utf-8", resp)
                    }

                    else -> {
                        val status = JSONObject().apply {
                            put("app", "Collecter")
                            put("device", Build.MODEL)
                            put("version", "2.9.0")
                            put("totalEntries", store.loadAll().size)
                        }.toString().toByteArray(StandardCharsets.UTF_8)
                        sendHttpResponse(out, "application/json; charset=utf-8", status)
                    }
                }

                client.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun sendHttpResponse(out: OutputStream, contentType: String, data: ByteArray) {
        val header = "HTTP/1.1 200 OK\r\nContent-Type: $contentType\r\nContent-Length: ${data.size}\r\nAccess-Control-Allow-Origin: *\r\nConnection: close\r\n\r\n"
        out.write(header.toByteArray(StandardCharsets.UTF_8))
        out.write(data)
        out.flush()
    }

    /** 弹出局域网互传面板与配对二维码 */
    fun showLanSyncDialog(activity: Activity, store: DataStore, onSyncCompleted: () -> Unit) {
        val ip = getLocalIpAddress(activity)
        startServer(activity) { receivedJson ->
            if (receivedJson.isNotBlank()) {
                val count = store.importBackupJson(receivedJson)
                Toast.makeText(activity, "⚡ 局域网极速同步成功！已合并 $count 条资产记录", Toast.LENGTH_LONG).show()
            }
            onSyncCompleted()
        }

        val webUrl = "http://$ip:$DEFAULT_PORT"
        val qrBitmap = generateQrCode(webUrl, 480)

        val dialogView = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 40, 48, 40)
            gravity = android.view.Gravity.CENTER_HORIZONTAL
        }

        val titleTv = TextView(activity).apply {
            text = "⚡ 局域网免密极速互传 & Web 大屏"
            textSize = 18f
            setTextColor(Color.WHITE)
            paint.isFakeBoldText = true
        }

        val descTv = TextView(activity).apply {
            text = "同一 Wi-Fi 下，直接在电脑浏览器打开下方地址：\n即可免装软件进入【Web 资产大屏控制台】！"
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
            text = "🌐 Web 控制台: $webUrl"
            textSize = 14f
            setTextColor(Color.parseColor("#10B981"))
            paint.isFakeBoldText = true
            setPadding(0, 16, 0, 8)
        }

        val btnCopy = Button(activity).apply {
            text = "📋 复制 Web 地址发给电脑"
            setOnClickListener {
                val cm = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Collecter Web Dashboard", webUrl))
                Toast.makeText(activity, "已复制局域网 Web 地址到剪贴板！", Toast.LENGTH_SHORT).show()
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
            .setPositiveButton("保持后台运行", null)
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

    /** 生成现代化黑曜石响应式 Web 控制台 HTML5/CSS3/JS */
    private fun buildWebDashboardHtml(store: DataStore): String {
        val entries = store.loadAll()
        val inStock = entries.filter { it.isIn && !it.isRetired }
        val totalWorth = inStock.sumOf { it.price * it.qty }
        val totalCount = inStock.sumOf { it.qty }
        val totalDaily = inStock.sumOf { it.getDailyCost() }

        return """
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Collecter · 资产与仓库大屏控制台</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "PingFang SC", sans-serif; }
        body { background: #0F172A; color: #F8FAFC; padding: 24px; }
        .header { display: flex; justify-content: space-between; align-items: center; padding-bottom: 20px; border-bottom: 1px solid #334155; margin-bottom: 24px; }
        .logo { font-size: 22px; font-weight: bold; color: #10B981; display: flex; align-items: center; gap: 8px; }
        .badge { background: #1E293B; border: 1px solid #475569; padding: 4px 12px; border-radius: 999px; font-size: 12px; color: #94A3B8; }
        .stats { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 16px; margin-bottom: 24px; }
        .stat-card { background: #1E293B; border: 1px solid #334155; border-radius: 16px; padding: 20px; }
        .stat-title { font-size: 13px; color: #94A3B8; margin-bottom: 8px; }
        .stat-value { font-size: 28px; font-weight: bold; }
        .stat-emerald { color: #10B981; }
        .stat-blue { color: #38BDF8; }
        .stat-amber { color: #F59E0B; }
        .toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; gap: 12px; flex-wrap: wrap; }
        .search-input { background: #1E293B; border: 1px solid #475569; color: #FFF; padding: 10px 16px; border-radius: 10px; width: 280px; font-size: 14px; }
        .btn { background: #10B981; color: #FFF; border: none; padding: 10px 18px; border-radius: 10px; font-size: 14px; font-weight: bold; cursor: pointer; transition: 0.2s; }
        .btn:hover { opacity: 0.9; }
        .btn-secondary { background: #334155; color: #F8FAFC; }
        .batch-bar { background: #064E3B; border: 1px solid #059669; padding: 12px 20px; border-radius: 12px; display: none; align-items: center; justify-content: space-between; margin-bottom: 16px; }
        table { width: 100%; border-collapse: collapse; background: #1E293B; border-radius: 16px; overflow: hidden; border: 1px solid #334155; }
        th, td { padding: 14px 18px; text-align: left; border-bottom: 1px solid #334155; font-size: 14px; }
        th { background: #0F172A; color: #94A3B8; font-weight: 600; }
        tr:hover { background: #243247; }
        .cat-chip { background: #334155; padding: 3px 8px; border-radius: 6px; font-size: 12px; }
        .modal { position: fixed; inset: 0; background: rgba(0,0,0,0.6); display: none; justify-content: center; align-items: center; z-index: 999; }
        .modal-box { background: #1E293B; border: 1px solid #475569; border-radius: 16px; padding: 24px; width: 440px; }
        .form-group { margin-bottom: 14px; }
        .form-group label { display: block; font-size: 13px; color: #94A3B8; margin-bottom: 6px; }
        .form-control { width: 100%; background: #0F172A; border: 1px solid #475569; color: #FFF; padding: 10px; border-radius: 8px; font-size: 14px; }
    </style>
</head>
<body>
    <div class="header">
        <div class="logo">💎 Collecter Web 控制台 <span class="badge">局域网免装直连</span></div>
        <div>
            <button class="btn btn-secondary" onclick="exportCsv()">📊 导出 CSV</button>
            <button class="btn" onclick="openAddModal()">➕ 记一笔 / 物品入库</button>
        </div>
    </div>

    <div class="stats">
        <div class="stat-card">
            <div class="stat-title">💰 在役资产总估值</div>
            <div class="stat-value stat-emerald">¥${String.format(Locale.getDefault(), "%,.2f", totalWorth)}</div>
        </div>
        <div class="stat-card">
            <div class="stat-title">📦 在库总件数 / 种类</div>
            <div class="stat-value stat-blue">$totalCount 件 <span style="font-size: 16px; color: #94A3B8">(${inStock.size} 种)</span></div>
        </div>
        <div class="stat-card">
            <div class="stat-title">📉 综合日均损耗/消费</div>
            <div class="stat-value stat-amber">¥${String.format(Locale.getDefault(), "%.2f", totalDaily)} /天</div>
        </div>
    </div>

    <div class="toolbar">
        <input type="text" class="search-input" id="searchBox" placeholder="🔍 搜索物品名称、分类、位置..." oninput="filterTable()">
        <div style="color: #94A3B8; font-size: 13px;">实时与手机双向互通 · 网页端操作毫秒级同步保存</div>
    </div>

    <div class="batch-bar" id="batchBar">
        <span id="batchCount">已选中 0 项</span>
        <div style="display: flex; gap: 10px;">
            <button class="btn btn-secondary" onclick="batchMove()">📍 批量移动位置</button>
            <button class="btn" style="background: #EF4444;" onclick="batchDelete()">🗑️ 批量删除</button>
        </div>
    </div>

    <table>
        <thead>
            <tr>
                <th width="40"><input type="checkbox" onchange="toggleSelectAll(this)"></th>
                <th>物品名称 / 品牌</th>
                <th>分类</th>
                <th>数量</th>
                <th>单价 (¥)</th>
                <th>总额 (¥)</th>
                <th>放置位置</th>
                <th>日均消费</th>
                <th>状态</th>
            </tr>
        </thead>
        <tbody id="itemTableBody">
        </tbody>
    </table>

    <div class="modal" id="addModal">
        <div class="modal-box">
            <h3 style="margin-bottom: 16px;">➕ 快速添加资产物品</h3>
            <div class="form-group">
                <label>物品名称 / 品牌 (*)</label>
                <input type="text" class="form-control" id="addBrand" placeholder="例如：索尼微单相机">
            </div>
            <div class="form-group">
                <label>所属分类</label>
                <input type="text" class="form-control" id="addCat" value="数码">
            </div>
            <div class="form-group" style="display: flex; gap: 10px;">
                <div style="flex: 1;">
                    <label>数量</label>
                    <input type="number" class="form-control" id="addQty" value="1">
                </div>
                <div style="flex: 1;">
                    <label>单位</label>
                    <input type="text" class="form-control" id="addUnit" value="件">
                </div>
            </div>
            <div class="form-group">
                <label>单价 (¥)</label>
                <input type="number" class="form-control" id="addPrice" value="0.0">
            </div>
            <div class="form-group">
                <label>放置位置 / 收纳箱</label>
                <input type="text" class="form-control" id="addLoc" placeholder="例如：主卧衣柜二层">
            </div>
            <div style="display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px;">
                <button class="btn btn-secondary" onclick="closeAddModal()">取消</button>
                <button class="btn" onclick="submitAdd()">确认入库</button>
            </div>
        </div>
    </div>

    <script>
        let allItems = [];
        let selectedIds = new Set();

        async function loadEntries() {
            try {
                const res = await fetch('/api/entries');
                allItems = await res.json();
                renderTable(allItems);
            } catch (e) {
                console.error(e);
            }
        }

        function renderTable(list) {
            const tbody = document.getElementById('itemTableBody');
            tbody.innerHTML = '';
            list.filter(e => e.isIn && !e.isRetired).forEach(e => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td><input type="checkbox" value="${'$'}{e.id}" onchange="toggleItemSelect('${'$'}{e.id}', this.checked)"></td>
                    <td style="font-weight: bold;">${'$'}{e.brand}</td>
                    <td><span class="cat-chip">${'$'}{e.category}</span></td>
                    <td>${'$'}{e.qty} ${'$'}{e.unit}</td>
                    <td>¥${'$'}{e.price.toFixed(2)}</td>
                    <td style="font-weight: bold; color: #10B981;">¥${'$'}{(e.price * e.qty).toFixed(2)}</td>
                    <td>${'$'}{e.location || '未设定'}</td>
                    <td>¥${'$'}{e.dailyCost.toFixed(2)}/天</td>
                    <td><span style="color: #10B981;">🟢 在役</span></td>
                `;
                tbody.appendChild(tr);
            });
            updateBatchBar();
        }

        function filterTable() {
            const q = document.getElementById('searchBox').value.toLowerCase();
            const filtered = allItems.filter(e => 
                e.brand.toLowerCase().includes(q) || 
                e.category.toLowerCase().includes(q) || 
                (e.location && e.location.toLowerCase().includes(q))
            );
            renderTable(filtered);
        }

        function toggleItemSelect(id, checked) {
            if (checked) selectedIds.add(id); else selectedIds.delete(id);
            updateBatchBar();
        }

        function toggleSelectAll(cb) {
            selectedIds.clear();
            const checkboxes = document.querySelectorAll('#itemTableBody input[type="checkbox"]');
            checkboxes.forEach(c => {
                c.checked = cb.checked;
                if (cb.checked) selectedIds.add(c.value);
            });
            updateBatchBar();
        }

        function updateBatchBar() {
            const bar = document.getElementById('batchBar');
            if (selectedIds.size > 0) {
                bar.style.display = 'flex';
                document.getElementById('batchCount').innerText = `已选中 ${'$'}{selectedIds.size} 项资产`;
            } else {
                bar.style.display = 'none';
            }
        }

        async function batchMove() {
            const loc = prompt('请输入新的放置位置 / 房间收纳箱名称：');
            if (!loc) return;
            await fetch('/api/entries/batch_move', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ ids: Array.from(selectedIds), location: loc, roomName: loc })
            });
            selectedIds.clear();
            await loadEntries();
            alert('批量移动成功！');
        }

        async function batchDelete() {
            if (!confirm(`确定要批量删除选中的 ${'$'}{selectedIds.size} 项资产吗？`)) return;
            await fetch('/api/entries/batch_delete', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ ids: Array.from(selectedIds) })
            });
            selectedIds.clear();
            await loadEntries();
            alert('批量删除成功！');
        }

        function openAddModal() { document.getElementById('addModal').style.display = 'flex'; }
        function closeAddModal() { document.getElementById('addModal').style.display = 'none'; }

        async function submitAdd() {
            const brand = document.getElementById('addBrand').value.trim();
            if (!brand) return alert('请输入物品名称！');
            await fetch('/api/entry/add', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    brand,
                    category: document.getElementById('addCat').value.trim(),
                    qty: parseInt(document.getElementById('addQty').value) || 1,
                    unit: document.getElementById('addUnit').value.trim() || '件',
                    price: parseFloat(document.getElementById('addPrice').value) || 0.0,
                    location: document.getElementById('addLoc').value.trim(),
                    isIn: true
                })
            });
            closeAddModal();
            await loadEntries();
            alert('物品添加成功，已即时同步至手机！');
        }

        function exportCsv() {
            let csv = '\uFEFF物品名称,分类,数量,单位,单价,总额,放置位置,日均消费\n';
            allItems.filter(e => e.isIn && !e.isRetired).forEach(e => {
                csv += `"${'$'}{e.brand}","${'$'}{e.category}",${'$'}{e.qty},"${'$'}{e.unit}",${'$'}{e.price},${'$'}{e.price * e.qty},"${'$'}{e.location || ''}",${'$'}{e.dailyCost}\n`;
            });
            const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
            const link = document.createElement('a');
            link.href = URL.createObjectURL(blob);
            link.download = `Collecter_Assets_${'$'}{new Date().toISOString().slice(0,10)}.csv`;
            link.click();
        }

        loadEntries();
    </script>
</body>
</html>
        """.trimIndent()
    }
}
