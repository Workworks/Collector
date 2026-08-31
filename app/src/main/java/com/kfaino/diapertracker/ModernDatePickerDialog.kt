package com.kfaino.diapertracker

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.GridLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kfaino.diapertracker.databinding.DialogModernDatePickerBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 现代化全景日期选择器 (Modern Date Picker Dialog)
 * - 支持直接点击年份/月份展开网格进行跨年代、跨月份秒速切换
 * - 提供 1年前 / 2年前 / 3年前 / 5年前 / 昨天 / 今天 常用快捷时间标签
 * - 动态实时计算「距今拥有天数 / 剩余天数」
 * - 完美适配高定深浅主题，注入线性触感震动与微动效
 */
object ModernDatePickerDialog {

    private enum class ViewMode {
        CALENDAR,
        YEAR_PICKER,
        MONTH_PICKER
    }

    fun show(
        activity: Activity,
        initialTimeMs: Long,
        title: String = "📅 选择日期",
        onDateSelected: (Long) -> Unit
    ) {
        val binding = DialogModernDatePickerBinding.inflate(LayoutInflater.from(activity))
        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(binding.root)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation

        binding.tvPickerDialogTitle.text = title

        // 选中的具体日期 (精确到年月日)
        val selectedCal = Calendar.getInstance().apply {
            timeInMillis = if (initialTimeMs > 0) initialTimeMs else System.currentTimeMillis()
        }

        // 当前正在浏览翻页的年与月
        var displayYear = selectedCal.get(Calendar.YEAR)
        var displayMonth = selectedCal.get(Calendar.MONTH) // 0 ~ 11
        var currentMode = ViewMode.CALENDAR

        val todayCal = Calendar.getInstance()

        fun updateHeaderAndConfirmButton() {
            val weekFormat = SimpleDateFormat("yyyy年 M月d日 EEEE", Locale.CHINA)
            val selectedTime = selectedCal.timeInMillis
            binding.tvSelectedDateFull.text = weekFormat.format(Date(selectedTime))

            // 计算相对天数
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val selectedStart = Calendar.getInstance().apply {
                timeInMillis = selectedTime
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val diffDays = ((todayStart - selectedStart) / (24L * 60 * 60 * 1000)).toInt()
            binding.tvSelectedDateRelative.text = when {
                diffDays == 0 -> "📅 就是今天"
                diffDays == 1 -> "📅 昨天 (1 天前)"
                diffDays > 1 -> "⏳ 距今 $diffDays 天前"
                else -> "⌛ 未来 ${-diffDays} 天后"
            }

            val btnFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            binding.btnPickerConfirm.text = "确定 (${btnFormat.format(Date(selectedTime))})"
            binding.btnSelectYear.text = "${displayYear} 年 ▼"
            binding.btnSelectMonth.text = "${displayMonth + 1} 月 ▼"
        }

        fun renderCalendarGrid() {
            binding.gridDays.removeAllViews()

            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, displayYear)
                set(Calendar.MONTH, displayMonth)
                set(Calendar.DAY_OF_MONTH, 1)
            }

            // 获取当月第一天是周几 (周一=1, 周日=7)
            var firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 转换为周日=0, 周一=1 ...
            if (firstDayOfWeek == 0) firstDayOfWeek = 7 // 修正为周日=7, 周一=1

            val maxDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

            // 填充前置空白格子 (属于上月的尾巴)
            val prevMonthMax = Calendar.getInstance().apply {
                set(Calendar.YEAR, displayYear)
                set(Calendar.MONTH, displayMonth - 1)
            }.getActualMaximum(Calendar.DAY_OF_MONTH)

            val totalCells = 42 // 6行 x 7列

            for (i in 1 until firstDayOfWeek) {
                val prevDay = prevMonthMax - (firstDayOfWeek - 1 - i)
                val tv = createDayTextView(activity, prevDay.toString(), isCurrentMonth = false, isSelected = false, isToday = false)
                tv.setOnClickListener {
                    if (displayMonth == 0) {
                        displayYear--
                        displayMonth = 11
                    } else {
                        displayMonth--
                    }
                    selectedCal.set(Calendar.YEAR, displayYear)
                    selectedCal.set(Calendar.MONTH, displayMonth)
                    selectedCal.set(Calendar.DAY_OF_MONTH, prevDay)
                    updateHeaderAndConfirmButton()
                    renderCalendarGrid()
                }
                binding.gridDays.addView(tv)
            }

            // 填充当月日期
            for (day in 1..maxDaysInMonth) {
                val isSelected = (selectedCal.get(Calendar.YEAR) == displayYear &&
                        selectedCal.get(Calendar.MONTH) == displayMonth &&
                        selectedCal.get(Calendar.DAY_OF_MONTH) == day)

                val isToday = (todayCal.get(Calendar.YEAR) == displayYear &&
                        todayCal.get(Calendar.MONTH) == displayMonth &&
                        todayCal.get(Calendar.DAY_OF_MONTH) == day)

                val tv = createDayTextView(activity, day.toString(), isCurrentMonth = true, isSelected = isSelected, isToday = isToday)
                tv.setOnClickListener {
                    selectedCal.set(Calendar.YEAR, displayYear)
                    selectedCal.set(Calendar.MONTH, displayMonth)
                    selectedCal.set(Calendar.DAY_OF_MONTH, day)
                    updateHeaderAndConfirmButton()
                    renderCalendarGrid()
                }
                binding.gridDays.addView(tv)
            }

            // 填充后置空白格子 (属于下月的开头)
            val filledCount = (firstDayOfWeek - 1) + maxDaysInMonth
            val nextDays = totalCells - filledCount
            for (day in 1..nextDays) {
                val tv = createDayTextView(activity, day.toString(), isCurrentMonth = false, isSelected = false, isToday = false)
                tv.setOnClickListener {
                    if (displayMonth == 11) {
                        displayYear++
                        displayMonth = 0
                    } else {
                        displayMonth++
                    }
                    selectedCal.set(Calendar.YEAR, displayYear)
                    selectedCal.set(Calendar.MONTH, displayMonth)
                    selectedCal.set(Calendar.DAY_OF_MONTH, day)
                    updateHeaderAndConfirmButton()
                    renderCalendarGrid()
                }
                binding.gridDays.addView(tv)
            }
        }

