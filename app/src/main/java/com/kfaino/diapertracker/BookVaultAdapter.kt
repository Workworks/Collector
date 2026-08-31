package com.kfaino.diapertracker

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kfaino.diapertracker.databinding.ItemBookRecordBinding
import java.util.Date

/**
 * 📚 书房藏书与阅读收纳列表适配器 (Bookshelf Vault Adapter)
 */
class BookVaultAdapter(
    private val activity: Activity,
    private val list: List<BookRecord>,
    private val onUpdateProgressClick: (BookRecord) -> Unit,
    private val onToggleLentClick: (BookRecord) -> Unit,
    private val onEditClick: (BookRecord) -> Unit,
    private val onDeleteClick: (BookRecord) -> Unit
) : RecyclerView.Adapter<BookVaultAdapter.BookViewHolder>() {

    class BookViewHolder(val binding: ItemBookRecordBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val binding = ItemBookRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookViewHolder(binding)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val record = list[position]
        val binding = holder.binding

        binding.itemBookCategoryBadge.text = record.category

        if (record.rating > 0.0f) {
            binding.itemBookRatingBadge.visibility = View.VISIBLE
            binding.itemBookRatingBadge.text = "⭐ ${record.rating}"
        } else {
            binding.itemBookRatingBadge.visibility = View.GONE
        }

        // 状态徽章与颜色
        binding.itemBookStatusBadge.text = record.getStatusDisplayName()
        when {
            record.isLent() -> {
                binding.itemBookStatusBadge.setTextColor(activity.getColor(R.color.danger))
            }
            record.isFinished() -> {
                binding.itemBookStatusBadge.setTextColor(activity.getColor(R.color.accent_dark))
            }
            record.isReading() -> {
                binding.itemBookStatusBadge.setTextColor(activity.getColor(R.color.primary))
            }
            else -> {
                binding.itemBookStatusBadge.setTextColor(activity.getColor(R.color.text_secondary))
            }
        }

        val authorPart = if (record.author.isNotBlank()) " · ${record.author}" else ""
        binding.itemBookTitleAuthor.text = "《${record.title}》$authorPart"

        if (record.bookshelfLocation.isNotBlank()) {
            binding.itemBookLocationText.visibility = View.VISIBLE
            binding.itemBookLocationText.text = "📍 存放于: ${record.bookshelfLocation}"
        } else {
            binding.itemBookLocationText.visibility = View.GONE
        }

        // 阅读进度
        if (record.totalPages > 0 && !record.isLent()) {
            binding.layoutBookProgress.visibility = View.VISIBLE
            val percent = record.getProgressPercent()
            binding.itemBookProgressBar.progress = percent
            binding.itemBookProgressText.text = "阅读进度: ${record.currentPages} / ${record.totalPages} 页 ($percent%)"
        } else {
            binding.layoutBookProgress.visibility = View.GONE
        }

        // 外借详情
        if (record.isLent()) {
            binding.itemBookLentText.visibility = View.VISIBLE
            val lentDateStr = if (record.lentDate > 0L) " (借出日期: ${VaultUiHelper.standardDateFormat.format(Date(record.lentDate))})" else ""
            binding.itemBookLentText.text = "📤 外借给: ${record.borrowerName}$lentDateStr"
            binding.btnItemToggleLent.text = "📥 登记还书"
        } else {
            binding.itemBookLentText.visibility = View.GONE
            binding.btnItemToggleLent.text = "📤 借出登记"
        }

        // 书摘笔记
        if (record.summaryNotes.isNotBlank()) {
            binding.itemBookNotes.visibility = View.VISIBLE
            binding.itemBookNotes.text = "📝 书摘: ${record.summaryNotes}"
        } else {
            binding.itemBookNotes.visibility = View.GONE
        }

        // 按钮交互动效与监听
        binding.btnItemUpdateProgress.applyPressScaleAnimation(0.92f)
        binding.btnItemUpdateProgress.setOnClickListener { onUpdateProgressClick(record) }
        binding.btnItemUpdateProgress.visibility = if (!record.isLent()) View.VISIBLE else View.GONE

        binding.btnItemToggleLent.applyPressScaleAnimation(0.92f)
        binding.btnItemToggleLent.setOnClickListener { onToggleLentClick(record) }

        binding.btnItemEditBook.applyPressScaleAnimation(0.90f)
        binding.btnItemEditBook.setOnClickListener { onEditClick(record) }

        binding.btnItemDeleteBook.applyPressScaleAnimation(0.90f)
        binding.btnItemDeleteBook.setOnClickListener { onDeleteClick(record) }
    }
}
