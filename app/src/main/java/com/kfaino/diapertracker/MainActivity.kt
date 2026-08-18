package com.kfaino.diapertracker

import android.content.res.ColorStateList
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

        // 加载数据
        entries.clear()
        entries.addAll(store.loadAll())

        setupNav()
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

    // ---------- 底部导航（5项：首页/生活流/+/报表/我的） ----------

    private fun setupNav() {
        binding.navHome.setOnClickListener {
            showFragment(HomeFragment())
            selectTab(0)
        }
        binding.navTimeline.setOnClickListener {
            showFragment(TimelineFragment())
            selectTab(1)
        }
        binding.navReport.setOnClickListener {
            showFragment(ReportFragment())
            selectTab(2)
        }
        binding.navProfile.setOnClickListener {
            showFragment(ProfileFragment())
            selectTab(3)
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
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

    // ---------- 记一笔对话框 (极致跟手与触控体验) ----------

    private fun showAddDialog(prefillBrand: String? = null, prefillCategory: String? = null) {
        val dialogBinding = DialogAddEntryBinding.inflate(layoutInflater)
        val categories = store.getCategories().toMutableList()
        var selectedCategory = prefillCategory ?: if (categories.isNotEmpty()) categories[0] else "S"

        // 渲染尺码选择横向药丸
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

            // + 自定义尺码药丸
            val addChip = TextView(this).apply {
                text = "+ 自定义"
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6))
                setBackgroundResource(R.drawable.bg_btn_custom_add)
                setTextColor(ContextCompat.getColor(context, R.color.primary))
                isClickable = true
                isFocusable = true
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

        // 品牌联想
        if (!prefillBrand.isNullOrEmpty()) {
            dialogBinding.brandInput.setText(prefillBrand)
        }
        val brandNames = entries.map { it.brand }.distinct().sorted()
        val brandAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, brandNames)
        dialogBinding.brandInput.setAdapter(brandAdapter)

        // 数量步进器
        fun curQty(): Int = dialogBinding.qtyInput.text.toString().toIntOrNull() ?: 1
        fun setQty(v: Int) { dialogBinding.qtyInput.setText(v.coerceIn(1, 99999).toString()) }
        dialogBinding.minusBtn.setOnClickListener { setQty(curQty() - 1) }
        dialogBinding.plusBtn.setOnClickListener { setQty(curQty() + 1) }
        dialogBinding.quick1.setOnClickListener { setQty(1) }
        dialogBinding.quick2.setOnClickListener { setQty(2) }
        dialogBinding.quick4.setOnClickListener { setQty(4) }
        dialogBinding.quick8.setOnClickListener { setQty(8) }
        dialogBinding.quick10.setOnClickListener { setQty(10) }
        dialogBinding.quick20.setOnClickListener { setQty(20) }
        dialogBinding.quick50.setOnClickListener { setQty(50) }

        // 实时金额预览
        fun updatePreview() {
            val q = dialogBinding.qtyInput.text.toString().toIntOrNull() ?: 0
            val p = dialogBinding.priceInput.text.toString().toDoubleOrNull() ?: 0.0
            val isBuy = dialogBinding.modeGroup.checkedButtonId == dialogBinding.modeBuy.id
            if (isBuy) {
                if (p > 0) {
                    dialogBinding.amountPreview.text = "本次金额：¥${String.format(Locale.getDefault(), "%.2f", q * p)} ($q 件 × ¥${String.format(Locale.getDefault(), "%.2f", p)})"
                } else {
                    dialogBinding.amountPreview.text = "本次入库：+ $q 件"
                }
            } else {
                dialogBinding.amountPreview.text = "本次出库消耗：- $q 件"
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

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.add_entry)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.confirm, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {
                    val category = selectedCategory
                    val brand = dialogBinding.brandInput.text?.toString()?.trim().orEmpty()
                    val isIn = dialogBinding.modeGroup.checkedButtonId == dialogBinding.modeBuy.id
                    val qty = dialogBinding.qtyInput.text.toString().toIntOrNull()
                    val price = dialogBinding.priceInput.text.toString().toDoubleOrNull() ?: 0.0
                    val notes = dialogBinding.notesInput.text?.toString()?.trim().orEmpty()

                    when {
                        brand.isEmpty() -> toast(R.string.err_brand_empty)
                        qty == null || qty < 1 -> toast(R.string.err_qty)
                        else -> {
                            entries.add(Entry(category, brand, qty, price, System.currentTimeMillis(), isIn, notes))
                            store.saveAll(entries)
                            dialog.dismiss()
                            Toast.makeText(this, "记录已成功添加", Toast.LENGTH_SHORT).show()

                            // 刷新当前 fragment
                            val current = supportFragmentManager.findFragmentById(R.id.fragment_container)
                            when (current) {
                                is HomeFragment -> current.onResume()
                                is TimelineFragment -> current.onResume()
                                is ReportFragment -> current.onResume()
                            }
                        }
                    }
                }
        }
        dialog.show()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density + 0.5f).toInt()
    }

    private fun toast(resId: Int) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
    }
}