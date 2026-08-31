package com.kfaino.diapertracker

/**
 * 🧩 端侧物理世界常识因果推理引擎 (Physical Causal Reasoner)
 * 自动推理物品物理依赖与安全防护装备联动
 */
object PhysicalCausalReasoner {

    data class CausalDependency(
        val triggeredItem: String,
        val requiredAccessories: List<String>,
        val safetyNotice: String
    )

    fun inferDependencies(itemName: String): CausalDependency? {
        return when {
            itemName.contains("电钻") || itemName.contains("冲击钻") || itemName.contains("切割机") ->
                CausalDependency(itemName, listOf("防护眼镜", "防尘口罩", "配套专用批头/钻头"), "⚠️ 高速电动工具作业前请确保佩戴护目镜！")
            itemName.contains("加湿器") ->
                CausalDependency(itemName, listOf("纯净水/蒸馏水", "除垢柠檬酸"), "💡 建议使用纯净水以防产生白色粉末水垢。")
            itemName.contains("微单") || itemName.contains("单反") ->
                CausalDependency(itemName, listOf("气吹", "镜头纸/清洁湿巾", "备用电池与SD卡"), "📸 外出拍摄请确认电池已充满且防潮盒密封完好。")
            itemName.contains("帐篷") || itemName.contains("天幕") ->
                CausalDependency(itemName, listOf("地钉", "风绳", "防潮垫", "营地锤"), "🏕️ 户外搭建请带齐防风绳与反光地钉。")
            else -> null
        }
    }
}