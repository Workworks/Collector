package com.kfaino.diapertracker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 多格式数据导出引擎 (支持生成带 UTF-8 BOM 的 Excel 兼容 CSV 与系统级一键分享)
 */
object ExportManager {

    private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val DAY_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val FILE_DATE_FORMAT = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    /** CSV 字段转义辅助方法 */
    private fun escapeCsv(value: Any?): String {
        if (value == null) return "\"\""
        val str = value.toString().replace("\"", "\"\"")
        return "\"$str\""
    }

    /** 生成资产全景清单 CSV 文本 (UTF-8 BOM) */
    fun generateAssetsCsv(entries: List<Entry>): String {
        val sb = StringBuilder()
        // 添加 UTF-8 BOM 头 (\uFEFF) 防止 Excel 打开中文乱码
        sb.append('\uFEFF')

        // 表头
        val headers = listOf(
            "所属分类", "物品/品牌名称", "物品管理类型", "在库数量", "单位",
            "购入单价(元)", "累计投入金额(元)", "当前二手估值(元)", "购入日期",
            "拥有天数", "日均使用成本(元/天)", "生产日期", "到期保质期", "所属空间", "具体放置位置",
            "所在房间", "重要关注物品", "在役状态", "退役渠道",
            "二手出掉回血(元)", "退役备注", "周期订阅资产", "订阅周期",
            "下次扣费日期", "记录时间", "备注说明"
        )
        sb.append(headers.joinToString(",") { escapeCsv(it) }).append("\r\n")

        for (e in entries) {
            val totalSpent = e.qty * e.price
            val pDateStr = if (e.purchaseDate > 0) DAY_FORMAT.format(Date(e.purchaseDate)) else ""
            val mfgStr = if (e.manufactureDate > 0) DAY_FORMAT.format(Date(e.manufactureDate)) else ""
            val expStr = if (e.expiryDate > 0) DAY_FORMAT.format(Date(e.expiryDate)) else ""
            val tsStr = if (e.ts > 0) DATE_FORMAT.format(Date(e.ts)) else ""
            val nextSubStr = if (e.isSubscription && e.subNextBillingDate > 0) DAY_FORMAT.format(Date(e.subNextBillingDate)) else ""

            val row = listOf(
                escapeCsv(e.category),
                escapeCsv(e.brand),
                escapeCsv(e.getAssetTypeDisplayName()),
                escapeCsv(e.qty),
                escapeCsv(e.unit),
                escapeCsv(String.format(Locale.getDefault(), "%.2f", e.price)),
                escapeCsv(String.format(Locale.getDefault(), "%.2f", totalSpent)),
                escapeCsv(String.format(Locale.getDefault(), "%.2f", e.currentValuation)),
                escapeCsv(pDateStr),
                escapeCsv(e.getDaysOwned()),
                escapeCsv(if (e.assetType == "depreciating") String.format(Locale.getDefault(), "%.2f", e.getDailyCost()) else "-"),
                escapeCsv(mfgStr),
                escapeCsv(expStr),
                escapeCsv(e.houseName),
                escapeCsv(e.location),
                escapeCsv(e.roomName),
                escapeCsv(if (e.isImportant) "是" else "否"),
                escapeCsv(if (e.isRetired) "已退役" else "在役中"),
                escapeCsv(e.retiredAction),
                escapeCsv(String.format(Locale.getDefault(), "%.2f", e.retiredSoldPrice)),
                escapeCsv(e.retiredNote),
                escapeCsv(if (e.isSubscription) "是" else "否"),
                escapeCsv(if (e.isSubscription) e.subCycle else ""),
                escapeCsv(nextSubStr),
                escapeCsv(tsStr),
                escapeCsv(e.notes)
            )
            sb.append(row.joinToString(",")).append("\r\n")
        }

        return sb.toString()
    }

