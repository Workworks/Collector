package com.kfaino.diapertracker

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.kfaino.collecter.core.BackupDocument
import com.kfaino.collecter.core.WorkspaceRecords
import java.io.File

/** Receives only explicitly shared content; no broad photo-library or clipboard access. */
class CollectShareActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) { finish(); return }
        val received = intent
        Thread {
            val message = try {
                require(received.action == Intent.ACTION_SEND) { "不支持此分享方式" }
                val text = received.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString().orEmpty()
                @Suppress("DEPRECATION")
                val uri = received.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                var photo = ""
                if (uri != null) {
                    require(uri.scheme == "content" && received.type?.startsWith("image/") == true) { "仅支持授权的图片分享" }
                    val bytes = contentResolver.openInputStream(uri)!!.use { input ->
                        val output = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (true) {
                            val n = input.read(buffer)
                            if (n < 0) break
                            require(output.size().toLong() + n <= BackupDocument.MAX_ASSET_BYTES) { "图片超过 16 MiB" }
                            output.write(buffer, 0, n)
                        }
                        output.toByteArray()
                    }
                    val file = File(filesDir, "inbox-media/${BackupDocument.sha256(bytes)}")
                    BackupDocument.atomicWrite(file, bytes)
                    photo = file.absolutePath
                }
                CollectionWorkspace(applicationContext).upsert("inbox", WorkspaceRecords.inbox(text, photo))
                "已保存到 Collecter 收集箱；原件已保留，可稍后整理"
            } catch (e: Exception) {
                android.util.Log.e("CollectShare", "保存分享失败", e)
                "未保存：${e.message}。请保留原内容后重试。"
            }
            runOnUiThread { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); finish() }
        }.start()
    }
}
