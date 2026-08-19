package com.kfaino.diapertracker

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kfaino.diapertracker.databinding.DialogPhotoPreviewBinding
import java.io.File

object PhotoPreviewDialog {

    fun show(activity: Activity, title: String, filename: String, subtitle: String? = null) {
        if (filename.isBlank()) return

        val binding = DialogPhotoPreviewBinding.inflate(LayoutInflater.from(activity))
        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(binding.root)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation

        binding.tvPreviewTitle.text = title
        if (!subtitle.isNullOrBlank()) {
            binding.tvPreviewInfo.text = subtitle
        }

        // 加载高清大图 (1200x1200采样)
        val bitmap = ImageVaultHelper.loadSampledBitmap(activity, filename, 1200, 1200)
        if (bitmap != null) {
            binding.ivPreviewImage.setImageBitmap(bitmap)
        } else {
            Toast.makeText(activity, "未找到图片文件", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnClosePreview.applyPressScaleAnimation(0.90f)
        binding.btnClosePreview.setOnClickListener { dialog.dismiss() }

        binding.btnSharePhoto.applyPressScaleAnimation(0.92f)
        binding.btnSharePhoto.setOnClickListener {
            val file = ImageVaultHelper.getVaultFile(activity, filename)
            if (file != null) {
                ExportManager.shareFile(activity, file, title = title, mimeType = "image/jpeg")
            } else {
                Toast.makeText(activity, "图片不存在", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }
}
