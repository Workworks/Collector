package com.kfaino.diapertracker

import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kfaino.collecter.core.BillQuickInputParser
import com.kfaino.diapertracker.databinding.DialogBillQuickInputBinding
import java.time.format.DateTimeFormatter

object BillQuickEntryDialog {
    private const val TAG = "BillQuickEntryDialog"
    private const val MAX_BATCH = 200
    private val example = """
        @2026-09-26
        支出 52 餐饮 小母鸡 #已入账 ; 原价45+配送7
        支出 28.82 采购 打包盒 #未入账
        支出 20USD 订阅 ChatGPT #每月 ; 自动续费
        收入 200 退款 采购退款 #已入账

        顺序：支出/收入 金额[币种] 分类 项目 #状态/#周期 ; 备注
        日期头可写 @M/D；缺省币种为 CNY，缺省状态为已入账。
    """.trimIndent()

    fun show(activity: AppCompatActivity, store: BillStore, initialText: String = "", onSaved: () -> Unit) {
        val binding = DialogBillQuickInputBinding.inflate(LayoutInflater.from(activity))
        binding.billQuickTextInput.setText(initialText)
        binding.billQuickTextInput.setSelection(initialText.length)
        binding.btnBillPasteClipboard.setOnClickListener {
            val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val text = clipboard.primaryClip?.getItemAt(0)?.coerceToText(activity)?.toString().orEmpty()
            if (text.isBlank()) Toast.makeText(activity, "剪贴板里没有文字", Toast.LENGTH_SHORT).show()
            else binding.billQuickTextInput.setText(text)
        }
        binding.btnBillQuickFormat.setOnClickListener { showFormat(activity) }

        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle("快速文本记账")
            .setView(binding.root)
            .setNegativeButton("取消", null)
            .setPositiveButton("解析预览", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                binding.billQuickTextLayout.error = null
                val raw = binding.billQuickTextInput.text?.toString().orEmpty().trim()
                if (raw.isBlank()) {
                    binding.billQuickTextLayout.error = "请先粘贴或输入账单文本"
                    return@setOnClickListener
                }
                val result = BillQuickInputParser.parse(raw)
                if (result.records.isEmpty()) {
                    binding.billQuickTextLayout.error = result.issues.firstOrNull()?.let { "第 ${it.sourceLine} 行：${it.message}" } ?: "没有识别到账单"
                    return@setOnClickListener
                }
                if (result.records.size > MAX_BATCH) {
                    binding.billQuickTextLayout.error = "单次最多保存 $MAX_BATCH 笔，请分批处理"
                    return@setOnClickListener
                }
                dialog.dismiss()
                showPreview(activity, store, raw, result, onSaved)
            }
        }
        dialog.show()
    }

    fun showFormat(activity: AppCompatActivity) {
        MaterialAlertDialogBuilder(activity)
            .setTitle("快速文本格式")
            .setMessage(example)
            .setPositiveButton("知道了", null)
            .show()
    }

    private fun showPreview(
        activity: AppCompatActivity,
        store: BillStore,
        original: String,
        result: com.kfaino.collecter.core.BillParseResult,
        onSaved: () -> Unit
    ) {
        val rows = result.records.take(30).joinToString("\n") { record ->
            val sign = if (record.direction == "income") "+" else "−"
            val pending = if (record.status == "pending") " · 未入账" else ""
            "${record.occurredOn.format(DateTimeFormatter.ofPattern("M/d"))}  ${record.title}  $sign${BillAmounts.format(record.amountMinor, record.currency)}$pending"
        }
        val more = if (result.records.size > 30) "\n…另有 ${result.records.size - 30} 笔" else ""
        val issues = if (result.issues.isEmpty()) "" else result.issues.take(8).joinToString("\n", "\n\n以下行不会保存：\n") {
            "第 ${it.sourceLine} 行：${it.sourceText}（${it.message}）"
        }
        MaterialAlertDialogBuilder(activity)
            .setTitle("确认 ${result.records.size} 笔账单")
            .setMessage(rows + more + issues)
            .setNegativeButton("返回修改") { _, _ -> show(activity, store, original, onSaved) }
            .setPositiveButton("保存 ${result.records.size} 笔") { _, _ ->
                try {
                    store.addAll(result.records.map(BillRecord::fromDraft))
                    Toast.makeText(activity, "已保存 ${result.records.size} 笔账单", Toast.LENGTH_LONG).show()
                    onSaved()
                } catch (e: Exception) {
                    Log.e(TAG, "批量保存账单失败: count=${result.records.size}", e)
                    Toast.makeText(activity, "批量保存失败，原数据已保留", Toast.LENGTH_LONG).show()
                }
            }
            .show()
    }
}
