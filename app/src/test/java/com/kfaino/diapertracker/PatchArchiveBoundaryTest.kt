package com.kfaino.diapertracker

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class PatchArchiveBoundaryTest {
    @get:Rule val temp = TemporaryFolder()
    @Test fun crossPlatformTraversalRejectedBeforeWriting() {
        listOf("../outside", "..\\outside", "/absolute", "C:/outside", "file:stream", "../active-evil/file").forEachIndexed { i, name ->
            val zip = temp.newFile("$i.zip")
            ZipOutputStream(zip.outputStream()).use { it.putNextEntry(ZipEntry(name)); it.write(byteArrayOf(1)); it.closeEntry() }
            val target = temp.newFolder("active$i")
            assertThrows(SecurityException::class.java) { PatchArchive.safeUnzip(zip, target) }
            assertTrue(target.listFiles()!!.isEmpty())
        }
        assertFalse(File(temp.root, "outside").exists())
    }
    @Test fun inflatedArchiveRejectedAtLimit() {
        val zip = temp.newFile("large.zip")
        ZipOutputStream(zip.outputStream()).use { out ->
            out.putNextEntry(ZipEntry("large.bin"))
            val block = ByteArray(1024 * 1024)
            repeat(65) { out.write(block) }
            out.closeEntry()
        }
        assertThrows(SecurityException::class.java) { PatchArchive.safeUnzip(zip, temp.newFolder("large")) }
    }
}
