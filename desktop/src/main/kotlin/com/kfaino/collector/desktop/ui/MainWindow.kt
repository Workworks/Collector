package com.kfaino.collector.desktop.ui

import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatLightLaf
import com.kfaino.collector.desktop.models.Entry
import com.kfaino.collector.desktop.storage.DesktopDataStore
import com.kfaino.collector.desktop.sync.DesktopWebDavHelper
import java.awt.*
import java.awt.event.KeyEvent
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.border.MatteBorder
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel

/**
 * 跨平台现代化桌面主窗口 (macOS & Linux 原生适配)
 */
class MainWindow(private val store: DesktopDataStore) : JFrame() {

    private val cardLayout = CardLayout()
    private val contentPanel = JPanel(cardLayout)

    private val inventoryTableModel = createTableModel()
    private val inventoryTable = JTable(inventoryTableModel)

    private val timelineTableModel = createTimelineTableModel()
    private val timelineTable = JTable(timelineTableModel)

    private val vouchersTableModel = createVouchersTableModel()
    private val vouchersTable = JTable(vouchersTableModel)

    private val medicinesTableModel = createMedicinesTableModel()
    private val medicinesTable = JTable(medicinesTableModel)

    private val statTotalWorth = JLabel("¥0.00")
    private val statTotalCount = JLabel("0 件")
    private val statDailyCost = JLabel("¥0.00 /天")
    private val lblModeBadge = JLabel("标准全功能模式")

    private val navButtons = mutableListOf<JButton>()
    private var currentNavIndex = 0

    init {
        title = "Collecter · 个人资产与仓库收纳管理系统 (跨平台桌面版)"
        defaultCloseOperation = EXIT_ON_CLOSE
        minimumSize = Dimension(1000, 680)
        setSize(1180, 760)
        setLocationRelativeTo(null)

        try {
            val iconUrl = javaClass.getResource("/icon.png")
            if (iconUrl != null) {
                iconImage = ImageIcon(iconUrl).image
            }
        } catch (e: Exception) {
            System.err.println("加载应用图标失败: ${e.message}")
        }

        setupUI()
        setupShortcuts()
        refreshData()
    }

    private fun setupUI() {
        val rootPanel = JPanel(BorderLayout())
        rootPanel.border = EmptyBorder(0, 0, 0, 0)

        // 1. 顶部操作工具栏
        val topBar = createTopBar()
        rootPanel.add(topBar, BorderLayout.NORTH)

        // 2. 左侧导航侧边栏
        val sideBar = createSideBar()
        rootPanel.add(sideBar, BorderLayout.WEST)

        // 3. 中央各模块面板
        contentPanel.add(createInventoryPanel(), "inventory")
        contentPanel.add(createTimelinePanel(), "timeline")
        contentPanel.add(createVaultsPanel(), "vaults")
        contentPanel.add(createReportPanel(), "report")
        contentPanel.add(createSettingsPanel(), "settings")

        rootPanel.add(contentPanel, BorderLayout.CENTER)
        contentPane = rootPanel
    }

