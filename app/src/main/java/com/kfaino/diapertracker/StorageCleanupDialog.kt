package com.kfaino.diapertracker

import android.app.Activity
import android.app.ProgressDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.io.FileOutputStream

/**
 * 🧹 图片沙盒清理与批量重压缩工具
 * - 扫描 item_vault/ 目录，识别不被任何资产引用的孤立图片
 * - 提供一键清理孤立文件（释放空间）
 * - 提供批量重压缩（JPEG 质量 75，低于存储时的 85，节省约 15-20% 空间）
 */
object StorageCleanupDialog {

    private const val TAG = "StorageCleanupDialog"
    private const val RECOMPRESS_QUALITY = 75  // 再压缩质量（低于原始 85），节省约 15-20%

    fun show(activity: Activity, store: DataStore) {
        try {
            val vaultDir = ImageVaultHelper.getVaultDir(activity)
            val allFiles = vaultDir.listFiles { f -> f.name.endsWith(".jpg") }?.toList() ?: emptyList()

            // 收集所有被资产引用的图片文件名
            val referencedFilenames = mutableSetOf<String>()
            try {
                store.loadAll().forEach { e ->
                    if (e.photoPath.isNotBlank()) referencedFilenames.add(e.photoPath)
                    if (e.receiptPath.isNotBlank()) referencedFilenames.add(e.receiptPath)
                }
            } catch (e: Exception) {
                android.util.Log.w(TAG, "扫描资产引用图片失败", e)
            }

            val orphanFiles = allFiles.filter { it.name !in referencedFilenames }
            val totalSizeKb = allFiles.sumOf { it.length() } / 1024
            val orphanSizeKb = orphanFiles.sumOf { it.length() } / 1024

            val message = buildString {
                appendLine("📂 图片沙盒总览")
                appendLine("• 总文件数：${allFiles.size} 个")
                appendLine("• 总占用空间：${totalSizeKb} KB（约 ${String.format("%.1f", totalSizeKb / 1024.0)} MB）")
                appendLine()
                appendLine("🔍 孤立图片（未被任何资产引用）：")
                appendLine("• 孤立文件数：${orphanFiles.size} 个")
                append("• 孤立占用空间：${orphanSizeKb} KB")
            }

            MaterialAlertDialogBuilder(activity)
                .setTitle("🧹 存储空间管理")
                .setMessage(message)
                .setNeutralButton("关闭") { d, _ -> d.dismiss() }
                .setNegativeButton("🗑️ 清理孤立图片（${orphanFiles.size} 个）") { d, _ ->
                    d.dismiss()
                    cleanOrphanFiles(activity, orphanFiles, orphanSizeKb)
                }
                .setPositiveButton("🔄 批量重压缩（节省空间）") { d, _ ->
                    d.dismiss()
                    recompressAllImages(activity, allFiles)
                }
                .show()
        } catch (e: Exception) {
            android.util.Log.w(TAG, "打开存储管理对话框失败", e)
            Toast.makeText(activity, "扫描失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cleanOrphanFiles(activity: Activity, orphanFiles: List<File>, orphanSizeKb: Long) {
        if (orphanFiles.isEmpty()) {
            Toast.makeText(activity, "✅ 没有孤立图片，沙盒干净！", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            var deleted = 0
            orphanFiles.forEach { file ->
                try {
                    if (file.delete()) deleted++
                } catch (e: Exception) {
                    android.util.Log.w(TAG, "删除孤立图片失败: ${file.name}", e)
                }
            }
            Toast.makeText(
                activity,
                "✅ 已清理 $deleted 个孤立图片，释放约 ${orphanSizeKb} KB",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            android.util.Log.w(TAG, "批量清理孤立图片失败", e)
            Toast.makeText(activity, "清理失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun recompressAllImages(activity: Activity, allFiles: List<File>) {
        if (allFiles.isEmpty()) {
            Toast.makeText(activity, "沙盒中暂无图片", Toast.LENGTH_SHORT).show()
            return
        }

        @Suppress("DEPRECATION")
        val progress = ProgressDialog(activity).apply {
            setTitle("批量重压缩中...")
            setMessage("正在处理 0 / ${allFiles.size}")
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            max = allFiles.size
            isIndeterminate = false
            setCancelable(false)
            show()
        }

        Thread {
            var savedKb = 0L
            var processed = 0
            var failed = 0

            allFiles.forEachIndexed { idx, file ->
                try {
                    val originalSize = file.length()
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    if (bitmap != null) {
                        val tmpFile = File(file.parent, "${file.nameWithoutExtension}_tmp.jpg")
                        FileOutputStream(tmpFile).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, RECOMPRESS_QUALITY, out)
                            out.flush()
                        }
                        bitmap.recycle()
                        val newSize = tmpFile.length()
                        if (newSize < originalSize) {
                            file.delete()
                            tmpFile.renameTo(file)
                            savedKb += (originalSize - newSize) / 1024
                        } else {
                            // 原图更小时不替换
                            tmpFile.delete()
                        }
                        processed++
                    } else {
                        android.util.Log.w(TAG, "解码图片失败，跳过: ${file.name}")
                        failed++
                    }
                } catch (e: Exception) {
                    android.util.Log.w(TAG, "重压缩图片失败: ${file.name}", e)
                    failed++
                }

                activity.runOnUiThread {
                    progress.progress = idx + 1
                    progress.setMessage("正在处理 ${idx + 1} / ${allFiles.size}")
                }
            }

            activity.runOnUiThread {
                progress.dismiss()
                val msg = buildString {
                    append("✅ 重压缩完成：处理 $processed 张")
                    if (failed > 0) append("，失败 $failed 张")
                    append("，共节省约 ${savedKb} KB")
                }
                Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()
            }
        }.start()
    }
}
