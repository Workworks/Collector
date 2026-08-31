package com.kfaino.diapertracker

import java.util.regex.Pattern

/**
 * 📬 跨应用智能账单与电商物流通知智能归档助手 (Smart Notification Hook)
 */
object SmartNotificationHook {

    data class ParsedPackageInfo(
        val courierCompany: String,
        val trackingNumber: String?,
        val itemSnippet: String,
        val estimatedArrivalText: String
    )

    fun parseNotificationText(title: String, body: String): ParsedPackageInfo? {
        val full = title + " " + body
        if (!full.contains("包裹") && !full.contains("快递") && !full.contains("发货") && !full.contains("已签收")) {
            return null
        }

        val courier = when {
            full.contains("顺丰") -> "顺丰速运"
            full.contains("京东") -> "京东快递"
            full.contains("菜鸟") || full.contains("中通") || full.contains("圆通") -> "通达系快递"
            else -> "普通快递"
        }

        val trackPattern = Pattern.compile("([A-Za-z0-9]{10,20})")
        val matcher = trackPattern.matcher(full)
        val trackNum = if (matcher.find()) matcher.group(1) else null

        return ParsedPackageInfo(
            courierCompany = courier,
            trackingNumber = trackNum,
            itemSnippet = title.take(20),
            estimatedArrivalText = if (full.contains("已签收")) "已送达待入库" else "运输派送中"
        )
    }
}