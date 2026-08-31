package com.kfaino.diapertracker

import android.content.Context
import java.util.concurrent.TimeUnit

/**
 * 🔔 12 馆综合时效预警聚合器
 * - 被 VaultAlertWidgetProvider（桌面 Widget）和 HomeFragment 今日看板双向复用
 * - 纯离线计算，零网络请求
 */
object VaultAlertAggregator {

    data class AlertItem(
        val emoji: String,
        val label: String,       // 展示给用户的完整文案
        val urgencyDays: Int,    // 剩余天数（越小越紧急，负数=已过期）
        val vaultKey: String,    // 来源馆 key
        val eventKey: String,
        val cycle: String
    )

    /**
     * 聚合全部 12 馆的紧迫待处理事项，按紧急度升序排列
     */
    fun getUrgentAlerts(context: Context, store: DataStore): List<AlertItem> {
        val alerts = mutableListOf<AlertItem>()
        val now = System.currentTimeMillis()
        val dayMs = TimeUnit.DAYS.toMillis(1)

        // ---- 🎟️ 时效卡券：3 天内到期 ----
        try {
            store.getVouchers().forEach { v ->
                if (!v.isUsed && v.expiryDate > 0L) {
                    val days = ((v.expiryDate - now) / dayMs).toInt()
                    if (days <= 3) {
                        val desc = if (days < 0) "已过期 ${-days} 天" else "${days} 天后到期"
                        alerts.add(AlertItem("🎟️", "卡券「${v.title}」$desc", days, "voucher_vault", "voucher_vault:0:${v.id}", v.expiryDate.toString()))
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("VaultAlertAggregator", "扫描卡券时效失败", e)
        }

        // ---- 💊 家庭药箱：30 天内过期 ----
        try {
            store.getMedicines().forEach { m ->
                val eff = m.getEffectiveExpiryDate()
                if (eff > 0L) {
                    val days = ((eff - now) / dayMs).toInt()
                    if (days <= 30) {
                        val desc = if (days < 0) "已过期 ${-days} 天 🚫" else "${days} 天后过期"
                        alerts.add(AlertItem("💊", "药品「${m.name}」$desc", days, "medicine_vault", "medicine_vault:1:${m.id}", eff.toString()))
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("VaultAlertAggregator", "扫描药箱时效失败", e)
        }

        // ---- 🥦 食材鲜度库：3 天内过期 ----
        try {
            store.getFoods().filter { !it.isConsumed }.forEach { f ->
                val eff = f.getEffectiveExpiryDate()
                if (eff > 0L) {
                    val days = ((eff - now) / dayMs).toInt()
                    if (days <= 3) {
                        val desc = if (days < 0) "已过期 ${-days} 天 ⚠️" else "${days} 天后过期"
                        alerts.add(AlertItem("🥦", "食材「${f.name}」$desc", days, "food_vault", "food_vault:2:${f.id}", eff.toString()))
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("VaultAlertAggregator", "扫描食材时效失败", e)
        }

        // ---- 🚨 应急物资：30 天内过期 ----
        try {
            store.getEmergencyItems().forEach { e ->
                if (e.expiryDate > 0L) {
                    val days = ((e.expiryDate - now) / dayMs).toInt()
                    if (days <= 30) {
                        val desc = if (days < 0) "已过期 ${-days} 天" else "${days} 天后失效"
                        alerts.add(AlertItem("🚨", "应急物资「${e.name}」$desc", days, "emergency_vault", "emergency_vault:3:${e.id}", e.expiryDate.toString()))
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("VaultAlertAggregator", "扫描应急物资时效失败", e)
        }

        // ---- 🔧 工具维保：15 天内到期 ----
        try {
            store.getToolRecords().forEach { t ->
                val nextMs = t.getNextMaintenanceDate()
                if (nextMs > 0L) {
                    val days = ((nextMs - now) / dayMs).toInt()
                    if (days <= 15) {
                        val desc = if (days < 0) "已逾期 ${-days} 天" else "${days} 天后需维保"
                        alerts.add(AlertItem("🔧", "工具「${t.name}」$desc", days, "tool_vault", "tool_vault:4:${t.id}", nextMs.toString()))
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("VaultAlertAggregator", "扫描工具维保时效失败", e)
        }

        // ---- 🪴 绿植：浇水逾期 ----
        try {
            store.getPlantRecords().forEach { p ->
                if (p.isWaterDue()) {
                    val nextMs = p.getNextWaterDate()
                    val days = if (nextMs > 0L) ((nextMs - now) / dayMs).toInt() else -1
                    alerts.add(AlertItem("🪴", "${p.name} 需要浇水（已逾期 ${(-days).coerceAtLeast(1)} 天）", days, "plant_vault", "plant_vault:5:${p.id}", p.getNextWaterDate().toString()))
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("VaultAlertAggregator", "扫描绿植养护时效失败", e)
        }

        // ---- 🐾 萌宠驱虫：7 天内到期 ----
        try {
            store.getPetRecords().forEach { p ->
                val nextMs = p.getNextDewormDate()
                if (nextMs > 0L) {
                    val days = ((nextMs - now) / dayMs).toInt()
                    if (days <= 7) {
                        val desc = if (days < 0) "驱虫已逾期 ${-days} 天" else "${days} 天后需驱虫"
                        alerts.add(AlertItem("🐾", "${p.name}（${p.species}）$desc", days, "pet_vault", "pet_vault:6:${p.id}", nextMs.toString()))
                    }
                }
                val nextVaccineMs = p.getNextVaccineDate()
                if (nextVaccineMs > 0L) {
                    val days = ((nextVaccineMs - now) / dayMs).toInt()
                    if (days <= 14) {
                        val desc = if (days < 0) "疫苗已逾期 ${-days} 天" else "${days} 天后需接种疫苗"
                        alerts.add(AlertItem("🐾", "${p.name} $desc", days, "pet_vault", "pet_vault:7:${p.id}", nextVaccineMs.toString()))
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("VaultAlertAggregator", "扫描萌宠健康时效失败", e)
        }

        // ---- 🍷 茶窖名酿：开封已超期 ----
        try {
            store.getBeverageRecords().forEach { b ->
                if (b.isOpenExpired()) {
                    alerts.add(AlertItem("🍷", "「${b.name}」开封已超期，请尽快品鉴", -1, "beverage_vault", "beverage_vault:8:${b.id}", b.openedAt.toString()))
                } else if (b.isPeakDrinkingNow() && !b.isOpened()) {
                    alerts.add(AlertItem("✨", "「${b.name}」已达适饮黄金期，建议开瓶", 0, "beverage_vault", "beverage_vault:9:${b.id}", b.bestDrinkingYear.toString()))
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("VaultAlertAggregator", "扫描茶窖名酿时效失败", e)
        }

        // ---- 📚 书籍外借：超 30 天未还 ----
        try {
            store.getBookRecords().filter { it.isLent() && it.lentDate > 0L }.forEach { b ->
                val lentDays = ((now - b.lentDate) / dayMs).toInt()
                if (lentDays >= 30) {
                    alerts.add(AlertItem("📚", "「${b.title}」已外借 ${lentDays} 天，考虑催还", -lentDays, "book_vault", "book_vault:10:${b.id}", b.lentDate.toString()))
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("VaultAlertAggregator", "扫描书籍外借时效失败", e)
        }

        return alerts.sortedBy { it.urgencyDays }
    }
}
