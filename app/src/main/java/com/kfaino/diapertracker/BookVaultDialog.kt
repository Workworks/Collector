package com.kfaino.diapertracker

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kfaino.diapertracker.databinding.DialogBookVaultBinding
import java.util.UUID

/**
 * 📚 书房藏书、借阅流转与阅读收纳控制器 (Bookshelf Vault Dialog)
 * - 藏书空间定位（哪格书架）与分类管理
 * - 阅读进度追踪、进度百分比计算与精读打卡
 * - 实体藏书外借登记与归还流转追踪
 */
object BookVaultDialog {

    fun show(activity: Activity, store: DataStore, onDataChanged: () -> Unit = {}) {
        val binding = DialogBookVaultBinding.inflate(LayoutInflater.from(activity))
        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(binding.root)
            .create()

        VaultUiHelper.setupVaultWindow(dialog)

        var currentFilter = "all" // "all", "reading", "unread", "finished", "lent"
        var currentSearchKeyword = ""

        fun reloadList() {
            val allRecords = store.getBookRecords()
            val readingCount = allRecords.count { it.isReading() }
            val finishedCount = allRecords.count { it.isFinished() }
            val lentCount = allRecords.count { it.isLent() }

            binding.tvStatTotalBooks.text = "${allRecords.size} 本"
            binding.tvStatReadingBooks.text = "$readingCount 本"
            binding.tvStatFinishedBooks.text = "$finishedCount 本"
            binding.tvStatLentBooks.text = "$lentCount 本"

            val filtered = allRecords.filter { record ->
                val matchesFilter = when (currentFilter) {
                    "reading" -> record.isReading()
                    "unread" -> !record.isReading() && !record.isFinished() && !record.isLent()
                    "finished" -> record.isFinished()
                    "lent" -> record.isLent()
                    else -> true
                }
                val matchesSearch = currentSearchKeyword.isEmpty() ||
                        record.title.contains(currentSearchKeyword, ignoreCase = true) ||
                        record.author.contains(currentSearchKeyword, ignoreCase = true) ||
                        record.category.contains(currentSearchKeyword, ignoreCase = true) ||
                        record.bookshelfLocation.contains(currentSearchKeyword, ignoreCase = true) ||
                        record.borrowerName.contains(currentSearchKeyword, ignoreCase = true) ||
                        record.summaryNotes.contains(currentSearchKeyword, ignoreCase = true)

                matchesFilter && matchesSearch
            }.sortedWith(
                compareByDescending<BookRecord> { it.isReading() }
                    .thenByDescending { it.isLent() }
                    .thenByDescending { it.rating }
            )

            binding.layoutBooksEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
            binding.rvBooks.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE

            binding.rvBooks.adapter = BookVaultAdapter(
                activity = activity,
                list = filtered,
                onUpdateProgressClick = { record ->
                    showProgressUpdateDialog(activity, store, record) {
                        reloadList()
                        onDataChanged()
                    }
                },
                onToggleLentClick = { record ->
                    showLendOrReturnDialog(activity, store, record) {
                        reloadList()
                        onDataChanged()
                    }
                },
                onEditClick = { record ->
                    showAddOrEditBookDialog(activity, store, record) {
                        reloadList()
                        onDataChanged()
                    }
                },
                onDeleteClick = { record ->
                    ModernDialogHelper.showConfirmDialog(
                        context = activity,
                        title = "移出藏书档案",
                        message = "确认移出《${record.title}》的藏书记录？",
                        emoji = "🗑️",
                        positiveText = "确认移出",
                        negativeText = "取消",
                        isDestructive = true
                    ) {
                        store.deleteBookRecord(record.id)
                        Toast.makeText(activity, "已移出藏书档案记录", Toast.LENGTH_SHORT).show()
                        reloadList()
                        onDataChanged()
                    }
                }
            )
        }

        binding.rvBooks.layoutManager = LinearLayoutManager(activity)
        reloadList()

        binding.chipGroupBookFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilter = when (checkedIds.firstOrNull()) {
                R.id.chip_book_reading -> "reading"
                R.id.chip_book_unread -> "unread"
                R.id.chip_book_finished -> "finished"
                R.id.chip_book_lent -> "lent"
                else -> "all"
            }
            reloadList()
        }

