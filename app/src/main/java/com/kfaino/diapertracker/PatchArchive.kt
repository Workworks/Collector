package com.kfaino.diapertracker

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * 🔐 补丁压缩包安全解压器 (Patch Archive)
 *
 * 独立于 Android 框架的纯 JVM 实现，因此可以被单元测试直接覆盖
 * （见 `app/src/test/java/com/kfaino/diapertracker/PatchArchiveTest.kt`）。
 *
 * ## 安全不变量（见 GEMINI.md 铁律 3，修改前必须先问用户）
 * 1. **Zip Slip 防护**：条目名形如 `../../databases/x` 会逃逸出目标沙盒目录，
 *    覆盖 App 私有数据甚至可执行文件。每个条目的规范化路径必须仍位于目标目录内。
 * 2. **zip 炸弹防护**：限制条目数量与解压后总字节数，避免一个几 KB 的补丁包撑爆存储。
 *
 * 任一校验不通过都会抛出 [SecurityException]，由调用方负责回滚。
 */
object PatchArchive {

    /** 单个补丁包允许的最大条目数 */
    const val MAX_ENTRY_COUNT = 512

    /** 单个补丁包允许的最大解压后总体积（64 MB） */
    const val MAX_TOTAL_UNCOMPRESSED_BYTES = 64L * 1024 * 1024

    /**
     * 安全解压 [zipFile] 到 [targetDir]。
     *
     * @throws SecurityException 检测到路径穿越、条目数超限或体积超限时抛出。
     */
    fun safeUnzip(zipFile: File, targetDir: File) {
        if (!targetDir.exists()) targetDir.mkdirs()

        val canonicalTargetPath = targetDir.canonicalPath
        val targetPrefix = canonicalTargetPath + File.separator

        var entryCount = 0
        var totalBytes = 0L

        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry: ZipEntry?
            while (zis.nextEntry.also { entry = it } != null) {
                val current = entry!!

                if (++entryCount > MAX_ENTRY_COUNT) {
                    throw SecurityException("补丁包条目数超过上限（$MAX_ENTRY_COUNT），拒绝解压")
                }

                val outFile = File(targetDir, current.name)
                val outPath = outFile.canonicalPath
                if (outPath != canonicalTargetPath && !outPath.startsWith(targetPrefix)) {
                    throw SecurityException("检测到非法补丁条目（路径穿越）：" + current.name)
                }

                if (current.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        while (zis.read(buffer).also { read = it } != -1) {
                            totalBytes += read
                            if (totalBytes > MAX_TOTAL_UNCOMPRESSED_BYTES) {
                                throw SecurityException("补丁包解压体积超过上限，疑似 zip 炸弹，拒绝解压")
                            }
                            fos.write(buffer, 0, read)
                        }
                    }
                }
                zis.closeEntry()
            }
        }
    }
}
