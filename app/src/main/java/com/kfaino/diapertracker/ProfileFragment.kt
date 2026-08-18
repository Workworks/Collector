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

        binding.aboutVersionCode.text = "2.2.0:260818"

        setupClicks()
    }

    private fun setupClicks() {
        // 1. 分类管理
        binding.btnCategoryManage.setOnClickListener {
            CategoryManagerDialog.showManageDialog(requireContext(), store) {
                Toast.makeText(requireContext(), "分类设置已更新", Toast.LENGTH_SHORT).show()
            }
        }

        // 2. 更多设置（主题设置、GitHub 仓库配置）
        binding.btnMoreSettings.setOnClickListener {
            showMoreSettingsDialog()
        }

        // 3. 数据备份恢复（导出、导入、清空）
        binding.btnBackupRestore.setOnClickListener {
            showBackupRestoreDialog()
        }

        // 4. 意见反馈
        binding.btnFeedback.setOnClickListener {
            showFeedbackDialog()
        }

        // 5. 用户使用条款
        binding.btnTerms.setOnClickListener {
            showDocDialog("用户使用条款", "欢迎使用 Collecter。\n\n1. 本应用为本地离线与 GitHub 开源版本，数据完全存储在您的本地设备中。\n2. 您可以自由管理个人收藏品、日用品及资产收纳记录。\n3. 请定期使用【数据备份恢复】功能备份您的重要数据。")
        }

        // 6. 隐私政策
        binding.btnPrivacy.setOnClickListener {
            showDocDialog("隐私政策", "Collecter 尊重并严格保护所有用户的个人隐私。\n\n1. 本应用不会在后台收集、上传任何个人隐私敏感数据。\n2. 检查更新功能仅与公开的 GitHub Releases API 通信，用于获取最新版本信息。\n3. 所有出入库与资产记录仅保存在本地设备应用沙盒中。")
        }

        // 7. 更新最新版本 (GitHub Releases)
        binding.btnUpdateVersion.setOnClickListener {
            UpdateManager.checkUpdate(requireActivity(), isManual = true)
        }

        // 8. 关于
        binding.btnAbout.setOnClickListener {
            showAboutDialog()
        }
    }

    /** 更多设置对话框：深浅色主题 & GitHub 仓库设置 */
    private fun showMoreSettingsDialog() {
        val options = arrayOf("外观与深浅主题", "GitHub 仓库设置 (热更新源)")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("更多设置")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showThemeDialog()
                    1 -> showRepoEditDialog()
                }
            }
            .setNegativeButton(R.string.cancel, null)
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
        val input = EditText(requireContext()).apply {
            setText(store.getGithubRepo())
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
            "📤 导出数据备份 (复制 JSON)",
            "📥 导入数据备份 (粘贴 JSON)",
            "🗑️ 清空所有记录"
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("数据备份恢复")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val json = store.exportBackupJson()
                        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Collecter Backup", json))
                        Toast.makeText(requireContext(), "备份数据已复制到剪贴板！", Toast.LENGTH_LONG).show()
                    }
                    1 -> {
                        showImportDialog()
                    }
                    2 -> {
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
            .setMessage("导入将合并备份中的分类与记录，请谨慎操作：")
            .setView(input)
            .setPositiveButton("导入恢复") { _, _ ->
                val text = input.text.toString().trim()
                if (text.isEmpty()) {
                    Toast.makeText(requireContext(), "内容不能为空", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val success = store.importBackupJson(text)
                if (success) {
                    Toast.makeText(requireContext(), "数据恢复成功！", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "JSON 数据格式不正确，恢复失败", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showClearDataDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("清空所有记录")
            .setMessage("此操作将清空所有出入库流水记录，分类设置将被保留。此操作不可逆，确定继续吗？")
            .setPositiveButton("确定清空") { _, _ ->
                store.clearAllData()
                Toast.makeText(requireContext(), "记录已全部清空", Toast.LENGTH_SHORT).show()
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
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("关于 Collecter")
            .setMessage("Collecter 智能物品收纳与资产追踪助手\n\n版本：v2.2 (Build 3)\n构建版本号：2.2.0:260818\n开源仓库：https://github.com/${store.getGithubRepo()}\n\n感谢您的使用与支持！")
            .setPositiveButton("确定", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}