        VaultUiHelper.bindSearchWatcher(binding.etSearchBooks) {
            currentSearchKeyword = it
            reloadList()
        }

        binding.btnAddBook.applyPressScaleAnimation(0.92f)
        binding.btnAddBook.setOnClickListener {
            showAddOrEditBookDialog(activity, store, null) {
                reloadList()
                onDataChanged()
            }
        }

        binding.btnCloseBookVault.applyPressScaleAnimation(0.92f)
        binding.btnCloseBookVault.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /** 快速更新阅读进度弹窗 */
    private fun showProgressUpdateDialog(
        activity: Activity,
        store: DataStore,
        record: BookRecord,
        onUpdated: () -> Unit
    ) {
        val input = EditText(activity).apply {
            hint = "输入当前已读页数 (总计 ${record.totalPages} 页)"
            setText("${record.currentPages}")
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setSelection(text.length)
            setBackgroundResource(R.drawable.bg_input_box)
            setPadding(36, 24, 36, 24)
        }

        ModernDialogHelper.showCustomViewDialog(
            context = activity,
            title = "📖 阅读进度打卡",
            customView = input,
            emoji = "📖",
            positiveText = "保存进度",
            negativeText = "取消"
        ) {
            val pages = input.text.toString().toIntOrNull() ?: record.currentPages
            store.updateBookReadingProgress(record.id, pages)
            val updated = store.getBookRecords().firstOrNull { it.id == record.id } ?: record
            val msg = if (updated.isFinished()) "🎉 恭喜读完《${record.title}》！" else "📖 已打卡至第 $pages 页 (${updated.getProgressPercent()}%)"
            Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
            onUpdated()
        }
    }

    /** 外借登记与归还确认弹窗 */
    private fun showLendOrReturnDialog(
        activity: Activity,
        store: DataStore,
        record: BookRecord,
        onUpdated: () -> Unit
    ) {
        if (record.isLent()) {
            ModernDialogHelper.showConfirmDialog(
                context = activity,
                title = "📥 登记还书确认",
                message = "确认借阅人【${record.borrowerName}】已归还《${record.title}》？\n书籍将重新归架在【${record.bookshelfLocation.ifBlank { "书房" }}】。",
                emoji = "📚",
                positiveText = "确认已还",
                negativeText = "取消"
            ) {
                store.markBookReturned(record.id)
                Toast.makeText(activity, "🎉 《${record.title}》已归还归架！", Toast.LENGTH_SHORT).show()
                onUpdated()
            }
        } else {
            val input = EditText(activity).apply {
                hint = "输入借阅人姓名 (如：同事小张 / 朋友李明)"
                setBackgroundResource(R.drawable.bg_input_box)
                setPadding(36, 24, 36, 24)
            }

            ModernDialogHelper.showCustomViewDialog(
                context = activity,
                title = "📤 实体书借出登记",
                customView = input,
                emoji = "📤",
                positiveText = "确认借出",
                negativeText = "取消"
            ) {
                val borrower = input.text.toString().trim()
                if (borrower.isBlank()) {
                    Toast.makeText(activity, "请输入借阅人姓名", Toast.LENGTH_SHORT).show()
                    return@showCustomViewDialog
                }
                store.markBookLent(record.id, borrower)
                Toast.makeText(activity, "📤 《${record.title}》已登记借给【$borrower】！", Toast.LENGTH_SHORT).show()
                onUpdated()
            }
        }
    }

