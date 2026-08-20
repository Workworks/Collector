package com.kfaino.diapertracker

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.net.Uri
import android.content.Intent
import android.os.Build
import android.nfc.NfcAdapter
import android.nfc.Tag
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kfaino.diapertracker.databinding.ActivityMainBinding
import com.kfaino.diapertracker.databinding.DialogAddEntryBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private val store by lazy { DataStore(this) }
    private val entries = mutableListOf<Entry>()

    private var currentTab = 0

    // 图片选择回调
    private var onPhotoPickedCallback: ((String) -> Unit)? = null

    private val pickPhotoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val filename = ImageVaultHelper.saveUriToVault(this, uri, prefix = "photo")
            if (filename != null) {
                onPhotoPickedCallback?.invoke(filename)
            } else {
                Toast.makeText(this, "图片处理失败，请重试", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val pickReceiptLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val filename = ImageVaultHelper.saveUriToVault(this, uri, prefix = "receipt")
            if (filename != null) {
                onPhotoPickedCallback?.invoke(filename)
            } else {
                Toast.makeText(this, "凭证处理失败，请重试", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private var onOcrPickedCallback: ((Uri) -> Unit)? = null
    private val ocrPhotoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            onOcrPickedCallback?.invoke(uri)
        }
    }

    private val scanQrLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val scanResult = com.journeyapps.barcodescanner.ScanIntentResult.parseActivityResult(result.resultCode, result.data)
            val scanned = scanResult?.contents ?: result.data?.getStringExtra("SCAN_RESULT")
            if (!scanned.isNullOrBlank()) {
                handleScannedResult(scanned)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 应用用户主题设置
        DataStore.applyThemeMode(store.getThemeMode())
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()
        setupFloatingAddButton()

        // 默认进入首页
        if (savedInstanceState == null) {
            switchFragment(HomeFragment())
            selectTab(0)
        }

        // 初始化通知渠道并请求权限 (Android 13+)
        NotificationHelper.createNotificationChannel(this)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationHelper.hasNotificationPermission(this)) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
        // 调度每日定时闹钟并在应用启动时后台核验一次提醒
        NotificationHelper.scheduleDailyReminder(this)
        java.util.concurrent.Executors.newSingleThreadExecutor().execute {
            NotificationHelper.checkAndSendReminders(this)
        }

        // 初始化类游戏热补丁沙盒引擎并静默检查增量补丁
        HotPatchEngine.init(this)
        HotUpdateManager.checkSilently(this)

        // 后台静默预下载最新版本 APK（无感缓存）
        UpdateManager.preloadSilently(this)

        // 响应小组件一键快速记账
        if (intent?.getBooleanExtra("action_open_add_dialog", false) == true) {
            binding.root.post { showAddDialog() }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent?.getBooleanExtra("action_open_add_dialog", false) == true) {
            binding.root.post { showAddDialog() }
            return
        }
        if (intent == null) return

        // 1. 尝试处理 NFC 标签写入
        val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }
        if (tag != null && NfcHelper.handleTagDiscoveredForWrite(this, tag)) {
            return
        }

        // 2. 尝试处理 NFC 标签「碰一碰」读取寻物
        val parsed = NfcHelper.parseNfcIntent(intent)
        if (parsed != null) {
            val (house, room) = parsed
            val all = store.loadAll()
            val target = all.firstOrNull {
                (it.roomName == room || it.location.contains(room)) && (house.isBlank() || it.houseName == house)
            } ?: Entry(houseName = house.ifBlank { "我的家" }, roomName = room)
            FloorPlanDialog.show(this, store, targetEntry = target)
            Toast.makeText(this, "🏷️ NFC 智能感应成功：$room (${target.houseName})", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        checkBiometricLock()
        NfcHelper.enableForegroundDispatch(this)
        ClipboardOrderBridge.checkClipboard(this)
    }

    override fun onPause() {
        super.onPause()
        NfcHelper.disableForegroundDispatch(this)
    }

    private fun checkBiometricLock() {
        if (store.isBiometricLockEnabled() && !BiometricLockHelper.isUnlockedThisSession) {
            if (BiometricLockHelper.canAuthenticate(this)) {
                BiometricLockHelper.authenticate(
                    activity = this,
                    onSuccess = {
                        // 认证通过
                    },
                    onError = {
                        finish()
                    }
                )
            }
        }
    }

    fun startQrScanner() {
        val intent = com.journeyapps.barcodescanner.ScanOptions()
            .setPrompt("对准收纳箱二维码秒查清单，或扫描商品条形码")
            .setBeepEnabled(true)
            .setOrientationLocked(true)
            .setCaptureActivity(ScannerActivity::class.java)
            .createScanIntent(this)
        scanQrLauncher.launch(intent)
    }

    private fun handleScannedResult(scanned: String) {
        if (scanned.startsWith("collecter://room")) {
            // 收纳箱/房间专属协议
            try {
                val uri = Uri.parse(scanned)
                val house = uri.getQueryParameter("house") ?: ""
                val room = uri.getQueryParameter("room") ?: ""

                val all = store.loadAll()
                val target = all.firstOrNull {
                    (it.roomName == room || it.location.contains(room)) && (house.isBlank() || it.houseName == house)
                } ?: Entry(houseName = house.ifBlank { "🏠 自己的家" }, roomName = room)
                FloorPlanDialog.show(this, store, targetEntry = target)
                Toast.makeText(this, "📍 已识别收纳箱：$room (${target.houseName})", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(this, "收纳箱二维码解析失败", Toast.LENGTH_SHORT).show()
            }
        } else {
            // 商品条码或普通文本
            val all = store.loadAll()
            val matched = all.firstOrNull {
                it.brand.contains(scanned, ignoreCase = true) || it.notes.contains(scanned, ignoreCase = true)
            }
            if (matched != null) {
                Toast.makeText(this, "🔍 已匹配库内资产：${matched.brand}", Toast.LENGTH_SHORT).show()
                showEditDialog(matched)
            } else {
                Toast.makeText(this, "条形码：$scanned，快速录入新资产", Toast.LENGTH_SHORT).show()
                showAddDialog(prefilledNotes = "条形码: $scanned")
            }
        }
    }

    private fun setupBottomNavigation() {
        binding.navHome.applyPressScaleAnimation(0.92f)
        binding.navTimeline.applyPressScaleAnimation(0.92f)
        binding.navReport.applyPressScaleAnimation(0.92f)
        binding.navProfile.applyPressScaleAnimation(0.92f)

        val isSimple = store.isSimpleMode()
        if (isSimple) {
            binding.navReport.visibility = View.GONE
            binding.navHomeLabel.text = "仓库库存"
            binding.navTimelineLabel.text = "出入流水"
            binding.navProfileLabel.text = "系统设置"
        } else {
            binding.navReport.visibility = View.VISIBLE
            binding.navHomeLabel.text = "资产"
            binding.navTimelineLabel.text = "生活流"
            binding.navReportLabel.text = "报表"
            binding.navProfileLabel.text = "我的"
        }

        binding.navHome.setOnClickListener {
            if (currentTab != 0) {
                switchFragment(HomeFragment())
                selectTab(0)
            }
        }

        binding.navTimeline.setOnClickListener {
            if (currentTab != 1) {
                switchFragment(TimelineFragment())
                selectTab(1)
            }
        }

        binding.navReport.setOnClickListener {
            if (currentTab != 2) {
                switchFragment(ReportFragment())
                selectTab(2)
            }
        }

        binding.navProfile.setOnClickListener {
            if (currentTab != 3) {
                switchFragment(ProfileFragment())
                selectTab(3)
            }
        }
    }

    private fun setupFloatingAddButton() {
        binding.fabAdd.setOnClickListener {
            binding.fabAdd.performAppHapticFeedback()
            binding.fabAdd.animate()
                .scaleX(0.84f)
                .scaleY(0.84f)
                .rotation(45f)
                .setDuration(120)
                .withEndAction {
                    binding.fabAdd.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .rotation(0f)
                        .setDuration(220)
                        .setInterpolator(android.view.animation.OvershootInterpolator(2.2f))
                        .start()

                    val homeFragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as? HomeFragment
                    if (homeFragment != null) {
                        when (homeFragment.getSelectedTab()) {
                            1 -> {
                                SubscriptionManagerDialog.showAddOrEditSubscriptionDialog(this, store, null) {
                                    homeFragment.refresh()
                                }
                            }
                            2 -> {
                                DigitalAssetManagerDialog.showAddOrEditDigitalDialog(this, store, null) {
                                    homeFragment.refresh()
                                }
                            }
                            else -> {
                                showAddDialog()
                            }
                        }
                    } else {
                        showAddDialog()
                    }
                }
                .start()
        }
    }

    fun navigateToTab(index: Int) {
        if (currentTab != index) {
            val fragment = when (index) {
                0 -> HomeFragment()
                1 -> TimelineFragment()
                2 -> ReportFragment()
                else -> ProfileFragment()
            }
            switchFragment(fragment)
            selectTab(index)
        }
    }

    private fun switchFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun selectTab(index: Int) {
        currentTab = index
        val active = ContextCompat.getColor(this, R.color.primary)
        val inactive = ContextCompat.getColor(this, R.color.text_secondary)

        data class TabInfo(val icon: ImageView, val label: TextView, val container: View)
        val tabs = listOf(
            TabInfo(binding.navHomeIcon, binding.navHomeLabel, binding.navHome),
            TabInfo(binding.navTimelineIcon, binding.navTimelineLabel, binding.navTimeline),
            TabInfo(binding.navReportIcon, binding.navReportLabel, binding.navReport),
            TabInfo(binding.navProfileIcon, binding.navProfileLabel, binding.navProfile)
        )

        tabs.forEachIndexed { i, tab ->
            val isSelected = (i == index)
            val color = if (isSelected) active else inactive
            tab.icon.imageTintList = ColorStateList.valueOf(color)
            tab.label.setTextColor(color)
            tab.label.paint.isFakeBoldText = isSelected

            if (isSelected) {
                tab.icon.animate()
                    .scaleX(1.18f)
                    .scaleY(1.18f)
                    .translationY(-3f)
                    .setDuration(220)
                    .setInterpolator(android.view.animation.OvershootInterpolator(2.4f))
                    .start()
                tab.label.animate()
                    .scaleX(1.05f)
                    .scaleY(1.05f)
                    .setDuration(220)
                    .start()
            } else {
                tab.icon.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .translationY(0f)
                    .setDuration(180)
                    .start()
                tab.label.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(180)
                    .start()
            }
        }
    }

    fun showEditDialog(entry: Entry) {
        val all = store.loadAll()
        val idx = all.indexOfFirst { it.id == entry.id }
        if (idx != -1) {
            showAddDialog(editEntry = entry, editPosition = idx)
        }
    }

    // ---------- 记一笔 / 编辑记录 高定现代卡片弹窗 (自带微动效、无原生框、折旧、待办归置与订阅) ----------

    fun showAddDialog(
        prefillBrand: String? = null,
        prefillCategory: String? = null,
        presetCategory: String? = null,
        prefilledNotes: String? = null,
        editEntry: Entry? = null,
        editPosition: Int? = null,
        presetParsedItem: SmartIntakeHelper.ParsedItem? = null
    ) {
        val isEditMode = (editEntry != null && editPosition != null)
        val dialogBinding = DialogAddEntryBinding.inflate(layoutInflater)
        val categories = store.getCategories().toMutableList()

        if (!prefilledNotes.isNullOrEmpty() && editEntry == null) {
            dialogBinding.notesInput.setText(prefilledNotes)
        } else if (editEntry != null) {
            dialogBinding.notesInput.setText(editEntry.notes)
        }

        val defaultCat = presetCategory ?: prefillCategory
        var selectedCategory = editEntry?.category ?: defaultCat ?: if (categories.isNotEmpty()) categories[0] else "数码"
        var selectedUnit = editEntry?.unit ?: store.getLastUsedUnit()

        // 购入时间与折旧估值
        var selectedPurchaseDate = editEntry?.purchaseDate ?: System.currentTimeMillis()

        // 空间与平面图图钉参数
        var selectedHouseName = editEntry?.houseName ?: "🏠 自己的家"
        var selectedRoomName = editEntry?.roomName ?: ""
        var selectedPinX = editEntry?.pinX ?: -1f
        var selectedPinY = editEntry?.pinY ?: -1f

        // 退役与待办归置
        var isRetired = editEntry?.isRetired ?: false
        var selectedRetireAction = editEntry?.retiredAction ?: "📦 挂闲鱼代售"

        // 订阅型资产
        var isSubscription = editEntry?.isSubscription ?: false
        var selectedSubCycle = editEntry?.subCycle ?: "按月"
        var selectedNextBillingDate = editEntry?.subNextBillingDate ?: (System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000)

        // 物品管理类型 (折旧资产 / 保质期物品 / 长期耐用 / 消耗品)
        var selectedAssetType = editEntry?.assetType ?: "depreciating"
        var selectedMfgDate = editEntry?.manufactureDate ?: 0L
        var selectedExpDate = editEntry?.expiryDate ?: 0L

        // 实物照片与发票凭证
        var currentPhotoPath = editEntry?.photoPath ?: ""
        var currentReceiptPath = editEntry?.receiptPath ?: ""

        // 重要物品防丢
        var isImportant = editEntry?.isImportant ?: false
        var reminderIntervalDays = editEntry?.reminderIntervalDays ?: 1

        // 记账模式 (true=按总额输入, false=按单件价格输入)
        var isTotalPriceMode = true

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation

        // 标题与图标
        if (isEditMode) {
            dialogBinding.dialogTitle.text = if (store.isSimpleMode()) "编辑库存记录" else "编辑资产记录"
            dialogBinding.btnDialogConfirm.text = "保存修改"
            if (editEntry != null && (editEntry.location.isNotBlank() || editEntry.locationHistory.isNotEmpty())) {
                dialogBinding.btnViewLocationHistory.visibility = if (store.isSimpleMode()) View.GONE else View.VISIBLE
                dialogBinding.btnViewLocationHistory.applyPressScaleAnimation(0.92f)
                dialogBinding.btnViewLocationHistory.setOnClickListener {
                    LocationHistoryDialog.show(this, editEntry)
                }
            }
        } else {
            dialogBinding.dialogTitle.text = if (store.isSimpleMode()) "📦 出入库记一笔" else "记一笔"
            dialogBinding.btnDialogConfirm.text = "确认添加"
            dialogBinding.btnViewLocationHistory.visibility = View.GONE
        }

        // 简易库存模式隐藏高阶非必要卡片与开关
        if (store.isSimpleMode()) {
            dialogBinding.typeSelectContainer.visibility = View.GONE
            dialogBinding.layoutDepreciationSection.visibility = View.GONE
            dialogBinding.layoutExpirySection.visibility = View.GONE
            dialogBinding.layoutDurableSection.visibility = View.GONE
            dialogBinding.btnOpenFloorplanPicker.visibility = View.GONE
            dialogBinding.cardPhotoSlot.visibility = View.GONE
            dialogBinding.cardReceiptSlot.visibility = View.GONE
            dialogBinding.cbIsSubscriptionAsset.visibility = View.GONE
            dialogBinding.layoutSubAssetDetails.visibility = View.GONE
        }

        // 1. 设置入库/出库模式
        val defaultIsIn = editEntry?.isIn ?: true
        if (defaultIsIn) {
            dialogBinding.modeGroup.check(dialogBinding.modeBuy.id)
        } else {
            dialogBinding.modeGroup.check(dialogBinding.modeUse.id)
        }

        // 2. 数量与实时预览辅助函数
        if (editEntry != null) {
            dialogBinding.qtyInput.setText(editEntry.qty.toString())
            dialogBinding.unitInput.setText(editEntry.unit)
        } else {
            dialogBinding.unitInput.setText(selectedUnit)
        }
        dialogBinding.minStockThresholdInput.setText((editEntry?.minStockThreshold ?: 0).toString())

        fun curQty(): Int = dialogBinding.qtyInput.text.toString().toIntOrNull() ?: 1
        fun setQty(v: Int) { dialogBinding.qtyInput.setText(v.coerceIn(1, 99999).toString()) }

        fun updatePreview() {
            val q = dialogBinding.qtyInput.text.toString().toIntOrNull() ?: 0
            val inputVal = dialogBinding.priceInput.text.toString().toDoubleOrNull() ?: 0.0
            val isBuy = dialogBinding.modeGroup.checkedButtonId == dialogBinding.modeBuy.id
            val u = dialogBinding.unitInput.text.toString().trim().ifEmpty { "件" }

            if (isBuy) {
                dialogBinding.priceSectionContainer.alpha = 1.0f
                if (inputVal > 0 && q > 0) {
                    if (isTotalPriceMode) {
                        val totalAmount = inputVal
                        val unitPrice = totalAmount / q
                        dialogBinding.amountPreview.text = "本次总额：¥${String.format(Locale.getDefault(), "%.2f", totalAmount)} · 折合 ¥${String.format(Locale.getDefault(), "%.2f", unitPrice)} / $u"
                    } else {
                        val unitPrice = inputVal
                        val totalAmount = unitPrice * q
                        dialogBinding.amountPreview.text = "本次总额：¥${String.format(Locale.getDefault(), "%.2f", totalAmount)} · $q $u × ¥${String.format(Locale.getDefault(), "%.2f", unitPrice)}"
                    }
                } else {
                    dialogBinding.amountPreview.text = "本次增加/购入：+ $q $u"
                }
            } else {
                dialogBinding.priceSectionContainer.alpha = 0.6f
                dialogBinding.amountPreview.text = "本次消耗出库：- $q $u"
            }
        }

        // 3. 物品类型切换逻辑
        fun updateTypeUI() {
            val activeColor = Color.WHITE
            val inactiveColor = ContextCompat.getColor(this, R.color.text_secondary)
            val buttons = listOf(
                Triple("depreciating", dialogBinding.typeBtnDepreciating, dialogBinding.layoutDepreciationSection),
                Triple("expiring", dialogBinding.typeBtnExpiring, dialogBinding.layoutExpirySection),
                Triple("durable", dialogBinding.typeBtnDurable, dialogBinding.layoutDurableSection),
                Triple("consumable", dialogBinding.typeBtnConsumable, null)
            )

            for ((type, btn, layout) in buttons) {
                val isSel = (type == selectedAssetType)
                btn.setBackgroundResource(if (isSel) R.drawable.bg_chip_active else R.drawable.bg_chip_inactive)
                btn.setTextColor(if (isSel) activeColor else inactiveColor)
                btn.paint.isFakeBoldText = isSel
                layout?.visibility = if (isSel) View.VISIBLE else View.GONE
            }
        }

        dialogBinding.typeBtnDepreciating.applyPressScaleAnimation(0.92f)
        dialogBinding.typeBtnExpiring.applyPressScaleAnimation(0.92f)
        dialogBinding.typeBtnDurable.applyPressScaleAnimation(0.92f)
        dialogBinding.typeBtnConsumable.applyPressScaleAnimation(0.92f)

        dialogBinding.typeBtnDepreciating.setOnClickListener { selectedAssetType = "depreciating"; updateTypeUI() }
        dialogBinding.typeBtnExpiring.setOnClickListener { selectedAssetType = "expiring"; updateTypeUI() }
        dialogBinding.typeBtnDurable.setOnClickListener { selectedAssetType = "durable"; updateTypeUI() }
        dialogBinding.typeBtnConsumable.setOnClickListener { selectedAssetType = "consumable"; updateTypeUI() }
        updateTypeUI()

        // 4. 分类下拉绑定
        if (!categories.contains(selectedCategory)) {
            categories.add(0, selectedCategory)
        }
        val catAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        dialogBinding.categorySpinner.adapter = catAdapter
        val catPos = categories.indexOf(selectedCategory)
        if (catPos != -1) dialogBinding.categorySpinner.setSelection(catPos)

        dialogBinding.categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                selectedCategory = categories[pos]
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        // 5. 品牌/物品名称
        if (!prefillBrand.isNullOrEmpty()) {
            dialogBinding.brandInput.setText(prefillBrand)
        } else if (editEntry != null) {
            dialogBinding.brandInput.setText(editEntry.brand)
        }

        // 6. 购入日期与折旧估值
        fun updatePurchaseDateButton() {
            val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateStr = df.format(Date(selectedPurchaseDate))
            val days = ((System.currentTimeMillis() - selectedPurchaseDate) / (24L * 60 * 60 * 1000)).toInt().coerceAtLeast(1)
            dialogBinding.btnPickPurchaseDate.text = "📅 购入: $dateStr ($days 天)"
        }

        fun updateDurableDateButton() {
            val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val days = ((System.currentTimeMillis() - selectedPurchaseDate) / (24L * 60 * 60 * 1000)).toInt().coerceAtLeast(1)
            dialogBinding.btnPickDurableDate.text = "📅 启用时间: ${df.format(Date(selectedPurchaseDate))} (已使用 $days 天)"
        }

        updatePurchaseDateButton()
        updateDurableDateButton()

        dialogBinding.btnPickPurchaseDate.applyPressScaleAnimation(0.92f)
        dialogBinding.btnPickPurchaseDate.setOnClickListener {
            ModernDatePickerDialog.show(this, selectedPurchaseDate, title = "📅 选择购入/启用日期") { pickedMs ->
                selectedPurchaseDate = pickedMs
                updatePurchaseDateButton()
                updateDurableDateButton()
            }
        }

        if (editEntry != null && editEntry.currentValuation > 0) {
            dialogBinding.inputCurrentValuation.setText(String.format(Locale.getDefault(), "%.2f", editEntry.currentValuation))
        }

        // 7. 保质期与耐用品日期绑定
        fun updateMfgDateButton() {
            if (selectedMfgDate > 0) {
                val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                dialogBinding.btnPickMfgDate.text = "🏭 生产: ${df.format(Date(selectedMfgDate))}"
            } else {
                dialogBinding.btnPickMfgDate.text = "🏭 生产: 未选"
            }
        }

        fun updateExpDateButton() {
            if (selectedExpDate > 0) {
                val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                dialogBinding.btnPickExpDate.text = "⌛ 到期: ${df.format(Date(selectedExpDate))}"
            } else {
                dialogBinding.btnPickExpDate.text = "⌛ 到期: 点击设定"
            }
        }

        updateMfgDateButton()
        updateExpDateButton()

        dialogBinding.btnPickMfgDate.applyPressScaleAnimation(0.92f)
        dialogBinding.btnPickMfgDate.setOnClickListener {
            val initTime = if (selectedMfgDate > 0) selectedMfgDate else System.currentTimeMillis()
            ModernDatePickerDialog.show(this, initTime, title = "🏭 选择生产日期") { pickedMs ->
                selectedMfgDate = pickedMs
                updateMfgDateButton()
            }
        }

        dialogBinding.btnPickExpDate.applyPressScaleAnimation(0.92f)
        dialogBinding.btnPickExpDate.setOnClickListener {
            val initTime = if (selectedExpDate > 0) selectedExpDate else (System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000)
            ModernDatePickerDialog.show(this, initTime, title = "⌛ 选择保质期到期日") { pickedMs ->
                selectedExpDate = pickedMs
                updateExpDateButton()
            }
        }

        fun addDaysToExp(days: Int) {
            val base = if (selectedMfgDate > 0) selectedMfgDate else System.currentTimeMillis()
            selectedExpDate = base + days.toLong() * 24 * 60 * 60 * 1000
            updateExpDateButton()
        }

        dialogBinding.chipExp30d.setOnClickListener { addDaysToExp(30) }
        dialogBinding.chipExp180d.setOnClickListener { addDaysToExp(180) }
        dialogBinding.chipExp1y.setOnClickListener { addDaysToExp(365) }
        dialogBinding.chipExp2y.setOnClickListener { addDaysToExp(730) }
        dialogBinding.chipExp3y.setOnClickListener { addDaysToExp(1095) }

        dialogBinding.btnPickDurableDate.applyPressScaleAnimation(0.92f)
        dialogBinding.btnPickDurableDate.setOnClickListener {
            ModernDatePickerDialog.show(this, selectedPurchaseDate, title = "📅 选择启用日期") { pickedMs ->
                selectedPurchaseDate = pickedMs
                updatePurchaseDateButton()
                updateDurableDateButton()
            }
        }

        if (editEntry != null && editEntry.currentValuation > 0) {
            dialogBinding.inputCurrentValuation.setText(String.format(Locale.getDefault(), "%.2f", editEntry.currentValuation))
        }

        // 5.1 ⚡ AI 智能免录助手绑定 (拍照发票/外包装识物 + 一句话记账)
        fun applySmartParsedItem(item: SmartIntakeHelper.ParsedItem) {
            if (item.brand.isNotBlank()) {
                dialogBinding.brandInput.setText(item.brand)
            }
            val catIdx = categories.indexOf(item.category)
            if (catIdx != -1) {
                dialogBinding.categorySpinner.setSelection(catIdx)
                selectedCategory = item.category
            }
            if (item.qty > 0) {
                dialogBinding.qtyInput.setText(item.qty.toString())
            }
            if (item.unit.isNotBlank()) {
                dialogBinding.unitInput.setText(item.unit)
            }
            if (item.price > 0.0) {
                dialogBinding.priceInput.setText(String.format(Locale.getDefault(), "%.2f", item.price))
            }
            selectedAssetType = item.assetType
            updateTypeUI()
            if (item.purchaseDate > 0) {
                selectedPurchaseDate = item.purchaseDate
                updatePurchaseDateButton()
                updateDurableDateButton()
            }
            if (item.mfgDate > 0) {
                selectedMfgDate = item.mfgDate
                updateMfgDateButton()
            }
            if (item.expDate > 0) {
                selectedExpDate = item.expDate
                updateExpDateButton()
            }
            if (item.notes.isNotBlank()) {
                dialogBinding.notesInput.setText(item.notes)
            }
            updatePreview()
        }

        if (presetParsedItem != null) {
            applySmartParsedItem(presetParsedItem)
        }

        dialogBinding.btnSmartOcr.applyPressScaleAnimation(0.92f)
        dialogBinding.btnSmartOcr.setOnClickListener {
            onOcrPickedCallback = { uri ->
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val bmp = android.graphics.BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    if (bmp != null) {
                        Toast.makeText(this, "🧠 AI 正在分析发票/包装图...", Toast.LENGTH_SHORT).show()
                        SmartIntakeHelper.parseImageOcr(this, bmp, onSuccess = { item ->
                            applySmartParsedItem(item)
                            Toast.makeText(this, "🎉 识别成功！已自动填充表单", Toast.LENGTH_SHORT).show()
                        }, onError = { err ->
                            Toast.makeText(this, "识别提示: $err", Toast.LENGTH_SHORT).show()
                        })
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "图片加载异常: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
            ocrPhotoLauncher.launch("image/*")
        }

        dialogBinding.btnSmartNlp.applyPressScaleAnimation(0.92f)
        dialogBinding.btnSmartNlp.setOnClickListener {
            ModernDialogHelper.showInputDialog(
                context = this,
                title = "自然语言一句话记账",
                subtitle = "粘贴或语音输入记账文本，AI 自动拆解并填充：",
                hint = "例如: 昨天在山姆买了2箱脱脂牛奶单价65保质期到2026-10-15",
                emoji = "💬",
                positiveText = "✨ 智能解析填充",
                isMultiLine = true
            ) { text ->
                if (text.isNotBlank()) {
                    val item = SmartIntakeHelper.parseNaturalLanguage(text)
                    applySmartParsedItem(item)
                    Toast.makeText(this, "🎉 解析完成！已自动填充", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 6. 空间与位置体系绑定
        val houses = store.getHouses()
        val houseNames = houses.map { it.name }
        val houseAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, houseNames)
        dialogBinding.houseSpinner.adapter = houseAdapter
        val housePos = houseNames.indexOf(selectedHouseName)
        if (housePos != -1) dialogBinding.houseSpinner.setSelection(housePos)

        dialogBinding.houseSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                selectedHouseName = houseNames[pos]
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        if (editEntry != null) {
            dialogBinding.locationInput.setText(editEntry.location)
        }

        fun setQuickRoom(roomName: String) {
            selectedRoomName = roomName
            val currentLoc = dialogBinding.locationInput.text.toString().trim()
            if (currentLoc.isEmpty() || !currentLoc.contains(roomName)) {
                dialogBinding.locationInput.setText("$roomName ")
                dialogBinding.locationInput.setSelection(dialogBinding.locationInput.text.length)
            }
        }
        dialogBinding.roomChipHall.setOnClickListener { setQuickRoom("玄关") }
        dialogBinding.roomChipLiving.setOnClickListener { setQuickRoom("客厅") }
        dialogBinding.roomChipBedroom.setOnClickListener { setQuickRoom("主卧") }
        dialogBinding.roomChipKitchen.setOnClickListener { setQuickRoom("厨房") }
        dialogBinding.roomChipStorage.setOnClickListener { setQuickRoom("储物间") }

        dialogBinding.btnOpenFloorplanPicker.applyPressScaleAnimation(0.92f)
        dialogBinding.btnOpenFloorplanPicker.setOnClickListener {
            FloorPlanDialog.show(this, store, isSelectMode = true, currentHouseName = selectedHouseName) { hName, rName, px, py ->
                selectedHouseName = hName
                selectedRoomName = rName
                selectedPinX = px
                selectedPinY = py
                val p = houseNames.indexOf(hName)
                if (p != -1) dialogBinding.houseSpinner.setSelection(p)
                val curLoc = dialogBinding.locationInput.text.toString().trim()
                if (curLoc.isEmpty() || !curLoc.contains(rName)) {
                    dialogBinding.locationInput.setText(if (rName.isNotBlank()) "$rName " else curLoc)
                }
            }
        }

        // 7. 退役与待办归置 (挂闲鱼/转转/赠送/封存/回收)
        val retireActions = DataStore.RETIRED_ACTIONS
        val retireAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, retireActions)
        dialogBinding.spinnerRetireAction.adapter = retireAdapter
        val actPos = retireActions.indexOf(selectedRetireAction)
        if (actPos != -1) dialogBinding.spinnerRetireAction.setSelection(actPos)

        dialogBinding.spinnerRetireAction.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                selectedRetireAction = retireActions[pos]
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        dialogBinding.cbIsRetired.isChecked = isRetired
        dialogBinding.layoutRetireDetails.visibility = if (isRetired) View.VISIBLE else View.GONE
        dialogBinding.cbIsRetired.setOnCheckedChangeListener { _, isChecked ->
            isRetired = isChecked
            dialogBinding.layoutRetireDetails.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        if (editEntry != null && editEntry.retiredSoldPrice > 0) {
            dialogBinding.inputRetireSoldPriceEdit.setText(String.format(Locale.getDefault(), "%.2f", editEntry.retiredSoldPrice))
        }

        dialogBinding.btnAiXianyuCopilot.applyPressScaleAnimation(0.92f)
        dialogBinding.btnAiXianyuCopilot.setOnClickListener {
            val q = dialogBinding.qtyInput.text.toString().toIntOrNull() ?: 1
            val p = dialogBinding.priceInput.text.toString().toDoubleOrNull() ?: 0.0
            val targetEntry = editEntry ?: Entry(
                brand = dialogBinding.brandInput.text.toString().ifBlank { "闲置物品" },
                category = selectedCategory,
                price = p,
                qty = q,
                purchaseDate = selectedPurchaseDate
            )
            ResaleCopilotHelper.showListingCopilotDialog(this, targetEntry) { fastSellPrice ->
                dialogBinding.inputRetireSoldPriceEdit.setText(String.format(Locale.getDefault(), "%.2f", fastSellPrice))
            }
        }

        // 8. 周期订阅资产设置
        val subCycles = listOf("按月", "按年", "按季", "按周")
        val subCycleAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, subCycles)
        dialogBinding.spinnerSubCycle.adapter = subCycleAdapter
        val cyclePos = subCycles.indexOf(selectedSubCycle)
        if (cyclePos != -1) dialogBinding.spinnerSubCycle.setSelection(cyclePos)

        dialogBinding.spinnerSubCycle.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                selectedSubCycle = subCycles[pos]
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        fun updateNextBillingDateButton() {
            val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            dialogBinding.btnPickNextBillingDate.text = "下次扣费: ${df.format(Date(selectedNextBillingDate))}"
        }
        updateNextBillingDateButton()

        dialogBinding.btnPickNextBillingDate.applyPressScaleAnimation(0.92f)
        dialogBinding.btnPickNextBillingDate.setOnClickListener {
            ModernDatePickerDialog.show(this, selectedNextBillingDate, title = "🔄 选择下次扣费日期") { pickedMs ->
                selectedNextBillingDate = pickedMs
                updateNextBillingDateButton()
            }
        }

        // 8. 订阅选项在实物模式中隐藏 (订阅由专属订阅弹窗独立管理)
        dialogBinding.cbIsSubscriptionAsset.visibility = View.GONE
        dialogBinding.layoutSubAssetDetails.visibility = View.GONE
        isSubscription = false

        // 9. 重要物品防丢标记
        dialogBinding.cbIsImportant.isChecked = isImportant
        dialogBinding.layoutSubscriptionOptions.visibility = if (isImportant) View.VISIBLE else View.GONE

        fun updateIntervalChips(days: Int) {
            reminderIntervalDays = days
            val activeColor = Color.WHITE
            val inactiveColor = ContextCompat.getColor(this, R.color.text_primary)
            val chips = listOf(
                Pair(1, dialogBinding.subInterval1),
                Pair(3, dialogBinding.subInterval3),
                Pair(7, dialogBinding.subInterval7),
                Pair(30, dialogBinding.subInterval30)
            )
            for ((d, btn) in chips) {
                if (d == days) {
                    btn.setBackgroundResource(R.drawable.bg_chip_active)
                    btn.setTextColor(activeColor)
                } else {
                    btn.setBackgroundResource(R.drawable.bg_chip_inactive)
                    btn.setTextColor(inactiveColor)
                }
            }
        }
        updateIntervalChips(reminderIntervalDays)

        dialogBinding.cbIsImportant.setOnCheckedChangeListener { _, isChecked ->
            isImportant = isChecked
            dialogBinding.layoutSubscriptionOptions.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        dialogBinding.subInterval1.setOnClickListener { updateIntervalChips(1) }
        dialogBinding.subInterval3.setOnClickListener { updateIntervalChips(3) }
        dialogBinding.subInterval7.setOnClickListener { updateIntervalChips(7) }
        dialogBinding.subInterval30.setOnClickListener { updateIntervalChips(30) }

        // 10. 数量加减与单价/总额模式
        dialogBinding.btnStepDec.applyPressScaleAnimation(0.90f)
        dialogBinding.btnStepInc.applyPressScaleAnimation(0.90f)
        dialogBinding.btnStepDec.setOnClickListener { setQty(curQty() - 1) }
        dialogBinding.btnStepInc.setOnClickListener { setQty(curQty() + 1) }

        fun refreshPriceModeUI() {
            val u = dialogBinding.unitInput.text.toString().trim().ifEmpty { "件" }
            if (isTotalPriceMode) {
                dialogBinding.priceModeLabel.text = "💰 购入总金额 / 订阅价格（元）"
                dialogBinding.priceInput.hint = "例如：5500.00"
                dialogBinding.btnTogglePriceMode.text = "🔄 切换为按单价输入"
            } else {
                dialogBinding.priceModeLabel.text = "🏷️ 单件价格（元/$u）"
                dialogBinding.priceInput.hint = "例如：550.00"
                dialogBinding.btnTogglePriceMode.text = "🔄 切换为按总金额输入"
            }
            updatePreview()
        }

        dialogBinding.btnTogglePriceMode.applyPressScaleAnimation(0.92f)
        dialogBinding.btnTogglePriceMode.setOnClickListener {
            val curVal = dialogBinding.priceInput.text.toString().toDoubleOrNull() ?: 0.0
            val q = curQty()
            isTotalPriceMode = !isTotalPriceMode

            if (curVal > 0 && q > 0) {
                if (isTotalPriceMode) {
                    dialogBinding.priceInput.setText(String.format(Locale.getDefault(), "%.2f", curVal * q))
                } else {
                    dialogBinding.priceInput.setText(String.format(Locale.getDefault(), "%.2f", curVal / q))
                }
            }
            refreshPriceModeUI()
        }

        if (editEntry != null) {
            val totalAmt = editEntry.qty * editEntry.price
            if (totalAmt > 0) {
                dialogBinding.priceInput.setText(String.format(Locale.getDefault(), "%.2f", totalAmt))
            }
            if (editEntry.notes.isNotEmpty()) {
                dialogBinding.notesInput.setText(editEntry.notes)
            }
        }

        dialogBinding.modeGroup.addOnButtonCheckedListener { _, _, _ -> updatePreview() }

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) = updatePreview()
        }
        dialogBinding.qtyInput.addTextChangedListener(watcher)
        dialogBinding.unitInput.addTextChangedListener(watcher)
        dialogBinding.priceInput.addTextChangedListener(watcher)
        updatePreview()

        // 11. 实物照片与购买发票/保修凭证插槽绑定
        fun updatePhotoSlotUI() {
            if (currentPhotoPath.isNotBlank()) {
                val bm = ImageVaultHelper.loadSampledBitmap(this, currentPhotoPath, 200, 200)
                if (bm != null) {
                    dialogBinding.layoutPhotoEmpty.visibility = View.GONE
                    dialogBinding.ivPhotoPreview.visibility = View.VISIBLE
                    dialogBinding.ivPhotoPreview.setImageBitmap(bm)
                    dialogBinding.btnDeletePhoto.visibility = View.VISIBLE
                } else {
                    dialogBinding.layoutPhotoEmpty.visibility = View.VISIBLE
                    dialogBinding.ivPhotoPreview.visibility = View.GONE
                    dialogBinding.btnDeletePhoto.visibility = View.GONE
                }
            } else {
                dialogBinding.layoutPhotoEmpty.visibility = View.VISIBLE
                dialogBinding.ivPhotoPreview.visibility = View.GONE
                dialogBinding.btnDeletePhoto.visibility = View.GONE
            }
        }

        fun updateReceiptSlotUI() {
            if (currentReceiptPath.isNotBlank()) {
                val bm = ImageVaultHelper.loadSampledBitmap(this, currentReceiptPath, 200, 200)
                if (bm != null) {
                    dialogBinding.layoutReceiptEmpty.visibility = View.GONE
                    dialogBinding.ivReceiptPreview.visibility = View.VISIBLE
                    dialogBinding.ivReceiptPreview.setImageBitmap(bm)
                    dialogBinding.btnDeleteReceipt.visibility = View.VISIBLE
                } else {
                    dialogBinding.layoutReceiptEmpty.visibility = View.VISIBLE
                    dialogBinding.ivReceiptPreview.visibility = View.GONE
                    dialogBinding.btnDeleteReceipt.visibility = View.GONE
                }
            } else {
                dialogBinding.layoutReceiptEmpty.visibility = View.VISIBLE
                dialogBinding.ivReceiptPreview.visibility = View.GONE
                dialogBinding.btnDeleteReceipt.visibility = View.GONE
            }
        }

        updatePhotoSlotUI()
        updateReceiptSlotUI()

        dialogBinding.cardPhotoSlot.applyPressScaleAnimation(0.94f)
        dialogBinding.cardPhotoSlot.setOnClickListener {
            if (currentPhotoPath.isBlank()) {
                onPhotoPickedCallback = { filename ->
                    currentPhotoPath = filename
                    updatePhotoSlotUI()
                }
                pickPhotoLauncher.launch("image/*")
            } else {
                PhotoPreviewDialog.show(this, "${dialogBinding.brandInput.text} · 实物照片", currentPhotoPath)
            }
        }

        dialogBinding.btnDeletePhoto.applyPressScaleAnimation(0.90f)
        dialogBinding.btnDeletePhoto.setOnClickListener {
            currentPhotoPath = ""
            updatePhotoSlotUI()
        }

        dialogBinding.cardReceiptSlot.applyPressScaleAnimation(0.94f)
        dialogBinding.cardReceiptSlot.setOnClickListener {
            if (currentReceiptPath.isBlank()) {
                onPhotoPickedCallback = { filename ->
                    currentReceiptPath = filename
                    updateReceiptSlotUI()
                }
                pickReceiptLauncher.launch("image/*")
            } else {
                PhotoPreviewDialog.show(this, "${dialogBinding.brandInput.text} · 发票/保修卡凭证", currentReceiptPath)
            }
        }

        dialogBinding.btnDeleteReceipt.applyPressScaleAnimation(0.90f)
        dialogBinding.btnDeleteReceipt.setOnClickListener {
            currentReceiptPath = ""
            updateReceiptSlotUI()
        }

        // 12. 弹窗操作按钮绑定
        dialogBinding.dialogCloseBtn.applyPressScaleAnimation(0.90f)
        dialogBinding.dialogCloseBtn.setOnClickListener { dialog.dismiss() }

        dialogBinding.btnDialogCancel.applyPressScaleAnimation(0.94f)
        dialogBinding.btnDialogCancel.setOnClickListener { dialog.dismiss() }

        dialogBinding.btnDialogConfirm.applyPressScaleAnimation(0.94f)
        dialogBinding.btnDialogConfirm.setOnClickListener {
            val category = selectedCategory
            val brand = dialogBinding.brandInput.text?.toString()?.trim().orEmpty()
            val isIn = dialogBinding.modeGroup.checkedButtonId == dialogBinding.modeBuy.id
            val qty = dialogBinding.qtyInput.text.toString().toIntOrNull()
            val inputPriceVal = dialogBinding.priceInput.text.toString().toDoubleOrNull() ?: 0.0
            val valuation = dialogBinding.inputCurrentValuation.text.toString().toDoubleOrNull() ?: 0.0
            val notes = dialogBinding.notesInput.text?.toString()?.trim().orEmpty()
            val unit = dialogBinding.unitInput.text.toString().trim().ifEmpty { "件" }
            val location = dialogBinding.locationInput.text?.toString()?.trim().orEmpty()

            val calculatedUnitPrice = if (isIn && qty != null && qty > 0) {
                if (isTotalPriceMode) {
                    inputPriceVal / qty
                } else {
                    inputPriceVal
                }
            } else {
                0.0
            }

            val soldPrice = dialogBinding.inputRetireSoldPriceEdit.text.toString().toDoubleOrNull() ?: 0.0

            when {
                brand.isEmpty() -> Toast.makeText(this, "请输入物品或品牌名称", Toast.LENGTH_SHORT).show()
                qty == null || qty < 1 -> Toast.makeText(this, "数量至少为 1", Toast.LENGTH_SHORT).show()
                else -> {
                    store.setLastUsedUnit(unit)
                    val targetTs = editEntry?.ts ?: System.currentTimeMillis()
                    val targetId = editEntry?.id ?: java.util.UUID.randomUUID().toString()
                    val existingHist = editEntry?.locationHistory ?: emptyList()

                    val newEntry = Entry(
                        id = targetId,
                        category = category,
                        brand = brand,
                        qty = qty,
                        price = calculatedUnitPrice,
                        currentValuation = valuation,
                        purchaseDate = selectedPurchaseDate,
                        ts = targetTs,
                        isIn = isIn,
                        notes = notes,
                        unit = unit,
                        location = location,
                        houseName = selectedHouseName,
                        roomName = selectedRoomName,
                        pinX = selectedPinX,
                        pinY = selectedPinY,
                        locationHistory = existingHist,
                        isImportant = isImportant,
                        reminderEnabled = isImportant,
                        reminderIntervalDays = reminderIntervalDays,
                        lastCheckedAt = if (isImportant && editEntry?.lastCheckedAt ?: 0L == 0L) System.currentTimeMillis() else (editEntry?.lastCheckedAt ?: 0L),
                        isRetired = isRetired,
                        retiredAt = if (isRetired) (if (editEntry?.retiredAt ?: 0L > 0L) editEntry!!.retiredAt else System.currentTimeMillis()) else 0L,
                        retiredAction = if (isRetired) selectedRetireAction else "",
                        retiredSoldPrice = soldPrice,
                        isSubscription = isSubscription,
                        subCycle = selectedSubCycle,
                        subNextBillingDate = selectedNextBillingDate,
                        subAutoRenew = true,
                        assetType = selectedAssetType,
                        manufactureDate = selectedMfgDate,
                        expiryDate = selectedExpDate,
                        photoPath = currentPhotoPath,
                        receiptPath = currentReceiptPath,
                        minStockThreshold = dialogBinding.minStockThresholdInput.text.toString().toIntOrNull() ?: 0
                    )

                    if (isEditMode && editPosition != null) {
                        store.updateEntry(editPosition, newEntry)
                        Toast.makeText(this, "记录已成功修改", Toast.LENGTH_SHORT).show()
                    } else {
                        val currentList = store.loadAll().toMutableList()
                        currentList.add(newEntry)
                        store.saveAll(currentList)
                        Toast.makeText(this, "记录已成功添加", Toast.LENGTH_SHORT).show()
                    }

                    dialog.dismiss()
                    refreshCurrentFragment()
                }
            }
        }

        dialog.show()
    }

    fun applySmartParsedItem(item: SmartIntakeHelper.ParsedItem) {
        showAddDialog(presetParsedItem = item)
    }

    fun refreshCurrentFragment() {
        entries.clear()
        entries.addAll(store.loadAll())
        val current = supportFragmentManager.findFragmentById(R.id.fragment_container)
        when (current) {
            is HomeFragment -> current.refresh()
            is TimelineFragment -> current.refresh()
            is ReportFragment -> current.refresh()
        }
    }
}