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
    private var backgroundAtElapsedMs = 0L

    lateinit var binding: ActivityMainBinding
    private val store by lazy { DataStore(this) }
    private val entries = mutableListOf<Entry>()

    private var currentTab = 0
    private var scanResultToInbox = false

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

    private val inboxPhotoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        val filename = ImageVaultHelper.saveUriToVault(this, uri, prefix = "inbox")
        if (filename == null) {
            Toast.makeText(this, "图片保存失败，请重试", Toast.LENGTH_LONG).show()
            return@registerForActivityResult
        }
        CollectionWorkspace(this).upsert("inbox", com.kfaino.collecter.core.WorkspaceRecords.inbox("", filename))
        if (store.consumeContextTip("photo")) {
            MaterialAlertDialogBuilder(this).setTitle("照片已经收好")
                .setMessage("原图会留在收集箱。需要时再补名称、分类或提取文字，不必现在整理。")
                .setPositiveButton("查看收集箱") { _, _ -> CollectionWorkspaceDialog.show(this) }
                .setNegativeButton("知道了", null).show()
        } else Toast.makeText(this, "照片已放入收集箱", Toast.LENGTH_SHORT).show()
        refreshCurrentFragment()
    }
    private val inboxPhotosLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        var saved = 0
        val workspace = CollectionWorkspace(this)
        uris.take(100).forEach { uri ->
            val filename = ImageVaultHelper.saveUriToVault(this, uri, prefix = "inbox")
            if (filename != null) {
                workspace.upsert("inbox", com.kfaino.collecter.core.WorkspaceRecords.inbox("", filename))
                saved++
            }
        }
        Toast.makeText(this, "已将 $saved 张图片放入收集箱", Toast.LENGTH_LONG).show()
        refreshCurrentFragment()
    }

    private val scanQrLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val scanResult = com.journeyapps.barcodescanner.ScanIntentResult.parseActivityResult(result.resultCode, result.data)
            val scanned = scanResult?.contents ?: result.data?.getStringExtra("SCAN_RESULT")
            if (!scanned.isNullOrBlank()) {
                handleScannedResult(scanned)
            }
        } else scanResultToInbox = false
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
            binding.root.post { LowFrictionEntry.showFirstRunIfNeeded(this, store) }
        }

        // 初始化通知渠道；权限在用户首次选择提醒时再请求，避免打断首次记录。
        NotificationHelper.createNotificationChannel(this)
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

    override fun onStart() {
        super.onStart()
        val now = android.os.SystemClock.elapsedRealtime()
        if (store.isBiometricLockEnabled() && BackgroundLockPolicy.shouldRelock(backgroundAtElapsedMs, now)) {
            BiometricLockHelper.isUnlockedThisSession = false
        }
        backgroundAtElapsedMs = 0L
        ScreenshotWatcherHelper.startListening(this)
    }

    override fun onStop() {
        backgroundAtElapsedMs = android.os.SystemClock.elapsedRealtime()
        super.onStop()
        ScreenshotWatcherHelper.stopListening(this)
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

    fun startInboxScanner() {
        scanResultToInbox = true
        startQrScanner()
    }

    fun collectPhotoToInbox() = inboxPhotoLauncher.launch("image/*")

    fun collectPhotosToInbox() = inboxPhotosLauncher.launch("image/*")

    fun requestReminderPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !NotificationHelper.hasNotificationPermission(this)) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
    }

    fun collectLinkToInbox() {
        val input = EditText(this).apply { hint = "粘贴网页链接或一段文字"; minLines = 2 }
        MaterialAlertDialogBuilder(this).setTitle("先收下，稍后整理").setView(input)
            .setNegativeButton("取消", null).setPositiveButton("放进收集箱") { _, _ ->
                val text = input.text.toString().trim()
                if (text.isBlank()) Toast.makeText(this, "内容不能为空", Toast.LENGTH_SHORT).show()
                else {
                    CollectionWorkspace(this).addText(text)
                    Toast.makeText(this, "已放入收集箱", Toast.LENGTH_SHORT).show()
                    refreshCurrentFragment()
                }
            }.show()
    }

    private fun handleScannedResult(scanned: String) {
        if (scanned.startsWith("collecter://family")) {
            FamilyClientDialog.showFromQr(this, scanned)
            return
        }
        if (scanResultToInbox) {
            scanResultToInbox = false
            CollectionWorkspace(this).addText("扫码内容：$scanned")
            Toast.makeText(this, "扫码内容已放入收集箱，可继续扫码", Toast.LENGTH_SHORT).show()
            refreshCurrentFragment()
            binding.root.postDelayed({ startInboxScanner() }, 600)
            return
        }
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

        binding.navAddCenterSlot.visibility = View.GONE
        binding.navReport.visibility = View.VISIBLE
        binding.navHomeLabel.text = "资产"
        binding.navTimelineLabel.text = "收集箱"
        binding.navReportLabel.text = "提醒"
        binding.navProfileLabel.text = "我的"
        binding.navTimelineIcon.setImageResource(R.drawable.ic_inbox)
        binding.navReportIcon.setImageResource(R.drawable.ic_notifications)

        binding.navHome.setOnClickListener {
            if (currentTab != 0) {
                switchFragment(HomeFragment())
                selectTab(0)
            }
        }

        binding.navTimeline.setOnClickListener {
            CollectionWorkspaceDialog.show(this)
        }

        binding.navReport.setOnClickListener {
            CollectionWorkspaceDialog.reminders(this)
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

    fun navigateToLegacyTab(index: Int) {
        val fragment = if (index == 1) TimelineFragment() else ReportFragment()
        switchFragment(fragment)
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

    fun openBackupManager() {
        switchFragment(ProfileFragment().apply { arguments = Bundle().apply { putBoolean("open_backup", true) } })
        selectTab(3)
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
        AddEntryDialog.show(
            activity = this,
            store = store,
            pickPhoto = { cb ->
                onPhotoPickedCallback = cb
                pickPhotoLauncher.launch("image/*")
            },
            pickReceipt = { cb ->
                onPhotoPickedCallback = cb
                pickReceiptLauncher.launch("image/*")
            },
            pickOcr = { cb ->
                onOcrPickedCallback = cb
                ocrPhotoLauncher.launch("image/*")
            },
            onSaved = { refreshCurrentFragment() },
            prefillBrand = prefillBrand,
            prefillCategory = prefillCategory,
            presetCategory = presetCategory,
            prefilledNotes = prefilledNotes,
            editEntry = editEntry,
            editPosition = editPosition,
            presetParsedItem = presetParsedItem
        )
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
