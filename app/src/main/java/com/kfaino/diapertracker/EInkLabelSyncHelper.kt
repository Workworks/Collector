package com.kfaino.diapertracker

import java.util.Base64

/**
 * 🏷️ 智能电子墨水屏（E-Ink）标签无线离线刷屏助手 (E-Ink Label Sync Helper)
 */
object EInkLabelSyncHelper {

    data class EInkBitmapPayload(
        val labelTagId: String,
        val widthPx: Int,
        val heightPx: Int,
        val binaryDataHex: String,
        val summaryText: String
    )

    fun generatePayload(tagId: String, title: String, itemCount: Int): EInkBitmapPayload {
        val summary = "🏷️ [" + title + "] 内含 " + itemCount + " 件物资"
        val hexMock = "FFAA5500" + Integer.toHexString(itemCount)
        return EInkBitmapPayload(
            labelTagId = tagId,
            widthPx = 250,
            heightPx = 122,
            binaryDataHex = hexMock,
            summaryText = summary
        )
    }
}