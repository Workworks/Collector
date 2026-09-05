package com.kfaino.diapertracker

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * 📅 会员订阅与周期服务专属录入/编辑控制器 (Dedicated Subscription Controller)
 * - 纯净专注订阅服务核心要素：名称、周期、扣费金额、下次续费日、自动续费与支付渠道
 * - 剥离不相关的出入库件数加减、房间地图坐标与实物保质期折旧等冗余要素
 */
object SubscriptionManagerDialog {

    fun showAddOrEditSubscriptionDialog(
        activity: Activity,
        store: DataStore,
        editEntry: Entry? = null,
        onSaved: () -> Unit
    ) {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_add_subscription, null)
        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(view)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation

        val tvTitle = view.findViewById<TextView>(R.id.tv_dialog_sub_title)
        val btnClose = view.findViewById<ImageView>(R.id.btn_close_add_sub)
        val etName = view.findViewById<EditText>(R.id.et_sub_name)
        val chipGroupCat = view.findViewById<ChipGroup>(R.id.chip_group_sub_category)
        val chipGroupCycle = view.findViewById<ChipGroup>(R.id.chip_group_sub_cycle)
        val etPrice = view.findViewById<EditText>(R.id.et_sub_price)
        val btnPickNextBilling = view.findViewById<android.view.View>(R.id.btn_pick_sub_next_billing)
        val tvNextBillingText = view.findViewById<TextView>(R.id.tv_sub_next_billing_text)
        val cbAutoRenew = view.findViewById<CheckBox>(R.id.cb_sub_auto_renew)
        val etPaymentChannel = view.findViewById<EditText>(R.id.et_sub_payment_channel)
        val etNotes = view.findViewById<EditText>(R.id.et_sub_notes)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btn_cancel_add_sub)
        val btnSave = view.findViewById<MaterialButton>(R.id.btn_save_sub)

        var selectedNextBillingDate = editEntry?.subNextBillingDate ?: (System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        fun updateBillingDateUi() {
            tvNextBillingText.text = "📅 下次续费日: ${sdf.format(Date(selectedNextBillingDate))}"
        }

        if (editEntry != null) {
            tvTitle.text = "📅 编辑会员订阅服务"
            etName.setText(editEntry.brand)
            when (editEntry.category) {
                "影音", "影音视听" -> chipGroupCat.check(R.id.chip_sub_cat_media)
                "AI", "AI工具" -> chipGroupCat.check(R.id.chip_sub_cat_ai)
                "生活", "商超生活" -> chipGroupCat.check(R.id.chip_sub_cat_life)
                "办公", "软件授权" -> chipGroupCat.check(R.id.chip_sub_cat_software)
                "其它", "其它订阅" -> chipGroupCat.check(R.id.chip_sub_cat_other)
                else -> chipGroupCat.check(R.id.chip_sub_cat_cloud)
            }
            when (editEntry.subCycle) {
                "按年" -> chipGroupCycle.check(R.id.chip_cycle_year)
                "按季" -> chipGroupCycle.check(R.id.chip_cycle_quarter)
                "按周" -> chipGroupCycle.check(R.id.chip_cycle_week)
                else -> chipGroupCycle.check(R.id.chip_cycle_month)
            }
            etPrice.setText(String.format(Locale.getDefault(), "%.2f", editEntry.price))
            cbAutoRenew.isChecked = editEntry.subAutoRenew
            etNotes.setText(editEntry.notes)
            updateBillingDateUi()
        } else {
            tvTitle.text = "📅 登记会员订阅服务"
            chipGroupCat.check(R.id.chip_sub_cat_cloud)
            chipGroupCycle.check(R.id.chip_cycle_month)
            updateBillingDateUi()
        }

        btnPickNextBilling.setOnClickListener {
            ModernDatePickerDialog.show(activity, selectedNextBillingDate, "选择下次扣费日期") { picked ->
                selectedNextBillingDate = picked
                updateBillingDateUi()
            }
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(activity, "请输入订阅服务名称", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val price = etPrice.text.toString().toDoubleOrNull() ?: 0.0
            val category = when (chipGroupCat.checkedChipId) {
                R.id.chip_sub_cat_media -> "影音视听"
                R.id.chip_sub_cat_ai -> "AI工具"
                R.id.chip_sub_cat_life -> "商超生活"
                R.id.chip_sub_cat_software -> "软件授权"
                R.id.chip_sub_cat_other -> "其它订阅"
                else -> "云存储"
            }

            val cycle = when (chipGroupCycle.checkedChipId) {
                R.id.chip_cycle_year -> "按年"
                R.id.chip_cycle_quarter -> "按季"
                R.id.chip_cycle_week -> "按周"
                else -> "按月"
            }

            val autoRenew = cbAutoRenew.isChecked
            val payChannel = etPaymentChannel.text.toString().trim()
            val rawNotes = etNotes.text.toString().trim()
            val finalNotes = if (payChannel.isNotEmpty()) {
                if (rawNotes.isNotEmpty()) "支付渠道: $payChannel | $rawNotes" else "支付渠道: $payChannel"
            } else rawNotes

            val all = store.loadAll().toMutableList()
            val newSub = Entry(
                id = editEntry?.id ?: UUID.randomUUID().toString(),
                brand = name,
                category = category,
                qty = 1,
                unit = "份",
                price = price,
                isIn = true,
                isSubscription = true,
                isDigital = false,
                subCycle = cycle,
                subNextBillingDate = selectedNextBillingDate,
                subAutoRenew = autoRenew,
                purchaseDate = editEntry?.purchaseDate ?: System.currentTimeMillis(),
                notes = finalNotes,
                location = "云端订阅",
                assetType = "durable"
            )

            if (editEntry != null) {
                val idx = all.indexOfFirst { it.id == editEntry.id }
                if (idx != -1) {
                    all[idx] = newSub
                } else {
                    all.add(0, newSub)
                }
            } else {
                all.add(0, newSub)
            }

            store.saveAll(all)
            Toast.makeText(activity, "🎉 已保存订阅服务【$name】", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            onSaved()
        }

        dialog.show()
    }
}
