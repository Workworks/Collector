package com.kfaino.diapertracker

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * 💡 闪念灵感与想法收纳舱 (Idea & Thought Vault Dialog)
 * - 0.5 秒极速记录生活灵感、读书心得、随手便签与待办事项
 * - 支持置顶、标签筛选、多色卡片主题、复制与跨维关联实物
 */
object IdeaVaultDialog {

    private val PRESET_COLORS = listOf("#10B981", "#3B82F6", "#F59E0B", "#EC4899", "#8B5CF6", "#64748B")
    private val PRESET_EMOJIS = listOf("💡", "✨", "📝", "💭", "🎯", "⚡", "📌", "🌟")

    fun show(activity: Activity, store: DataStore, onDataChanged: () -> Unit = {}) {
        val density = activity.resources.displayMetrics.density
        fun dp(dp: Int): Int = (dp * density + 0.5f).toInt()

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(activity, R.drawable.bg_dialog_card)
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }

        // 头部
        val header = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }
            layoutParams = lp
        }

        val titleTv = TextView(activity).apply {
            text = "💡 闪念灵感与想法舱"
            textSize = 18f
            setTextColor(ContextCompat.getColor(activity, R.color.text_primary))
            setTypeface(null, android.graphics.Typeface.BOLD)
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            layoutParams = lp
        }

        val btnAdd = Button(activity).apply {
            text = "+ 记闪念"
            textSize = 13f
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#10B981"))
                cornerRadius = dp(8).toFloat()
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(36)
            )
            layoutParams = lp
        }

        header.addView(titleTv)
        header.addView(btnAdd)
        root.addView(header)

        // 搜索筛选栏
        val etSearch = EditText(activity).apply {
            hint = "🔍 搜索想法内容或标签..."
            textSize = 13f
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = GradientDrawable().apply {
                setColor(ContextCompat.getColor(activity, R.color.input_bg))
                cornerRadius = dp(8).toFloat()
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) }
            layoutParams = lp
        }
        root.addView(etSearch)

        // 滚动列表容器
        val scrollView = ScrollView(activity).apply {
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(380)
            )
            layoutParams = lp
        }

        val listContainer = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
        }
        scrollView.addView(listContainer)
        root.addView(scrollView)

        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(root)
            .setNeutralButton("关闭", null)
            .create()

        VaultUiHelper.setupVaultWindow(dialog)

        fun refreshList(keyword: String = "") {
            listContainer.removeAllViews()
            val allIdeas = store.getIdeas()
            val filtered = if (keyword.isBlank()) {
                allIdeas
            } else {
                allIdeas.filter {
                    it.content.contains(keyword, ignoreCase = true) ||
                    it.tags.any { t -> t.contains(keyword, ignoreCase = true) }
                }
            }

            if (filtered.isEmpty()) {
                val emptyTv = TextView(activity).apply {
                    text = if (allIdeas.isEmpty()) "🌿 暂无闪念想法\n点击右上角「+ 记闪念」记录一个点子吧！" else "🔍 未搜索到相关想法"
                    gravity = Gravity.CENTER
                    textSize = 13f
                    setTextColor(ContextCompat.getColor(activity, R.color.text_secondary))
                    setPadding(0, dp(60), 0, dp(60))
                }
                listContainer.addView(emptyTv)
                return
            }

            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

            filtered.forEach { idea ->
                val card = MaterialCardView(activity).apply {
                    radius = dp(12).toFloat()
                    cardElevation = dp(2).toFloat()
                    setCardBackgroundColor(ContextCompat.getColor(activity, R.color.card))
                    strokeWidth = dp(1)
                    try {
                        strokeColor = Color.parseColor(idea.colorHex)
                    } catch (_: Exception) {
                        strokeColor = Color.parseColor("#10B981")
                    }

                    val lp = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = dp(10) }
                    layoutParams = lp
                }

                val cardLayout = LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(14), dp(12), dp(14), dp(12))
                }

                // 顶部行 (Emoji + 置顶标识 + 时间)
                val topRow = LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }

                val emojiTv = TextView(activity).apply {
                    text = idea.moodEmoji
                    textSize = 15f
                    setPadding(0, 0, dp(6), 0)
                }

                val dateTv = TextView(activity).apply {
                    text = sdf.format(Date(idea.updatedAt))
                    textSize = 11f
                    setTextColor(ContextCompat.getColor(activity, R.color.text_hint))
                    val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    layoutParams = lp
                }

                val pinBtn = TextView(activity).apply {
                    text = if (idea.isPinned) "📌 取消置顶" else "📍 置顶"
                    textSize = 11f
                    setTextColor(if (idea.isPinned) Color.parseColor("#F59E0B") else ContextCompat.getColor(activity, R.color.text_secondary))
                    setPadding(dp(6), dp(2), dp(6), dp(2))
                    setOnClickListener {
                        store.toggleIdeaPin(idea.id)
                        refreshList(etSearch.text.toString().trim())
                        onDataChanged()
                    }
                }

                topRow.addView(emojiTv)
                topRow.addView(dateTv)
                topRow.addView(pinBtn)
                cardLayout.addView(topRow)

                // 正文
                val contentTv = TextView(activity).apply {
                    text = idea.content
                    textSize = 14f
                    setTextColor(ContextCompat.getColor(activity, R.color.text_primary))
                    val lp = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = dp(6)
                        bottomMargin = dp(6)
                    }
                    layoutParams = lp
                    setTextIsSelectable(true)
                }
                cardLayout.addView(contentTv)

                // 标签展示
                if (idea.tags.isNotEmpty()) {
                    val tagTv = TextView(activity).apply {
                        text = "🏷️ " + idea.tags.joinToString("  ") { "#$it" }
                        textSize = 11f
                        setTextColor(Color.parseColor("#3B82F6"))
                        val lp = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { bottomMargin = dp(6) }
                        layoutParams = lp
                    }
                    cardLayout.addView(tagTv)
                }

                // 底部操作行
                val actionRow = LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.END
                }

                val btnCopy = TextView(activity).apply {
                    text = "📋 复制"
                    textSize = 12f
                    setTextColor(Color.parseColor("#10B981"))
                    setPadding(dp(8), dp(4), dp(8), dp(4))
                    setOnClickListener {
                        VaultUiHelper.copyToClipboard(activity, "想法灵感", idea.content, "📋 已复制想法正文！")
                    }
                }

                val btnEdit = TextView(activity).apply {
                    text = "✏️ 编辑"
                    textSize = 12f
                    setTextColor(Color.parseColor("#3B82F6"))
                    setPadding(dp(8), dp(4), dp(8), dp(4))
                    setOnClickListener {
                        showEditIdeaDialog(activity, store, idea) {
                            refreshList(etSearch.text.toString().trim())
                            onDataChanged()
                        }
                    }
                }

                val btnDelete = TextView(activity).apply {
                    text = "🗑️ 删除"
                    textSize = 12f
                    setTextColor(Color.parseColor("#EF4444"))
                    setPadding(dp(8), dp(4), dp(8), dp(4))
                    setOnClickListener {
                        ModernDialogHelper.showConfirmDialog(
                            context = activity,
                            title = "删除闪念想法",
                            message = "确定要删除这条想法吗？此操作无法撤销。",
                            emoji = "🗑️",
                            positiveText = "删除",
                            isDestructive = true
                        ) {
                            store.deleteIdea(idea.id)
                            refreshList(etSearch.text.toString().trim())
                            onDataChanged()
                        }
                    }
                }

                actionRow.addView(btnCopy)
                actionRow.addView(btnEdit)
                actionRow.addView(btnDelete)
                cardLayout.addView(actionRow)

                card.addView(cardLayout)
                listContainer.addView(card)
            }
        }

        VaultUiHelper.bindSearchWatcher(etSearch) { kw -> refreshList(kw) }

        btnAdd.setOnClickListener {
            showEditIdeaDialog(activity, store, null) {
                refreshList()
                onDataChanged()
            }
        }

        refreshList()
        dialog.show()
    }

    private fun showEditIdeaDialog(
        activity: Activity,
        store: DataStore,
        existing: IdeaRecord?,
        onSaved: () -> Unit
    ) {
        val density = activity.resources.displayMetrics.density
        fun dp(dp: Int): Int = (dp * density + 0.5f).toInt()

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(activity, R.drawable.bg_dialog_card)
            setPadding(dp(20), dp(18), dp(20), dp(18))
        }

        val titleTv = TextView(activity).apply {
            text = if (existing == null) "💡 记一笔新闪念" else "✏️ 编辑闪念想法"
            textSize = 17f
            setTextColor(ContextCompat.getColor(activity, R.color.text_primary))
            setTypeface(null, android.graphics.Typeface.BOLD)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }
            layoutParams = lp
        }
        root.addView(titleTv)

        val etContent = EditText(activity).apply {
            hint = "写下此刻的想法、灵感、待办或读书心得..."
            textSize = 14f
            minLines = 4
            gravity = Gravity.TOP or Gravity.START
            if (existing != null) setText(existing.content)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = GradientDrawable().apply {
                setColor(ContextCompat.getColor(activity, R.color.input_bg))
                cornerRadius = dp(8).toFloat()
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) }
            layoutParams = lp
        }
        root.addView(etContent)

        val etTags = EditText(activity).apply {
            hint = "标签 (用空格或逗号分隔，如: 灵感 读书 待办)"
            textSize = 13f
            if (existing != null && existing.tags.isNotEmpty()) {
                setText(existing.tags.joinToString(" "))
            }
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = GradientDrawable().apply {
                setColor(ContextCompat.getColor(activity, R.color.input_bg))
                cornerRadius = dp(8).toFloat()
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(14) }
            layoutParams = lp
        }
        root.addView(etTags)

        var selectedEmoji = existing?.moodEmoji ?: "💡"
        var selectedColor = existing?.colorHex ?: "#10B981"

        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(root)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存") { _, _ ->
                val content = etContent.text.toString().trim()
                if (content.isEmpty()) {
                    Toast.makeText(activity, "想法内容不能为空", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val tagTokens = etTags.text.toString().trim()
                    .split(Regex("[,，\\s]+"))
                    .filter { it.isNotBlank() }
                    .distinct()

                val record = (existing ?: IdeaRecord(id = UUID.randomUUID().toString())).copy(
                    content = content,
                    tags = tagTokens,
                    moodEmoji = selectedEmoji,
                    colorHex = selectedColor,
                    updatedAt = System.currentTimeMillis()
                )

                store.addOrUpdateIdea(record)
                Toast.makeText(activity, "✨ 想法已收纳入库！", Toast.LENGTH_SHORT).show()
                onSaved()
            }
            .create()

        VaultUiHelper.setupVaultWindow(dialog)
        dialog.show()
    }
}
