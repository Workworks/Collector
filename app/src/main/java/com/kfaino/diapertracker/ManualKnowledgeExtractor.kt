package com.kfaino.diapertracker

import java.util.regex.Pattern

/**
 * 📖 物品使用说明书与保修卡智能知识提取器 (Manual Knowledge Extractor)
 * 从说明书 OCR 全文或备注中自动提取服务热线、保修期限与保养周期
 */
object ManualKnowledgeExtractor {

    data class ExtractedManualInfo(
        val serviceHotline: String?,
        val warrantyMonths: Int?,
        val maintenanceTips: List<String>
    )

    fun extractInfo(manualText: String): ExtractedManualInfo {
        if (manualText.isBlank()) return ExtractedManualInfo(null, null, emptyList())

        // 提取客服电话 (400-xxx-xxxx / 0xx-xxxxxxxx)
        val phonePattern = Pattern.compile("(400-[0-9]{3,4}-[0-9]{3,4}|400[0-9]{7}|0[0-9]{2,3}-[0-9]{7,8})")
        val phoneMatcher = phonePattern.matcher(manualText)
        val phone = if (phoneMatcher.find()) phoneMatcher.group(1) else null

        // 提取保修月份 (如 "质保1年" "保修24个月")
        var warrantyMonths: Int? = null
        if (manualText.contains("质保1年") || manualText.contains("保修1年") || manualText.contains("质保一年")) {
            warrantyMonths = 12
        } else if (manualText.contains("质保2年") || manualText.contains("保修2年") || manualText.contains("质保两年")) {
            warrantyMonths = 24
        } else if (manualText.contains("质保3年") || manualText.contains("保修3年")) {
            warrantyMonths = 36
        }

        val tips = mutableListOf<String>()
        if (manualText.contains("防潮") || manualText.contains("干燥")) tips.add("建议放置于干燥通风处保存")
        if (manualText.contains("避免阳光") || manualText.contains("避光")) tips.add("避免阳光直射与高温环境")
        if (manualText.contains("定期清理") || manualText.contains("滤网")) tips.add("建议每月定期清理滤网与集尘盒")

        return ExtractedManualInfo(phone, warrantyMonths, tips)
    }
}
