package com.kfaino.diapertracker

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.nfc.*
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.nio.charset.StandardCharsets

/**
 * NFC 智能标签「碰一碰」写卡与零点击寻物引擎
 * - 支持将收纳箱与空间信息写入标准 NTAG213/215/216 贴纸
 * - 支持背部碰触一秒呼出箱内清单与空间平面图
 */
object NfcHelper {

    private var pendingWritePayload: String? = null
    private var writeDialog: androidx.appcompat.app.AlertDialog? = null

    fun isNfcAvailable(context: Context): Boolean {
        val adapter = NfcAdapter.getDefaultAdapter(context)
        return adapter != null && adapter.isEnabled
    }

    fun enableForegroundDispatch(activity: Activity) {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        val intent = Intent(activity, activity.javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(
            activity, 0, intent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val filters = arrayOf(
            IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
                addDataScheme("collector")
            },
            IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
        )
        adapter.enableForegroundDispatch(activity, pendingIntent, filters, null)
    }

    fun disableForegroundDispatch(activity: Activity) {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        adapter.disableForegroundDispatch(activity)
    }

    /** 准备写入 NFC 标签 */
    fun prepareWriteBoxTag(activity: Activity, houseName: String, roomName: String) {
        val adapter = NfcAdapter.getDefaultAdapter(activity)
        if (adapter == null) {
            Toast.makeText(activity, "当前设备不支持 NFC 硬件！", Toast.LENGTH_SHORT).show()
            return
        }
        if (!adapter.isEnabled) {
            Toast.makeText(activity, "请先在手机设置中开启 NFC 功能！", Toast.LENGTH_SHORT).show()
            return
        }

        val uriString = "collector://box?house=${Uri.encode(houseName)}&room=${Uri.encode(roomName)}"
        pendingWritePayload = uriString

        val binding = com.kfaino.diapertracker.databinding.DialogModernBaseBinding.inflate(activity.layoutInflater)
        binding.tvDialogEmoji.text = "🏷️"
        binding.tvDialogTitle.text = "靠近 NFC 智能标签写入"
        binding.tvDialogMessage.text = "正在等待感应...\n\n请将手机背部 NFC 感应区轻贴收纳箱上的 NFC 贴纸 (NTAG213/215 等)"
        binding.btnDialogPositive.visibility = View.GONE
        binding.btnDialogNegative.text = "取消"
        binding.btnDialogNegative.applyPressScaleAnimation(0.92f)

        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(binding.root)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation

        binding.btnDialogNegative.setOnClickListener {
            pendingWritePayload = null
            dialog.dismiss()
        }
        dialog.setOnDismissListener {
            pendingWritePayload = null
        }
        writeDialog = dialog
        dialog.show()
    }

    /** 处理 NFC Tag 写入事件 */
    fun handleTagDiscoveredForWrite(activity: Activity, tag: Tag): Boolean {
        val payload = pendingWritePayload ?: return false

        try {
            val record = NdefRecord.createUri(Uri.parse(payload))
            val message = NdefMessage(arrayOf(record))

            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                if (!ndef.isWritable) {
                    Toast.makeText(activity, "该 NFC 标签为只读，无法写入！", Toast.LENGTH_LONG).show()
                    return false
                }
                if (ndef.maxSize < message.byteArrayLength) {
                    Toast.makeText(activity, "NFC 标签容量不足！", Toast.LENGTH_LONG).show()
                    return false
                }
                ndef.writeNdefMessage(message)
                ndef.close()
            } else {
                val formatable = NdefFormatable.get(tag)
                if (formatable != null) {
                    formatable.connect()
                    formatable.format(message)
                    formatable.close()
                } else {
                    Toast.makeText(activity, "不支持此类型的 NFC 标签！", Toast.LENGTH_LONG).show()
                    return false
                }
            }

            activity.runOnUiThread {
                writeDialog?.dismiss()
                writeDialog = null
                pendingWritePayload = null
                Toast.makeText(activity, "🎉 NFC 智能标签写入成功！碰一碰即可查看箱内清单", Toast.LENGTH_LONG).show()
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            activity.runOnUiThread {
                Toast.makeText(activity, "NFC 写入失败: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
            return false
        }
    }

    /** 解析 NFC 扫入数据 */
    fun parseNfcIntent(intent: Intent): Pair<String, String>? {
        val action = intent.action ?: return null
        if (action != NfcAdapter.ACTION_NDEF_DISCOVERED && action != NfcAdapter.ACTION_TAG_DISCOVERED) return null

        val rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES) ?: return null
        for (raw in rawMsgs) {
            val msg = raw as? NdefMessage ?: continue
            for (record in msg.records) {
                val uri = record.toUri()
                if (uri != null && uri.scheme == "collector" && uri.host == "box") {
                    val house = uri.getQueryParameter("house") ?: "我的家"
                    val room = uri.getQueryParameter("room") ?: ""
                    return Pair(house, room)
                }
            }
        }
        return null
    }
}
