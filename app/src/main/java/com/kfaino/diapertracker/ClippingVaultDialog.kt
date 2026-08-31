package com.kfaino.diapertracker

import android.app.Activity
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * 📰 智能剪藏与文章知识舱 (Clipping & Knowledge Vault Dialog)
 * - 集中管理截图自动提取、网络链接深度剪藏与离线 Markdown 稍后读
 * - 支持 OCR 全文索引查看、文章沉浸式阅读、归档与跨维关联实物
 */
object ClippingVaultDialog {

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
            text = "📰 智能剪藏与知识库"
            textSize = 18f
            setTextColor(ContextCompat.getColor(activity, R.color.text_primary))
            setTypeface(null, android.graphics.Typeface.BOLD)
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            layoutParams = lp
        }

        val btnAdd = Button(activity).apply {
            text = "+ 手动剪藏"
            textSize = 13f
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#3B82F6"))
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
            hint = "🔍 搜索剪藏标题、正文、OCR文字或标签..."
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
                dp(400)
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
            val allClippings = store.getClippings()
            val filtered = if (keyword.isBlank()) {
                allClippings
            } else {
                allClippings.filter {
                    it.getSearchableContent().contains(keyword, ignoreCase = true)
                }
            }

            if (filtered.isEmpty()) {
                val emptyTv = TextView(activity).apply {
                    text = if (allClippings.isEmpty()) "🌿 暂无剪藏文章与截图\n开启截图监听或粘贴网页链接即可自动收纳！" else "🔍 未搜索到相关剪藏内容"
                    gravity = Gravity.CENTER
                    textSize = 13f
                    setTextColor(ContextCompat.getColor(activity, R.color.text_secondary))
                    setPadding(0, dp(60), 0, dp(60))
                }
                listContainer.addView(emptyTv)
                return
            }

            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

            filtered.forEach { clip ->
                val card = MaterialCardView(activity).apply {
                    radius = dp(12).toFloat()
                    cardElevation = dp(2).toFloat()
                    setCardBackgroundColor(ContextCompat.getColor(activity, R.color.card))
                    strokeWidth = dp(1)
                    strokeColor = Color.parseColor(if (clip.sourcePlatform == "screenshot") "#F59E0B" else "#3B82F6")

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

                // 顶部行 (平台徽章 + 标题 + 时间)
                val topRow = LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }

                val platformBadge = TextView(activity).apply {
                    text = when (clip.sourcePlatform) {
                        "screenshot" -> "📸 截图快照"
                        "wechat" -> "💬 微信长文"
                        "zhihu" -> "知 乎"
                        else -> "🌐 网页剪藏"
                    }
                    textSize = 10f
                    setTextColor(Color.WHITE)
                    background = GradientDrawable().apply {
                        setColor(if (clip.sourcePlatform == "screenshot") Color.parseColor("#F59E0B") else Color.parseColor("#3B82F6"))
                        cornerRadius = dp(4).toFloat()
                    }
                    setPadding(dp(6), dp(2), dp(6), dp(2))
                }

                val dateTv = TextView(activity).apply {
                    text = sdf.format(Date(clip.capturedAt))
                    textSize = 11f
                    setTextColor(ContextCompat.getColor(activity, R.color.text_hint))
                    gravity = Gravity.END
                    val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    layoutParams = lp
                }

                topRow.addView(platformBadge)
                topRow.addView(dateTv)
                cardLayout.addView(topRow)

                // 标题
                val titleView = TextView(activity).apply {
                    text = if (clip.title.isNotBlank()) clip.title else if (clip.ocrRawText.isNotBlank()) clip.ocrRawText.take(30) + "..." else "未命名剪藏"
                    textSize = 15f
                    setTextColor(ContextCompat.getColor(activity, R.color.text_primary))
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    val lp = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = dp(6)
                        bottomMargin = dp(4)
                    }
                    layoutParams = lp
                }
                cardLayout.addView(titleView)

                // 摘要或 OCR 文本预览
                val previewText = if (clip.summary.isNotBlank()) {
                    clip.summary
                } else if (clip.ocrRawText.isNotBlank()) {
                    "🔍 OCR 提取: ${clip.ocrRawText.replace("\n", " ").take(80)}..."
                } else if (clip.fullMarkdown.isNotBlank()) {
                    clip.fullMarkdown.replace("\n", " ").take(80) + "..."
                } else {
                    "暂无正文文本"
                }

                val summaryTv = TextView(activity).apply {
                    text = previewText
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(activity, R.color.text_secondary))
                    val lp = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = dp(6) }
                    layoutParams = lp
                }
                cardLayout.addView(summaryTv)

                // 标签展示
                if (clip.tags.isNotEmpty()) {
                    val tagTv = TextView(activity).apply {
                        text = "🏷️ " + clip.tags.joinToString("  ") { "#$it" }
                        textSize = 11f
                        setTextColor(Color.parseColor("#10B981"))
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

                val btnRead = TextView(activity).apply {
                    text = "📖 沉浸阅读"
                    textSize = 12f
                    setTextColor(Color.parseColor("#3B82F6"))
                    setPadding(dp(8), dp(4), dp(8), dp(4))
                    setOnClickListener {
                        showReaderModal(activity, clip)
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
                            title = "删除剪藏记录",
                            message = "确定要删除此条剪藏内容吗？此操作无法撤销。",
                            emoji = "🗑️",
                            positiveText = "删除",
                            isDestructive = true
                        ) {
                            store.deleteClipping(clip.id)
                            refreshList(etSearch.text.toString().trim())
                            onDataChanged()
                        }
                    }
                }

                actionRow.addView(btnRead)
                actionRow.addView(btnDelete)
                cardLayout.addView(actionRow)

                card.setOnClickListener { showReaderModal(activity, clip) }
                card.addView(cardLayout)
                listContainer.addView(card)
            }
        }

        VaultUiHelper.bindSearchWatcher(etSearch) { kw -> refreshList(kw) }

        btnAdd.setOnClickListener {
            showAddClippingDialog(activity, store) {
                refreshList()
                onDataChanged()
            }
        }

        refreshList()
        dialog.show()
    }

    private fun showReaderModal(activity: Activity, clip: ClippingRecord) {
        val density = activity.resources.displayMetrics.density
        fun dp(dp: Int): Int = (dp * density + 0.5f).toInt()

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(activity, R.drawable.bg_dialog_card)
            setPadding(dp(20), dp(18), dp(20), dp(18))
        }

        val titleTv = TextView(activity).apply {
            text = if (clip.title.isNotBlank()) clip.title else "剪藏详情"
            textSize = 17f
            setTextColor(ContextCompat.getColor(activity, R.color.text_primary))
            setTypeface(null, android.graphics.Typeface.BOLD)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }
            layoutParams = lp
        }
        root.addView(titleTv)

        val metaTv = TextView(activity).apply {
            text = "来源: ${clip.sourcePlatform}  |  时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(clip.capturedAt))}"
            textSize = 11f
            setTextColor(ContextCompat.getColor(activity, R.color.text_hint))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }
            layoutParams = lp
        }
        root.addView(metaTv)

        val scrollView = ScrollView(activity).apply {
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(420)
            )
            layoutParams = lp
        }

        val contentLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
        }

        // 如果包含本地图片，显示首张图片预览
        if (clip.localImagePaths.isNotEmpty()) {
            val firstPath = clip.localImagePaths.first()
            val imgFile = File(firstPath)
            if (imgFile.exists()) {
                val iv = ImageView(activity).apply {
                    val bmp = BitmapFactory.decodeFile(imgFile.absolutePath)
                    if (bmp != null) setImageBitmap(bmp)
                    adjustViewBounds = true
                    val lp = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = dp(12) }
                    layoutParams = lp
                }
                contentLayout.addView(iv)
            }
        }

        // Markdown / 正文
        val bodyText = if (clip.fullMarkdown.isNotBlank()) clip.fullMarkdown else clip.ocrRawText
        val contentTv = TextView(activity).apply {
            text = bodyText
            textSize = 14f
            setTextColor(ContextCompat.getColor(activity, R.color.text_primary))
            setTextIsSelectable(true)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(14) }
            layoutParams = lp
        }
        contentLayout.addView(contentTv)
        scrollView.addView(contentLayout)
        root.addView(scrollView)

        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(root)
            .setNeutralButton("关闭", null)
            .setPositiveButton("📋 复制全部文字") { _, _ ->
                VaultUiHelper.copyToClipboard(activity, "剪藏文本", bodyText, "📋 已复制剪藏全部文本！")
            }
            .create()

        VaultUiHelper.setupVaultWindow(dialog)
        dialog.show()
    }

    private fun showAddClippingDialog(activity: Activity, store: DataStore, onSaved: () -> Unit) {
        val density = activity.resources.displayMetrics.density
        fun dp(dp: Int): Int = (dp * density + 0.5f).toInt()

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(activity, R.drawable.bg_dialog_card)
            setPadding(dp(20), dp(18), dp(20), dp(18))
        }

        val titleTv = TextView(activity).apply {
            text = "📰 手动新增网络剪藏"
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

        val etTitle = EditText(activity).apply {
            hint = "文章/剪藏标题"
            textSize = 14f
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
        root.addView(etTitle)

        val etContent = EditText(activity).apply {
            hint = "粘贴文章正文、笔记或重点摘录..."
            textSize = 14f
            minLines = 5
            gravity = Gravity.TOP or Gravity.START
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
            hint = "分类标签 (用空格分隔，如: 技术 投资 摄影)"
            textSize = 13f
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

        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(root)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存入库") { _, _ ->
                val title = etTitle.text.toString().trim()
                val content = etContent.text.toString().trim()
                if (title.isEmpty() && content.isEmpty()) {
                    Toast.makeText(activity, "标题或正文至少填写一项", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val tagTokens = etTags.text.toString().trim()
                    .split(Regex("[,，\\s]+"))
                    .filter { it.isNotBlank() }
                    .distinct()

                val record = ClippingRecord(
                    id = UUID.randomUUID().toString(),
                    title = if (title.isNotBlank()) title else "剪藏笔记",
                    sourcePlatform = "note",
                    fullMarkdown = content,
                    tags = tagTokens,
                    capturedAt = System.currentTimeMillis()
                )

                store.addOrUpdateClipping(record)
                Toast.makeText(activity, "✨ 剪藏已收纳入库！", Toast.LENGTH_SHORT).show()
                onSaved()
            }
            .create()

        VaultUiHelper.setupVaultWindow(dialog)
        dialog.show()
    }
}
