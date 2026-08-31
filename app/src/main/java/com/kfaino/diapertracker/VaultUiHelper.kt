package com.kfaino.diapertracker

import android.app.Activity
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 🏛️ 收纳馆弹窗统一 UI 辅助基建 (Vault UI Helper)
 * 统一 8 大收纳馆的视窗动效、剪贴板复制、日期选择器与搜索监听，减少模板冗余代码。
 */
object VaultUiHelper {

    val standardDateFormat: SimpleDateFormat
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /** 统一设置收纳馆弹窗半透明底色与入场/退场平滑微动效 */
    fun setupVaultWindow(dialog: Dialog) {
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation
    }

    /** 复制文本到系统剪贴板并给用户友好提示 */
    fun copyToClipboard(
        context: Context,
        label: String,
        text: String,
        successMessage: String = "📋 已复制到剪贴板！"
    ) {
        if (text.isEmpty()) {
            Toast.makeText(context, "内容为空，无需复制", Toast.LENGTH_SHORT).show()
            return
        }
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText(label, text))
            Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()
        }
    }

    /** 统一快速绑定搜索栏文字监听 */
    fun bindSearchWatcher(editText: EditText, onKeywordChanged: (String) -> Unit) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                onKeywordChanged(s?.toString()?.trim() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    /** 弹出统一标准日期选择器 */
    fun showDatePicker(
        activity: Activity,
        initialTimeMillis: Long = System.currentTimeMillis(),
        onDateSelected: (Long, String) -> Unit
    ) {
        val cal = Calendar.getInstance()
        if (initialTimeMillis > 0L) {
            cal.timeInMillis = initialTimeMillis
        }
        DatePickerDialog(
            activity,
            { _, year, month, dayOfMonth ->
                val pickedCal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val formatted = standardDateFormat.format(Date(pickedCal.timeInMillis))
                onDateSelected(pickedCal.timeInMillis, formatted)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}
