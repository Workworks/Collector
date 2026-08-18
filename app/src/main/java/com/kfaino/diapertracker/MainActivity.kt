package com.kfaino.diapertracker

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kfaino.diapertracker.databinding.ActivityMainBinding
import com.kfaino.diapertracker.databinding.DialogAddEntryBinding
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val store by lazy { DataStore(this) }
    private val entries = mutableListOf<Entry>()

    // 0=首页, 1=生活流, 2=报表, 3=我的
    private var currentTab = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        // 应用保存的主题模式（跟随系统/浅色/深色）
        DataStore.applyThemeMode(store.getThemeMode())

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        entries.clear()
        entries.addAll(store.loadAll())

        setupNav()
        binding.fabAdd.applyPressScaleAnimation(0.90f)
        binding.fabAdd.setOnClickListener { showAddDialog() }

        // 默认显示首页
        showFragment(HomeFragment())
        selectTab(0)
    }

    override fun onResume() {
        super.onResume()
        entries.clear()
        entries.addAll(store.loadAll())
    }

    // ---------- 底部导航（5项：首页/生活流/+/报表/我的，带柔和淡入转场） ----------

    private fun setupNav() {
        binding.navHome.applyPressScaleAnimation(0.92f)
        binding.navTimeline.applyPressScaleAnimation(0.92f)
        binding.navReport.applyPressScaleAnimation(0.92f)
        binding.navProfile.applyPressScaleAnimation(0.92f)

        binding.navHome.setOnClickListener {
            if (currentTab != 0) {
                showFragment(HomeFragment())
                selectTab(0)
            }
        }
        binding.navTimeline.setOnClickListener {
            if (currentTab != 1) {
                showFragment(TimelineFragment())
                selectTab(1)
            }
        }
        binding.navReport.setOnClickListener {
            if (currentTab != 2) {
                showFragment(ReportFragment())
                selectTab(2)
            }
        }
        binding.navProfile.setOnClickListener {
            if (currentTab != 3) {
                showFragment(ProfileFragment())
                selectTab(3)
            }
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.anim_fade_in, R.anim.anim_fade_out)
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun selectTab(index: Int) {
        currentTab = index
        val active = ContextCompat.getColor(this, R.color.primary)
        val inactive = ContextCompat.getColor(this, R.color.text_secondary)

        data class TabInfo(val icon: ImageView, val label: TextView)
        val tabs = listOf(
            TabInfo(binding.navHomeIcon, binding.navHomeLabel),
            TabInfo(binding.navTimelineIcon, binding.navTimelineLabel),
            TabInfo(binding.navReportIcon, binding.navReportLabel),
            TabInfo(binding.navProfileIcon, binding.navProfileLabel)
        )

        tabs.forEachIndexed { i, tab ->
            val color = if (i == index) active else inactive
            tab.icon.imageTintList = ColorStateList.valueOf(color)
            tab.label.setTextColor(color)
        }
    }

    // ---------- 记一笔 / 编辑记录 高定现代卡片弹窗 (自带微动效、无原生框、无穿模) ----------

    fun showAddDialog(
        prefillBrand: String? = null,
        prefillCategory: String? = null,
        editEntry: Entry? = null,
        editPosition: Int? = null
    ) {
        val isEditMode = (editEntry != null && editPosition != null)
        val dialogBinding = DialogAddEntryBinding.inflate(layoutInflater)
        val categories = store.getCategories().toMutableList()

        var selectedCategory = editEntry?.category ?: prefillCategory ?: if (categories.isNotEmpty()) categories[0] else "数码"
        var selectedUnit = editEntry?.unit ?: store.getLastUsedUnit()

        // 默认按总金额记账模式 (true=按实付总额输入, false=按单件价格输入)
        var isTotalPriceMode = true

        // 弹窗创建与窗口动画设置
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation

        // 标题与图标
        if (isEditMode) {
            dialogBinding.dialogTitle.text = "编辑记录"
            dialogBinding.btnDialogConfirm.text = "保存修改"
        } else {
            dialogBinding.dialogTitle.text = "记一笔"
            dialogBinding.btnDialogConfirm.text = "确认添加"
        }

        // 1. 设置入库/出库模式 (默认增加/入库)
        val defaultIsIn = editEntry?.isIn ?: true
        if (defaultIsIn) {
            dialogBinding.modeGroup.check(dialogBinding.modeBuy.id)
        } else {
            dialogBinding.modeGroup.check(dialogBinding.modeUse.id)
        }

        // 2. 数量与实时预览辅助函数
        if (editEntry != null) {
            dialogBinding.qtyInput.setText(editEntry.qty.toString())
        }
        fun curQty(): Int = dialogBinding.qtyInput.text.toString().toIntOrNull() ?: 1
        fun setQty(v: Int) { dialogBinding.qtyInput.setText(v.coerceIn(1, 99999).toString()) }

        fun updatePreview() {
            val q = dialogBinding.qtyInput.text.toString().toIntOrNull() ?: 0
            val inputVal = dialogBinding.priceInput.text.toString().toDoubleOrNull() ?: 0.0
            val isBuy = dialogBinding.modeGroup.checkedButtonId == dialogBinding.modeBuy.id
            val u = selectedUnit.ifEmpty { "片" }

            if (isBuy) {
                dialogBinding.priceSectionContainer.alpha = 1.0f
                if (inputVal > 0 && q > 0) {
                    if (isTotalPriceMode) {
                        val totalAmount = inputVal
                        val unitPrice = totalAmount / q
                        dialogBinding.amountPreview.text = "本次实付：¥${String.format(Locale.getDefault(), "%.2f", totalAmount)} · 折合 ¥${String.format(Locale.getDefault(), "%.2f", unitPrice)} / $u"
                    } else {
                        val unitPrice = inputVal
                        val totalAmount = unitPrice * q
                        dialogBinding.amountPreview.text = "本次实付：¥${String.format(Locale.getDefault(), "%.2f", totalAmount)} · $q $u × ¥${String.format(Locale.getDefault(), "%.2f", unitPrice)}"
                    }
                } else {
                    dialogBinding.amountPreview.text = "本次入库：+ $q $u"
                }
            } else {
                dialogBinding.priceSectionContainer.alpha = 0.6f
                dialogBinding.amountPreview.text = "本次消耗出库：- $q $u"
            }
        }

        // 3. 渲染分类横向药丸
        fun renderCategoryChips() {
            dialogBinding.dialogCategoryChips.removeAllViews()
            for (cat in categories) {
                val chip = TextView(this).apply {
                    text = cat
                    textSize = 13f
                    gravity = Gravity.CENTER
                    setPadding(dpToPx(14), dpToPx(6), dpToPx(14), dpToPx(6))
                    val isSelected = (selectedCategory == cat)

                    if (isSelected) {
                        setBackgroundResource(R.drawable.bg_chip_active)
                        setTextColor(ContextCompat.getColor(context, android.R.color.white))
                    } else {
                        setBackgroundResource(R.drawable.bg_chip_inactive)
                        setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                    }

                    isClickable = true
                    isFocusable = true
                    applyPressScaleAnimation(0.92f)
                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        marginEnd = dpToPx(8)
                    }
                    layoutParams = params

                    setOnClickListener {
                        selectedCategory = cat
                        renderCategoryChips()
                    }
                }
                dialogBinding.dialogCategoryChips.addView(chip)
            }

            // + 自定义分类药丸
            val addChip = TextView(this).apply {
                text = "+ 自定义"
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6))
                setBackgroundResource(R.drawable.bg_btn_custom_add)
                setTextColor(ContextCompat.getColor(context, R.color.primary))
                isClickable = true
                isFocusable = true
                applyPressScaleAnimation(0.92f)
                setOnClickListener {
                    CategoryManagerDialog.showAddCategoryDialog(this@MainActivity, store) { newCat ->
                        if (!categories.contains(newCat)) {
                            categories.add(newCat)
                        }
                        selectedCategory = newCat
                        renderCategoryChips()
                    }
                }
            }
            dialogBinding.dialogCategoryChips.addView(addChip)
        }

        renderCategoryChips()

        // 4. 品牌/物品名称联想
        if (!prefillBrand.isNullOrEmpty()) {
            dialogBinding.brandInput.setText(prefillBrand)
        } else if (editEntry != null) {
            dialogBinding.brandInput.setText(editEntry.brand)
        }
        val brandNames = store.loadAll().map { it.brand }.distinct().sorted()
        val brandAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, brandNames)
        dialogBinding.brandInput.setAdapter(brandAdapter)

        // 5. 单位下拉选择与自定义
        val unitList = DataStore.COMMON_UNITS.toMutableList()
        if (!unitList.contains(selectedUnit)) {
            unitList.add(0, selectedUnit)
        }
        val unitAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, unitList)
        dialogBinding.unitDropdown.setAdapter(unitAdapter)
        dialogBinding.unitDropdown.setText(selectedUnit, false)

        dialogBinding.unitDropdown.setOnItemClickListener { _, _, position, _ ->
            selectedUnit = unitAdapter.getItem(position) ?: "片"
            store.setLastUsedUnit(selectedUnit)
            updatePreview()
        }

        dialogBinding.unitDropdown.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val inputUnit = s?.toString()?.trim().orEmpty()
                if (inputUnit.isNotEmpty()) {
                    selectedUnit = inputUnit
                    store.setLastUsedUnit(selectedUnit)
                    updatePreview()
                }
            }
        })

        // 6. 步进器按钮绑定 (带弹簧轻触动效)
        dialogBinding.minusBtn.applyPressScaleAnimation(0.90f)
        dialogBinding.plusBtn.applyPressScaleAnimation(0.90f)
        dialogBinding.quick1.applyPressScaleAnimation(0.92f)
        dialogBinding.quick10.applyPressScaleAnimation(0.92f)
        dialogBinding.quick20.applyPressScaleAnimation(0.92f)
        dialogBinding.quick50.applyPressScaleAnimation(0.92f)
        dialogBinding.quick100.applyPressScaleAnimation(0.92f)

        dialogBinding.minusBtn.setOnClickListener { setQty(curQty() - 1) }
        dialogBinding.plusBtn.setOnClickListener { setQty(curQty() + 1) }
        dialogBinding.quick1.setOnClickListener { setQty(1) }
        dialogBinding.quick10.setOnClickListener { setQty(10) }
        dialogBinding.quick20.setOnClickListener { setQty(20) }
        dialogBinding.quick50.setOnClickListener { setQty(50) }
        dialogBinding.quick100.setOnClickListener { setQty(100) }

        // 7. 价格与金额输入模式切换
        fun refreshPriceModeUI() {
            if (isTotalPriceMode) {
                dialogBinding.priceModeLabel.text = "💰 实付总金额（元，可选）"
                dialogBinding.priceInput.hint = "例如：120.00 (出库可不填)"
                dialogBinding.btnTogglePriceMode.text = "🔄 切换为按单价输入"
            } else {
                dialogBinding.priceModeLabel.text = "🏷️ 单件价格（元/$selectedUnit，可选）"
                dialogBinding.priceInput.hint = "例如：1.20 (出库可不填)"
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
        dialogBinding.priceInput.addTextChangedListener(watcher)
        updatePreview()

        // 8. 弹窗操作按钮绑定
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
            val notes = dialogBinding.notesInput.text?.toString()?.trim().orEmpty()
            val unit = selectedUnit.ifEmpty { "片" }

            val calculatedUnitPrice = if (isIn && qty != null && qty > 0) {
                if (isTotalPriceMode) {
                    inputPriceVal / qty
                } else {
                    inputPriceVal
                }
            } else {
                0.0
            }

            when {
                brand.isEmpty() -> Toast.makeText(this, "请输入物品或品牌名称", Toast.LENGTH_SHORT).show()
                qty == null || qty < 1 -> Toast.makeText(this, "数量至少为 1", Toast.LENGTH_SHORT).show()
                else -> {
                    val targetTs = editEntry?.ts ?: System.currentTimeMillis()
                    val newEntry = Entry(
                        category = category,
                        brand = brand,
                        qty = qty,
                        price = calculatedUnitPrice,
                        ts = targetTs,
                        isIn = isIn,
                        notes = notes,
                        unit = unit
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

    fun refreshCurrentFragment() {
        entries.clear()
        entries.addAll(store.loadAll())
        val current = supportFragmentManager.findFragmentById(R.id.fragment_container)
        when (current) {
            is HomeFragment -> current.onResume()
            is TimelineFragment -> current.refresh()
            is ReportFragment -> current.onResume()
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density + 0.5f).toInt()
    }
}