        fun renderYearGrid(onYearPicked: (Int) -> Unit) {
            binding.gridYears.removeAllViews()
            val currentYear = todayCal.get(Calendar.YEAR)
            // 年份范围 1980 到当前年份 + 10
            val startYear = 1980
            val endYear = currentYear + 10

            for (y in startYear..endYear) {
                val isSelected = (y == displayYear)
                val tv = TextView(activity).apply {
                    text = "${y}年"
                    textSize = 14f
                    gravity = Gravity.CENTER
                    val lp = GridLayout.LayoutParams().apply {
                        width = 0
                        height = dpToPx(activity, 44)
                        columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                        setMargins(dpToPx(activity, 4), dpToPx(activity, 4), dpToPx(activity, 4), dpToPx(activity, 4))
                    }
                    layoutParams = lp

                    if (isSelected) {
                        setTextColor(Color.WHITE)
                        paint.isFakeBoldText = true
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.RECTANGLE
                            cornerRadius = dpToPx(activity, 10).toFloat()
                            setColor(ContextCompat.getColor(context, R.color.primary))
                        }
                    } else {
                        setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.RECTANGLE
                            cornerRadius = dpToPx(activity, 10).toFloat()
                            setColor(ContextCompat.getColor(context, R.color.input_bg))
                        }
                    }

                    applyPressScaleAnimation(0.92f)
                    setOnClickListener {
                        onYearPicked(y)
                    }
                }
                binding.gridYears.addView(tv)
            }
        }

        fun renderMonthGrid(onMonthPicked: (Int) -> Unit) {
            binding.layoutMonthPicker.removeAllViews()
            val monthNames = listOf("1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月")

            for ((mIdx, name) in monthNames.withIndex()) {
                val isSelected = (mIdx == displayMonth)
                val tv = TextView(activity).apply {
                    text = name
                    textSize = 15f
                    gravity = Gravity.CENTER
                    val lp = GridLayout.LayoutParams().apply {
                        width = 0
                        height = 0
                        columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                        rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                        setMargins(dpToPx(activity, 6), dpToPx(activity, 6), dpToPx(activity, 6), dpToPx(activity, 6))
                    }
                    layoutParams = lp

                    if (isSelected) {
                        setTextColor(Color.WHITE)
                        paint.isFakeBoldText = true
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.RECTANGLE
                            cornerRadius = dpToPx(activity, 12).toFloat()
                            setColor(ContextCompat.getColor(context, R.color.primary))
                        }
                    } else {
                        setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.RECTANGLE
                            cornerRadius = dpToPx(activity, 12).toFloat()
                            setColor(ContextCompat.getColor(context, R.color.input_bg))
                        }
                    }

                    applyPressScaleAnimation(0.92f)
                    setOnClickListener {
                        onMonthPicked(mIdx)
                    }
                }
                binding.layoutMonthPicker.addView(tv)
            }
        }

        fun switchMode(mode: ViewMode) {
            currentMode = mode
            binding.layoutCalendarView.visibility = if (mode == ViewMode.CALENDAR) android.view.View.VISIBLE else android.view.View.GONE
            binding.layoutYearPicker.visibility = if (mode == ViewMode.YEAR_PICKER) android.view.View.VISIBLE else android.view.View.GONE
            binding.layoutMonthPicker.visibility = if (mode == ViewMode.MONTH_PICKER) android.view.View.VISIBLE else android.view.View.GONE

            binding.btnSelectYear.setBackgroundResource(if (mode == ViewMode.YEAR_PICKER) R.drawable.bg_chip_active else R.drawable.bg_chip_inactive)
            binding.btnSelectYear.setTextColor(if (mode == ViewMode.YEAR_PICKER) Color.WHITE else ContextCompat.getColor(activity, R.color.text_primary))

            binding.btnSelectMonth.setBackgroundResource(if (mode == ViewMode.MONTH_PICKER) R.drawable.bg_chip_active else R.drawable.bg_chip_inactive)
            binding.btnSelectMonth.setTextColor(if (mode == ViewMode.MONTH_PICKER) Color.WHITE else ContextCompat.getColor(activity, R.color.text_primary))

            if (mode == ViewMode.YEAR_PICKER) {
                renderYearGrid { y ->
                    displayYear = y
                    selectedCal.set(Calendar.YEAR, y)
                    switchMode(ViewMode.CALENDAR)
                    updateHeaderAndConfirmButton()
                    renderCalendarGrid()
                }
            }
            if (mode == ViewMode.MONTH_PICKER) {
                renderMonthGrid { m ->
                    displayMonth = m
                    selectedCal.set(Calendar.MONTH, m)
                    switchMode(ViewMode.CALENDAR)
                    updateHeaderAndConfirmButton()
                    renderCalendarGrid()
                }
            }
        }

        // 年份/月份切换按钮
        binding.btnSelectYear.applyPressScaleAnimation(0.92f)
        binding.btnSelectYear.setOnClickListener {
            if (currentMode == ViewMode.YEAR_PICKER) {
                switchMode(ViewMode.CALENDAR)
            } else {
                switchMode(ViewMode.YEAR_PICKER)
            }
        }

        binding.btnSelectMonth.applyPressScaleAnimation(0.92f)
        binding.btnSelectMonth.setOnClickListener {
            if (currentMode == ViewMode.MONTH_PICKER) {
                switchMode(ViewMode.CALENDAR)
            } else {
                switchMode(ViewMode.MONTH_PICKER)
            }
        }

        // 上个月 / 下个月
        binding.btnPrevMonth.applyPressScaleAnimation(0.90f)
        binding.btnPrevMonth.setOnClickListener {
            if (displayMonth == 0) {
                displayYear--
                displayMonth = 11
            } else {
                displayMonth--
            }
            updateHeaderAndConfirmButton()
            if (currentMode == ViewMode.CALENDAR) renderCalendarGrid()
            if (currentMode == ViewMode.YEAR_PICKER) switchMode(ViewMode.YEAR_PICKER)
            if (currentMode == ViewMode.MONTH_PICKER) switchMode(ViewMode.MONTH_PICKER)
        }

        binding.btnNextMonth.applyPressScaleAnimation(0.90f)
        binding.btnNextMonth.setOnClickListener {
            if (displayMonth == 11) {
                displayYear++
                displayMonth = 0
            } else {
                displayMonth++
            }
            updateHeaderAndConfirmButton()
            if (currentMode == ViewMode.CALENDAR) renderCalendarGrid()
            if (currentMode == ViewMode.YEAR_PICKER) switchMode(ViewMode.YEAR_PICKER)
            if (currentMode == ViewMode.MONTH_PICKER) switchMode(ViewMode.MONTH_PICKER)
        }

        // 快捷标签选择
        fun jumpToRelativeDate(daysAgo: Int) {
            val target = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -daysAgo)
            }
            selectedCal.timeInMillis = target.timeInMillis
            displayYear = selectedCal.get(Calendar.YEAR)
            displayMonth = selectedCal.get(Calendar.MONTH)
            switchMode(ViewMode.CALENDAR)
            updateHeaderAndConfirmButton()
            renderCalendarGrid()
        }

        binding.chipToday.applyPressScaleAnimation(0.92f)
        binding.chipToday.setOnClickListener { jumpToRelativeDate(0) }

        binding.chipYesterday.applyPressScaleAnimation(0.92f)
        binding.chipYesterday.setOnClickListener { jumpToRelativeDate(1) }

        binding.chip1MonthAgo.applyPressScaleAnimation(0.92f)
        binding.chip1MonthAgo.setOnClickListener { jumpToRelativeDate(30) }

        binding.chip1YearAgo.applyPressScaleAnimation(0.92f)
        binding.chip1YearAgo.setOnClickListener { jumpToRelativeDate(365) }

        binding.chip2YearsAgo.applyPressScaleAnimation(0.92f)
        binding.chip2YearsAgo.setOnClickListener { jumpToRelativeDate(730) }

        binding.chip3YearsAgo.applyPressScaleAnimation(0.92f)
        binding.chip3YearsAgo.setOnClickListener { jumpToRelativeDate(1095) }

        binding.chip5YearsAgo.applyPressScaleAnimation(0.92f)
        binding.chip5YearsAgo.setOnClickListener { jumpToRelativeDate(1825) }

        // 底部确认与关闭
        binding.btnClosePicker.applyPressScaleAnimation(0.90f)
        binding.btnClosePicker.setOnClickListener { dialog.dismiss() }

        binding.btnPickerCancel.applyPressScaleAnimation(0.94f)
        binding.btnPickerCancel.setOnClickListener { dialog.dismiss() }

        binding.btnPickerConfirm.applyPressScaleAnimation(0.94f)
        binding.btnPickerConfirm.setOnClickListener {
            onDateSelected(selectedCal.timeInMillis)
            dialog.dismiss()
        }

        updateHeaderAndConfirmButton()
        renderCalendarGrid()
        dialog.show()
    }

    private fun createDayTextView(
        activity: Activity,
        text: String,
        isCurrentMonth: Boolean,
        isSelected: Boolean,
        isToday: Boolean
    ): TextView {
        return TextView(activity).apply {
            this.text = text
            textSize = 13f
            gravity = Gravity.CENTER
            val lp = GridLayout.LayoutParams().apply {
                width = 0
                height = 0
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(dpToPx(activity, 2), dpToPx(activity, 2), dpToPx(activity, 2), dpToPx(activity, 2))
            }
            layoutParams = lp

            when {
                isSelected -> {
                    setTextColor(Color.WHITE)
                    paint.isFakeBoldText = true
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(ContextCompat.getColor(context, R.color.primary))
                    }
                }
                isToday -> {
                    setTextColor(ContextCompat.getColor(context, R.color.primary))
                    paint.isFakeBoldText = true
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(ContextCompat.getColor(context, R.color.tag_custom_bg))
                    }
                }
                isCurrentMonth -> {
                    setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                    background = null
                }
                else -> {
                    // 非当月日期浅灰色
                    setTextColor(ContextCompat.getColor(context, R.color.text_hint))
                    background = null
                }
            }

            applyPressScaleAnimation(0.90f)
        }
    }

    private fun dpToPx(activity: Activity, dp: Int): Int {
        return (dp * activity.resources.displayMetrics.density + 0.5f).toInt()
    }
}
