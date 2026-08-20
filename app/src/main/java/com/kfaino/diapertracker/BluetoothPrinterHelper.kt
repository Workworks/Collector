package com.kfaino.diapertracker

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.graphics.Bitmap
import android.graphics.Color
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

/**
 * 蓝牙便携热敏标签打印机直连引擎 (ESC/POS & TSPL 协议标准)
 * 支持市面主流 58mm / 80mm 便携蓝牙热敏打印机（汉印、精臣、得力、佳博等）
 */
object BluetoothPrinterHelper {

    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val executor = Executors.newSingleThreadExecutor()

    @SuppressLint("MissingPermission")
    fun printBoxLabel(
        activity: Activity,
        houseName: String,
        roomName: String,
        items: List<Entry>,
        qrBitmap: Bitmap?
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
            sendPrintJob(activity, device, houseName, roomName, items, qrBitmap)
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendPrintJob(
        activity: Activity,
        device: BluetoothDevice,
        houseName: String,
        roomName: String,
        items: List<Entry>,
        qrBitmap: Bitmap?
    ) {
        Toast.makeText(activity, "正在连接打印机【${device.name}】...", Toast.LENGTH_SHORT).show()

        executor.execute {
            var socket: BluetoothSocket? = null
            try {
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket.connect()
                val os = socket.outputStream

                val data = buildEscPosPayload(houseName, roomName, items, qrBitmap)
                os.write(data)
                os.flush()

                activity.runOnUiThread {
                    Toast.makeText(activity, "🎉 标签打印任务已成功发送！", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                activity.runOnUiThread {
                    Toast.makeText(activity, "打印机连接或发送失败: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            } finally {
                try {
                    socket?.close()
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

    private fun buildEscPosPayload(
        houseName: String,
        roomName: String,
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
        baos.write("📦 收纳箱标签\n".toByteArray(gbk))

        // 3. 恢复标准字号，打印空间与房间
        baos.write(byteArrayOf(0x1D, 0x21, 0x00)) // 正常
        baos.write(byteArrayOf(0x1B, 0x45, 0x00)) // 取消加粗
        baos.write("--------------------------------\n".toByteArray(gbk))
        baos.write("🏠 空间: $houseName\n".toByteArray(gbk))
        baos.write("📍 区域/箱号: $roomName\n".toByteArray(gbk))
        baos.write("📊 箱内物品总数: ${items.size} 种 / ${items.sumOf { it.qty }} 件\n".toByteArray(gbk))
        baos.write("--------------------------------\n".toByteArray(gbk))

        // 4. 左对齐打印在库清单摘要 (最多打印 8 项)
        baos.write(byteArrayOf(0x1B, 0x61, 0x00)) // 左对齐
        val displayItems = items.take(8)
        for ((idx, item) in displayItems.withIndex()) {
            val line = "${idx + 1}. ${item.brand} × ${item.qty} ${item.unit}\n"
            baos.write(line.toByteArray(gbk))
        }
        if (items.size > 8) {
            baos.write("... 及其余 ${items.size - 8} 项 (扫码查看)\n".toByteArray(gbk))
        }
        baos.write("--------------------------------\n".toByteArray(gbk))

        // 5. 打印时间戳与 Collecter 标识
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        baos.write(byteArrayOf(0x1B, 0x61, 0x01)) // 居中
        baos.write("打印时间: ${sdf.format(Date())}\n".toByteArray(gbk))
        baos.write("由 Collecter 智能资产生成\n\n\n".toByteArray(gbk))

        // 6. 进纸切纸
        baos.write(byteArrayOf(0x1B, 0x64, 0x04)) // 进纸 4 行
        return baos.toByteArray()
    }
}
