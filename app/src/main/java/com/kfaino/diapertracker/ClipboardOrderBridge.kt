package com.kfaino.diapertracker

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast

/**
 * 📋 淘口令 / 电商分享链接剪贴板无感桥接引擎 (Clipboard Order Bridge)
 * 侦测剪贴板中的淘宝、京东、拼多多电商分享与订单文本，弹出提示一键结构化快速入库
 */
object ClipboardOrderBridge {

    private var lastProcessedHash: Int = 0

    fun checkClipboard(activity: Activity) {
        try {
            val cm = activity.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
            val clip = cm.primaryClip ?: return
            if (clip.itemCount == 0) return

            val text = clip.getItemAt(0)?.text?.toString()?.trim() ?: return
            if (text.length < 5 || text.hashCode() == lastProcessedHash) return

            if (SmartIntakeHelper.isEcommerceContent(text)) {
                lastProcessedHash = text.hashCode()
                val parsed = SmartIntakeHelper.parseNaturalLanguage(text)

                val msg = "检测到来自电商平台（淘宝/京东/拼多多）的商品信息：\n\n" +
                          "• 商品名称：${parsed.brand}\n" +
                          "• 预估单价：¥${parsed.price}\n" +
                          "• 推荐分类：${parsed.category}\n" +
                          (if (parsed.notes.isNotBlank()) "• 规格备注：${parsed.notes}\n" else "") +
                          "\n是否立即一键结构化填写入库？"

                ModernDialogHelper.showConfirmDialog(
                    context = activity,
                    title = "发现电商商品/订单",
                    message = msg,
                    emoji = "🛒",
                    positiveText = "✨ 立即智能入库",
                    negativeText = "忽略"
                ) {
                    (activity as? MainActivity)?.applySmartParsedItem(parsed)
                    Toast.makeText(activity, "🎉 已自动提取并填充电商商品信息！", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("ClipboardOrderBridge", "检查剪贴板电商内容异常", e)
        }
    }
}
