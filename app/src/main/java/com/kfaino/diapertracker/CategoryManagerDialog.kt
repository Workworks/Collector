package com.kfaino.diapertracker

import android.content.Context
import android.view.LayoutInflater
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kfaino.diapertracker.databinding.DialogInputCategoryBinding
import com.kfaino.diapertracker.databinding.DialogManageCategoriesBinding

object CategoryManagerDialog {

    /** 弹出新增分类对话框 */
    fun showAddCategoryDialog(
        context: Context,
        store: DataStore,
        onAdded: (String) -> Unit
    ) {
        val binding = DialogInputCategoryBinding.inflate(LayoutInflater.from(context))
        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle("新增分类")
            .setView(binding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.confirm) { _, _ ->
                val name = binding.categoryInput.text?.toString()?.trim().orEmpty()
                if (name.isEmpty()) {
                    Toast.makeText(context, "分类名称不能为空", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val success = store.addCategory(name)
                if (success) {
                    Toast.makeText(context, "已成功添加分类: $name", Toast.LENGTH_SHORT).show()
                    onAdded(name)
                } else {
                    Toast.makeText(context, "该分类已存在", Toast.LENGTH_SHORT).show()
                }
            }
            .create()

        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation
        dialog.show()
    }

    /** 弹出分类管理及排序对话框 */
    fun showManageDialog(
        context: Context,
        store: DataStore,
        onUpdated: () -> Unit
    ) {
        val binding = DialogManageCategoriesBinding.inflate(LayoutInflater.from(context))
        var categories = store.getCategories().toMutableList()

        lateinit var adapter: CategoryManageAdapter
        adapter = CategoryManageAdapter(
            dataStore = store,
            onMoveUp = { pos ->
                if (pos > 0) {
                    val item = categories.removeAt(pos)
                    categories.add(pos - 1, item)
                    store.saveCategories(categories)
                    adapter.submit(categories.toList())
                    onUpdated()
                }
            },
            onMoveDown = { pos ->
                if (pos < categories.size - 1) {
                    val item = categories.removeAt(pos)
                    categories.add(pos + 1, item)
                    store.saveCategories(categories)
                    adapter.submit(categories.toList())
                    onUpdated()
                }
            },
            onDelete = { pos, cat ->
                val hasEntries = store.loadAll().any { it.category == cat }
                val msg = if (hasEntries) {
                    "当前已有记录使用分类【$cat】，删除分类后已有记录仍会保留，确定要移除该分类吗？"
                } else {
                    "确定要删除分类【$cat】吗？"
                }

                val delDialog = MaterialAlertDialogBuilder(context)
                    .setTitle("删除分类")
                    .setMessage(msg)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton("删除") { _, _ ->
                        categories.removeAt(pos)
                        store.saveCategories(categories)
                        adapter.submit(categories.toList())
                        Toast.makeText(context, "已删除分类: $cat", Toast.LENGTH_SHORT).show()
                        onUpdated()
                    }
                    .create()
                delDialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation
                delDialog.show()
            }
        )

        binding.categoryRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.categoryRecyclerView.adapter = adapter
        adapter.submit(categories.toList())

        binding.btnAddCategory.applyPressScaleAnimation(0.94f)
        binding.btnResetPreset.applyPressScaleAnimation(0.94f)

        // 新增分类
        binding.btnAddCategory.setOnClickListener {
            showAddCategoryDialog(context, store) { _ ->
                categories = store.getCategories().toMutableList()
                adapter.submit(categories.toList())
                onUpdated()
            }
        }

        // 恢复默认
        binding.btnResetPreset.setOnClickListener {
            val resetDialog = MaterialAlertDialogBuilder(context)
                .setTitle("恢复默认推荐")
                .setMessage("确定要恢复默认推荐分类列表吗？")
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.confirm) { _, _ ->
                    categories = store.resetCategories().toMutableList()
                    adapter.submit(categories.toList())
                    Toast.makeText(context, "已恢复推荐分类", Toast.LENGTH_SHORT).show()
                    onUpdated()
                }
                .create()
            resetDialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation
            resetDialog.show()
        }

        val manageDialog = MaterialAlertDialogBuilder(context)
            .setTitle("分类管理")
            .setView(binding.root)
            .setPositiveButton("完成", null)
            .create()
        manageDialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation
        manageDialog.show()
    }
}
