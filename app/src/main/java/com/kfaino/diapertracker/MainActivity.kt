package com.kfaino.diapertracker

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.ImageView
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

    // ---------- 记一笔对话框 ----------

    private fun showAddDialog() {
        val dialogBinding = DialogAddEntryBinding.inflate(layoutInflater)

        // 分类下拉（动态加载自定义+预设）
        val categories = store.getCategories().toMutableList()
        val catAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.categorySpinner.adapter = catAdapter
        dialogBinding.categorySpinner.setSelection(0)

        // + 自定义分类快捷创建
        dialogBinding.btnAddCustomCategory.setOnClickListener {
            CategoryManagerDialog.showAddCategoryDialog(this, store) { newCat ->
                if (!categories.contains(newCat)) {
                    categories.add(newCat)
                    catAdapter.notifyDataSetChanged()
                }
                val index = categories.indexOf(newCat)
                if (index >= 0) {
                    dialogBinding.categorySpinner.setSelection(index)
                }
            }
        }

        // 品牌联想
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

        // 实时金额预览
        fun updatePreview() {
            val q = dialogBinding.qtyInput.text.toString().toIntOrNull() ?: 0
            val p = dialogBinding.priceInput.text.toString().toDoubleOrNull() ?: 0.0
            dialogBinding.amountPreview.text = "本次金额：¥${String.format(Locale.getDefault(), "%.2f", q * p)}"
        }
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) = updatePreview()
        }
        dialogBinding.qtyInput.addTextChangedListener(watcher)
        dialogBinding.priceInput.addTextChangedListener(watcher)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.add_entry)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.confirm, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {
                    val category = dialogBinding.categorySpinner.selectedItem?.toString() ?: "S"
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

    private fun toast(resId: Int) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
    }
}