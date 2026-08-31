package com.kfaino.diapertracker

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kfaino.diapertracker.databinding.DialogAddIdentityDocBinding
import com.kfaino.diapertracker.databinding.DialogFamilyVaultBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * 🪪 家庭多成员证照安全夹控制器 (Family Vault Dialog Controller)
 * - 全家身份证、护照、户口本、结婚证、房产证分类收纳
 * - 证号一键脱敏复制、到期换证预警与 Canvas 倾斜防盗流水印导出
 */
object FamilyVaultDialog {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /** 打开家庭证照安全夹主弹窗 */
    fun showFamilyVaultDialog(
        activity: Activity,
        store: DataStore,
        onUpdated: () -> Unit
    ) {
        val binding = DialogFamilyVaultBinding.inflate(activity.layoutInflater)
        var currentMemberFilter = "all" // "all", "本人", "伴侣", "孩子", "父母长辈"

        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(binding.root)
            .setCancelable(true)
            .create()
        VaultUiHelper.setupVaultWindow(dialog)

        fun refreshList() {
            val all = store.getIdentityDocs()
            val displayList = if (currentMemberFilter == "all") {
                all
            } else if (currentMemberFilter == "父母长辈") {
                all.filter { it.member == "父亲" || it.member == "母亲" || it.member == "长辈" }
            } else {
                all.filter { it.member == currentMemberFilter }
            }

            renderDocList(activity, store, displayList, currentMemberFilter, binding.familyVaultListContainer) {
                refreshList()
                onUpdated()
            }
        }

        fun updateTabs() {
            val activeBg = R.drawable.bg_chip_active
            val inActiveBg = R.drawable.bg_chip_inactive
            val white = Color.WHITE
            val secColor = ContextCompat.getColor(activity, R.color.text_secondary)

            binding.tabMemberAll.setBackgroundResource(if (currentMemberFilter == "all") activeBg else inActiveBg)
            binding.tabMemberAll.setTextColor(if (currentMemberFilter == "all") white else secColor)

            binding.tabMemberSelf.setBackgroundResource(if (currentMemberFilter == "本人") activeBg else inActiveBg)
            binding.tabMemberSelf.setTextColor(if (currentMemberFilter == "本人") white else secColor)

            binding.tabMemberPartner.setBackgroundResource(if (currentMemberFilter == "伴侣") activeBg else inActiveBg)
            binding.tabMemberPartner.setTextColor(if (currentMemberFilter == "伴侣") white else secColor)

            binding.tabMemberChild.setBackgroundResource(if (currentMemberFilter == "孩子") activeBg else inActiveBg)
            binding.tabMemberChild.setTextColor(if (currentMemberFilter == "孩子") white else secColor)

            binding.tabMemberParents.setBackgroundResource(if (currentMemberFilter == "父母长辈") activeBg else inActiveBg)
            binding.tabMemberParents.setTextColor(if (currentMemberFilter == "父母长辈") white else secColor)
        }

        binding.tabMemberAll.setOnClickListener { currentMemberFilter = "all"; updateTabs(); refreshList() }
        binding.tabMemberSelf.setOnClickListener { currentMemberFilter = "本人"; updateTabs(); refreshList() }
        binding.tabMemberPartner.setOnClickListener { currentMemberFilter = "伴侣"; updateTabs(); refreshList() }
        binding.tabMemberChild.setOnClickListener { currentMemberFilter = "孩子"; updateTabs(); refreshList() }
        binding.tabMemberParents.setOnClickListener { currentMemberFilter = "父母长辈"; updateTabs(); refreshList() }

        binding.btnCloseFamilyVault.applyPressScaleAnimation(0.92f)
        binding.btnCloseFamilyVault.setOnClickListener { dialog.dismiss() }

        binding.btnOpenAddIdentityDoc.applyPressScaleAnimation(0.92f)
        binding.btnOpenAddIdentityDoc.setOnClickListener {
            showAddOrEditDocDialog(activity, store, doc = null) {
                refreshList()
                onUpdated()
            }
        }

        refreshList()
        dialog.show()
    }

