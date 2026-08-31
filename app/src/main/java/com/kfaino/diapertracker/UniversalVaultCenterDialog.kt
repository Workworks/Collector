package com.kfaino.diapertracker

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kfaino.diapertracker.databinding.DialogUniversalVaultCenterBinding
import com.kfaino.diapertracker.databinding.ItemAggregatedExpiryRecordBinding
import java.util.Date
import java.util.Locale

/**
 * 🏛️ 全维度第一性原理收纳总厅与全景时效生命线透视看板 (Universal Vault Center Dialog)
 * - 聚合家庭 7 大专业收纳馆（卡券 / 证照 / 药箱 / 食材 / 荣誉 / 衣橱 / 应急）
 * - 集中扫描并展示全景时效红绿灯（临期预警 / 已失效过期 / 待定期测试）
 * - 计算家庭资产收纳健康度综合评分 (0-100分)
 */
object UniversalVaultCenterDialog {

    data class AggregatedExpiryItem(
        val vaultType: String,
        val vaultDisplayName: String,
        val title: String,
        val detail: String,
        val isExpired: Boolean,
        val statusText: String,
        val onClickAction: () -> Unit
    )

    fun show(activity: Activity, store: DataStore, onDataChanged: () -> Unit = {}) {
        val binding = DialogUniversalVaultCenterBinding.inflate(LayoutInflater.from(activity))
        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(binding.root)
            .create()

        VaultUiHelper.setupVaultWindow(dialog)

        fun reloadAll() {
            val vouchers = store.getVouchers()
            val docs = store.getIdentityDocs()
            val medicines = store.getMedicines()
            val foods = store.getFoods()
            val honors = store.getHonorCredentials()
            val wardrobes = store.getWardrobeRecords()
            val emergencies = store.getEmergencyItems()

            // 1. 各馆实时角标数据
            binding.tvTileVoucherSub.text = "${vouchers.count { !it.isUsed }} 张"
            binding.tvTileIdentitySub.text = "${docs.size} 份"
            binding.tvTileMedicineSub.text = "${medicines.size} 种"
            binding.tvTileFoodSub.text = "${foods.size} 项"
            binding.tvTileHonorSub.text = "${honors.size} 项"
            binding.tvTileWardrobeSub.text = "${wardrobes.size} 件"
            binding.tvTileEmergencySub.text = "${emergencies.size} 件"

            val totalItems = vouchers.count { !it.isUsed } + docs.size + medicines.size + foods.size + honors.size + wardrobes.size + emergencies.size
            binding.tvStatTotalVaultItems.text = "$totalItems 项"

            // 2. 收集全景临期与失效预警列表
            val expiryList = mutableListOf<AggregatedExpiryItem>()

            // 卡券
            vouchers.filter { !it.isUsed }.forEach { v ->
                if (v.isExpired()) {
                    expiryList.add(
                        AggregatedExpiryItem(
                            vaultType = "voucher",
                            vaultDisplayName = "🎟️ 时效卡券",
                            title = v.title,
                            detail = "面额 ¥${v.valueAmount} · 已于 ${VaultUiHelper.standardDateFormat.format(Date(v.expiryDate))} 过期",
                            isExpired = true,
                            statusText = "🔴 已过期作废",
                            onClickAction = {
                                VoucherVaultDialog.showVoucherVaultDialog(activity, store) { reloadAll(); onDataChanged() }
                            }
                        )
                    )
                } else if (v.isExpiringSoon()) {
                    expiryList.add(
                        AggregatedExpiryItem(
                            vaultType = "voucher",
                            vaultDisplayName = "🎟️ 时效卡券",
                            title = v.title,
                            detail = "面额 ¥${v.valueAmount} · 截止: ${VaultUiHelper.standardDateFormat.format(Date(v.expiryDate))}",
                            isExpired = false,
                            statusText = "🟡 3天内到期",
                            onClickAction = {
                                VoucherVaultDialog.showVoucherVaultDialog(activity, store) { reloadAll(); onDataChanged() }
                            }
                        )
                    )
                }
            }

            // 药品
            medicines.forEach { m ->
                if (m.isExpired()) {
                    expiryList.add(
                        AggregatedExpiryItem(
                            vaultType = "medicine",
                            vaultDisplayName = "💊 家庭药箱",
                            title = m.name,
                            detail = "对症: ${m.symptom} · 已于 ${VaultUiHelper.standardDateFormat.format(Date(m.getEffectiveExpiryDate()))} 过期",
                            isExpired = true,
                            statusText = "🔴 严禁服用",
                            onClickAction = {
                                FamilyMedicineDialog.showMedicineVaultDialog(activity, store) { reloadAll(); onDataChanged() }
                            }
                        )
                    )
                } else if (m.isExpiringSoon()) {
                    expiryList.add(
                        AggregatedExpiryItem(
                            vaultType = "medicine",
                            vaultDisplayName = "💊 家庭药箱",
                            title = m.name,
                            detail = "对症: ${m.symptom} · 截止: ${VaultUiHelper.standardDateFormat.format(Date(m.getEffectiveExpiryDate()))}",
                            isExpired = false,
                            statusText = "🟡 30天内到期",
                            onClickAction = {
                                FamilyMedicineDialog.showMedicineVaultDialog(activity, store) { reloadAll(); onDataChanged() }
                            }
                        )
                    )
                }
            }

            // 食材
            foods.forEach { f ->
                if (f.isExpired()) {
                    expiryList.add(
                        AggregatedExpiryItem(
                            vaultType = "food",
                            vaultDisplayName = "🥦 食材鲜度",
                            title = f.name,
                            detail = "${f.location.ifBlank { "保鲜在库" }} · 已于 ${VaultUiHelper.standardDateFormat.format(Date(f.expiryDate))} 过期",
                            isExpired = true,
                            statusText = "🔴 已变质过期",
                            onClickAction = {
                                FoodVaultDialog.show(activity, store) { reloadAll(); onDataChanged() }
                            }
                        )
                    )
                } else if (f.isExpiringSoon()) {
                    expiryList.add(
                        AggregatedExpiryItem(
                            vaultType = "food",
                            vaultDisplayName = "🥦 食材鲜度",
                            title = f.name,
                            detail = "${f.location.ifBlank { "保鲜在库" }} · 截止: ${VaultUiHelper.standardDateFormat.format(Date(f.expiryDate))}",
                            isExpired = false,
                            statusText = "🟡 3天内急需消灭",
                            onClickAction = {
                                FoodVaultDialog.show(activity, store) { reloadAll(); onDataChanged() }
                            }
                        )
                    )
                }
            }

            // 荣誉
            honors.forEach { h ->
                if (h.isExpired()) {
                    expiryList.add(
                        AggregatedExpiryItem(
                            vaultType = "honor",
                            vaultDisplayName = "🏆 荣誉资质",
                            title = h.title,
                            detail = "持有人: ${h.member} · 年审到期日: ${VaultUiHelper.standardDateFormat.format(Date(h.expiryDate))}",
                            isExpired = true,
                            statusText = "🔴 年审已逾期",
                            onClickAction = {
                                HonorVaultDialog.show(activity, store) { reloadAll(); onDataChanged() }
                            }
                        )
                    )
                } else if (h.isExpiringSoon()) {
                    expiryList.add(
                        AggregatedExpiryItem(
                            vaultType = "honor",
                            vaultDisplayName = "🏆 荣誉资质",
                            title = h.title,
                            detail = "持有人: ${h.member} · 年审至: ${VaultUiHelper.standardDateFormat.format(Date(h.expiryDate))}",
                            isExpired = false,
                            statusText = "🟡 90天内待复审",
                            onClickAction = {
                                HonorVaultDialog.show(activity, store) { reloadAll(); onDataChanged() }
                            }
                        )
                    )
                }
            }

            // 应急防灾
            emergencies.forEach { e ->
                if (e.isExpired()) {
                    expiryList.add(
                        AggregatedExpiryItem(
                            vaultType = "emergency",
                            vaultDisplayName = "🚨 应急防灾",
                            title = e.name,
                            detail = "${e.location} · 失效期: ${VaultUiHelper.standardDateFormat.format(Date(e.expiryDate))}",
                            isExpired = true,
                            statusText = "🔴 物资已失效",
                            onClickAction = {
                                EmergencyVaultDialog.show(activity, store) { reloadAll(); onDataChanged() }
                            }
                        )
                    )
                } else if (e.isExpiringSoon()) {
                    expiryList.add(
                        AggregatedExpiryItem(
                            vaultType = "emergency",
                            vaultDisplayName = "🚨 应急防灾",
                            title = e.name,
                            detail = "${e.location} · 截止: ${VaultUiHelper.standardDateFormat.format(Date(e.expiryDate))}",
                            isExpired = false,
                            statusText = "🟡 30天内临期",
                            onClickAction = {
                                EmergencyVaultDialog.show(activity, store) { reloadAll(); onDataChanged() }
                            }
                        )
                    )
                } else if (e.isNeedsCheck()) {
                    expiryList.add(
                        AggregatedExpiryItem(
                            vaultType = "emergency",
                            vaultDisplayName = "🚨 应急防灾",
                            title = e.name,
                            detail = "${e.location} · 建议每 ${e.rotationIntervalMonths} 个月测试自放电",
                            isExpired = false,
                            statusText = "⏳ 待定期测试",
                            onClickAction = {
                                EmergencyVaultDialog.show(activity, store) { reloadAll(); onDataChanged() }
                            }
                        )
                    )
                }
            }

            // 换季衣橱沉睡
            wardrobes.filter { it.isSleeping() }.forEach { w ->
                expiryList.add(
                    AggregatedExpiryItem(
                        vaultType = "wardrobe",
                        vaultDisplayName = "👗 换季衣橱",
                        title = w.name,
                        detail = "收纳于: ${w.storageLocation} · 超过 180 天未穿出镜",
                        isExpired = false,
                        statusText = "⏳ 沉睡未穿",
                        onClickAction = {
                            WardrobeVaultDialog.show(activity, store) { reloadAll(); onDataChanged() }
                        }
                    )
                )
            }

            // 排序：失效/过期置顶，临期次之
            expiryList.sortByDescending { it.isExpired }

            val totalExpired = expiryList.count { it.isExpired }
            val totalExpiring = expiryList.count { !it.isExpired }

            binding.tvStatExpiringVaultItems.text = "$totalExpiring 项"
            binding.tvStatExpiredVaultItems.text = "$totalExpired 项"

            // 3. 计算家庭收纳健康度评分 (100分制)
            val penalty = (totalExpired * 6 + totalExpiring * 2).coerceAtMost(60)
            val healthScore = 100 - penalty
            binding.tvVaultHealthScore.text = "💯 健康分 $healthScore"
            if (healthScore < 80) {
                binding.tvVaultHealthScore.setTextColor(activity.getColor(R.color.accent_dark))
            } else {
                binding.tvVaultHealthScore.setTextColor(activity.getColor(R.color.primary))
            }

            // 4. 列表渲染
            binding.layoutAggregatedEmpty.visibility = if (expiryList.isEmpty()) View.VISIBLE else View.GONE
            binding.rvAggregatedExpiry.visibility = if (expiryList.isEmpty()) View.GONE else View.VISIBLE

            binding.rvAggregatedExpiry.adapter = AggregatedExpiryAdapter(activity, expiryList)
        }

        binding.rvAggregatedExpiry.layoutManager = LinearLayoutManager(activity)
        reloadAll()

        // 7 大收纳馆穿梭跳转
        binding.tileVoucherVault.applyPressScaleAnimation(0.92f)
        binding.tileVoucherVault.setOnClickListener {
            VoucherVaultDialog.showVoucherVaultDialog(activity, store) { reloadAll(); onDataChanged() }
        }

        binding.tileFamilyVault.applyPressScaleAnimation(0.92f)
        binding.tileFamilyVault.setOnClickListener {
            FamilyVaultDialog.showFamilyVaultDialog(activity, store) { reloadAll(); onDataChanged() }
        }

        binding.tileMedicineVault.applyPressScaleAnimation(0.92f)
        binding.tileMedicineVault.setOnClickListener {
            FamilyMedicineDialog.showMedicineVaultDialog(activity, store) { reloadAll(); onDataChanged() }
        }

        binding.tileFoodVault.applyPressScaleAnimation(0.92f)
        binding.tileFoodVault.setOnClickListener {
            FoodVaultDialog.show(activity, store) { reloadAll(); onDataChanged() }
        }

        binding.tileHonorVault.applyPressScaleAnimation(0.92f)
        binding.tileHonorVault.setOnClickListener {
            HonorVaultDialog.show(activity, store) { reloadAll(); onDataChanged() }
        }

        binding.tileWardrobeVault.applyPressScaleAnimation(0.92f)
        binding.tileWardrobeVault.setOnClickListener {
            WardrobeVaultDialog.show(activity, store) { reloadAll(); onDataChanged() }
        }

        binding.tileEmergencyVault.applyPressScaleAnimation(0.92f)
        binding.tileEmergencyVault.setOnClickListener {
            EmergencyVaultDialog.show(activity, store) { reloadAll(); onDataChanged() }
        }

        binding.btnCloseUniversalCenter.applyPressScaleAnimation(0.92f)
        binding.btnCloseUniversalCenter.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private class AggregatedExpiryAdapter(
        private val activity: Activity,
        private val list: List<AggregatedExpiryItem>
    ) : RecyclerView.Adapter<AggregatedExpiryAdapter.ViewHolder>() {

        class ViewHolder(val binding: ItemAggregatedExpiryRecordBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemAggregatedExpiryRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun getItemCount(): Int = list.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            val binding = holder.binding

            binding.itemAggregatedVaultBadge.text = item.vaultDisplayName
            binding.itemAggregatedStatusBadge.text = item.statusText
            if (item.isExpired) {
                binding.itemAggregatedStatusBadge.setTextColor(activity.getColor(R.color.danger))
            } else {
                binding.itemAggregatedStatusBadge.setTextColor(activity.getColor(R.color.accent_dark))
            }

            binding.itemAggregatedTitle.text = item.title
            binding.itemAggregatedDesc.text = item.detail

            binding.root.applyPressScaleAnimation(0.95f)
            binding.root.setOnClickListener { item.onClickAction() }
        }
    }
}
