package com.kfaino.diapertracker

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kfaino.diapertracker.databinding.DialogRetireItemBinding
import com.kfaino.diapertracker.databinding.FragmentHomeBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val store by lazy { DataStore(requireContext()) }
    private lateinit var assetAdapter: AssetAdapter
    private lateinit var subscriptionAdapter: SubscriptionAdapter

    private var selectedTab = 0 // 0 = 物品, 1 = 订阅
    private var selectedStatusFilter = 0 // 0 = 全部, 1 = 仅在役, 2 = 仅已退役
    private var selectedSortType = 0 // 0 = 按拥有天数降序, 1 = 按价格降序, 2 = 按日均成本降序, 3 = 按最新入库
    private var selectedCategory: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapters()
        setupTopBarAndTabs()
        setupFilters()
        setupBackupBanner()
        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun setupAdapters() {
        assetAdapter = AssetAdapter(
            onEntryClick = { entry ->
                (activity as? MainActivity)?.showEditDialog(entry)
            },
            onMoreClick = { entry, anchorView ->
                showAssetMoreMenu(entry, anchorView)
            }
        )

        subscriptionAdapter = SubscriptionAdapter(
            onSubClick = { sub ->
                SubscriptionManagerDialog.showAddOrEditSubscriptionDialog(requireActivity(), store, sub) {
                    refresh()
                }
            },
            onMoreClick = { sub, anchorView ->
                showSubMoreMenu(sub, anchorView)
            }
        )

        binding.rvAssetList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAssetList.adapter = assetAdapter
    }

    private fun setupTopBarAndTabs() {
        val isSimple = store.isSimpleMode()
        binding.btnOpenFloorplanTop.visibility = if (isSimple) View.GONE else View.VISIBLE
        (binding.tabItems.parent as? View)?.visibility = if (isSimple) View.GONE else View.VISIBLE
        binding.btnFilterMapView.visibility = if (isSimple) View.GONE else View.VISIBLE

        // 1. 多账本快速切换器
        binding.layoutLedgerSwitcher.applyPressScaleAnimation(0.94f)
        binding.layoutLedgerSwitcher.setOnClickListener {
            LedgerManager.showLedgerPicker(requireActivity()) {
                refresh()
            }
        }

        // 2. 搜索过滤
        binding.btnSearchItems.applyPressScaleAnimation(0.92f)
        binding.btnSearchItems.setOnClickListener {
            showSearchDialog()
        }

        // 3. 记一笔快捷入口 (随着实物、订阅、数字相册 Tab 切换不同的专属录入弹窗)
        binding.btnTopAdd.applyPressScaleAnimation(0.92f)
        binding.btnTopAdd.setOnClickListener {
            when (selectedTab) {
                1 -> {
                    SubscriptionManagerDialog.showAddOrEditSubscriptionDialog(requireActivity(), store, null) {
                        refresh()
                    }
                }
                2 -> {
                    DigitalAssetManagerDialog.showAddOrEditDigitalDialog(requireActivity(), store, null) {
                        refresh()
                    }
                }
                else -> {
                    (activity as? MainActivity)?.showAddDialog(presetCategory = selectedCategory)
                }
            }
        }

        // 折叠 / 展开工具箱 (···)
        binding.btnToggleToolsTop.applyPressScaleAnimation(0.90f)
        binding.btnToggleToolsTop.setOnClickListener {
            val isVisible = binding.layoutToolsBar.visibility == View.VISIBLE
            if (isVisible) {
                binding.layoutToolsBar.visibility = View.GONE
                binding.btnToggleToolsTop.imageTintList = ContextCompat.getColorStateList(requireContext(), R.color.text_secondary)
            } else {
                binding.layoutToolsBar.visibility = View.VISIBLE
                binding.btnToggleToolsTop.imageTintList = ContextCompat.getColorStateList(requireContext(), R.color.primary)
            }
        }

        // 可折叠快捷工具箱按钮
        binding.btnAiConciergeTop.applyPressScaleAnimation(0.92f)
        binding.btnAiConciergeTop.setOnClickListener {
            AiConciergeHelper.showConciergeDialog(requireActivity(), store)
        }

        binding.btnKitsTop.applyPressScaleAnimation(0.92f)
        binding.btnKitsTop.setOnClickListener {
            KitManager.showKitListDialog(requireActivity(), store)
        }

        binding.btnScanQrTop.applyPressScaleAnimation(0.92f)
        binding.btnScanQrTop.setOnClickListener {
            (activity as? MainActivity)?.startQrScanner()
        }

        binding.btnOpenFloorplanTop.applyPressScaleAnimation(0.92f)
        binding.btnOpenFloorplanTop.setOnClickListener {
            FloorPlanDialog.show(requireActivity(), store, isSelectMode = false)
        }

        binding.btnInventoryAuditTop.applyPressScaleAnimation(0.92f)
        binding.btnInventoryAuditTop.setOnClickListener {
            InventoryAuditDialog.startAudit(requireActivity(), store) {
                refresh()
            }
        }

        binding.btnLanSyncTop.applyPressScaleAnimation(0.92f)
        binding.btnLanSyncTop.setOnClickListener {
            LanSyncHelper.showLanSyncDialog(requireActivity(), store) {
                refresh()
            }
        }

        binding.btnLendingHubTop.applyPressScaleAnimation(0.92f)
        binding.btnLendingHubTop.setOnClickListener {
            LendingManagerDialog.showLendingHubDialog(requireActivity(), store) {
                refresh()
            }
        }

        binding.btnUniversalVaultCenterTop.applyPressScaleAnimation(0.95f)
        binding.btnUniversalVaultCenterTop.setOnClickListener {
            UniversalVaultCenterDialog.show(requireActivity(), store) {
                refresh()
            }
        }

        binding.btnVoucherVaultTop.applyPressScaleAnimation(0.92f)
        binding.btnVoucherVaultTop.setOnClickListener {
            VoucherVaultDialog.showVoucherVaultDialog(requireActivity(), store) {
                refresh()
            }
        }

        binding.btnFamilyVaultTop.applyPressScaleAnimation(0.92f)
        binding.btnFamilyVaultTop.setOnClickListener {
            FamilyVaultDialog.showFamilyVaultDialog(requireActivity(), store) {
                refresh()
            }
        }

        binding.btnFamilyMedicineTop.applyPressScaleAnimation(0.92f)
        binding.btnFamilyMedicineTop.setOnClickListener {
            FamilyMedicineDialog.showMedicineVaultDialog(requireActivity(), store) {
                refresh()
            }
        }

        binding.btnFoodVaultTop.applyPressScaleAnimation(0.92f)
        binding.btnFoodVaultTop.setOnClickListener {
            FoodVaultDialog.show(requireActivity(), store) {
                refresh()
            }
        }

        binding.btnHonorVaultTop.applyPressScaleAnimation(0.92f)
        binding.btnHonorVaultTop.setOnClickListener {
            HonorVaultDialog.show(requireActivity(), store) {
                refresh()
            }
        }

        binding.btnWardrobeVaultTop.applyPressScaleAnimation(0.92f)
        binding.btnWardrobeVaultTop.setOnClickListener {
            WardrobeVaultDialog.show(requireActivity(), store) {
                refresh()
            }
        }

        binding.btnEmergencyVaultTop.applyPressScaleAnimation(0.92f)
        binding.btnEmergencyVaultTop.setOnClickListener {
            EmergencyVaultDialog.show(requireActivity(), store) {
                refresh()
            }
        }

        binding.btnToolVaultTop.applyPressScaleAnimation(0.92f)
        binding.btnToolVaultTop.setOnClickListener {
            ToolMaintenanceDialog.show(requireActivity(), store) {
                refresh()
            }
        }

        binding.btnPlantVaultTop.applyPressScaleAnimation(0.92f)
        binding.btnPlantVaultTop.setOnClickListener {
            PlantCareDialog.show(requireActivity(), store) {
                refresh()
            }
        }

        binding.btnPetVaultTop.applyPressScaleAnimation(0.92f)
        binding.btnPetVaultTop.setOnClickListener {
            PetCareDialog.show(requireActivity(), store) {
                refresh()
            }
        }

        binding.btnBookVaultTop.applyPressScaleAnimation(0.92f)
        binding.btnBookVaultTop.setOnClickListener {
            BookVaultDialog.show(requireActivity(), store) {
                refresh()
            }
        }

        binding.btnBeverageVaultTop.applyPressScaleAnimation(0.92f)
        binding.btnBeverageVaultTop.setOnClickListener {
            BeverageTeaDialog.show(requireActivity(), store) {
                refresh()
            }
        }

        // 分段切换器 (实物 / 数字相册 / 订阅)
        binding.tabItems.applyPressScaleAnimation(0.94f)
        binding.tabDigitalAssets.applyPressScaleAnimation(0.94f)
        binding.tabSubs.applyPressScaleAnimation(0.94f)

        binding.tabItems.setOnClickListener {
            switchMainTab(0)
        }

        binding.tabDigitalAssets.setOnClickListener {
            switchMainTab(2)
        }

        binding.tabSubs.setOnClickListener {
            switchMainTab(1)
        }
    }

    private fun switchMainTab(tab: Int) {
        selectedTab = tab
        val activeBg = R.drawable.bg_chip_active
        val transparent = Color.TRANSPARENT
        val white = Color.WHITE
        val secColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)

        when (tab) {
            0 -> {
                // 实物 Tab
                binding.tabItems.setBackgroundResource(activeBg)
                binding.tabItems.setTextColor(white)
                binding.tabItems.paint.isFakeBoldText = true

                binding.tabDigitalAssets.setBackgroundColor(transparent)
                binding.tabDigitalAssets.setTextColor(secColor)
                binding.tabDigitalAssets.paint.isFakeBoldText = false

                binding.tabSubs.setBackgroundColor(transparent)
                binding.tabSubs.setTextColor(secColor)
                binding.tabSubs.paint.isFakeBoldText = false

                binding.cardMyAssets.visibility = View.VISIBLE
                binding.cardMySubscriptions.visibility = View.GONE
                binding.rvAssetList.adapter = assetAdapter
            }
            2 -> {
                // 数字相册 Tab
                binding.tabDigitalAssets.setBackgroundResource(activeBg)
                binding.tabDigitalAssets.setTextColor(white)
                binding.tabDigitalAssets.paint.isFakeBoldText = true

                binding.tabItems.setBackgroundColor(transparent)
                binding.tabItems.setTextColor(secColor)
                binding.tabItems.paint.isFakeBoldText = false

                binding.tabSubs.setBackgroundColor(transparent)
                binding.tabSubs.setTextColor(secColor)
                binding.tabSubs.paint.isFakeBoldText = false

                binding.cardMyAssets.visibility = View.VISIBLE
                binding.cardMySubscriptions.visibility = View.GONE
                binding.rvAssetList.adapter = assetAdapter
            }
            else -> {
                // 订阅 Tab
                binding.tabSubs.setBackgroundResource(activeBg)
                binding.tabSubs.setTextColor(white)
                binding.tabSubs.paint.isFakeBoldText = true

                binding.tabItems.setBackgroundColor(transparent)
                binding.tabItems.setTextColor(secColor)
                binding.tabItems.paint.isFakeBoldText = false

                binding.tabDigitalAssets.setBackgroundColor(transparent)
                binding.tabDigitalAssets.setTextColor(secColor)
                binding.tabDigitalAssets.paint.isFakeBoldText = false

                binding.cardMyAssets.visibility = View.GONE
                binding.cardMySubscriptions.visibility = View.VISIBLE
                binding.rvAssetList.adapter = subscriptionAdapter
            }
        }
        refresh()
    }

    private fun setupBackupBanner() {
        binding.btnCloseBackupBanner.applyPressScaleAnimation(0.92f)
        binding.btnCloseBackupBanner.setOnClickListener {
            store.snoozeBackupPrompt(3)
            binding.cardBackupBanner.visibility = View.GONE
        }

        binding.btnRemindLater.applyPressScaleAnimation(0.92f)
        binding.btnRemindLater.setOnClickListener {
            store.snoozeBackupPrompt(3)
            binding.cardBackupBanner.visibility = View.GONE
            Toast.makeText(requireContext(), "已推迟备份提醒，3 天内将不再提示", Toast.LENGTH_SHORT).show()
        }

        binding.btnBackupNow.applyPressScaleAnimation(0.94f)
        binding.btnBackupNow.setOnClickListener {
            (activity as? MainActivity)?.openBackupManager()
        }
    }

    private fun refreshTodayAlertsBanner() {
        try {
            val alerts = VaultAlertAggregator.getUrgentAlerts(requireContext(), store)
            if (alerts.isEmpty()) {
                binding.cardTodayAlertsBanner.visibility = View.GONE
                return
            }

            binding.cardTodayAlertsBanner.visibility = View.VISIBLE
            binding.tvTodayAlertsBadge.text = "${alerts.size} 项待处理"
            binding.layoutTodayAlertsContainer.removeAllViews()

            val display = alerts.take(3)
            for (item in display) {
                val row = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(0, 4, 0, 4)
                }

                val tv = TextView(requireContext()).apply {
                    text = "${item.emoji} ${item.label}"
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                    isSingleLine = true
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                row.addView(tv)
                binding.layoutTodayAlertsContainer.addView(row)
            }

            if (alerts.size > 3) {
                binding.tvTodayAlertsFooter.text = "+ ${alerts.size - 3} 项更多 · 查看全部 12 馆时效看板 ➔"
            } else {
                binding.tvTodayAlertsFooter.text = "点击打开全景收纳大厅 ➔"
            }

            binding.cardTodayAlertsBanner.applyPressScaleAnimation(0.96f)
            binding.cardTodayAlertsBanner.setOnClickListener {
                UniversalVaultCenterDialog.show(requireActivity(), store) {
                    refresh()
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("HomeFragment", "刷新今日待办看板失败", e)
            binding.cardTodayAlertsBanner.visibility = View.GONE
        }
    }

    private fun setupFilters() {
        binding.btnFilterStatus.applyPressScaleAnimation(0.92f)
        binding.btnFilterSort.applyPressScaleAnimation(0.92f)
        binding.btnFilterCategory.applyPressScaleAnimation(0.92f)
        binding.btnFilterMapView.applyPressScaleAnimation(0.92f)

        // 状态筛选：全部 / 仅在役 / 仅已退役
        binding.btnFilterStatus.setOnClickListener {
            val options = listOf("全部状态", "🟢 仅在役物品", "🔴 仅已退役 / 待办归置")
            ModernDialogHelper.showSingleChoiceDialog(
                context = requireContext(),
                title = "筛选在役/退役状态",
                emoji = "🏷️",
                options = options,
                selectedIndex = selectedStatusFilter
            ) { which, _ ->
                selectedStatusFilter = which
                binding.btnFilterStatus.text = when (which) {
                    1 -> "在役 ▾"
                    2 -> "退役 ▾"
                    else -> "全部 ▾"
                }
                refresh()
            }
        }

        // 排序方式
        binding.btnFilterSort.setOnClickListener {
            val options = listOf("⏳ 按拥有天数 (长→短)", "💰 按物品价值 (高→低)", "📉 按日均消费 (高→低)", "🕒 按添加时间 (新→旧)")
            ModernDialogHelper.showSingleChoiceDialog(
                context = requireContext(),
                title = "排序方式",
                emoji = "📊",
                options = options,
                selectedIndex = selectedSortType
            ) { which, _ ->
                selectedSortType = which
                binding.btnFilterSort.text = when (which) {
                    0 -> "天数 ▾"
                    1 -> "价值 ▾"
                    2 -> "日均 ▾"
                    else -> "时间 ▾"
                }
                refresh()
            }
        }

        // 分类筛选
        binding.btnFilterCategory.setOnClickListener {
            val cats = listOf("全部") + store.getCategories()
            val curIdx = if (selectedCategory == null) 0 else cats.indexOf(selectedCategory).coerceAtLeast(0)
            ModernDialogHelper.showSingleChoiceDialog(
                context = requireContext(),
                title = "选择所属分类",
                emoji = "📁",
                options = cats,
                selectedIndex = curIdx
            ) { which, _ ->
                selectedCategory = if (which == 0) null else cats[which]
                binding.btnFilterCategory.text = if (selectedCategory == null) "全部分类 ▾" else "$selectedCategory ▾"
                refresh()
            }
        }

        // 筛选入口/平面图快速进入
        binding.btnFilterMapView.setOnClickListener {
            FloorPlanDialog.show(requireActivity(), store, isSelectMode = false)
        }
    }

    private fun showSearchDialog() {
        GlobalSearchDialog.show(requireActivity(), store) { query ->
            val q = query.trim().lowercase()
            if (q.isNotEmpty()) {
                val all = if (selectedTab == 0) store.getNonSubscriptionEntries() else store.getSubscriptionEntries()
                val filtered = all.filter {
                    it.brand.lowercase().contains(q) ||
                    it.category.lowercase().contains(q) ||
                    it.location.lowercase().contains(q) ||
                    it.notes.lowercase().contains(q) ||
                    it.roomName.lowercase().contains(q)
                }
                if (selectedTab == 0) {
                    assetAdapter.submitList(filtered)
                } else {
                    subscriptionAdapter.submitList(filtered)
                }
                Toast.makeText(requireContext(), "🎉 找到 ${filtered.size} 项主库记录", Toast.LENGTH_SHORT).show()
            } else {
                refresh()
            }
        }
    }

    private fun showAssetMoreMenu(entry: Entry, anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menu.add(0, 7, 0, "关联资料、发票与说明书")
        popup.menu.add(0, 5, 0, "🎞️ 时光胶囊与生活画册")
        if (entry.isLentOut) {
            popup.menu.add(0, 6, 1, "✅ 确认物品归还打卡")
        } else {
            popup.menu.add(0, 6, 1, "📤 登记物品借出")
        }
        popup.menu.add(0, 1, 2, if (entry.isRetired) "🟢 恢复为在役状态" else "📦 物品退役与待办归置 (闲鱼/赠送)")
        popup.menu.add(0, 2, 3, "📍 查看位置轨迹")
        popup.menu.add(0, 3, 4, "✏️ 编辑物品信息")
        popup.menu.add(0, 4, 5, "🗑️ 删除此记录")

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                7 -> { CollectionWorkspaceDialog.showRecord(requireActivity(), "entries:${entry.id}"); true }
                5 -> {
                    LifeCapsuleDialog.showCapsuleDialog(requireActivity(), store, entry) {
                        refresh()
                    }
                    true
                }
                6 -> {
                    if (entry.isLentOut) {
                        LendingManagerDialog.showReturnDialog(requireActivity(), store, entry) {
                            refresh()
                        }
                    } else {
                        LendingManagerDialog.showLendOutDialog(requireActivity(), store, entry) {
                            refresh()
                        }
                    }
                    true
                }
                1 -> {
                    if (entry.isRetired) {
                        store.setRetired(entry.id, false)
                        Toast.makeText(requireContext(), "已恢复为在役状态", Toast.LENGTH_SHORT).show()
                        refresh()
                    } else {
                        showRetireDialog(entry)
                    }
                    true
                }
                2 -> {
                    LocationHistoryDialog.show(requireActivity(), entry)
                    true
                }
                3 -> {
                    (activity as? MainActivity)?.showEditDialog(entry)
                    true
                }
                4 -> {
                    val all = store.loadAll()
                    val idx = all.indexOfFirst { it.id == entry.id }
                    if (idx != -1) {
                        store.deleteEntryAt(idx)
                        Toast.makeText(requireContext(), "已删除【${entry.brand}】", Toast.LENGTH_SHORT).show()
                        refresh()
                    }
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showSubMoreMenu(sub: Entry, anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menu.add(0, 1, 0, "✏️ 编辑订阅信息")
        popup.menu.add(0, 2, 1, "🗑️ 取消/删除此订阅")

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    (activity as? MainActivity)?.showEditDialog(sub)
                    true
                }
                2 -> {
                    val all = store.loadAll()
                    val idx = all.indexOfFirst { it.id == sub.id }
                    if (idx != -1) {
                        store.deleteEntryAt(idx)
                        Toast.makeText(requireContext(), "已删除订阅【${sub.brand}】", Toast.LENGTH_SHORT).show()
                        refresh()
                    }
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    /** 弹出物品退役归置对话框 (挂闲鱼/转转/赠送/封箱/回收) */
    private fun showRetireDialog(entry: Entry) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_retire_item, null)
        val b = DialogRetireItemBinding.bind(dialogView)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()

        b.retireItemInfo.text = "正在为【${entry.brand}】设置退役归置方案"
        b.btnCloseRetire.setOnClickListener { dialog.dismiss() }

        b.btnRestoreActive.setOnClickListener {
            store.setRetired(entry.id, false)
            Toast.makeText(requireContext(), "已恢复为在役状态", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            refresh()
        }

        b.btnConfirmRetire.setOnClickListener {
            val selectedAction = when {
                b.rbXianyu.isChecked -> "📦 挂闲鱼代售"
                b.rbZhuanzhuan.isChecked -> "📱 挂转转二手"
                b.rbGift.isChecked -> "🎁 赠送亲友"
                b.rbArchive.isChecked -> "🗄️ 封箱入库收藏"
                b.rbRecycle.isChecked -> "♻️ 环保回收"
                else -> "🗑️ 损坏报废"
            }
            val soldPrice = b.inputRetireSoldPrice.text.toString().toDoubleOrNull() ?: 0.0
            val note = b.inputRetireNote.text.toString().trim()

            store.setRetired(entry.id, isRetired = true, action = selectedAction, soldPrice = soldPrice, note = note)
            Toast.makeText(requireContext(), "已标记为退役，待办归置：$selectedAction", Toast.LENGTH_LONG).show()
            dialog.dismiss()
            refresh()
        }

        dialog.show()
    }

    fun refresh() {
        if (_binding == null) return
        val allEntries = store.loadAll()

        // 0. 更新当前账本标题
        val curLedger = LedgerManager.getCurrentLedger(requireContext())
        binding.tvHomeTitle.text = "${curLedger.icon} ${curLedger.name}"

        // 0.1 耗材安全库存预警与采购卡片控制
        val lowStock = store.getLowStockItems()
        if (lowStock.isNotEmpty()) {
            binding.cardLowStockAlert.visibility = View.VISIBLE
            binding.tvLowStockTitle.text = "⚠️ 耗材安全库存告急 (${lowStock.size} 项需补货)"
            binding.tvLowStockDesc.text = "【${lowStock.take(3).joinToString("、") { it.brand }}】等耗材已低于预设安全库存线，点击即可复制格式化采购单。"
        } else {
            binding.cardLowStockAlert.visibility = View.GONE
        }

        // 0.2 数据备份横幅持久化显示控制
        binding.cardBackupBanner.visibility = if (store.shouldShowBackupBanner()) View.VISIBLE else View.GONE

        // 0.3 🔔 今日 12 馆时效待办与预警看板
        refreshTodayAlertsBanner()

        val isSimple = store.isSimpleMode()

        // 1. VIP 重要物品核对卡片 (简易模式隐藏)
        val vipEntries = allEntries.filter { (it.isImportant || it.reminderEnabled) && !it.isRetired }
        if (!isSimple && vipEntries.isNotEmpty()) {
            binding.cardImportantVip.visibility = View.VISIBLE
            binding.vipItemsCountBadge.text = "${vipEntries.size} 件重要关注"
            binding.vipItemsListContainer.removeAllViews()

            for (vip in vipEntries.take(3)) {
                val row = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(0, 8, 0, 8)
                }

                val titleTv = TextView(requireContext()).apply {
                    text = "🔑 ${vip.brand}"
                    textSize = 14f
                    setTextColor(Color.parseColor("#F3E8FF"))
                    paint.isFakeBoldText = true
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                val locTv = TextView(requireContext()).apply {
                    text = if (vip.location.isNotBlank()) "📍 ${vip.location}" else "未设位置"
                    textSize = 12f
                    setTextColor(Color.parseColor("#C084FC"))
                    setPadding(0, 0, 12, 0)
                }

                val checkBtn = TextView(requireContext()).apply {
                    text = "✅ 确认在位"
                    textSize = 12f
                    setTextColor(Color.WHITE)
                    setBackgroundResource(R.drawable.bg_chip_active)
                    setPadding(20, 10, 20, 10)
                    applyPressScaleAnimation(0.92f)
                    setOnClickListener {
                        store.confirmItemChecked(vip.id)
                        Toast.makeText(context, "已确认【${vip.brand}】在位！", Toast.LENGTH_SHORT).show()
                        refresh()
                    }
                }

                row.addView(titleTv)
                row.addView(locTv)
                row.addView(checkBtn)
                binding.vipItemsListContainer.addView(row)
            }
        } else {
            binding.cardImportantVip.visibility = View.GONE
        }

        // 2. 根据选中的 Tab 填充数据
        if (selectedTab == 2) {
            // 【📷 数字相册 & 电子资产 Tab】
            val digitalEntries = allEntries.filter { it.isDigital }
            val albumCount = digitalEntries.count { it.digitalType == "album" }
            val softwareCount = digitalEntries.count { it.digitalType == "software" }
            val otherDigital = digitalEntries.size - albumCount - softwareCount
            val totalWorth = digitalEntries.sumOf { it.price }

            binding.tvActiveRetiredRatio.text = "$albumCount 相册 / $softwareCount 授权"
            binding.tvTotalAssetAmount.text = "${digitalEntries.size} 项数字资产"
            binding.tvTotalDailyCost.text = "¥${String.format(Locale.getDefault(), "%,.2f", totalWorth)}"

            var filtered = digitalEntries
            if (selectedCategory != null) {
                filtered = filtered.filter { it.category == selectedCategory }
            }
            filtered = when (selectedSortType) {
                0 -> filtered.sortedByDescending { it.getDaysOwned() }
                1 -> filtered.sortedByDescending { it.price }
                else -> filtered.sortedByDescending { it.ts }
            }

            assetAdapter.submitList(filtered)
            binding.layoutEmptyAssets.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
            binding.tvEmptyText.text = "暂无数字相册或电子资产\n点击右上角 + 登记您的第一份数字回忆/授权Key"

        } else if (selectedTab == 0 || isSimple) {
            // 【物品 Tab】
            val nonSubs = allEntries.filter { !it.isSubscription && !it.isDigital }
            val activeCount = nonSubs.count { !it.isRetired }
            val retiredCount = nonSubs.count { it.isRetired }
            val totalAssetWorth = nonSubs.filter { it.isIn && !it.isRetired }.sumOf { it.price * it.qty }

            if (isSimple) {
                val totalStockQty = nonSubs.filter { it.isIn && !it.isRetired }.sumOf { it.qty }
                binding.tvActiveRetiredRatio.text = "$activeCount 种在库物品"
                binding.tvTotalAssetAmount.text = "$totalStockQty 件"
                binding.tvTotalDailyCost.text = "¥${String.format(Locale.getDefault(), "%,.2f", totalAssetWorth)}"
            } else {
                binding.tvActiveRetiredRatio.text = "$activeCount 在役 / $retiredCount 退役"
                binding.tvTotalAssetAmount.text = "¥${String.format(Locale.getDefault(), "%,.2f", totalAssetWorth)}"

                val activeItems = nonSubs.filter { !it.isRetired }
                val totalDaily = activeItems.sumOf { it.getDailyCost() }
                binding.tvTotalDailyCost.text = "¥${String.format(Locale.getDefault(), "%.2f", totalDaily)}"
            }

            // 过滤列表
            var filtered = nonSubs
            if (selectedStatusFilter == 1) {
                filtered = filtered.filter { !it.isRetired }
            } else if (selectedStatusFilter == 2) {
                filtered = filtered.filter { it.isRetired }
            }

            if (selectedCategory != null) {
                filtered = filtered.filter { it.category == selectedCategory }
            }

            filtered = when (selectedSortType) {
                0 -> filtered.sortedByDescending { it.getDaysOwned() }
                1 -> filtered.sortedByDescending { it.price * it.qty }
                2 -> filtered.sortedByDescending { it.getDailyCost() }
                else -> filtered.sortedByDescending { it.ts }
            }

            assetAdapter.submitList(filtered)
            binding.layoutEmptyAssets.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
            binding.tvEmptyText.text = "暂无物品资产\n点击下方 + 开始记一笔"

        } else {
            // 【订阅 Tab】
            val subs = allEntries.filter { it.isSubscription }
            binding.tvActiveSubsCount.text = "${subs.size} 项活跃订阅"

            var monthlyTotal = 0.0
            for (s in subs) {
                val p = s.price
                monthlyTotal += when (s.subCycle) {
                    "按年" -> p / 12.0
                    "按季" -> p / 3.0
                    "按周" -> p * 4.33
                    else -> p
                }
            }

            binding.tvMonthlySubAmount.text = "¥${String.format(Locale.getDefault(), "%,.2f", monthlyTotal)}"
            binding.tvAnnualSubAmount.text = "¥${String.format(Locale.getDefault(), "%,.2f", monthlyTotal * 12)}"

            val sortedSubs = subs.sortedBy { if (it.subNextBillingDate > 0) it.subNextBillingDate else Long.MAX_VALUE }
            subscriptionAdapter.submitList(sortedSubs)
            binding.layoutEmptyAssets.visibility = if (sortedSubs.isEmpty()) View.VISIBLE else View.GONE
            binding.tvEmptyText.text = "暂无订阅资产 (如 iCloud、宽带、ChatGPT)\n点击下方 + 新增订阅"
        }
    }

    fun getSelectedTab(): Int = selectedTab

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