    /** 动态渲染证照列表卡片 */
    private fun renderDocList(
        activity: Activity,
        store: DataStore,
        list: List<IdentityDocument>,
        currentFilter: String,
        container: LinearLayout,
        onRefreshNeeded: () -> Unit
    ) {
        container.removeAllViews()
        val density = activity.resources.displayMetrics.density
        fun dp(dp: Int): Int = (dp * density + 0.5f).toInt()

        if (list.isEmpty()) {
            val emptyTv = TextView(activity).apply {
                text = "🪪 暂无【$currentFilter】的证照档案\n点击下方按钮立即登记全家人的身份证、护照或户口本吧~"
                textSize = 13f
                setTextColor(ContextCompat.getColor(activity, R.color.text_secondary))
                gravity = Gravity.CENTER
                setPadding(dp(20), dp(40), dp(20), dp(40))
            }
            container.addView(emptyTv)
            return
        }

        for (d in list) {
            val card = MaterialCardView(activity).apply {
                radius = dp(14).toFloat()
                cardElevation = 0f
                strokeWidth = dp(1)
                setStrokeColor(ContextCompat.getColor(activity, R.color.card_border))
                setCardBackgroundColor(ContextCompat.getColor(activity, R.color.card))
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(10) }
                layoutParams = lp
            }

            val root = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12))
            }

            // 1. 顶行：成员徽章 + 证件类型 + 姓名
            val topRow = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val memberBadge = TextView(activity).apply {
                text = "👤 ${d.member}"
                textSize = 11f
                paint.isFakeBoldText = true
                setTextColor(ContextCompat.getColor(activity, R.color.primary))
                setBackgroundResource(R.drawable.bg_chip_inactive)
                setPadding(dp(6), dp(2), dp(6), dp(2))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = dp(6)
                }
            }

            val titleTv = TextView(activity).apply {
                text = "${d.getDocTypeDisplayName()} · ${d.nameOnDoc}"
                textSize = 14f
                paint.isFakeBoldText = true
                setTextColor(ContextCompat.getColor(activity, R.color.text_primary))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val expBadge = TextView(activity).apply {
                val isExpiring = d.isExpiringSoon()
                val isExpired = d.isExpired()

                val textStr = if (isExpired) "🔴 已过期" else if (isExpiring) "⚠️ 临期换证" else "🟢 有效"
                val textColor = if (isExpiring) ContextCompat.getColor(activity, R.color.accent_dark) else if (isExpired) ContextCompat.getColor(activity, R.color.danger) else ContextCompat.getColor(activity, R.color.text_secondary)
                text = textStr
                textSize = 11f
                paint.isFakeBoldText = true
                setTextColor(textColor)
                setBackgroundResource(R.drawable.bg_chip_inactive)
                setPadding(dp(6), dp(2), dp(6), dp(2))
            }

            topRow.addView(memberBadge)
            topRow.addView(titleTv)
            topRow.addView(expBadge)
            root.addView(topRow)

            // 2. 证号与有效期
            val infoLayout = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, dp(6), 0, dp(4))
            }

            val numberTv = TextView(activity).apply {
                text = "🔢 证号: ${d.getMaskedNumber()} (点击复制完整证号)"
                textSize = 13f
                paint.isFakeBoldText = true
                setTextColor(ContextCompat.getColor(activity, R.color.accent_dark))
                setOnClickListener {
                    VaultUiHelper.copyToClipboard(activity, "ID Number", d.docNumber, "已复制【${d.nameOnDoc}】完整证号")
                }
            }
            infoLayout.addView(numberTv)

            val expStr = if (d.expiryDate > 0L) "📅 有效期截止: ${dateFormat.format(Date(d.expiryDate))}" else "📅 长期有效"
            val authStr = if (d.issuingAuthority.isNotBlank()) " · 签发: ${d.issuingAuthority}" else ""
            val descTv = TextView(activity).apply {
                text = "$expStr$authStr"
                textSize = 12f
                setTextColor(ContextCompat.getColor(activity, R.color.text_secondary))
            }
            infoLayout.addView(descTv)

            if (d.notes.isNotBlank()) {
                val notesTv = TextView(activity).apply {
                    text = "📝 存放/备注: ${d.notes}"
                    textSize = 11f
                    setTextColor(ContextCompat.getColor(activity, R.color.text_hint))
                }
                infoLayout.addView(notesTv)
            }

            root.addView(infoLayout)

            // 3. 底部快捷操作栏
            val btnRow = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
                setPadding(0, dp(4), 0, 0)
            }

            val btnEdit = TextView(activity).apply {
                text = "✏️ 编辑"
                textSize = 12f
                setTextColor(ContextCompat.getColor(activity, R.color.text_secondary))
                setPadding(dp(8), dp(4), dp(8), dp(4))
                setOnClickListener {
                    showAddOrEditDocDialog(activity, store, d) {
                        onRefreshNeeded()
                    }
                }
            }

            val btnDelete = TextView(activity).apply {
                text = "🗑️ 删除"
                textSize = 12f
                setTextColor(ContextCompat.getColor(activity, R.color.danger))
                setPadding(dp(8), dp(4), dp(8), dp(4))
                setOnClickListener {
                    ModernDialogHelper.showConfirmDialog(
                        context = activity,
                        title = "确认删除证照？",
                        message = "确定要删除【${d.nameOnDoc}】的【${d.getDocTypeDisplayName()}】吗？此操作无法撤销。",
                        emoji = "🗑️",
                        positiveText = "确认删除",
                        negativeText = "取消"
                    ) {
                        store.deleteIdentityDoc(d.id)
                        onRefreshNeeded()
                    }
                }
            }

            btnRow.addView(btnEdit)
            btnRow.addView(btnDelete)
            root.addView(btnRow)

            card.addView(root)
            container.addView(card)
        }
    }

    /** 登记 / 编辑证照档案弹窗 */
    fun showAddOrEditDocDialog(
        activity: Activity,
        store: DataStore,
        doc: IdentityDocument?,
        onSaved: () -> Unit
    ) {
        val binding = DialogAddIdentityDocBinding.inflate(activity.layoutInflater)
        var selectedMember = doc?.member ?: "本人"
        var selectedDocType = doc?.docType ?: "id_card"
        var selectedExpiryDate = doc?.expiryDate ?: 0L

        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(binding.root)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation

        val memberList = listOf("本人", "伴侣", "孩子", "父亲", "母亲", "长辈", "其他")
        val docTypeList = listOf(
            "id_card" to "🪪 居民身份证",
            "passport" to "🛂 出入境护照",
            "hk_macau_pass" to "🧳 港澳通行证",
            "driver_license" to "🚗 机动车驾驶证",
            "household" to "👨‍👩‍👧 居民户口簿",
            "marriage" to "💍 结婚证/公证书",
            "property" to "🏠 不动产权证",
            "contract" to "📄 电子劳动/租赁合同",
            "other" to "📑 其他重要凭证"
        )

        fun updateMemberText() {
            binding.tvSelectMember.text = "👤 成员: $selectedMember"
        }
        fun updateDocTypeText() {
            val name = docTypeList.firstOrNull { it.first == selectedDocType }?.second ?: "🪪 证件"
            binding.tvSelectDocType.text = name
        }
        fun updateExpiryText() {
            if (selectedExpiryDate > 0L) {
                binding.tvDocExpiryPicker.text = "📅 截止日期: " + dateFormat.format(Date(selectedExpiryDate))
            } else {
                binding.tvDocExpiryPicker.text = "📅 长期有效 (点击选择截止日)"
            }
        }

        updateMemberText()
        updateDocTypeText()
        updateExpiryText()

        binding.tvSelectMember.setOnClickListener {
            ModernDialogHelper.showSingleChoiceDialog(
                context = activity,
                title = "选择归属家庭成员",
                emoji = "👤",
                options = memberList,
                selectedIndex = memberList.indexOf(selectedMember).coerceAtLeast(0)
            ) { which, _ ->
                selectedMember = memberList[which]
                updateMemberText()
            }
        }

        binding.tvSelectDocType.setOnClickListener {
            val names = docTypeList.map { it.second }
            val currentIdx = docTypeList.indexOfFirst { it.first == selectedDocType }.coerceAtLeast(0)
            ModernDialogHelper.showSingleChoiceDialog(
                context = activity,
                title = "选择证件类型",
                emoji = "🪪",
                options = names,
                selectedIndex = currentIdx
            ) { which, _ ->
                selectedDocType = docTypeList[which].first
                updateDocTypeText()
            }
        }

        binding.tvDocExpiryPicker.setOnClickListener {
            ModernDatePickerDialog.show(activity, if (selectedExpiryDate > 0) selectedExpiryDate else System.currentTimeMillis(), "选择证件有效期截止日") { timeMs ->
                selectedExpiryDate = timeMs
                updateExpiryText()
            }
        }

        if (doc != null) {
            binding.tvAddDocDialogTitle.text = "✏️ 编辑证照档案"
            binding.etDocName.setText(doc.nameOnDoc)
            binding.etDocNumber.setText(doc.docNumber)
            binding.etDocAuthority.setText(doc.issuingAuthority)
            binding.etDocNotes.setText(doc.notes)
        }

        binding.btnCloseAddDoc.applyPressScaleAnimation(0.92f)
        binding.btnCloseAddDoc.setOnClickListener { dialog.dismiss() }

        binding.btnCancelDoc.applyPressScaleAnimation(0.92f)
        binding.btnCancelDoc.setOnClickListener { dialog.dismiss() }

        binding.btnConfirmDoc.applyPressScaleAnimation(0.92f)
        binding.btnConfirmDoc.setOnClickListener {
            val name = binding.etDocName.text.toString().trim()
            val number = binding.etDocNumber.text.toString().trim()

            if (name.isBlank()) {
                Toast.makeText(activity, "请输入证件姓名", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (number.isBlank()) {
                Toast.makeText(activity, "请输入证件号码", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val authority = binding.etDocAuthority.text.toString().trim()
            val notes = binding.etDocNotes.text.toString().trim()

            val record = IdentityDocument(
                id = doc?.id ?: UUID.randomUUID().toString(),
                member = selectedMember,
                docType = selectedDocType,
                docNumber = number,
                nameOnDoc = name,
                issueDate = doc?.issueDate ?: System.currentTimeMillis(),
                expiryDate = selectedExpiryDate,
                frontPhotoPath = doc?.frontPhotoPath ?: "",
                backPhotoPath = doc?.backPhotoPath ?: "",
                issuingAuthority = authority,
                notes = notes
            )

            store.addOrUpdateIdentityDoc(record)
            Toast.makeText(activity, "🎉 【$name】的证照档案已安全保存！", Toast.LENGTH_LONG).show()
            dialog.dismiss()
            onSaved()
        }

        dialog.show()
    }
}