    private fun createTopBar(): JPanel {
        val bar = JPanel(BorderLayout())
        bar.preferredSize = Dimension(0, 56)
        bar.border = BorderFactory.createCompoundBorder(
            MatteBorder(0, 0, 1, 0, Color(45, 55, 72)),
            EmptyBorder(8, 16, 8, 16)
        )

        // 左侧 Logo 与模式徽章
        val left = JPanel(FlowLayout(FlowLayout.LEFT, 12, 0))
        val titleLabel = JLabel("💎 Collecter Desktop").apply {
            font = font.deriveFont(Font.BOLD, 17f)
            foreground = Color(16, 185, 129)
        }
        lblModeBadge.apply {
            font = font.deriveFont(Font.PLAIN, 12f)
            foreground = Color(156, 163, 175)
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color(55, 65, 81), 1, true),
                EmptyBorder(2, 8, 2, 8)
            )
        }
        left.add(titleLabel)
        left.add(lblModeBadge)
        bar.add(left, BorderLayout.WEST)

        // 右侧快捷按钮组
        val right = JPanel(FlowLayout(FlowLayout.RIGHT, 10, 0))

        val btnAdd = JButton("➕ 记一笔 / 出入库 (⌘N)").apply {
            background = Color(16, 185, 129)
            foreground = Color.WHITE
            font = font.deriveFont(Font.BOLD, 13f)
            isFocusPainted = false
            addActionListener { showAddEditDialog() }
        }

        val btnSync = JButton("☁️ WebDAV 同步 (⌘S)").apply {
            isFocusPainted = false
            addActionListener { performWebDavSync() }
        }

        val btnExportCsv = JButton("📊 导出 CSV").apply {
            isFocusPainted = false
            addActionListener { exportCsvToFile() }
        }

        right.add(btnAdd)
        right.add(btnSync)
        right.add(btnExportCsv)
        bar.add(right, BorderLayout.EAST)

        return bar
    }

    private fun createSideBar(): JPanel {
        val side = JPanel()
        side.layout = BoxLayout(side, BoxLayout.Y_AXIS)
        side.preferredSize = Dimension(180, 0)
        side.border = BorderFactory.createCompoundBorder(
            MatteBorder(0, 0, 0, 1, Color(45, 55, 72)),
            EmptyBorder(16, 10, 16, 10)
        )

        fun addNavBtn(index: Int, text: String, cardName: String) {
            val btn = JButton(text).apply {
                maximumSize = Dimension(160, 42)
                alignmentX = Component.CENTER_ALIGNMENT
                horizontalAlignment = SwingConstants.LEFT
                font = font.deriveFont(Font.PLAIN, 13f)
                isFocusPainted = false
                addActionListener {
                    switchTab(index, cardName)
                }
            }
            navButtons.add(btn)
            side.add(btn)
            side.add(Box.createVerticalStrut(8))
        }

        addNavBtn(0, "📦 仓库库存", "inventory")
        addNavBtn(1, "📋 出入流水", "timeline")
        addNavBtn(2, "🎟️ 专业收纳", "vaults")
        addNavBtn(3, "📊 数据报表", "report")
        addNavBtn(4, "⚙️ 系统设置", "settings")

        side.add(Box.createVerticalGlue())
        updateNavSelection(0)
        return side
    }

    private fun switchTab(index: Int, cardName: String) {
        currentNavIndex = index
        cardLayout.show(contentPanel, cardName)
        updateNavSelection(index)
    }

    private fun updateNavSelection(selectedIndex: Int) {
        navButtons.forEachIndexed { idx, btn ->
            if (idx == selectedIndex) {
                btn.background = Color(16, 185, 129)
                btn.foreground = Color.WHITE
                btn.font = btn.font.deriveFont(Font.BOLD, 13f)
            } else {
                btn.background = UIManager.getColor("Button.background")
                btn.foreground = UIManager.getColor("Button.foreground")
                btn.font = btn.font.deriveFont(Font.PLAIN, 13f)
            }
        }
    }

    // ==================== 1. 仓库库存面板 ====================

    private fun createInventoryPanel(): JPanel {
        val panel = JPanel(BorderLayout(0, 12))
        panel.border = EmptyBorder(16, 16, 16, 16)

        // 统计数据大卡片
        val statPanel = JPanel(GridLayout(1, 3, 16, 0))
        statPanel.preferredSize = Dimension(0, 80)

        fun createStatCard(title: String, valueLabel: JLabel, color: Color): JPanel {
            val card = JPanel(BorderLayout())
            card.border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color(55, 65, 81), 1, true),
                EmptyBorder(12, 16, 12, 16)
            )
            val lblTitle = JLabel(title).apply {
                font = font.deriveFont(Font.PLAIN, 12f)
                foreground = Color(156, 163, 175)
            }
            valueLabel.apply {
                font = font.deriveFont(Font.BOLD, 22f)
                foreground = color
            }
            card.add(lblTitle, BorderLayout.NORTH)
            card.add(valueLabel, BorderLayout.CENTER)
            return card
        }

        statPanel.add(createStatCard("在库总估值", statTotalWorth, Color(16, 185, 129)))
        statPanel.add(createStatCard("在库总件数", statTotalCount, Color(59, 130, 246)))
        statPanel.add(createStatCard("日均损耗/消费", statDailyCost, Color(245, 158, 11)))
        panel.add(statPanel, BorderLayout.NORTH)

        // 表格与搜索过滤条
        val tableContainer = JPanel(BorderLayout(0, 8))
        val searchBar = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0))
        val searchField = JTextField(24).apply {
            toolTipText = "输入物品、分类、位置或备注搜索"
        }
        val btnSearch = JButton("🔍 搜索").apply {
            addActionListener { applyFilter(searchField.text.trim()) }
        }
        val btnReset = JButton("重置").apply {
            addActionListener {
                searchField.text = ""
                applyFilter("")
            }
        }
        searchBar.add(JLabel("搜索资产: "))
        searchBar.add(searchField)
        searchBar.add(btnSearch)
        searchBar.add(btnReset)

        tableContainer.add(searchBar, BorderLayout.NORTH)

        setupTableStyle(inventoryTable)
        val scroll = JScrollPane(inventoryTable)
        tableContainer.add(scroll, BorderLayout.CENTER)

        // 底部快捷操作条
        val bottomBar = JPanel(FlowLayout(FlowLayout.RIGHT, 8, 0))
        val btnEdit = JButton("✏️ 编辑所选").apply {
            addActionListener { editSelectedEntry(inventoryTable) }
        }
        val btnDelete = JButton("🗑️ 删除所选").apply {
            addActionListener { deleteSelectedEntry(inventoryTable) }
        }
        bottomBar.add(btnEdit)
        bottomBar.add(btnDelete)
        tableContainer.add(bottomBar, BorderLayout.SOUTH)

        panel.add(tableContainer, BorderLayout.CENTER)
        return panel
    }

    // ==================== 2. 出入流水面板 ====================

    private fun createTimelinePanel(): JPanel {
        val panel = JPanel(BorderLayout(0, 12))
        panel.border = EmptyBorder(16, 16, 16, 16)

        setupTableStyle(timelineTable)
        val scroll = JScrollPane(timelineTable)
        panel.add(scroll, BorderLayout.CENTER)
        return panel
    }

    // ==================== 3. 报表分析面板 ====================

    private fun createReportPanel(): JPanel {
        val panel = JPanel(BorderLayout(0, 16))
        panel.border = EmptyBorder(16, 16, 16, 16)

        val desc = JLabel("📊 资产分布与分类统计（实时计算折旧与持有时长）").apply {
            font = font.deriveFont(Font.BOLD, 15f)
        }
        panel.add(desc, BorderLayout.NORTH)

        val reportArea = JTextArea().apply {
            isEditable = false
            font = Font(Font.MONOSPACED, Font.PLAIN, 13)
            border = EmptyBorder(12, 12, 12, 12)
        }
        panel.add(JScrollPane(reportArea), BorderLayout.CENTER)

        fun updateReportText() {
            val list = store.loadAll().filter { it.isIn && !it.isRetired }
            val sb = StringBuilder()
            val totalWorth = list.sumOf { it.price * it.qty }
            sb.append("========================================================================\n")
            sb.append("  💰 Collecter 资产总览统计报表\n")
            sb.append("========================================================================\n\n")
            sb.append(String.format("  在役资产总值: ¥%,.2f   总件数: %d 件\n\n", totalWorth, list.sumOf { it.qty }))
            sb.append("  [分类版图占比明细]\n")
            sb.append("  ----------------------------------------------------------------------\n")

            val byCategory = list.groupBy { it.category }
            for ((cat, items) in byCategory.entries.sortedByDescending { it.value.sumOf { i -> i.price * i.qty } }) {
                val catWorth = items.sumOf { it.price * it.qty }
                val pct = if (totalWorth > 0) (catWorth / totalWorth) * 100.0 else 0.0
                sb.append(String.format("  • %-12s : ¥%,10.2f  (占比 %5.1f%%)  |  共 %d 件\n", cat, catWorth, pct, items.sumOf { it.qty }))
            }

            sb.append("\n  ----------------------------------------------------------------------\n")
            sb.append("  [闲置资产预警 (>180 天未打卡)]\n")
            val idleItems = list.filter { it.getDaysOwned() > 180 }
            if (idleItems.isEmpty()) {
                sb.append("  🎉 暂无超过 180 天的闲置资产，资产流转非常健康！\n")
            } else {
                for (item in idleItems) {
                    sb.append(String.format("  ⚠️ %-16s | 拥有天数: %4d 天 | 购入单价: ¥%,.2f\n", item.brand, item.getDaysOwned(), item.price))
                }
            }
            reportArea.text = sb.toString()
        }

        updateReportText()
        return panel
    }

    // ==================== 4. 系统设置面板 ====================

    private fun createSettingsPanel(): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = EmptyBorder(20, 24, 20, 24)

        fun createSectionTitle(title: String): JLabel {
            return JLabel(title).apply {
                font = font.deriveFont(Font.BOLD, 15f)
                foreground = Color(16, 185, 129)
                alignmentX = Component.LEFT_ALIGNMENT
            }
        }

        // 1. 简易模式开关
        panel.add(createSectionTitle("📦 模式切换"))
        panel.add(Box.createVerticalStrut(8))

        val chkSimple = JCheckBox("开启「简易库存模式」（隐藏折旧/订阅/复杂报表，纯粹聚焦库存出入库）").apply {
            isSelected = store.isSimpleMode()
            alignmentX = Component.LEFT_ALIGNMENT
            addActionListener {
                store.setSimpleMode(isSelected)
                refreshData()
                JOptionPane.showMessageDialog(this@MainWindow, if (isSelected) "已切换至「简易库存模式」" else "已恢复「标准全功能模式」")
            }
        }
        panel.add(chkSimple)
        panel.add(Box.createVerticalStrut(20))

        // 2. WebDAV 私有云配置
        panel.add(createSectionTitle("☁️ WebDAV 私有云同步配置 (与手机移动端互通)"))
        panel.add(Box.createVerticalStrut(8))

        val form = JPanel(GridLayout(3, 2, 10, 10)).apply {
            alignmentX = Component.LEFT_ALIGNMENT
            maximumSize = Dimension(600, 120)
        }
        val tfUrl = JTextField(store.getWebDavUrl())
        val tfUser = JTextField(store.getWebDavUsername())
        val tfPass = JPasswordField(store.getWebDavPassword())

        form.add(JLabel("服务器 URL (支持坚果云/Nextcloud):"))
        form.add(tfUrl)
        form.add(JLabel("用户名 / 账号:"))
        form.add(tfUser)
        form.add(JLabel("应用独立密码 / Token:"))
        form.add(tfPass)
        panel.add(form)

        panel.add(Box.createVerticalStrut(10))
        val btnSaveWebDav = JButton("💾 保存 WebDAV 配置并测试连接").apply {
            alignmentX = Component.LEFT_ALIGNMENT
            addActionListener {
                store.setWebDavUrl(tfUrl.text)
                store.setWebDavUsername(tfUser.text)
                store.setWebDavPassword(String(tfPass.password))
                val res = DesktopWebDavHelper.testConnection(store)
                JOptionPane.showMessageDialog(this@MainWindow, res.message, if (res.isSuccess) "连接成功" else "连接失败", if (res.isSuccess) JOptionPane.INFORMATION_MESSAGE else JOptionPane.ERROR_MESSAGE)
            }
        }
        panel.add(btnSaveWebDav)

        panel.add(Box.createVerticalStrut(24))
        panel.add(createSectionTitle("💾 数据备份与本地恢复"))
        panel.add(Box.createVerticalStrut(8))

        val btnRestoreCloud = JButton("📥 从 WebDAV 云端一键恢复并合并数据").apply {
            alignmentX = Component.LEFT_ALIGNMENT
            addActionListener {
                val res = DesktopWebDavHelper.downloadAndRestore(store) { preview ->
                    JOptionPane.showConfirmDialog(this@MainWindow, preview, "恢复前预览", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION
                }
                if (res.isSuccess) {
                    refreshData()
                    JOptionPane.showMessageDialog(this@MainWindow, res.message)
                } else {
                    JOptionPane.showMessageDialog(this@MainWindow, res.message, "恢复失败", JOptionPane.ERROR_MESSAGE)
                }
            }
        }
        panel.add(btnRestoreCloud)

        panel.add(Box.createVerticalGlue())
        return panel
    }

    // ==================== 表格渲染与数据更新 ====================

    private fun createTableModel(): DefaultTableModel {
        val cols = arrayOf("物品名称/品牌", "分类", "数量", "单位", "单价(¥)", "总额(¥)", "放置位置", "日均消费(元/天)", "状态", "ID")
        return object : DefaultTableModel(cols, 0) {
            override fun isCellEditable(row: Int, column: Int) = false
        }
    }

    private fun createTimelineTableModel(): DefaultTableModel {
        val cols = arrayOf("记录时间", "类型", "物品名称/品牌", "分类", "数量变动", "金额(¥)", "放置位置", "备注")
        return object : DefaultTableModel(cols, 0) {
            override fun isCellEditable(row: Int, column: Int) = false
        }
    }

    private fun createVouchersTableModel(): DefaultTableModel {
        val cols = arrayOf("卡券名称", "类型", "面额/余次", "到期时间", "适用平台", "状态", "备注", "ID")
        return object : DefaultTableModel(cols, 0) {
            override fun isCellEditable(row: Int, column: Int) = false
        }
    }

    private fun createMedicinesTableModel(): DefaultTableModel {
        val cols = arrayOf("药品名称", "分类", "剂型", "余量", "单位", "存放位置", "用法用量", "保质期", "状态", "ID")
        return object : DefaultTableModel(cols, 0) {
            override fun isCellEditable(row: Int, column: Int) = false
        }
    }

    private fun createVaultsPanel(): JPanel {
        val panel = JPanel(BorderLayout(0, 12))
        panel.border = EmptyBorder(16, 16, 16, 16)

        val tabbed = JTabbedPane()

        // 🎟️ 卡券 Tab
        val voucherPanel = JPanel(BorderLayout(0, 8))
        val voucherToolBar = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0))
        val btnAddVoucher = JButton("➕ 新增卡券").apply {
            addActionListener { showAddVoucherDialog() }
        }
        val btnDeleteVoucher = JButton("🗑️ 删除卡券").apply {
            addActionListener {
                val row = vouchersTable.selectedRow
                if (row >= 0) {
                    val id = vouchersTableModel.getValueAt(row, 7) as String
                    val list = store.getVouchers().filterNot { it.id == id }
                    store.saveVouchers(list)
                    refreshData()
                } else {
                    JOptionPane.showMessageDialog(this@MainWindow, "请先在列表中选中一张卡券")
                }
            }
        }
        voucherToolBar.add(btnAddVoucher)
        voucherToolBar.add(btnDeleteVoucher)
        voucherPanel.add(voucherToolBar, BorderLayout.NORTH)
        setupTableStyle(vouchersTable)
        voucherPanel.add(JScrollPane(vouchersTable), BorderLayout.CENTER)
        tabbed.addTab("🎟️ 时效卡券", voucherPanel)

        // 💊 药箱 Tab
        val medicinePanel = JPanel(BorderLayout(0, 8))
        val medicineToolBar = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0))
        val btnAddMedicine = JButton("➕ 新增药品").apply {
            addActionListener { showAddMedicineDialog() }
        }
        val btnDeleteMedicine = JButton("🗑️ 删除药品").apply {
            addActionListener {
                val row = medicinesTable.selectedRow
                if (row >= 0) {
                    val id = medicinesTableModel.getValueAt(row, 9) as String
                    val list = store.getMedicines().filterNot { it.id == id }
                    store.saveMedicines(list)
                    refreshData()
                } else {
                    JOptionPane.showMessageDialog(this@MainWindow, "请先在列表中选中一项药品")
                }
            }
        }
        medicineToolBar.add(btnAddMedicine)
        medicineToolBar.add(btnDeleteMedicine)
        medicinePanel.add(medicineToolBar, BorderLayout.NORTH)
        setupTableStyle(medicinesTable)
        medicinePanel.add(JScrollPane(medicinesTable), BorderLayout.CENTER)
        tabbed.addTab("💊 家庭药箱", medicinePanel)

        panel.add(tabbed, BorderLayout.CENTER)
        return panel
    }

    private fun showAddVoucherDialog() {
        val dialog = JDialog(this, "➕ 登记时效卡券", true)
        dialog.setSize(420, 360)
        dialog.setLocationRelativeTo(this)

        val form = JPanel(GridLayout(5, 2, 8, 10)).apply {
            border = EmptyBorder(16, 20, 16, 20)
        }
        val tfTitle = JTextField()
        val cbType = JComboBox(arrayOf("满减券 (coupon)", "计次卡 (times_card)", "代金券 (cash_voucher)", "会员权益 (privilege)"))
        val tfAmount = JTextField("10.0")
        val tfPlatform = JTextField("美团/京东/线下通用")
        val tfNotes = JTextField()

        form.add(JLabel("券名/权益名称 (*):"))
        form.add(tfTitle)
        form.add(JLabel("卡券类型:"))
        form.add(cbType)
        form.add(JLabel("面额金额 (¥):"))
        form.add(tfAmount)
        form.add(JLabel("适用平台/商家:"))
        form.add(tfPlatform)
        form.add(JLabel("使用规则/备注:"))
        form.add(tfNotes)

        val btnSave = JButton("💾 保存卡券").apply {
            addActionListener {
                val title = tfTitle.text.trim()
                if (title.isBlank()) {
                    JOptionPane.showMessageDialog(dialog, "请输入卡券名称！")
                    return@addActionListener
                }
                val amount = tfAmount.text.toDoubleOrNull() ?: 0.0
                val v = com.kfaino.collector.desktop.models.VoucherRecord(
                    title = title,
                    type = when (cbType.selectedIndex) {
                        1 -> "times_card"
                        2 -> "cash_voucher"
                        3 -> "privilege"
                        else -> "coupon"
                    },
                    valueAmount = amount,
                    platform = tfPlatform.text.trim(),
                    notes = tfNotes.text.trim(),
                    expiryDate = System.currentTimeMillis() + 30L * 24 * 3600 * 1000
                )
                val list = store.getVouchers().toMutableList().apply { add(0, v) }
                store.saveVouchers(list)
                dialog.dispose()
                refreshData()
            }
        }
        val p = JPanel(BorderLayout())
        p.add(form, BorderLayout.CENTER)
        p.add(btnSave, BorderLayout.SOUTH)
        dialog.contentPane = p
        dialog.isVisible = true
    }

    private fun showAddMedicineDialog() {
        val dialog = JDialog(this, "➕ 登记家庭常备药", true)
        dialog.setSize(420, 420)
        dialog.setLocationRelativeTo(this)

        val form = JPanel(GridLayout(7, 2, 8, 10)).apply {
            border = EmptyBorder(16, 20, 16, 20)
        }
        val tfName = JTextField()
        val cbCategory = JComboBox(arrayOf("发热镇痛 (fever)", "感冒咳嗽 (cold)", "肠胃消化 (digest)", "外伤消炎 (trauma)", "抗过敏 (allergy)", "慢病常备 (chronic)", "其他"))
        val spQty = JSpinner(SpinnerNumberModel(1, 1, 9999, 1))
        val tfUnit = JTextField("盒")
        val tfLocation = JTextField("家庭药箱")
        val tfDosage = JTextField("一次1片，一日2次")
        val tfNotes = JTextField()

        form.add(JLabel("药品名称 (*):"))
        form.add(tfName)
        form.add(JLabel("药品分类:"))
        form.add(cbCategory)
        form.add(JLabel("数量与单位:"))
        val qtyP = JPanel(GridLayout(1, 2, 4, 0)).apply {
            add(spQty)
            add(tfUnit)
        }
        form.add(qtyP)
        form.add(JLabel("存放位置:"))
        form.add(tfLocation)
        form.add(JLabel("用法用量:"))
        form.add(tfDosage)
        form.add(JLabel("禁忌/备忘:"))
        form.add(tfNotes)

        val btnSave = JButton("💾 保存药品").apply {
            addActionListener {
                val name = tfName.text.trim()
                if (name.isBlank()) {
                    JOptionPane.showMessageDialog(dialog, "请输入药品名称！")
                    return@addActionListener
                }
                val m = com.kfaino.collector.desktop.models.MedicineRecord(
                    name = name,
                    category = when (cbCategory.selectedIndex) {
                        0 -> "fever"
                        1 -> "cold"
                        2 -> "digest"
                        3 -> "trauma"
                        4 -> "allergy"
                        5 -> "chronic"
                        else -> "other"
                    },
                    qty = spQty.value as Int,
                    unit = tfUnit.text.trim().ifBlank { "盒" },
                    location = tfLocation.text.trim().ifBlank { "家庭药箱" },
                    dosage = tfDosage.text.trim(),
                    contraindications = tfNotes.text.trim(),
                    expiryDate = System.currentTimeMillis() + 365L * 24 * 3600 * 1000
                )
                val list = store.getMedicines().toMutableList().apply { add(0, m) }
                store.saveMedicines(list)
                dialog.dispose()
                refreshData()
            }
        }
        val p = JPanel(BorderLayout())
        p.add(form, BorderLayout.CENTER)
        p.add(btnSave, BorderLayout.SOUTH)
        dialog.contentPane = p
        dialog.isVisible = true
    }

    private fun setupTableStyle(table: JTable) {
        table.rowHeight = 32
        table.setShowGrid(true)
        table.gridColor = Color(45, 55, 72)
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        table.tableHeader.reorderingAllowed = false
        table.tableHeader.font = table.tableHeader.font.deriveFont(Font.BOLD, 12f)

        val centerRenderer = DefaultTableCellRenderer().apply { horizontalAlignment = SwingConstants.CENTER }
        val rightRenderer = DefaultTableCellRenderer().apply { horizontalAlignment = SwingConstants.RIGHT }

        if (table.columnCount > 1) table.columnModel.getColumn(1).cellRenderer = centerRenderer
        if (table.columnCount > 2) table.columnModel.getColumn(2).cellRenderer = centerRenderer
        if (table.columnCount > 3) table.columnModel.getColumn(3).cellRenderer = centerRenderer
        if (table.columnCount > 4) table.columnModel.getColumn(4).cellRenderer = rightRenderer
        if (table.columnCount > 5) table.columnModel.getColumn(5).cellRenderer = rightRenderer
    }

    private fun refreshData() {
        val all = store.loadAll()
        val isSimple = store.isSimpleMode()
        lblModeBadge.text = if (isSimple) "📦 简易库存模式" else "🌟 标准全功能模式"

        // 1. 刷新统计数据
        val inStock = all.filter { it.isIn && !it.isRetired }
        val totalWorth = inStock.sumOf { it.price * it.qty }
        val totalQty = inStock.sumOf { it.qty }
        val totalDaily = inStock.sumOf { it.getDailyCost() }

        statTotalWorth.text = String.format(Locale.getDefault(), "¥%,.2f", totalWorth)
        statTotalCount.text = "$totalQty 件 (${inStock.size} 种)"
        statDailyCost.text = String.format(Locale.getDefault(), "¥%.2f /天", totalDaily)

        // 2. 填充库存表格
        inventoryTableModel.rowCount = 0
        for (e in inStock) {
            inventoryTableModel.addRow(arrayOf(
                e.brand,
                e.category,
                e.qty,
                e.unit,
                String.format(Locale.getDefault(), "%.2f", e.price),
                String.format(Locale.getDefault(), "%.2f", e.price * e.qty),
                if (e.location.isNotBlank()) e.location else "未设定",
                String.format(Locale.getDefault(), "%.2f", e.getDailyCost()),
                if (e.isRetired) "已退役" else "在役中",
                e.id
            ))
        }

        // 3. 填充流水表格
        timelineTableModel.rowCount = 0
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        for (e in all.sortedByDescending { it.ts }) {
            timelineTableModel.addRow(arrayOf(
                sdf.format(Date(e.ts)),
                if (e.isIn) "🟢 入库 / 拥有" else "🔴 出库 / 消耗",
                e.brand,
                e.category,
                "${if (e.isIn) "+" else "-"}${e.qty} ${e.unit}",
                String.format(Locale.getDefault(), "%.2f", e.price * e.qty),
                e.location,
                e.notes
            ))
        }

        // 4. 填充卡券表格
        vouchersTableModel.rowCount = 0
        val daySdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        for (v in store.getVouchers()) {
            vouchersTableModel.addRow(arrayOf(
                v.title,
                when (v.type) {
                    "times_card" -> "🎫 计次服务卡"
                    "cash_voucher" -> "💰 无门槛代金券"
                    "privilege" -> "👑 会员专属权益"
                    else -> "🎟️ 满减优惠券"
                },
                if (v.type == "times_card") "${v.remainingTimes}/${v.totalTimes} 次" else "¥${String.format(Locale.getDefault(), "%.2f", v.valueAmount)}",
                if (v.expiryDate > 0) daySdf.format(Date(v.expiryDate)) else "长期有效",
                v.platform,
                if (v.isExpired()) "⚠️ 已过期" else if (v.isExpiringSoon()) "🟠 临期" else "🟢 有效",
                v.notes,
                v.id
            ))
        }

        // 5. 填充药箱表格
        medicinesTableModel.rowCount = 0
        for (m in store.getMedicines()) {
            medicinesTableModel.addRow(arrayOf(
                m.name,
                when (m.category) {
                    "fever" -> "🤒 发热镇痛"
                    "cold" -> "🤧 感冒咳嗽"
                    "digest" -> "🤢 肠胃消化"
                    "trauma" -> "🩹 外伤消炎"
                    "allergy" -> "🌿 抗过敏"
                    "chronic" -> "💊 慢病常备"
                    else -> "📦 其他"
                },
                m.form,
                m.qty,
                m.unit,
                m.location,
                m.dosage,
                if (m.expiryDate > 0) daySdf.format(Date(m.expiryDate)) else "长期有效",
                if (m.isExpired()) "🔴 已过期" else if (m.isExpiringSoon()) "🟠 临期" else "🟢 正常",
                m.id
            ))
        }
    }

    private fun applyFilter(query: String) {
        val all = store.loadAll().filter { it.isIn && !it.isRetired }
        val filtered = if (query.isBlank()) all else all.filter {
            it.brand.contains(query, true) ||
            it.category.contains(query, true) ||
            it.location.contains(query, true) ||
            it.notes.contains(query, true)
        }

        inventoryTableModel.rowCount = 0
        for (e in filtered) {
            inventoryTableModel.addRow(arrayOf(
                e.brand,
                e.category,
                e.qty,
                e.unit,
                String.format(Locale.getDefault(), "%.2f", e.price),
                String.format(Locale.getDefault(), "%.2f", e.price * e.qty),
                if (e.location.isNotBlank()) e.location else "未设定",
                String.format(Locale.getDefault(), "%.2f", e.getDailyCost()),
                if (e.isRetired) "已退役" else "在役中",
                e.id
            ))
        }
    }

    // ==================== 对话框操作 ====================

    private fun showAddEditDialog(editEntry: Entry? = null) {
        val isEdit = (editEntry != null)
        val dialog = JDialog(this, if (isEdit) "编辑物品记录" else "➕ 记一笔 / 物品出入库", true)
        dialog.setSize(480, 520)
        dialog.setLocationRelativeTo(this)

        val form = JPanel(GridLayout(7, 2, 10, 12)).apply {
            border = EmptyBorder(20, 24, 20, 24)
        }

        val tfName = JTextField(editEntry?.brand ?: "")
        val cbCategory = JComboBox(store.getCategories().toTypedArray()).apply {
            selectedItem = editEntry?.category ?: "日用品"
        }
        val spQty = JSpinner(SpinnerNumberModel(editEntry?.qty ?: 1, 1, 99999, 1))
        val tfUnit = JTextField(editEntry?.unit ?: "件")
        val tfPrice = JTextField(if (editEntry != null) editEntry.price.toString() else "0.0")
        val tfLocation = JTextField(editEntry?.location ?: "")
        val tfNotes = JTextField(editEntry?.notes ?: "")

        val cbDirection = JComboBox(arrayOf("入库 / 增加拥有", "出库 / 减少消耗")).apply {
            selectedIndex = if (editEntry?.isIn == false) 1 else 0
        }

        form.add(JLabel("物品名称 / 品牌 (*):"))
        form.add(tfName)
        form.add(JLabel("所属分类:"))
        form.add(cbCategory)
        form.add(JLabel("出入库类型:"))
        form.add(cbDirection)
        form.add(JLabel("数量与单位:"))
        val qtyPanel = JPanel(GridLayout(1, 2, 4, 0))
        qtyPanel.add(spQty)
        qtyPanel.add(tfUnit)
        form.add(qtyPanel)
        form.add(JLabel("单价 (¥):"))
        form.add(tfPrice)
        form.add(JLabel("放置位置 (选填):"))
        form.add(tfLocation)
        form.add(JLabel("备注说明:"))
        form.add(tfNotes)

        val btnSave = JButton(if (isEdit) "保存修改" else "确认添加").apply {
            background = Color(16, 185, 129)
            foreground = Color.WHITE
            font = font.deriveFont(Font.BOLD, 13f)
            addActionListener {
                val name = tfName.text.trim()
                if (name.isBlank()) {
                    JOptionPane.showMessageDialog(dialog, "请输入物品名称！")
                    return@addActionListener
                }
                val price = tfPrice.text.toDoubleOrNull() ?: 0.0
                val qty = spQty.value as Int
                val isIn = (cbDirection.selectedIndex == 0)

                val newEntry = editEntry?.copy(
                    brand = name,
                    category = cbCategory.selectedItem as String,
                    price = price,
                    qty = qty,
                    unit = tfUnit.text.trim().ifEmpty { "件" },
                    location = tfLocation.text.trim(),
                    notes = tfNotes.text.trim(),
                    isIn = isIn
                ) ?: Entry(
                    brand = name,
                    category = cbCategory.selectedItem as String,
                    price = price,
                    qty = qty,
                    unit = tfUnit.text.trim().ifEmpty { "件" },
                    location = tfLocation.text.trim(),
                    notes = tfNotes.text.trim(),
                    isIn = isIn
                )

                if (isEdit) {
                    store.updateEntry(newEntry)
                } else {
                    store.addEntry(newEntry)
                }
                dialog.dispose()
                refreshData()
            }
        }

        val btnCancel = JButton("取消").apply {
            addActionListener { dialog.dispose() }
        }

        val bottom = JPanel(FlowLayout(FlowLayout.RIGHT))
        bottom.add(btnCancel)
        bottom.add(btnSave)

        dialog.layout = BorderLayout()
        dialog.add(form, BorderLayout.CENTER)
        dialog.add(bottom, BorderLayout.SOUTH)
        dialog.isVisible = true
    }

    private fun editSelectedEntry(table: JTable) {
        val row = table.selectedRow
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "请先在列表中选中一行！")
            return
        }
        val id = table.model.getValueAt(row, 9) as String
        val entry = store.loadAll().find { it.id == id }
        if (entry != null) {
            showAddEditDialog(entry)
        }
    }

    private fun deleteSelectedEntry(table: JTable) {
        val row = table.selectedRow
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "请先在列表中选中一行！")
            return
        }
        val id = table.model.getValueAt(row, 9) as String
        val name = table.model.getValueAt(row, 0) as String

        val confirm = JOptionPane.showConfirmDialog(
            this,
            "确定要删除【$name】吗？",
            "确认删除",
            JOptionPane.YES_NO_OPTION
        )
        if (confirm == JOptionPane.YES_OPTION) {
            store.deleteEntry(id)
            refreshData()
        }
    }

    private fun performWebDavSync() {
        val res = DesktopWebDavHelper.uploadBackup(store)
        JOptionPane.showMessageDialog(
            this,
            res.message,
            if (res.isSuccess) "WebDAV 同步成功" else "同步失败",
            if (res.isSuccess) JOptionPane.INFORMATION_MESSAGE else JOptionPane.ERROR_MESSAGE
        )
    }

    private fun exportCsvToFile() {
        val chooser = JFileChooser().apply {
            selectedFile = java.io.File("Collecter_Assets_${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}.csv")
        }
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                val csv = store.exportCsv()
                chooser.selectedFile.writeText(csv, Charsets.UTF_8)
                JOptionPane.showMessageDialog(this, "CSV 资产总表导出成功！\n文件已保存至: ${chooser.selectedFile.absolutePath}")
            } catch (e: Exception) {
                JOptionPane.showMessageDialog(this, "导出失败: ${e.localizedMessage}", "错误", JOptionPane.ERROR_MESSAGE)
            }
        }
    }

    private fun setupShortcuts() {
        val root = rootPane
        val isMac = System.getProperty("os.name", "").lowercase().contains("mac")
        val mask = if (isMac) Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx else KeyEvent.CTRL_DOWN_MASK

        // ⌘+N / Ctrl+N -> 记一笔
        root.registerKeyboardAction({
            showAddEditDialog()
        }, KeyStroke.getKeyStroke(KeyEvent.VK_N, mask), JComponent.WHEN_IN_FOCUSED_WINDOW)

        // ⌘+S / Ctrl+S -> 同步 WebDAV
        root.registerKeyboardAction({
            performWebDavSync()
        }, KeyStroke.getKeyStroke(KeyEvent.VK_S, mask), JComponent.WHEN_IN_FOCUSED_WINDOW)
    }
}
