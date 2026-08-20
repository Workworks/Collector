package com.kfaino.diapertracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 🔐 补丁解压安全测试。
 *
 * 覆盖 GEMINI.md 铁律 3 的第 3 条安全不变量：解压必须防路径穿越（Zip Slip）与 zip 炸弹。
 * 这些用例是回归护栏 —— 如果日后有人（人或 AI）把校验删掉「让它先跑起来」，这里会立刻变红。
 */
class PatchArchiveTest {

    @get:Rule
    val tmp = TemporaryFolder()

    /** 构造一个 zip，条目名可以是任意字符串（包括恶意的 ../ 路径） */
    private fun buildZip(vararg entries: Pair<String, String>): File {
        val zipFile = tmp.newFile("patch-${System.nanoTime()}.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            for ((name, content) in entries) {
                zos.putNextEntry(ZipEntry(name))
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
        }
        return zipFile
    }

    @Test
    fun `正常补丁包可以正确解压`() {
        val zip = buildZip(
            "manifest.json" to """{"patchVersion":"4.3.0"}""",
            "web/index.html" to "<html>dashboard</html>"
        )
        val target = tmp.newFolder("active")

        PatchArchive.safeUnzip(zip, target)

        assertTrue("manifest.json 应被解压", File(target, "manifest.json").exists())
        assertEquals(
            "<html>dashboard</html>",
            File(target, "web/index.html").readText(Charsets.UTF_8)
        )
    }

    @Test
    fun `含 dot dot 路径穿越的条目必须被拒绝`() {
        val zip = buildZip("../../evil.txt" to "pwned")
        val target = tmp.newFolder("active")

        try {
            PatchArchive.safeUnzip(zip, target)
            fail("路径穿越条目必须抛出 SecurityException，但解压竟然成功了")
        } catch (e: SecurityException) {
            assertTrue(
                "异常信息应指明路径穿越，实际为：${e.message}",
                e.message.orEmpty().contains("路径穿越")
            )
        }
    }

    @Test
    fun `路径穿越条目不得在沙盒目录之外落地任何文件`() {
        // target 的父目录代表 App 私有数据区，穿越成功就意味着能覆盖它
        val parent = tmp.newFolder("sandbox_parent")
        val target = File(parent, "active").apply { mkdirs() }
        val escaped = File(parent, "escaped.txt")

        val zip = buildZip("../escaped.txt" to "pwned")

        try {
            PatchArchive.safeUnzip(zip, target)
            fail("路径穿越条目必须抛出 SecurityException")
        } catch (e: SecurityException) {
            // 期望路径
        }

        assertFalse("沙盒外不得出现任何文件，实际被写入了 ${escaped.absolutePath}", escaped.exists())
    }

    @Test
    fun `绝对路径条目同样必须被拒绝`() {
        // 某些打包器会写出以 / 开头的条目名
        val zip = buildZip("/tmp/evil-absolute.txt" to "pwned")
        val target = tmp.newFolder("active")

        try {
            PatchArchive.safeUnzip(zip, target)
            // File(target, "/tmp/x") 在多数平台会被解析为 target/tmp/x，属于安全落点；
            // 若平台把它解析到 target 之外，则必须抛异常。两种结果都可接受，
            // 唯一不可接受的是「落到了 target 之外却没抛异常」。
            val landed = File(target, "tmp/evil-absolute.txt")
            assertTrue("绝对路径条目要么被拒绝，要么必须落在沙盒内", landed.exists())
        } catch (e: SecurityException) {
            // 也是可接受的结果
        }
    }

    @Test
    fun `条目数量超过上限时必须拒绝解压`() {
        val entries = (0..PatchArchive.MAX_ENTRY_COUNT + 1)
            .map { "file_$it.txt" to "x" }
            .toTypedArray()
        val zip = buildZip(*entries)
        val target = tmp.newFolder("active")

        try {
            PatchArchive.safeUnzip(zip, target)
            fail("条目数超限必须抛出 SecurityException")
        } catch (e: SecurityException) {
            assertTrue(
                "异常信息应指明条目数超限，实际为：${e.message}",
                e.message.orEmpty().contains("条目数")
            )
        }
    }
}
