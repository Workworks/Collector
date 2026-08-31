package com.kfaino.collector.desktop.storage

import com.kfaino.collecter.core.*
import com.kfaino.collector.desktop.ui.DesktopWorkbench
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class WorkbenchStorageTest {
    @get:Rule val temp=TemporaryFolder()
    @Test fun failedImportRemovesOnlyNewCopiesAndPreservesExistingRecords() {
        val store=DesktopDataStore(temp.newFolder())
        val old=temp.newFile("existing.txt").apply {writeText("existing content")}
        DesktopWorkbench.importFiles(store,listOf(old))
        val before=store.exportJson()
        val folder=File(store.dataDir,"workbench-files")
        val existing=folder.listFiles()!!.map {it.name}.toSet()
        val added=temp.newFile("new.txt").apply {writeText("new content")}
        val missing=File(temp.root,"missing.txt")
        assertThrows(Exception::class.java) {DesktopWorkbench.importFiles(store,listOf(old,added,missing))}
        assertEquals(existing,folder.listFiles()!!.map {it.name}.toSet())
        assertEquals(before,store.exportJson())
        assertTrue(old.isFile);assertTrue(added.isFile)
    }
    @Test fun fileImportAndEncryptedRestorePreserveAttachmentAndWorkflow() {
        val source=temp.newFile("说明书.txt").apply {writeText("原始说明书")}
        val store=DesktopDataStore(temp.newFolder())
        DesktopWorkbench.importFiles(store,listOf(source))
        val row=store.workbenchSnapshot().getJSONArray("inbox").getJSONObject(0)
        assertEquals("原始说明书",File(row.getString("photo")).readText());assertTrue(source.exists())
        store.executeWorkbench(JSONObject().put("op","life").put("refs",org.json.JSONArray(listOf("inbox:"+row.getString("id")))).put("action","maintenance").put("note","检查完成"))
        val password="test-only-encrypted-backup".toCharArray()
        val encrypted=EncryptedBackup.encrypt(store.exportJson(),password)
        val restored=DesktopDataStore(temp.newFolder())
        assertTrue(restored.importJson(EncryptedBackup.decrypt(encrypted,password)))
        val recovered=restored.workbenchSnapshot().getJSONArray("inbox").getJSONObject(0)
        assertEquals("原始说明书",File(recovered.getString("photo")).readText())
        assertEquals("检查完成",recovered.getJSONArray("_lifeEvents").getJSONObject(0).getString("note"))
        val before=restored.exportJson()
        assertThrows(Exception::class.java){restored.importJson(EncryptedBackup.decrypt(encrypted,"wrong-password-0000".toCharArray()))}
        assertEquals(before,restored.exportJson())
    }
}
