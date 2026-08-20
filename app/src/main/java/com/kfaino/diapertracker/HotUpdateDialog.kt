package com.kfaino.diapertracker

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kfaino.diapertracker.databinding.DialogHotUpdateBinding
import java.util.Locale

/**
 * 类游戏沉浸式热更新弹窗
 */
object HotUpdateDialog {

    fun show(activity: Activity, patchInfo: HotUpdateManager.HotPatchInfo) {
        val binding = DialogHotUpdateBinding.inflate(LayoutInflater.from(activity))
        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(binding.root)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation

        val baseVer = UpdateManager.getAppVersionName(activity)
        binding.tvVersionFlow.text = "v$baseVer ➔ ⚡ v${patchInfo.patchVersion} 增量补丁"

        val sizeKb = patchInfo.sizeBytes / 1024
        val sizeStr = if (sizeKb > 1024) String.format(Locale.getDefault(), "%.1f MB", sizeKb / 1024f) else "$sizeKb KB"
        binding.tvPatchSize.text = "📦 增量体积: $sizeStr"
        binding.tvPatchChangelog.text = patchInfo.changelog.ifBlank { "• 关键功能热修复与性能流畅度增强\n• 修复已知问题并提升稳定性" }

        binding.btnPatchCancel.setOnClickListener {
            dialog.dismiss()
        }

        binding.btnPatchApply.setOnClickListener {
            binding.btnPatchApply.isEnabled = false
            binding.btnPatchCancel.isEnabled = false
            binding.layoutPatchProgress.visibility = View.VISIBLE
            binding.tvPatchStatus.text = "⚡ 正在极速下载增量补丁..."

            HotUpdateManager.downloadAndApplyPatch(activity, patchInfo, onProgress = { percent, speedKb ->
                binding.pbPatchDownload.progress = percent
                binding.tvPatchSpeed.text = "$percent% · ${speedKb} KB/s"
            }, onCompleted = { success, errorMsg ->
                if (success) {
                    binding.tvPatchStatus.text = "🎉 补丁应用成功！即刻无感生效"
                    Toast.makeText(activity, "⚡ 热更新已完成！最新特性已就绪", Toast.LENGTH_LONG).show()
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        dialog.dismiss()
                    }, 1200)
                } else {
                    binding.btnPatchApply.isEnabled = true
                    binding.btnPatchCancel.isEnabled = true
                    binding.tvPatchStatus.text = "❌ 更新失败: $errorMsg"
                    Toast.makeText(activity, "热更新失败: $errorMsg", Toast.LENGTH_SHORT).show()
                }
            })
        }

        dialog.show()
    }
}