    /** 生成进销存收支流水账明细 CSV 文本 */
    fun generateTimelineCsv(entries: List<Entry>): String {
        val sb = StringBuilder()
        sb.append('\uFEFF')

        val headers = listOf(
            "记录时间", "操作类型", "分类", "物品/品牌名称",
            "变动数量", "单位", "单价(元)", "本次金额(元)",
            "所属空间", "具体放置位置", "所在房间", "拥有天数", "备注说明"
        )
        sb.append(headers.joinToString(",") { escapeCsv(it) }).append("\r\n")

        for (e in entries) {
            val typeStr = if (e.isIn) "增加/入库" else "减少/消耗"
            val totalAmt = e.qty * e.price
            val tsStr = if (e.ts > 0) DATE_FORMAT.format(Date(e.ts)) else ""

            val row = listOf(
                escapeCsv(tsStr),
                escapeCsv(typeStr),
                escapeCsv(e.category),
                escapeCsv(e.brand),
                escapeCsv(if (e.isIn) "+${e.qty}" else "-${e.qty}"),
                escapeCsv(e.unit),
                escapeCsv(String.format(Locale.getDefault(), "%.2f", e.price)),
                escapeCsv(String.format(Locale.getDefault(), "%.2f", if (e.isIn) totalAmt else 0.0)),
                escapeCsv(e.houseName),
                escapeCsv(e.location),
                escapeCsv(e.roomName),
                escapeCsv(e.getDaysOwned()),
                escapeCsv(e.notes)
            )
            sb.append(row.joinToString(",")).append("\r\n")
        }

        return sb.toString()
    }

