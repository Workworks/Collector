package com.kfaino.diapertracker

import android.app.DatePickerDialog
import android.util.Log
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kfaino.diapertracker.databinding.DialogBillEntryBinding
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object BillEntryDialog {
    private const val TAG = "BillEntryDialog"
    private val categories = listOf("餐饮", "采购", "交通", "订阅", "居家", "医疗", "教育", "娱乐", "退款", "其他")
    private val currencies = listOf("CNY", "USD")
    private val recurrenceLabels = listOf("不重复", "每周", "每月", "每年")

    fun show(activity: AppCompatActivity, store: BillStore, editing: BillRecord? = null, onSaved: () -> Unit) {
        val binding = DialogBillEntryBinding.inflate(LayoutInflater.from(activity))
        var selectedDate = editing?.let {
            Instant.ofEpochMilli(it.occurredAt).atZone(ZoneId.systemDefault()).toLocalDate()
        } ?: LocalDate.now()

        binding.billCurrencyInput.adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item, currencies)
        binding.billCategoryInput.adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item, categories)
        binding.billRecurrenceInput.adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item, recurrenceLabels)
        binding.billTitleInput.setText(editing?.title.orEmpty())
        binding.billAmountInput.setText(editing?.let { java.math.BigDecimal.valueOf(it.amountMinor, 2).stripTrailingZeros().toPlainString() }.orEmpty())
        binding.billDirectionGroup.check(if (editing?.direction == "income") R.id.bill_direction_income else R.id.bill_direction_expense)
        binding.billCurrencyInput.setSelection(currencies.indexOf(editing?.currency ?: "CNY").coerceAtLeast(0))
        binding.billCategoryInput.setSelection(categories.indexOf(editing?.category ?: "其他").coerceAtLeast(0))
        binding.billPostedInput.isChecked = editing?.status != "pending"
        binding.billRecurrenceInput.setSelection(when (editing?.recurrence) {
            "weekly" -> 1; "monthly" -> 2; "yearly" -> 3; else -> 0
        })
        binding.billNoteInput.setText(editing?.note.orEmpty())

        fun updateDate() { binding.billDateInput.setText(selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)) }
        fun updatePostedLabel() { binding.billPostedInput.text = if (binding.billPostedInput.isChecked) "已入账" else "未入账" }
        fun updateCurrencyPrefix() { binding.billAmountLayout.prefixText = if (binding.billCurrencyInput.selectedItem == "USD") "$ " else "¥ " }
        updateDate(); updatePostedLabel(); updateCurrencyPrefix()
        binding.billPostedInput.setOnCheckedChangeListener { _, _ -> updatePostedLabel() }
        binding.billCurrencyInput.onItemSelectedListener = SimpleItemSelectedListener { updateCurrencyPrefix() }
        binding.billDateInput.setOnClickListener {
            DatePickerDialog(activity, { _, year, month, day ->
                selectedDate = LocalDate.of(year, month + 1, day)
                updateDate()
            }, selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth).show()
        }

        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(if (editing == null) "记一笔" else "编辑账单")
            .setView(binding.root)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                binding.billTitleLayout.error = null
                binding.billAmountLayout.error = null
                val title = binding.billTitleInput.text?.toString()?.trim().orEmpty()
                val amount = BillAmounts.parseMinor(binding.billAmountInput.text?.toString().orEmpty())
                if (title.isBlank()) {
                    binding.billTitleLayout.error = "请输入项目名称"
                    return@setOnClickListener
                }
                if (amount == null) {
                    binding.billAmountLayout.error = "请输入大于 0 的有效金额"
                    return@setOnClickListener
                }
                val now = System.currentTimeMillis()
                val record = BillRecord(
                    id = editing?.id ?: java.util.UUID.randomUUID().toString(),
                    title = title,
                    amountMinor = amount,
                    currency = binding.billCurrencyInput.selectedItem.toString(),
                    direction = if (binding.billDirectionIncome.isChecked) "income" else "expense",
                    category = binding.billCategoryInput.selectedItem.toString(),
                    status = if (binding.billPostedInput.isChecked) "posted" else "pending",
                    recurrence = listOf("none", "weekly", "monthly", "yearly")[binding.billRecurrenceInput.selectedItemPosition],
                    occurredAt = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    note = binding.billNoteInput.text?.toString()?.trim().orEmpty(),
                    sourceText = editing?.sourceText.orEmpty(),
                    createdAt = editing?.createdAt ?: now,
                    updatedAt = now
                )
                try {
                    store.upsert(record)
                    dialog.dismiss()
                    Toast.makeText(activity, if (editing == null) "账单已保存" else "账单已更新", Toast.LENGTH_SHORT).show()
                    onSaved()
                } catch (e: Exception) {
                    Log.e(TAG, "保存账单失败: id=${record.id}", e)
                    Toast.makeText(activity, "账单保存失败，原数据已保留", Toast.LENGTH_LONG).show()
                }
            }
        }
        dialog.show()
    }

    private class SimpleItemSelectedListener(private val callback: () -> Unit) : android.widget.AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) = callback()
        override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
    }
}
