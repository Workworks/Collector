package com.kfaino.diapertracker

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kfaino.diapertracker.databinding.DialogBackupManageBinding
import com.kfaino.diapertracker.databinding.DialogEditRepoBinding
import com.kfaino.diapertracker.databinding.DialogImportBackupBinding
import com.kfaino.diapertracker.databinding.DialogMoreSettingsBinding
import com.kfaino.diapertracker.databinding.DialogThemePickerBinding
import com.kfaino.diapertracker.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val store by lazy { DataStore(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val verName = UpdateManager.getAppVersionName(requireContext())
        binding.currentVersionBadge.text = "v$verName"

        setupClicks()
    }

    private fun setupClicks() {
        binding.btnCategoryManage.applyPressScaleAnimation(0.94f)
        binding.btnFloorplanManage.applyPressScaleAnimation(0.94f)
        binding.btnMoreSettings.applyPressScaleAnimation(0.94f)
        binding.btnBackupRestore.applyPressScaleAnimation(0.94f)
        binding.btnFeedback.applyPressScaleAnimation(0.94f)
        binding.btnTerms.applyPressScaleAnimation(0.94f)
        binding.btnPrivacy.applyPressScaleAnimation(0.94f)
        binding.btnUpdateVersion.applyPressScaleAnimation(0.94f)
        binding.btnAbout.applyPressScaleAnimation(0.94f)

        // 1. 分类管理
        binding.btnCategoryManage.setOnClickListener {
            CategoryManagerDialog.showManageDialog(requireContext(), store) {
                Toast.makeText(requireContext(), "分类设置已更新", Toast.LENGTH_SHORT).show()
            }
        }

        // 2. 空间平面图与寻物地图
        binding.btnFloorplanManage.setOnClickListener {
            FloorPlanDialog.show(requireActivity(), store, isSelectMode = false)
        }

        // 3. 更多设置（主题设置、触感震动、通知提醒、GitHub 仓库配置）
        binding.btnMoreSettings.setOnClickListener {
            showMoreSettingsDialog()
        }

        // 4. 数据备份恢复（导出、导入、清空）
        binding.btnBackupRestore.setOnClickListener {
            showBackupRestoreDialog()
        }

        // 5. 意见反馈
        binding.btnFeedback.setOnClickListener {
            showFeedbackDialog()
        }

        // 6. 用户使用条款
        binding.btnTerms.setOnClickListener {
            showDocDialog("用户使用条款", "欢迎使用 Collecter。\n\n1. 本应用为本地离线与 GitHub 开源版本，数据完全存储在您的本地设备中。\n2. 您可以自由管理个人收藏品、日用品、贵重物品及多空间平面图收纳记录。\n3. 请定期使用【数据备份恢复】功能备份您的重要数据。")
        }

        // 7. 隐私政策
        binding.btnPrivacy.setOnClickListener {
            showDocDialog("隐私政策", "Collecter 尊重并严格保护所有用户的个人隐私。\n\n1. 本应用不会在后台收集、上传任何个人隐私敏感数据。\n2. 检查更新功能仅与公开的 GitHub Releases API 通信，用于获取最新版本信息。\n3. 所有空间平面图与资产记录仅保存在本地设备应用沙盒中。")
        }

        // 8. 更新最新版本 (GitHub Releases)
        binding.btnUpdateVersion.setOnClickListener {
            UpdateManager.checkUpdate(requireActivity(), isManual = true)
        }

        // 9. 关于
        binding.btnAbout.setOnClickListener {
            showAboutDialog()
        }
    }

    // =========================================================================
    // ⚙️ 现代化「更多设置」主面板
    // =========================================================================

    private fun showMoreSettingsDialog() {
        val dBinding = DialogMoreSettingsBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dBinding.root)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation

        fun updateThemeText() {
            dBinding.tvCurrentThemeDesc.text = when (store.getThemeMode()) {
                1 -> "☀️ 浅色明亮模式"
                2 -> "🌙 深色暗黑模式"
                else -> "📱 跟随系统偏好"
            }
        }

        fun updateRepoText() {
            val repo = store.getGithubRepo()
            dBinding.tvCurrentRepoDesc.text = "$repo (热更新源)"
        }

        updateThemeText()
        updateRepoText()

        // 1. 外观与主题
        dBinding.cardSettingTheme.applyPressScaleAnimation(0.96f)
        dBinding.cardSettingTheme.setOnClickListener {
            showThemeDialog {
                updateThemeText()
            }
        }

        // 2. 触感震动反馈开关
        dBinding.switchHapticFeedback.isChecked = store.isHapticFeedbackEnabled()
        dBinding.switchHapticFeedback.setOnCheckedChangeListener { _, isChecked ->
            store.setHapticFeedbackEnabled(isChecked)
            if (isChecked) {
                dBinding.root.performAppHapticFeedback()
                Toast.makeText(requireContext(), "已开启触感震动反馈 📳", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "已关闭触感震动反馈", Toast.LENGTH_SHORT).show()
            }
        }

        // 3. 资产与订阅提醒开关与测试
        val isNotifOn = store.isNotificationEnabled()
        dBinding.switchDailyReminder.isChecked = isNotifOn
        dBinding.switchDailyReminder.setOnCheckedChangeListener { _, isChecked ->
            store.setNotificationEnabled(isChecked)
            if (isChecked) {
                NotificationHelper.scheduleDailyReminder(requireContext())
                Toast.makeText(requireContext(), "已开启每日自动提醒 🔔", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "已关闭通知提醒", Toast.LENGTH_SHORT).show()
            }
        }

        dBinding.btnSendTestNotification.applyPressScaleAnimation(0.94f)
        dBinding.btnSendTestNotification.setOnClickListener {
            NotificationHelper.sendTestNotification(requireContext())
            Toast.makeText(requireContext(), "已发送测试通知，请查看通知栏！", Toast.LENGTH_SHORT).show()
        }

        // 4. GitHub 仓库配置
        dBinding.cardSettingRepo.applyPressScaleAnimation(0.96f)
        dBinding.cardSettingRepo.setOnClickListener {
            showRepoEditDialog {
                updateRepoText()
            }
        }

        // 5. 生物识别隐私锁开关
        dBinding.switchBiometricLock.isChecked = store.isBiometricLockEnabled()
        dBinding.switchBiometricLock.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (!BiometricLockHelper.canAuthenticate(requireContext())) {
                    Toast.makeText(requireContext(), "当前设备未设置指纹/锁屏密码或硬件不支持", Toast.LENGTH_LONG).show()
                    dBinding.switchBiometricLock.isChecked = false
                    return@setOnCheckedChangeListener
                }
                // 开启前先验证一次
                BiometricLockHelper.authenticate(
                    activity = requireActivity(),
                    title = "启用生物识别锁",
                    subtitle = "请验证指纹以确认开启",
                    onSuccess = {
                        store.setBiometricLockEnabled(true)
                        dBinding.root.performAppHapticFeedback()
                        Toast.makeText(requireContext(), "已开启生物识别隐私锁 🔐", Toast.LENGTH_SHORT).show()
                    },
                    onError = { err ->
                        dBinding.switchBiometricLock.isChecked = false
                        Toast.makeText(requireContext(), "验证失败: $err", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                store.setBiometricLockEnabled(false)
                Toast.makeText(requireContext(), "已关闭生物识别隐私锁", Toast.LENGTH_SHORT).show()
            }
        }

        // 6. WebDAV 私有云同步
        dBinding.cardSettingWebdav.applyPressScaleAnimation(0.96f)
        dBinding.cardSettingWebdav.setOnClickListener {
            showWebDavDialog()
        }

        // 关闭与完成按钮
        dBinding.btnCloseSettings.applyPressScaleAnimation(0.90f)
        dBinding.btnCloseSettings.setOnClickListener { dialog.dismiss() }

        dBinding.btnDialogDone.applyPressScaleAnimation(0.94f)
        dBinding.btnDialogDone.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    // =========================================================================
    // 🎨 现代化「外观与深浅主题」选择面板
    // =========================================================================

    private fun showThemeDialog(onThemeChanged: (() -> Unit)? = null) {
        val tBinding = DialogThemePickerBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(tBinding.root)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation

        val curMode = store.getThemeMode()
        tBinding.checkThemeAuto.visibility = if (curMode == 0) View.VISIBLE else View.GONE
        tBinding.checkThemeLight.visibility = if (curMode == 1) View.VISIBLE else View.GONE
        tBinding.checkThemeDark.visibility = if (curMode == 2) View.VISIBLE else View.GONE

        fun selectMode(mode: Int) {
            store.setThemeMode(mode)
            onThemeChanged?.invoke()
            dialog.dismiss()
        }

        tBinding.cardThemeAuto.applyPressScaleAnimation(0.96f)
        tBinding.cardThemeLight.applyPressScaleAnimation(0.96f)
        tBinding.cardThemeDark.applyPressScaleAnimation(0.96f)

        tBinding.cardThemeAuto.setOnClickListener { selectMode(0) }
        tBinding.cardThemeLight.setOnClickListener { selectMode(1) }
        tBinding.cardThemeDark.setOnClickListener { selectMode(2) }

        tBinding.btnCancelTheme.applyPressScaleAnimation(0.94f)
        tBinding.btnCancelTheme.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    // =========================================================================
    // 🐙 现代化「GitHub 仓库源配置」面板
    // =========================================================================

    private fun showRepoEditDialog(onRepoSaved: (() -> Unit)? = null) {
        val rBinding = DialogEditRepoBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(rBinding.root)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation

        val currentRepo = store.getGithubRepo()
        rBinding.inputRepo.setText(currentRepo)
        rBinding.inputRepo.setSelection(currentRepo.length)

        rBinding.btnRestoreDefaultRepo.applyPressScaleAnimation(0.94f)
        rBinding.btnRestoreDefaultRepo.setOnClickListener {
            rBinding.inputRepo.setText("Workworks/Collector")
            rBinding.inputRepo.setSelection(rBinding.inputRepo.text.length)
        }

        rBinding.btnCancelRepo.applyPressScaleAnimation(0.94f)
        rBinding.btnCancelRepo.setOnClickListener { dialog.dismiss() }

        rBinding.btnSaveRepo.applyPressScaleAnimation(0.94f)
        rBinding.btnSaveRepo.setOnClickListener {
            val newRepo = rBinding.inputRepo.text.toString().trim()
            if (newRepo.isNotEmpty() && newRepo.contains("/")) {
                store.setGithubRepo(newRepo)
                Toast.makeText(requireContext(), "已更新仓库源: $newRepo", Toast.LENGTH_SHORT).show()
                onRepoSaved?.invoke()
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "仓库格式不正确 (如 Workworks/Collector)", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    // =========================================================================
    // 💾 现代化「数据备份与导出」面板
    // =========================================================================

    private fun showBackupRestoreDialog() {
        val bBinding = DialogBackupManageBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(bBinding.root)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation

        bBinding.btnExportAssetsCsv.applyPressScaleAnimation(0.96f)
        bBinding.btnExportAssetsCsv.setOnClickListener {
            val entries = store.loadAll()
            ExportManager.exportAndShareAssetsCsv(requireActivity(), entries)
        }

        bBinding.btnExportTimelineCsv.applyPressScaleAnimation(0.96f)
        bBinding.btnExportTimelineCsv.setOnClickListener {
            val entries = store.loadAll()
            ExportManager.exportAndShareTimelineCsv(requireActivity(), entries)
        }

        bBinding.btnExportJsonBackup.applyPressScaleAnimation(0.96f)
        bBinding.btnExportJsonBackup.setOnClickListener {
            val json = store.exportBackupJson()
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Collecter Backup", json))
            Toast.makeText(requireContext(), "备份 JSON 数据已复制到剪贴板！", Toast.LENGTH_LONG).show()
        }

        bBinding.btnImportJsonBackup.applyPressScaleAnimation(0.96f)
        bBinding.btnImportJsonBackup.setOnClickListener {
            dialog.dismiss()
            showImportDialog()
        }

        bBinding.btnClearAllData.applyPressScaleAnimation(0.96f)
        bBinding.btnClearAllData.setOnClickListener {
            dialog.dismiss()
            showClearDataDialog()
        }

        bBinding.btnCloseBackupDialog.applyPressScaleAnimation(0.90f)
        bBinding.btnCloseBackupDialog.setOnClickListener { dialog.dismiss() }

        bBinding.btnDoneBackupDialog.applyPressScaleAnimation(0.94f)
        bBinding.btnDoneBackupDialog.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    // =========================================================================
    // 📥 现代化「导入数据备份」面板
    // =========================================================================

    private fun showImportDialog() {
        val iBinding = DialogImportBackupBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(iBinding.root)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation

        iBinding.btnPasteFromClipboard.applyPressScaleAnimation(0.94f)
        iBinding.btnPasteFromClipboard.setOnClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val item = clipboard.primaryClip?.getItemAt(0)
            val pastedText = item?.text?.toString()?.trim()
            if (!pastedText.isNullOrEmpty()) {
                iBinding.inputBackupJson.setText(pastedText)
                Toast.makeText(requireContext(), "已从剪贴板粘贴", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "剪贴板中没有文本内容", Toast.LENGTH_SHORT).show()
            }
        }

        iBinding.btnCancelImport.applyPressScaleAnimation(0.94f)
        iBinding.btnCancelImport.setOnClickListener { dialog.dismiss() }

        iBinding.btnConfirmImport.applyPressScaleAnimation(0.94f)
        iBinding.btnConfirmImport.setOnClickListener {
            val text = iBinding.inputBackupJson.text.toString().trim()
            if (text.isNotEmpty()) {
                val ok = store.importBackupJson(text)
                if (ok) {
                    Toast.makeText(requireContext(), "🎉 数据恢复成功！", Toast.LENGTH_SHORT).show()
                    (activity as? MainActivity)?.refreshCurrentFragment()
                    dialog.dismiss()
                } else {
                    Toast.makeText(requireContext(), "备份解析失败，请确认 JSON 数据格式正确", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "请先输入或粘贴备份 JSON 内容", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun showClearDataDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("⚠️ 警告：清空全部数据")
            .setMessage("确定要彻底清空本地所有物品记录、分类与空间位置历史吗？此操作不可逆！")
            .setPositiveButton("确定清空") { _, _ ->
                store.clearAllData()
                Toast.makeText(requireContext(), "所有记录已彻底清空", Toast.LENGTH_SHORT).show()
                (activity as? MainActivity)?.refreshCurrentFragment()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showFeedbackDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("意见反馈")
            .setMessage("如在使用过程中遇到任何问题或有新功能建议，欢迎前往 GitHub 提交 Issue：\n\nhttps://github.com/${store.getGithubRepo()}/issues")
            .setPositiveButton("访问 GitHub") { _, _ ->
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/${store.getGithubRepo()}/issues"))
                    startActivity(intent)
                } catch (_: Exception) {
                    Toast.makeText(requireContext(), "无法打开浏览器", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun showDocDialog(title: String, content: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(content)
            .setPositiveButton("知道了", null)
            .show()
    }

    // =========================================================================
    // ☁️ 现代化「WebDAV 私有云同步」面板
    // =========================================================================

    private fun showWebDavDialog() {
        val wBinding = com.kfaino.diapertracker.databinding.DialogWebdavConfigBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(wBinding.root)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation

        wBinding.inputWebdavUrl.setText(store.getWebDavUrl())
        wBinding.inputWebdavUser.setText(store.getWebDavUsername())
        wBinding.inputWebdavPass.setText(store.getWebDavPassword())

        wBinding.btnCloseWebdav.applyPressScaleAnimation(0.90f)
        wBinding.btnCloseWebdav.setOnClickListener { dialog.dismiss() }

        // 保存配置
        wBinding.btnSaveWebdavConfig.applyPressScaleAnimation(0.94f)
        wBinding.btnSaveWebdavConfig.setOnClickListener {
            val url = wBinding.inputWebdavUrl.text.toString().trim()
            val user = wBinding.inputWebdavUser.text.toString().trim()
            val pass = wBinding.inputWebdavPass.text.toString()

            store.setWebDavUrl(url)
            store.setWebDavUsername(user)
            store.setWebDavPassword(pass)
            Toast.makeText(requireContext(), "WebDAV 配置已保存！", Toast.LENGTH_SHORT).show()
        }

        // 测试连接
        wBinding.btnTestWebdav.applyPressScaleAnimation(0.94f)
        wBinding.btnTestWebdav.setOnClickListener {
            val url = wBinding.inputWebdavUrl.text.toString().trim()
            val user = wBinding.inputWebdavUser.text.toString().trim()
            val pass = wBinding.inputWebdavPass.text.toString()

            if (url.isBlank() || user.isBlank() || pass.isBlank()) {
                Toast.makeText(requireContext(), "请先完整填写 URL、用户名和密码", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(requireContext(), "正在测试连接 WebDAV 服务器...", Toast.LENGTH_SHORT).show()
            Thread {
                val (_, msg) = WebDavSyncHelper.testConnection(url, user, pass)
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                }
            }.start()
        }

        // 立即上传备份
        wBinding.btnUploadToWebdav.applyPressScaleAnimation(0.94f)
        wBinding.btnUploadToWebdav.setOnClickListener {
            val url = wBinding.inputWebdavUrl.text.toString().trim()
            val user = wBinding.inputWebdavUser.text.toString().trim()
            val pass = wBinding.inputWebdavPass.text.toString()

            if (url.isBlank() || user.isBlank() || pass.isBlank()) {
                Toast.makeText(requireContext(), "请先完整填写 WebDAV 配置", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val json = store.exportBackupJson()
            Toast.makeText(requireContext(), "正在上传备份至 WebDAV...", Toast.LENGTH_SHORT).show()
            Thread {
                val (ok, msg) = WebDavSyncHelper.uploadBackup(url, user, pass, json)
                activity?.runOnUiThread {
                    if (ok) {
                        store.recordBackupDone()
                        Toast.makeText(requireContext(), "🎉 $msg", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "⚠️ $msg", Toast.LENGTH_LONG).show()
                    }
                }
            }.start()
        }

        // 从云端恢复备份
        wBinding.btnDownloadFromWebdav.applyPressScaleAnimation(0.94f)
        wBinding.btnDownloadFromWebdav.setOnClickListener {
            val url = wBinding.inputWebdavUrl.text.toString().trim()
            val user = wBinding.inputWebdavUser.text.toString().trim()
            val pass = wBinding.inputWebdavPass.text.toString()

            if (url.isBlank() || user.isBlank() || pass.isBlank()) {
                Toast.makeText(requireContext(), "请先完整填写 WebDAV 配置", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("📥 从云端恢复数据")
                .setMessage("恢复云端备份将合并/覆盖现有数据，确定继续？")
                .setPositiveButton("立即恢复") { _, _ ->
                    Toast.makeText(requireContext(), "正在从 WebDAV 下载备份...", Toast.LENGTH_SHORT).show()
                    Thread {
                        val (ok, result) = WebDavSyncHelper.downloadBackup(url, user, pass)
                        activity?.runOnUiThread {
                            if (ok) {
                                val importOk = store.importBackupJson(result)
                                if (importOk) {
                                    Toast.makeText(requireContext(), "🎉 云端数据恢复成功！", Toast.LENGTH_SHORT).show()
                                    (activity as? MainActivity)?.refreshCurrentFragment()
                                    dialog.dismiss()
                                } else {
                                    Toast.makeText(requireContext(), "解析云端备份失败，数据可能已损坏", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(requireContext(), "⚠️ $result", Toast.LENGTH_LONG).show()
                            }
                        }
                    }.start()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }

        dialog.show()
    }

    private fun showAboutDialog() {
        val ver = UpdateManager.getAppVersionName(requireContext())
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("关于 Collecter")
            .setMessage("Collecter 智能物品收纳与资产追踪助手\n\n版本：v$ver\n构建版本号：$ver:260819\n开源仓库：https://github.com/${store.getGithubRepo()}\n\n感谢您的使用与支持！")
            .setPositiveButton("确定", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}