    /** 弹出登记/编辑藏书档案弹窗 */
    fun showAddOrEditBookDialog(
        activity: Activity,
        store: DataStore,
        editingRecord: BookRecord?,
        onSaved: () -> Unit
    ) {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_add_book, null)
        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(view)
            .create()

        VaultUiHelper.setupVaultWindow(dialog)

        val tvTitle = view.findViewById<TextView>(R.id.tv_dialog_book_title)
        val chipGroupCategory = view.findViewById<ChipGroup>(R.id.chip_group_book_category_edit)
        val etTitle = view.findViewById<EditText>(R.id.et_book_title)
        val etAuthor = view.findViewById<EditText>(R.id.et_book_author)
        val etTotalPages = view.findViewById<EditText>(R.id.et_book_total_pages)
        val etCurrentPages = view.findViewById<EditText>(R.id.et_book_current_pages)
        val etLocation = view.findViewById<EditText>(R.id.et_book_location)
        val etRating = view.findViewById<EditText>(R.id.et_book_rating)
        val etNotes = view.findViewById<EditText>(R.id.et_book_notes)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btn_cancel_add_book)
        val btnSave = view.findViewById<MaterialButton>(R.id.btn_save_book)

        if (editingRecord != null) {
            tvTitle.text = "📚 编辑书房藏书"
            when (editingRecord.category) {
                "文学小说" -> chipGroupCategory.check(R.id.chip_edit_cat_novel)
                "经管商业" -> chipGroupCategory.check(R.id.chip_edit_cat_business)
                "科技专业" -> chipGroupCategory.check(R.id.chip_edit_cat_tech)
                "亲子绘本" -> chipGroupCategory.check(R.id.chip_edit_cat_child)
                else -> chipGroupCategory.check(R.id.chip_edit_cat_social)
            }
            etTitle.setText(editingRecord.title)
            etAuthor.setText(editingRecord.author)
            etTotalPages.setText("${editingRecord.totalPages}")
            etCurrentPages.setText("${editingRecord.currentPages}")
            etLocation.setText(editingRecord.bookshelfLocation)
            etRating.setText("${editingRecord.rating}")
            etNotes.setText(editingRecord.summaryNotes)
        } else {
            tvTitle.text = "📚 登记书房藏书"
            chipGroupCategory.check(R.id.chip_edit_cat_social)
            etTotalPages.setText("340")
            etCurrentPages.setText("0")
            etRating.setText("5.0")
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            if (title.isBlank()) {
                Toast.makeText(activity, "请输入书名", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val author = etAuthor.text.toString().trim()
            val category = when (chipGroupCategory.checkedChipId) {
                R.id.chip_edit_cat_novel -> "文学小说"
                R.id.chip_edit_cat_business -> "经管商业"
                R.id.chip_edit_cat_tech -> "科技专业"
                R.id.chip_edit_cat_child -> "亲子绘本"
                else -> "社科人文"
            }

            val totalP = etTotalPages.text.toString().toIntOrNull() ?: 300
            val curP = etCurrentPages.text.toString().toIntOrNull() ?: 0
            val location = etLocation.text.toString().trim()
            val rating = etRating.text.toString().toFloatOrNull() ?: 5.0f
            val notes = etNotes.text.toString().trim()

            val record = editingRecord?.copy(
                title = title,
                author = author,
                category = category,
                bookshelfLocation = location,
                totalPages = totalP,
                currentPages = curP,
                rating = rating,
                summaryNotes = notes
            ) ?: BookRecord(
                id = UUID.randomUUID().toString(),
                title = title,
                author = author,
                category = category,
                bookshelfLocation = location,
                totalPages = totalP,
                currentPages = curP,
                readingStatus = if (curP > 0) "reading" else "unread",
                rating = rating,
                summaryNotes = notes
            )

            store.addOrUpdateBookRecord(record)
            Toast.makeText(activity, "🎉 《$title》已成功归入书房藏书舱！", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            onSaved()
        }

        dialog.show()
    }
}
