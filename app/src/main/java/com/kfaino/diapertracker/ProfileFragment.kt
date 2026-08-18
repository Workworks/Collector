package com.kfaino.diapertracker

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

    companion object {
        val THEME_OPTIONS = arrayOf("跟随系统", "浅色模式", "深色模式")
    }

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

        // 分类管理入口
        binding.btnCategorySettings.setOnClickListener {
            CategoryManagerDialog.showManageDialog(requireContext(), store) {
                refresh()
            }
        }

        // 主题模式设置入口
        binding.btnThemeSettings.setOnClickListener {
            showThemeDialog()
        }

        // 检查版本更新（GitHub Release）
        binding.btnCheckUpdate.setOnClickListener {
            UpdateManager.checkUpdate(requireActivity(), isManual = true)
        }

        // 长按版本更新项：可快速修改或查看 GitHub 仓库地址
        binding.btnCheckUpdate.setOnLongClickListener {
            showEditRepoDialog()
            true
        }

        // 清空数据
        binding.clearData.setOnClickListener {
            confirmClear()
        }

        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        if (_binding == null) return

        val ver = UpdateManager.getAppVersionName(requireContext())
        binding.versionLabel.text = "Collecter v$ver"

        val entries = store.loadAll()
        binding.dataSummary.text = "共 ${entries.size} 条记录"

        val categories = store.getCategories()
        binding.categorySummary.text = "已配置 ${categories.size} 个分组，点击调整排序"

        val curTheme = store.getThemeMode()
        binding.themeModeLabel.text = when (curTheme) {
            DataStore.THEME_LIGHT -> "浅色模式"
            DataStore.THEME_DARK -> "深色模式"
            else -> "跟随系统"
        }

        val repo = store.getGithubRepo()
        binding.updateSummary.text = "当前版本 v$ver · 仓库: $repo"
    }

    private fun showThemeDialog() {
        val curMode = store.getThemeMode()
        val checkedItem = curMode.coerceIn(0, 2)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("外观与主题模式")
            .setSingleChoiceItems(THEME_OPTIONS, checkedItem) { dialog, which ->
                store.setThemeMode(which)
                refresh()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showEditRepoDialog() {
        val input = EditText(requireContext()).apply {
            setText(store.getGithubRepo())
            setSingleLine(true)
            setPadding(48, 24, 48, 24)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("配置 GitHub 更新仓库")
            .setMessage("请输入用于检测更新的 GitHub 仓库（格式：用户名/仓库名，如 kfaino/DiaperTracker）：")
            .setView(input)
            .setPositiveButton("保存") { _, _ ->
                val newRepo = input.text.toString().trim()
                if (newRepo.isNotEmpty() && newRepo.contains("/")) {
                    store.setGithubRepo(newRepo)
                    refresh()
                    Toast.makeText(requireContext(), "已更新仓库为：$newRepo", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "格式不正确，应为 owner/repo", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun confirmClear() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.clear_confirm_title)
            .setMessage(R.string.clear_confirm_msg)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete_confirm) { _, _ ->
                store.saveAll(emptyList())
                refresh()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}