package com.kfaino.diapertracker

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.graphics.Bitmap
import android.widget.Toast
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

/**
 * 蓝牙便携热敏标签打印机直连引擎 (ESC/POS & TSPL 协议标准)
 * 支持市面主流 58mm / 80mm 便携蓝牙热敏打印机（汉印、精臣、得力、佳博等）
 * 支持 5 大多模态收纳便签模版：箱盒清单、食材保鲜、常备药用法、线缆规格、防丢联系卡
 */
object BluetoothPrinterHelper {

    private const val TAG = "BluetoothPrinterHelper"
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val executor = Executors.newSingleThreadExecutor()

    const val TPL_BOX = 0
    const val TPL_FOOD = 1
    const val TPL_MEDICINE = 2
    const val TPL_CABLE = 3
    const val TPL_ANTI_LOST = 4

    @SuppressLint("MissingPermission")
    fun printLabel(
        activity: Activity,
        templateType: Int,
        title: String,
        subtitle: String,
        attrLines: List<String>,
        items: List<Entry> = emptyList(),
        qrBitmap: Bitmap? = null
    ) {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null || !adapter.isEnabled) {
            Toast.makeText(activity, "请先开启手机蓝牙！", Toast.LENGTH_SHORT).show()
            return
        }

        val bondedDevices = adapter.bondedDevices.toList()
        if (bondedDevices.isEmpty()) {
            Toast.makeText(activity, "未找到已配对的蓝牙标签机，请先在手机系统设置中配对蓝牙打印机！", Toast.LENGTH_LONG).show()
            return
        }

        val names = bondedDevices.map { "${it.name ?: "未知设备"} (${it.address})" }.toTypedArray()

        ModernDialogHelper.showSingleChoiceDialog(
            context = activity,
            title = "选择蓝牙标签打印机",
            emoji = "🖨️",
            options = names.toList(),
            selectedIndex = 0
        ) { which, _ ->
            val device = bondedDevices[which]
            sendPrintJob(activity, device, templateType, title, subtitle, attrLines, items, qrBitmap)
        }
    }

    /** 兼容旧版调用 */
    fun printBoxLabel(
        activity: Activity,
        houseName: String,
        roomName: String,
        items: List<Entry>,
        qrBitmap: Bitmap?
    ) {
        val attrLines = listOf(
            "• 在库明细：${items.size} 种物品 (共 ${items.sumOf { it.qty }} 件)",
            "• 核心物品：${items.take(3).joinToString("、") { "${it.brand}×${it.qty}" }}",
            "• 收纳位置：$houseName / $roomName"
        )
        printLabel(
            activity = activity,
            templateType = TPL_BOX,
            title = "📦 $roomName",
            subtitle = "🏠 $houseName · 共 ${items.size} 种在库物品",
            attrLines = attrLines,
            items = items,
            qrBitmap = qrBitmap
        )
    }

    @SuppressLint("MissingPermission")
    private fun sendPrintJob(
        activity: Activity,
        device: BluetoothDevice,
        templateType: Int,
        title: String,
        subtitle: String,
        attrLines: List<String>,
        items: List<Entry>,
        qrBitmap: Bitmap?
    ) {
        Toast.makeText(activity, "正在连接打印机【${device.name ?: "热敏机"}】...", Toast.LENGTH_SHORT).show()

        executor.execute {
            var socket: BluetoothSocket? = null
            try {
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket.connect()
                val os = socket.outputStream

                val data = buildEscPosPayload(templateType, title, subtitle, attrLines, items, qrBitmap)
                os.write(data)
                os.flush()

                activity.runOnUiThread {
                    Toast.makeText(activity, "🎉 便签打印任务已成功发送！", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.w(TAG, "打印机连接或发送失败", e)
                activity.runOnUiThread {
                    Toast.makeText(activity, "打印机连接或发送失败: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            } finally {
                try {
                    socket?.close()
                } catch (e: Exception) {
                    android.util.Log.w(TAG, "关闭打印机 socket 失败", e)
                }
            }
        }
    }

    private fun buildEscPosPayload(
        templateType: Int,
        title: String,
        subtitle: String,
        attrLines: List<String>,
        items: List<Entry>,
        qrBitmap: Bitmap?
    ): ByteArray {
        val baos = ByteArrayOutputStream()
        val gbk = Charset.forName("GBK")

        // 1. 初始化打印机
        baos.write(byteArrayOf(0x1B, 0x40))

        // 2. 居中打印标题
        baos.write(byteArrayOf(0x1B, 0x61, 0x01)) // 居中
        baos.write(byteArrayOf(0x1B, 0x45, 0x01)) // 加粗
        baos.write(byteArrayOf(0x1D, 0x21, 0x11)) // 双倍宽高

        val headerText = when (templateType) {
            TPL_FOOD -> "🥫 食材保鲜便签\n"
            TPL_MEDICINE -> "💊 药箱对症便签\n"
            TPL_CABLE -> "🔌 线缆规格便签\n"
            TPL_ANTI_LOST -> "🪪 资产防丢联系卡\n"
            else -> "📦 收纳箱全景便签\n"
        }
        baos.write(headerText.toByteArray(gbk))

        // 3. 恢复标准字号，打印副标题与属性
        baos.write(byteArrayOf(0x1D, 0x21, 0x00)) // 正常
        baos.write(byteArrayOf(0x1B, 0x45, 0x00)) // 取消加粗
        baos.write("--------------------------------\n".toByteArray(gbk))
        baos.write("📌 名称: $title\n".toByteArray(gbk))
        if (subtitle.isNotBlank()) {
            baos.write("ℹ️ 备注: $subtitle\n".toByteArray(gbk))
        }
        baos.write("--------------------------------\n".toByteArray(gbk))

        // 4. 打印动态属性明细
        baos.write(byteArrayOf(0x1B, 0x61, 0x00)) // 左对齐
        for (line in attrLines) {
            baos.write("$line\n".toByteArray(gbk))
        }

        // 如果是箱盒模版且有物品列表，打印前 6 项清单
        if (templateType == TPL_BOX && items.isNotEmpty()) {
            baos.write("--------------------------------\n".toByteArray(gbk))
            baos.write("【箱内物品明细】\n".toByteArray(gbk))
            val displayItems = items.take(6)
            for ((idx, item) in displayItems.withIndex()) {
                baos.write("${idx + 1}. ${item.brand} × ${item.qty} ${item.unit}\n".toByteArray(gbk))
            }
            if (items.size > 6) {
                baos.write("... 及其余 ${items.size - 6} 项 (扫码查看)\n".toByteArray(gbk))
            }
        }
        baos.write("--------------------------------\n".toByteArray(gbk))

        // 5. 打印时间戳与 Collecter 标识
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        baos.write(byteArrayOf(0x1B, 0x61, 0x01)) // 居中
        baos.write("打印时间: ${sdf.format(Date())}\n".toByteArray(gbk))
        baos.write("由 Collecter 智能便签工坊生成\n\n\n".toByteArray(gbk))

        // 6. 进纸切纸
        baos.write(byteArrayOf(0x1B, 0x64, 0x04)) // 进纸 4 行
        return baos.toByteArray()
    }
}
