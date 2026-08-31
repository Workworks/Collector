package com.kfaino.diapertracker

import android.content.Context
import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import com.kfaino.collecter.core.EncryptedBackup
import org.json.JSONObject
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

class WorkbenchDeviceTest {
    @Test fun desktopEncryptedFixtureRoundTripsThroughAndroid() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val password="public-qa-password-455".toCharArray()
        val bytes=instrumentation.context.assets.open("desktop-public-qa.collecter").use {it.readBytes()}
        val json=JSONObject(EncryptedBackup.decrypt(bytes,password))
        assertEquals("interop-desktop",json.getJSONArray("entries").getJSONObject(0).getString("id"))
        json.getJSONArray("entries").getJSONObject(0).put("future","android-verified")
        val output=File(instrumentation.targetContext.getExternalFilesDir(null),"qa455-android-encrypted.collecter")
        output.writeBytes(EncryptedBackup.encrypt(json.toString(),password))
        assertTrue(output.isFile)
    }
    private fun isolated():Context {
        val base=InstrumentationRegistry.getInstrumentation().targetContext
        val prefix="workbench-qa-${UUID.randomUUID()}-"
        val dir=File(base.cacheDir,prefix).apply {mkdirs()}
        return object:ContextWrapper(base) {
            override fun getSharedPreferences(name:String,mode:Int)=base.getSharedPreferences(prefix+name,mode)
            override fun getFilesDir()=dir
            override fun getExternalFilesDir(type:String?):File?=null
        }
    }
    @Test fun encryptedWorkflowRoundTripUsesRealAndroidCryptoAndPreferences() {
        val source=isolated();val repository=WorkbenchRepository(source)
        repository.execute(JSONObject().put("op","collect").put("text","手机收录的原始凭证"))
        val row=repository.snapshot().getJSONArray("inbox").getJSONObject(0)
        val refs=JSONArray(listOf("inbox:"+row.getString("id")))
        repository.execute(JSONObject().put("op","batch").put("refs",refs).put("patch",JSONObject().put("loc","书房")))
        repository.execute(JSONObject().put("op","life").put("refs",refs).put("action","maintenance").put("note","已核对"))
        repository.execute(JSONObject().put("op","save-search").put("query","凭证"))
        val exported=CompleteBackupStore(source,"entries_v4").exportJson()
        val password="android-test-only-password".toCharArray()
        val encrypted=EncryptedBackup.encrypt(exported,password)
        val target=isolated();val backup=CompleteBackupStore(target,"entries_v4")
        assertTrue(backup.importJson(EncryptedBackup.decrypt(encrypted,password)))
        val recovered=WorkbenchRepository(target).snapshot()
        assertEquals("书房",recovered.getJSONArray("inbox").getJSONObject(0).getString("loc"))
        assertEquals("已核对",recovered.getJSONArray("inbox").getJSONObject(0).getJSONArray("_lifeEvents").getJSONObject(0).getString("note"))
        assertEquals(1,recovered.getJSONArray("saved_searches").length())
        val before=recovered.toString()
        assertThrows(Exception::class.java) {backup.importJson(EncryptedBackup.decrypt(encrypted,"incorrect-password-0000".toCharArray()))}
        assertEquals(before,WorkbenchRepository(target).snapshot().toString())
    }
}
