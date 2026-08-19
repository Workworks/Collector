package com.kfaino.diapertracker

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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

        // 3. 更多设置（主题设置、GitHub 仓库配置）
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

    /** 更多设置对话框：深浅色主题 & 通知提醒设置 & GitHub 仓库设置 */
    private fun showMoreSettingsDialog() {
        val options = arrayOf(
            "外观与深浅主题",
            "🔔 资产与订阅提醒设置",
            "GitHub 仓库设置 (热更新源)"
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("更多设置")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showThemeDialog()
                    1 -> showReminderSettingsDialog()
                    2 -> showRepoEditDialog()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showReminderSettingsDialog() {
        val isEnabled = store.isNotificationEnabled()
        val options = arrayOf(
            if (isEnabled) "🔔 提醒功能：【已开启】(点击关闭)" else "🔕 提醒功能：【已关闭】(点击开启)",
            "🚀 立即发送一条测试通知"
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("🔔 资产与订阅提醒设置")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val newState = !isEnabled
                        store.setNotificationEnabled(newState)
                        if (newState) {
                            NotificationHelper.scheduleDailyReminder(requireContext())
                            Toast.makeText(requireContext(), "已开启每日自动提醒", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "已关闭通知提醒", Toast.LENGTH_SHORT).show()
                        }
                    }
                    1 -> {
                        NotificationHelper.sendTestNotification(requireContext())
                        Toast.makeText(requireContext(), "已发送测试通知，请查看手机通知栏！", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun showThemeDialog() {
        val themes = arrayOf("跟随系统", "浅色模式", "深色模式")
        val current = store.getThemeMode()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("外观与深浅主题")
            .setSingleChoiceItems(themes, current) { dialog, which ->
                store.setThemeMode(which)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showRepoEditDialog() {
        val currentRepo = store.getGithubRepo()
        val input = EditText(requireContext()).apply {
            setText(currentRepo)
            setSelection(text.length)
            setPadding(48, 36, 48, 36)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("配置 GitHub 仓库")
            .setMessage("请输入 GitHub 仓库地址 (格式: owner/repo):")
            .setView(input)
            .setPositiveButton(R.string.confirm) { _, _ ->
                val newRepo = input.text.toString().trim()
                if (newRepo.isNotEmpty() && newRepo.contains("/")) {
                    store.setGithubRepo(newRepo)
                    Toast.makeText(requireContext(), "已更新仓库: $newRepo", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "仓库格式不正确 (如 Workworks/Collector)", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** 数据备份恢复对话框 */
    private fun showBackupRestoreDialog() {
        val options = arrayOf(
            "📊 导出【资产全景总表】(CSV / Excel 兼容)",
            "📋 导出【收支流水明细】(CSV / Excel 兼容)",
            "📤 导出数据备份 (复制 JSON)",
            "📥 导入数据备份 (粘贴 JSON)",
            "🗑️ 清空所有记录"
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("数据备份与导出")
            .setItems(options) { _, which ->
                val entries = store.loadAll()
                when (which) {
                    0 -> {
                        ExportManager.exportAndShareAssetsCsv(requireActivity(), entries)
                    }
                    1 -> {
                        ExportManager.exportAndShareTimelineCsv(requireActivity(), entries)
                    }
                    2 -> {
                        val json = store.exportBackupJson()
                        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Collecter Backup", json))
                        Toast.makeText(requireContext(), "备份数据已复制到剪贴板！", Toast.LENGTH_LONG).show()
                    }
                    3 -> {
                        showImportDialog()
                    }
                    4 -> {
                        showClearDataDialog()
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showImportDialog() {
        val input = EditText(requireContext()).apply {
            hint = "在此粘贴导出的备份 JSON 内容"
            setPadding(48, 36, 48, 36)
            maxLines = 6
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("导入数据备份")
            .setMessage("导入将合并备份中的分类、空间与记录，请谨慎操作：")
            .setView(input)
            .setPositiveButton("导入恢复") { _, _ ->
                val text = input.text.toString().trim()
                if (text.isNotEmpty()) {
                    val ok = store.importBackupJson(text)
                    if (ok) {
                        Toast.makeText(requireContext(), "数据恢复成功！", Toast.LENGTH_SHORT).show()
                        (activity as? MainActivity)?.refreshCurrentFragment()
                    } else {
                        Toast.makeText(requireContext(), "备份解析失败，请检查格式", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showClearDataDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("警告：清空全部数据")
            .setMessage("确定要清空所有物品记录与位置历史吗？此操作不可逆！")
            .setPositiveButton("确定清空") { _, _ ->
                store.clearAllData()
                Toast.makeText(requireContext(), "记录已全部清空", Toast.LENGTH_SHORT).show()
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

    private fun showAboutDialog() {
        val ver = UpdateManager.getAppVersionName(requireContext())
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("关于 Collecter")
            .setMessage("Collecter 智能物品收纳与资产追踪助手\n\n版本：v$ver\n构建版本号：$ver:260818\n开源仓库：https://github.com/${store.getGithubRepo()}\n\n感谢您的使用与支持！")
            .setPositiveButton("确定", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}