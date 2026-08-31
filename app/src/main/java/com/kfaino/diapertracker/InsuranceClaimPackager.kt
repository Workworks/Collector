package com.kfaino.diapertracker

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 📑 商业保险契约理赔一键证据链打包引擎 (Insurance Claim Packager)
 * 发生水浸/失窃/火灾等突发意外时，一键聚合全家资产带发票、购买凭证、序列号与照片的审计清单
 */
object InsuranceClaimPackager {

    data class ClaimAuditPackage(
        val totalClaimValue: Double,
        val totalItemsCount: Int,
        val reportGeneratedAt: String,
        val auditMarkdownText: String
    )

    fun buildClaimPackage(store: DataStore, incidentType: String = "家庭财产意外损失"): ClaimAuditPackage {
        val entries = store.loadAll().filter { it.isIn && !it.isRetired }
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val dateStr = sdf.format(Date())

        val sb = StringBuilder()
        sb.append("# 📑 家庭财产商业保险理赔证据清单\n\n")
        sb.append("> **理赔事由**：  \n")
        sb.append("> **审计导出时间**：  \n")
        sb.append("> **数据源**：Collecter 100% 离线私有沙盒凭据库  \n\n")
        sb.append("---\n\n")
        sb.append("## 一、 损失物资清单与发票凭证核验\n\n")
        sb.append("| # | 物品名称/品牌 | 分类 | 购入原价 | 购入时间 | 凭证状态 | 存放位置 |\n")
        sb.append("| :-: | :--- | :--- | :--- | :--- | :--- | :--- |\n")

        var totalVal = 0.0
        entries.forEachIndexed { idx, e ->
            val hasReceipt = if (e.receiptPath.isNotBlank()) "✅ 有发票" else "📸 有实物照片"
            val pDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(e.purchaseDate))
            val subTotal = e.price * e.qty
            totalVal += subTotal
            sb.append("|  |  |  | ¥ |  |  |  |\n")
        }

        sb.append("\n---\n\n")
        sb.append("## 二、 损失总计与审计结论\n\n")
        sb.append("- **申报损失物品总件数**： 件\n")
        sb.append("- **累计损失申报总金额**：**¥**\n")
        sb.append("- **完整照片与发票沙盒原图已加密留存**。\n")

        return ClaimAuditPackage(totalVal, entries.size, dateStr, sb.toString())
    }
}
