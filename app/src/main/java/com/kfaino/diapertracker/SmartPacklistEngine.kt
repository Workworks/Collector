package com.kfaino.diapertracker

import java.util.UUID

/**
 * 🎒 智能出行场景装备动态装配引擎 (Smart Packlist Engine)
 * 结合目的地天气、出行天数、衣橱与数码资产自动组装装箱清单
 */
object SmartPacklistEngine {

    data class PacklistProposal(
        val sceneName: String,
        val totalDays: Int,
        val itemsToPack: List<String>,
        val luggageChecklistId: String
    )

    fun generateProposal(store: DataStore, scene: String, days: Int, isColdWeather: Boolean): PacklistProposal {
        val items = mutableListOf<String>()

        items.add("身份证 / 护照 (家庭证照夹)")
        items.add("充电器与多合一数据线")
        items.add("移动电源 (数码分类)")

        if (isColdWeather) {
            items.add("防风硬壳/羽绒冲锋衣 (衣橱冬季舱)")
            items.add("保暖内衣 × " + days + " 套")
        } else {
            items.add("速干短袖 T恤 × " + days + " 件")
            items.add("防晒衣与遮阳伞")
        }

        items.add("肠胃药与创口贴 (家庭健康药箱)")
        if (scene.contains("露营") || scene.contains("徒步")) {
            items.add("手摇发电手电筒 (应急生命线舱)")
            items.add("驱蚊喷雾与多功能折叠钳 (工具舱)")
        }

        return PacklistProposal(scene, days, items, UUID.randomUUID().toString())
    }
}