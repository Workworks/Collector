package com.kfaino.diapertracker

import android.app.Activity
import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kfaino.diapertracker.databinding.DialogHonorVaultBinding
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 🏆 全家成长履历、职业荣誉与考级证书勋章馆控制器 (Honor & Credential Trophy Dialog)
 * - 覆盖全家多成员（本人/伴侣/孩子/父母）与全维度荣誉资质（学历/职称/考级/竞赛/勋章）
 * - 证书编号脱敏一键复制与复审年审到期预警
 * - 原生 Canvas 离线 1080P「全家成长足迹与高光成就长卷」导出
 */
object HonorVaultDialog {

    fun show(activity: Activity, store: DataStore, onDataChanged: () -> Unit = {}) {
        val binding = DialogHonorVaultBinding.inflate(LayoutInflater.from(activity))
        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(binding.root)
            .create()

        VaultUiHelper.setupVaultWindow(dialog)

        var currentMemberFilter = "all"
        var currentCategoryFilter = "all"
        var currentSearchKeyword = ""

        fun reloadList() {
            val allHonors = store.getHonorCredentials()

            val filtered = allHonors.filter { h ->
                val matchesMember = when (currentMemberFilter) {
                    "all" -> true
                    else -> h.member == currentMemberFilter
                }
                val matchesCategory = when (currentCategoryFilter) {
                    "all" -> true
                    else -> h.category == currentCategoryFilter
                }
                val matchesSearch = currentSearchKeyword.isEmpty() ||
                        h.title.contains(currentSearchKeyword, ignoreCase = true) ||
                        h.issuer.contains(currentSearchKeyword, ignoreCase = true) ||
                        h.certNumber.contains(currentSearchKeyword, ignoreCase = true) ||
                        h.notes.contains(currentSearchKeyword, ignoreCase = true)

                matchesMember && matchesCategory && matchesSearch
            }.sortedByDescending { it.issueDate }

            binding.layoutHonorEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
            binding.rvHonors.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE

            binding.rvHonors.adapter = HonorAdapter(
                activity = activity,
                list = filtered,
                onCopyClick = { honor ->
                    VaultUiHelper.copyToClipboard(
                        context = activity,
                        label = "Cert Number",
                        text = honor.certNumber,
                        successMessage = "📋 已复制完整证书编号到剪贴板！"
                    )
                },
                onItemClick = { honor ->
                    showAddOrEditHonorDialog(activity, store, honor) {
                        reloadList()
                        onDataChanged()
                    }
                },
                onDeleteClick = { honor ->
                    ModernDialogHelper.showConfirmDialog(
                        context = activity,
                        title = "移出荣誉记录",
                        message = "确认从荣誉馆移出【${honor.title}】？",
                        emoji = "🗑️",
                        positiveText = "确认移出",
                        negativeText = "取消"
                    ) {
                        store.deleteHonorCredential(honor.id)
                        Toast.makeText(activity, "已移出【${honor.title}】", Toast.LENGTH_SHORT).show()
                        reloadList()
                        onDataChanged()
                    }
                }
            )
        }

        binding.rvHonors.layoutManager = LinearLayoutManager(activity)
        reloadList()

        binding.chipGroupHonorMembers.setOnCheckedStateChangeListener { _, checkedIds ->
            currentMemberFilter = when (checkedIds.firstOrNull()) {
                R.id.chip_member_self -> "本人"
                R.id.chip_member_partner -> "伴侣"
                R.id.chip_member_child -> "孩子"
                R.id.chip_member_parents -> "父母"
                else -> "all"
            }
            reloadList()
        }

        binding.chipGroupHonorCategories.setOnCheckedStateChangeListener { _, checkedIds ->
            currentCategoryFilter = when (checkedIds.firstOrNull()) {
                R.id.chip_cat_degree -> "degree"
                R.id.chip_cat_career -> "career"
                R.id.chip_cat_exam -> "exam"
                R.id.chip_cat_competition -> "competition"
                R.id.chip_cat_medal -> "medal"
                else -> "all"
            }
            reloadList()
        }

        VaultUiHelper.bindSearchWatcher(binding.etSearchHonor) {
            currentSearchKeyword = it
            reloadList()
        }

        binding.btnAddHonor.applyPressScaleAnimation(0.92f)
        binding.btnAddHonor.setOnClickListener {
            showAddOrEditHonorDialog(activity, store, null) {
                reloadList()
                onDataChanged()
            }
        }

        binding.btnGenerateTimelinePoster.applyPressScaleAnimation(0.94f)
        binding.btnGenerateTimelinePoster.setOnClickListener {
            val honors = store.getHonorCredentials()
            if (honors.isEmpty()) {
                Toast.makeText(activity, "荣誉馆尚无记录，请先录入荣誉！", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            generateAndExportHonorPoster(activity, honors)
        }

        binding.btnCloseHonorVault.applyPressScaleAnimation(0.92f)
        binding.btnCloseHonorVault.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /** 弹出添加/编辑荣誉弹窗 */
    fun showAddOrEditHonorDialog(
        activity: Activity,
        store: DataStore,
        editingHonor: HonorCredential?,
        onSaved: () -> Unit
    ) {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_add_honor, null)
        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(view)
            .create()

        VaultUiHelper.setupVaultWindow(dialog)

        val tvTitle = view.findViewById<TextView>(R.id.tv_dialog_honor_title)
        val chipGroupMember = view.findViewById<ChipGroup>(R.id.chip_group_honor_member_edit)
        val chipGroupCat = view.findViewById<ChipGroup>(R.id.chip_group_honor_cat_edit)
        val etTitle = view.findViewById<EditText>(R.id.et_honor_title)
        val etCertNumber = view.findViewById<EditText>(R.id.et_honor_cert_number)
        val etIssuer = view.findViewById<EditText>(R.id.et_honor_issuer)
        val etScore = view.findViewById<EditText>(R.id.et_honor_score)
        val btnPickIssueDate = view.findViewById<View>(R.id.btn_pick_honor_issue_date)
        val tvIssueDateText = view.findViewById<TextView>(R.id.tv_honor_issue_date_text)
        val btnPickExpiryDate = view.findViewById<View>(R.id.btn_pick_honor_expiry_date)
        val tvExpiryDateText = view.findViewById<TextView>(R.id.tv_honor_expiry_date_text)
        val etNotes = view.findViewById<EditText>(R.id.et_honor_notes)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btn_cancel_add_honor)
        val btnSave = view.findViewById<MaterialButton>(R.id.btn_save_honor)

        var selectedIssueDate = editingHonor?.issueDate ?: System.currentTimeMillis()
        var selectedExpiryDate = editingHonor?.expiryDate ?: 0L

        fun updateDatesUi() {
            tvIssueDateText.text = "📅 获得发证时间: ${VaultUiHelper.standardDateFormat.format(Date(selectedIssueDate))}"
            if (selectedExpiryDate > 0L) {
                tvExpiryDateText.text = "📅 有效期/复审至: ${VaultUiHelper.standardDateFormat.format(Date(selectedExpiryDate))}"
                tvExpiryDateText.setTextColor(activity.getColor(R.color.primary))
            } else {
                tvExpiryDateText.text = "📅 设为终身有效 / 长期"
                tvExpiryDateText.setTextColor(activity.getColor(R.color.text_secondary))
            }
        }

        if (editingHonor != null) {
            tvTitle.text = "🏆 编辑荣誉与资质记录"
            when (editingHonor.member) {
                "伴侣" -> chipGroupMember.check(R.id.chip_edit_mem_partner)
                "孩子" -> chipGroupMember.check(R.id.chip_edit_mem_child)
                "父母" -> chipGroupMember.check(R.id.chip_edit_mem_parents)
                else -> chipGroupMember.check(R.id.chip_edit_mem_self)
            }
            when (editingHonor.category) {
                "degree" -> chipGroupCat.check(R.id.chip_edit_cat_degree)
                "exam" -> chipGroupCat.check(R.id.chip_edit_cat_exam)
                "competition" -> chipGroupCat.check(R.id.chip_edit_cat_competition)
                "medal" -> chipGroupCat.check(R.id.chip_edit_cat_medal)
                else -> chipGroupCat.check(R.id.chip_edit_cat_career)
            }
            etTitle.setText(editingHonor.title)
            etCertNumber.setText(editingHonor.certNumber)
            etIssuer.setText(editingHonor.issuer)
            etScore.setText(editingHonor.scoreOrLevel)
            etNotes.setText(editingHonor.notes)
            updateDatesUi()
        } else {
            tvTitle.text = "🏆 录入新荣誉证书"
            chipGroupMember.check(R.id.chip_edit_mem_self)
            chipGroupCat.check(R.id.chip_edit_cat_career)
            updateDatesUi()
        }

        btnPickIssueDate.setOnClickListener {
            VaultUiHelper.showDatePicker(activity, selectedIssueDate) { time, _ ->
                selectedIssueDate = time
                updateDatesUi()
            }
        }

        btnPickExpiryDate.setOnClickListener {
            VaultUiHelper.showDatePicker(activity, selectedExpiryDate) { time, _ ->
                selectedExpiryDate = time
                updateDatesUi()
            }
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            if (title.isEmpty()) {
                Toast.makeText(activity, "请输入证书或荣誉名称", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val member = when (chipGroupMember.checkedChipId) {
                R.id.chip_edit_mem_partner -> "伴侣"
                R.id.chip_edit_mem_child -> "孩子"
                R.id.chip_edit_mem_parents -> "父母"
                else -> "本人"
            }

            val cat = when (chipGroupCat.checkedChipId) {
                R.id.chip_edit_cat_degree -> "degree"
                R.id.chip_edit_cat_exam -> "exam"
                R.id.chip_edit_cat_competition -> "competition"
                R.id.chip_edit_cat_medal -> "medal"
                else -> "career"
            }

            val certNum = etCertNumber.text.toString().trim()
            val issuer = etIssuer.text.toString().trim()
            val score = etScore.text.toString().trim()
            val notes = etNotes.text.toString().trim()

            val honor = HonorCredential(
                id = editingHonor?.id ?: java.util.UUID.randomUUID().toString(),
                member = member,
                category = cat,
                title = title,
                certNumber = certNum,
                issuer = issuer,
                issueDate = selectedIssueDate,
                expiryDate = selectedExpiryDate,
                scoreOrLevel = score,
                notes = notes
            )

            store.addOrUpdateHonorCredential(honor)
            Toast.makeText(activity, "🎉 已将【$title】存入荣誉勋章馆！", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            onSaved()
        }

        dialog.show()
    }

    /** 生成 1080P 人生高光足迹长卷海报并保存分享 */
    private fun generateAndExportHonorPoster(activity: Activity, honors: List<HonorCredential>) {
        val sorted = honors.sortedBy { it.issueDate }
        val width = 1080
        val headerHeight = 360
        val itemHeight = 180
        val footerHeight = 180
        val height = headerHeight + sorted.size * itemHeight + footerHeight

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. 背景渐变 (深邃黑曜石)
        val bgPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2. 头部
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 52f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#10B981")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 24f
        }

        canvas.drawText("🏆 全家成长足迹与高光成就长卷", 80f, 130f, titlePaint)
        canvas.drawText("COLLECTER · FAMILY HONOR & CAREER TIMELINE", 80f, 185f, subPaint)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        canvas.drawText("生成日期: ${sdf.format(Date())}  ·  收录荣誉: ${sorted.size} 项", 80f, 240f, metaPaint)

        // 分割线
        val linePaint = Paint().apply {
            color = Color.parseColor("#334155")
            strokeWidth = 3f
        }
        canvas.drawLine(80f, 290f, (width - 80).toFloat(), 290f, linePaint)

        // 3. 绘制时间轴条目
        var currentY = (headerHeight).toFloat()
        val timelinePaint = Paint().apply {
            color = Color.parseColor("#10B981")
            strokeWidth = 4f
        }
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#10B981")
            style = Paint.Style.FILL
        }
        val cardBgPaint = Paint().apply {
            color = Color.parseColor("#1E293B")
            style = Paint.Style.FILL
        }

        val itemTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val itemSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            textSize = 24f
        }
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F59E0B")
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        for ((idx, h) in sorted.withIndex()) {
            val cardRect = RectF(140f, currentY, (width - 80).toFloat(), currentY + 140f)
            canvas.drawRoundRect(cardRect, 16f, 16f, cardBgPaint)

            // 时间轴小圆点
            canvas.drawCircle(95f, currentY + 70f, 10f, dotPaint)
            if (idx < sorted.size - 1) {
                canvas.drawLine(95f, currentY + 70f, 95f, currentY + itemHeight + 70f, timelinePaint)
            }

            val dateStr = if (h.issueDate > 0L) sdf.format(Date(h.issueDate)) else "历史荣誉"
            canvas.drawText("${h.member} · ${h.title}", 170f, currentY + 54f, itemTitlePaint)
            canvas.drawText("$dateStr  |  ${h.issuer.ifEmpty { "官方认证" }}", 170f, currentY + 104f, itemSubPaint)

            if (h.scoreOrLevel.isNotEmpty()) {
                canvas.drawText("🏅 ${h.scoreOrLevel}", (width - 240).toFloat(), currentY + 54f, badgePaint)
            }

            currentY += itemHeight
        }

        // 4. 底部水印
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 22f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("📦 由 Collecter 智能资产与全维度收纳宇宙离线渲染生成", (width / 2).toFloat(), (height - 60).toFloat(), footerPaint)

        // 5. 保存并弹窗
        try {
            val dir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: activity.cacheDir
            val file = File(dir, "Collecter_Honor_Timeline_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            ModernDialogHelper.showInfoDialog(
                context = activity,
                title = "🎉 高光成就长卷已生成！",
                emoji = "📜",
                message = "1080P 高清足迹海报已成功导出至：\n${file.absolutePath}\n\n已收录 ${sorted.size} 项家庭荣誉与资质证书，可随时打印或分享给亲友！",
                buttonText = "太棒了"
            )
        } catch (e: Exception) {
            Toast.makeText(activity, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private class HonorAdapter(
        private val activity: Activity,
        private val list: List<HonorCredential>,
        private val onCopyClick: (HonorCredential) -> Unit,
        private val onItemClick: (HonorCredential) -> Unit,
        private val onDeleteClick: (HonorCredential) -> Unit
    ) : RecyclerView.Adapter<HonorAdapter.HonorViewHolder>() {

        class HonorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvMemberBadge: TextView = itemView.findViewById(R.id.item_honor_member_badge)
            val tvCategoryBadge: TextView = itemView.findViewById(R.id.item_honor_category_badge)
            val tvScoreBadge: TextView = itemView.findViewById(R.id.item_honor_score_badge)
            val tvExpiryWarning: TextView = itemView.findViewById(R.id.item_honor_expiry_warning)
            val tvTitle: TextView = itemView.findViewById(R.id.item_honor_title)
            val tvCertNumber: TextView = itemView.findViewById(R.id.item_honor_cert_number)
            val btnCopy: View = itemView.findViewById(R.id.btn_item_copy_cert_number)
            val tvIssuerDate: TextView = itemView.findViewById(R.id.item_honor_issuer_date)
            val tvNotes: TextView = itemView.findViewById(R.id.item_honor_notes)
            val tvValidity: TextView = itemView.findViewById(R.id.item_honor_validity_text)
            val btnDelete: ImageButton = itemView.findViewById(R.id.btn_item_delete_honor)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HonorViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_honor_credential, parent, false)
            return HonorViewHolder(view)
        }

        override fun onBindViewHolder(holder: HonorViewHolder, position: Int) {
            val h = list[position]
            holder.tvMemberBadge.text = "👤 ${h.member}"
            holder.tvCategoryBadge.text = h.getCategoryDisplayName()
            holder.tvTitle.text = h.title

            if (h.scoreOrLevel.isNotEmpty()) {
                holder.tvScoreBadge.visibility = View.VISIBLE
                holder.tvScoreBadge.text = "🏅 ${h.scoreOrLevel}"
            } else {
                holder.tvScoreBadge.visibility = View.GONE
            }

            if (h.isExpiringSoon()) {
                holder.tvExpiryWarning.visibility = View.VISIBLE
                holder.tvExpiryWarning.text = "⏳ 临近年审换证"
                holder.tvExpiryWarning.setTextColor(Color.parseColor("#F59E0B"))
            } else if (h.isExpired()) {
                holder.tvExpiryWarning.visibility = View.VISIBLE
                holder.tvExpiryWarning.text = "🔴 已过换证期"
                holder.tvExpiryWarning.setTextColor(Color.parseColor("#EF4444"))
            } else {
                holder.tvExpiryWarning.visibility = View.GONE
            }

            if (h.certNumber.isNotEmpty()) {
                holder.tvCertNumber.text = "证书编号: ${h.getMaskedCertNumber()}"
                holder.btnCopy.visibility = View.VISIBLE
            } else {
                holder.tvCertNumber.text = "证书编号: 暂无编号"
                holder.btnCopy.visibility = View.GONE
            }

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateStr = if (h.issueDate > 0L) sdf.format(Date(h.issueDate)) else "未设"
            val issuerStr = h.issuer.ifEmpty { "官方认证" }
            holder.tvIssuerDate.text = "🏛️ 发证: $issuerStr  ·  📅 获得: $dateStr"

            if (h.notes.isNotEmpty()) {
                holder.tvNotes.visibility = View.VISIBLE
                holder.tvNotes.text = "💡 ${h.notes}"
            } else {
                holder.tvNotes.visibility = View.GONE
            }

            if (h.expiryDate > 0L) {
                holder.tvValidity.text = "📅 有效至: ${sdf.format(Date(h.expiryDate))}"
                holder.tvValidity.setTextColor(Color.parseColor("#94A3B8"))
            } else {
                holder.tvValidity.text = "🟢 终身有效"
                holder.tvValidity.setTextColor(Color.parseColor("#10B981"))
            }

            holder.itemView.setOnClickListener { onItemClick(h) }
            holder.btnCopy.setOnClickListener { onCopyClick(h) }
            holder.btnDelete.setOnClickListener { onDeleteClick(h) }
        }

        override fun getItemCount(): Int = list.size
    }
}
