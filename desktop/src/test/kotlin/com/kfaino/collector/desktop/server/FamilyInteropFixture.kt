package com.kfaino.collector.desktop.server

import com.kfaino.collector.desktop.storage.DesktopDataStore
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Isolated process fixture for a real Android-to-desktop family API run. */
object FamilyInteropFixture {
    @JvmStatic fun main(args: Array<String>) {
        val root = File("build/family-interop").apply { deleteRecursively(); mkdirs() }
        val stop = File(root, "stop")
        val revoke = File(root, "revoke")
        val revoked = File(root, "revoked")
        val session = File(root, "session.json")
        val store = DesktopDataStore(File(root, "data"))
        val server = EmbeddedWebServer(store, port = 8848, allowLan = true)
        val viewer = server.familyAccess.issue("Android 查看者", "viewer")
        val editor = server.familyAccess.issue("Android 编辑者", "editor")
        val sharedWith = JSONArray(listOf(viewer.member.id, editor.member.id))
        check(store.importJson(JSONObject().put("entries", JSONArray()
            .put(JSONObject().put("id", "shared").put("brand", "联调咖啡机").put("loc", "厨房").put("_sharedWith", sharedWith).put("secretField", "不得泄露"))
            .put(JSONObject().put("id", "private").put("brand", "私人记录"))
            .put(JSONObject().put("id", "sensitive").put("brand", "敏感记录").put("_sensitive", true).put("_sharedWith", sharedWith))).toString()))
        server.start()
        session.writeText(JSONObject()
            .put("address", "http://10.0.2.2:${server.boundPort}/api/v1/family")
            .put("viewerToken", viewer.token)
            .put("editorToken", editor.token)
            .put("editorId", editor.member.id)
            .toString())
        println("FAMILY_INTEROP_READY ${session.absolutePath}")
        try {
            val deadline = System.currentTimeMillis() + 180_000
            while (!stop.exists() && System.currentTimeMillis() < deadline) {
                if (revoke.exists() && !revoked.exists()) {
                    server.familyAccess.revoke(viewer.member.id)
                    server.familyAccess.revoke(editor.member.id)
                    revoked.createNewFile()
                }
                Thread.sleep(200)
            }
            check(stop.exists()) { "Android 联调在 180 秒内未完成" }
        } finally {
            server.stop()
            check(root.deleteRecursively()) { "联调临时目录清理失败：${root.absolutePath}" }
        }
    }
}
