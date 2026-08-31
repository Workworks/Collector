package com.kfaino.diapertracker

/**
 * 🎨 自适应极简与极繁收纳人格空间动力学引擎 (Storage Persona Dynamics)
 */
object StoragePersonaDynamics {

    enum class PersonaType {
        MINIMALIST, // 极简主义者（偏好高周转、低保有、大留白）
        COLLECTOR,   // 仓储收藏家（偏好全量档案、精细分类、实物照片）
        EMERGENCY_READY, // 应急战备型（偏好临期监控、防灾物资、高冗余）
        BALANCED     // 均衡家庭管家
    }

    data class PersonaProfile(
        val type: PersonaType,
        val title: String,
        val description: String,
        val recommendedUiLayout: String
    )

    fun analyzeProfile(store: DataStore): PersonaProfile {
        val all = store.loadAll()
        val inUse = all.filter { it.isIn && !it.isRetired }
        val retiredCount = all.count { it.isRetired }
        val photoCount = inUse.count { it.photoPath.isNotBlank() }
        val emergCount = store.getEmergencyItems().size

        return when {
            retiredCount > inUse.size * 0.5 ->
                PersonaProfile(PersonaType.MINIMALIST, "极简断舍离达人", "高流转、极简留白，建议启用极简紧凑视图", "compact_grid")
            photoCount > inUse.size * 0.6 ->
                PersonaProfile(PersonaType.COLLECTOR, "数字档案收藏家", "注重凭证细节与高清图谱，建议开启沉浸画册大图瀑布流", "large_album_waterfall")
            emergCount >= 10 ->
                PersonaProfile(PersonaType.EMERGENCY_READY, "家庭安全卫士", "重视防灾生命线与时效安全，建议置顶时效看板", "dashboard_urgent_focus")
            else ->
                PersonaProfile(PersonaType.BALANCED, "全能家庭管家", "全品类科学规划", "standard_cards")
        }
    }
}