    /** 保存 CSV 字符串至应用缓存或下载目录文件 */
    fun writeCsvToFile(context: Context, filename: String, csvContent: String): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
        val file = File(dir, filename)
        FileOutputStream(file).use { fos ->
            OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                writer.write(csvContent)
                writer.flush()
            }
        }
        return file
    }

    /** 导出资产清单并拉起系统分享面板 */
    fun exportAndShareAssetsCsv(activity: Activity, entries: List<Entry>) {
        try {
            val timeStr = FILE_DATE_FORMAT.format(Date())
            val filename = "Collecter_资产总表_$timeStr.csv"
            val csvText = generateAssetsCsv(entries)
            val file = writeCsvToFile(activity, filename, csvText)

            shareFile(
                activity,
                file,
                title = "分享【Collecter 资产总表】",
                mimeType = "text/csv"
            )
        } catch (e: Exception) {
            Toast.makeText(activity, "导出失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /** 导出流水明细并拉起系统分享面板 */
    fun exportAndShareTimelineCsv(activity: Activity, entries: List<Entry>) {
        try {
            val timeStr = FILE_DATE_FORMAT.format(Date())
            val filename = "Collecter_收支流水明细_$timeStr.csv"
            val csvText = generateTimelineCsv(entries)
            val file = writeCsvToFile(activity, filename, csvText)

            shareFile(
                activity,
                file,
                title = "分享【Collecter 收支流水明细】",
                mimeType = "text/csv"
            )
        } catch (e: Exception) {
            Toast.makeText(activity, "导出失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /** 调起 Android 原生分享对话框 (发送到微信/QQ/钉钉/邮件/网盘或保存到文件) */
    fun shareFile(activity: Activity, file: File, title: String, mimeType: String = "text/csv") {
        val uri: Uri = FileProvider.getUriForFile(
            activity,
            "${activity.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        activity.startActivity(Intent.createChooser(intent, title))
    }

    /**
     * 生成 12 馆时效物资联合报表 CSV (UTF-8 BOM)
     * 字段：来源馆 / 物品名称 / 数量与单位 / 关键日期 / 剩余天数 / 存放位置 / 状态标签 / 备注
     */
    fun generateAllVaultsCsv(store: DataStore): String {
        val sb = StringBuilder()
        sb.append('\uFEFF')  // UTF-8 BOM，防 Excel 中文乱码
        val headers = listOf("来源馆", "物品名称", "数量与单位", "关键日期", "剩余天数", "存放位置", "状态标签", "备注")
        sb.append(headers.joinToString(",") { escapeCsv(it) }).append("\r\n")

        val now = System.currentTimeMillis()
        val dayMs = 86400000L

        fun daysLeftStr(ts: Long): String = when {
            ts <= 0L -> "-"
            ts < now -> "已过期 ${(now - ts) / dayMs} 天"
            else -> "剩余 ${(ts - now) / dayMs} 天"
        }

        // 🎟️ 时效卡券
        try {
            store.getVouchers().filter { !it.isUsed }.forEach { v ->
                sb.append(listOf(
                    escapeCsv("🎟️ 时效卡券"), escapeCsv(v.title),
                    escapeCsv("${v.remainingTimes} 次"),
                    escapeCsv(if (v.expiryDate > 0) DAY_FORMAT.format(java.util.Date(v.expiryDate)) else "长期有效"),
                    escapeCsv(daysLeftStr(v.expiryDate)), escapeCsv(v.platform),
                    escapeCsv(if (v.isExpired()) "⚠️ 已过期" else if (v.isExpiringSoon()) "🟠 临期" else "🟢 有效"),
                    escapeCsv(v.notes)
                ).joinToString(",")).append("\r\n")
            }
        } catch (e: Exception) { android.util.Log.w("ExportManager", "导出卡券数据失败", e) }

        // 💊 家庭药箱
        try {
            store.getMedicines().forEach { m ->
                val eff = m.getEffectiveExpiryDate()
                sb.append(listOf(
                    escapeCsv("💊 家庭药箱"), escapeCsv(m.name),
                    escapeCsv("${m.qty} ${m.unit}"),
                    escapeCsv(if (eff > 0) DAY_FORMAT.format(java.util.Date(eff)) else "长期有效"),
                    escapeCsv(daysLeftStr(eff)), escapeCsv(m.location),
                    escapeCsv(if (m.isExpired()) "🔴 已过期" else if (m.isExpiringSoon()) "🟠 临期" else "🟢 正常"),
                    escapeCsv(m.dosage)
                ).joinToString(",")).append("\r\n")
            }
        } catch (e: Exception) { android.util.Log.w("ExportManager", "导出药箱数据失败", e) }

        // 🥦 食材鲜度库
        try {
            store.getFoods().filter { !it.isConsumed }.forEach { f ->
                val eff = f.getEffectiveExpiryDate()
                sb.append(listOf(
                    escapeCsv("🥦 食材鲜度库"), escapeCsv(f.name),
                    escapeCsv("${f.qty} ${f.unit}"),
                    escapeCsv(if (eff > 0) DAY_FORMAT.format(java.util.Date(eff)) else "长期在库"),
                    escapeCsv(daysLeftStr(eff)), escapeCsv(f.location),
                    escapeCsv(f.getFreshnessStatusText()), escapeCsv(f.notes)
                ).joinToString(",")).append("\r\n")
            }
        } catch (e: Exception) { android.util.Log.w("ExportManager", "导出食材数据失败", e) }

        // 🚨 应急防灾
        try {
            store.getEmergencyItems().forEach { e ->
                sb.append(listOf(
                    escapeCsv("🚨 应急防灾"), escapeCsv(e.name),
                    escapeCsv("${e.qty} ${e.unit}"),
                    escapeCsv(if (e.expiryDate > 0) DAY_FORMAT.format(java.util.Date(e.expiryDate)) else "长期有效"),
                    escapeCsv(daysLeftStr(e.expiryDate)), escapeCsv(e.location),
                    escapeCsv(if (e.isExpired()) "🔴 已过期" else if (e.isExpiringSoon()) "🟠 临期" else "🟢 正常"),
                    escapeCsv(e.notes)
                ).joinToString(",")).append("\r\n")
            }
        } catch (e: Exception) { android.util.Log.w("ExportManager", "导出应急物资数据失败", e) }

        // 🔧 工具维保
        try {
            store.getToolRecords().forEach { t ->
                val nextMs = t.getNextMaintenanceDate()
                sb.append(listOf(
                    escapeCsv("🔧 工具五金"), escapeCsv(t.name),
                    escapeCsv("${t.qty} ${t.unit}"),
                    escapeCsv(if (nextMs > 0) DAY_FORMAT.format(java.util.Date(nextMs)) else "-"),
                    escapeCsv(daysLeftStr(nextMs)), escapeCsv(t.location),
                    escapeCsv(t.getCategoryDisplayName()), escapeCsv(t.notes)
                ).joinToString(",")).append("\r\n")
            }
        } catch (e: Exception) { android.util.Log.w("ExportManager", "导出工具维保数据失败", e) }

        // 🪴 绿植水肥
        try {
            store.getPlantRecords().forEach { p ->
                val nextWaterMs = p.getNextWaterDate()
                sb.append(listOf(
                    escapeCsv("🪴 绿植水肥"), escapeCsv("${p.name}（${p.species}）"),
                    escapeCsv("1 盆"),
                    escapeCsv(if (nextWaterMs > 0) DAY_FORMAT.format(java.util.Date(nextWaterMs)) else "-"),
                    escapeCsv(daysLeftStr(nextWaterMs)), escapeCsv(p.location),
                    escapeCsv(p.getLightDemandDisplayName()),
                    escapeCsv(p.careTips)
                ).joinToString(",")).append("\r\n")
            }
        } catch (e: Exception) { android.util.Log.w("ExportManager", "导出绿植养护数据失败", e) }

        // 🐾 萌宠健康
        try {
            store.getPetRecords().forEach { p ->
                val nextDewormMs = p.getNextDewormDate()
                sb.append(listOf(
                    escapeCsv("🐾 萌宠健康"), escapeCsv("${p.name}（${p.species}）"),
                    escapeCsv("-"),
                    escapeCsv(if (nextDewormMs > 0) DAY_FORMAT.format(java.util.Date(nextDewormMs)) else "-"),
                    escapeCsv(daysLeftStr(nextDewormMs)), escapeCsv(""),
                    escapeCsv(if (p.isDewormDue()) "⚠️ 驱虫逾期" else if (p.isDewormDueSoon()) "🟠 即将驱虫" else "🟢 正常"),
                    escapeCsv(p.notes)
                ).joinToString(",")).append("\r\n")
            }
        } catch (e: Exception) { android.util.Log.w("ExportManager", "导出萌宠健康数据失败", e) }

        // 📚 书房藏书
        try {
            store.getBookRecords().forEach { b ->
                sb.append(listOf(
                    escapeCsv("📚 书房藏书"), escapeCsv("${b.title}（${b.author}）"),
                    escapeCsv("1 册"),
                    escapeCsv(if (b.lentDate > 0) DAY_FORMAT.format(java.util.Date(b.lentDate)) else "-"),
                    escapeCsv("-"), escapeCsv(b.bookshelfLocation),
                    escapeCsv(b.getStatusDisplayName()),
                    escapeCsv(b.summaryNotes.take(50))
                ).joinToString(",")).append("\r\n")
            }
        } catch (e: Exception) { android.util.Log.w("ExportManager", "导出书房藏书数据失败", e) }

        // 🍷 茶窖名酿
        try {
            store.getBeverageRecords().forEach { bv ->
                sb.append(listOf(
                    escapeCsv("🍷 茶窖名酿"), escapeCsv(bv.name),
                    escapeCsv("${bv.qty} ${bv.unit}"),
                    escapeCsv(if (bv.vintageYear > 0) "${bv.vintageYear} 年起酿" else "-"),
                    escapeCsv("已陈化 ${bv.getAgingYears()} 年"), escapeCsv(bv.storageLocation),
                    escapeCsv(bv.getStatusDisplayName()),
                    escapeCsv(bv.tastingNotes.take(50))
                ).joinToString(",")).append("\r\n")
            }
        } catch (e: Exception) { android.util.Log.w("ExportManager", "导出茶窖名酿数据失败", e) }

        return sb.toString()
    }

    /** 导出 12 馆时效物资联合报表并拉起系统分享面板 */
    fun exportAndShareAllVaultsCsv(activity: android.app.Activity, store: DataStore) {
        try {
            val timeStr = FILE_DATE_FORMAT.format(java.util.Date())
            val filename = "Collecter_12馆时效联合报表_$timeStr.csv"
            val csvText = generateAllVaultsCsv(store)
            val file = writeCsvToFile(activity, filename, csvText)
            shareFile(activity, file, title = "分享【12 馆时效物资联合报表】", mimeType = "text/csv")
        } catch (e: Exception) {
            android.widget.Toast.makeText(activity, "导出失败: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            android.util.Log.w("ExportManager", "导出 12 馆联合报表失败", e)
        }
    }
}
