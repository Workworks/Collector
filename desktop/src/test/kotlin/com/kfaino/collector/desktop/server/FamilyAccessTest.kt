package com.kfaino.collector.desktop.server

import com.kfaino.collector.desktop.storage.DesktopDataStore
import org.json.JSONObject
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.net.URI
import java.net.http.*

class FamilyAccessTest {
    @get:Rule val temp=TemporaryFolder()
    @Test fun expiryFreesQuotaAndReissueDoesNotInheritSharing() {
        val store=DesktopDataStore(temp.newFolder())
        var now=1000L
        val access=FamilyAccess(store) {now}
        val old=access.issue("成员","editor")
        store.importJson("""{"entries":[{"id":"x","brand":"shared","_sharedWith":["${old.member.id}"]}]}""")
        assertEquals(1,access.read(old.token).length())
        repeat(99) {access.issue("成员$it","viewer")}
        now=old.member.expires
        assertNull(access.identify(old.token))
        assertThrows(SecurityException::class.java) { access.read(old.token) }
        val fresh=access.issue("成员","editor")
        assertNotEquals(old.member.id,fresh.member.id)
        assertEquals(0,access.read(fresh.token).length())
        store.executeWorkbench(JSONObject("""{"op":"batch","refs":["entries:x"],"patch":{"_sharedWith":["${fresh.member.id}"]}}"""))
        assertEquals(1,access.read(fresh.token).length())
        assertEquals(1,access.members().size)
    }
    @Test fun actualHttpEnforcesScopeRolesRevocationAndAudit() {
        val store=DesktopDataStore(temp.newFolder())
        val server=EmbeddedWebServer(store,port=0)
        val viewer=server.familyAccess.issue("查看者","viewer")
        val editor=server.familyAccess.issue("编辑者","editor")
        val shared=JSONArray(listOf(viewer.member.id,editor.member.id))
        assertTrue(store.importJson(JSONObject().put("entries",JSONArray()
            .put(JSONObject().put("id","shared").put("brand","家庭咖啡机").put("_sharedWith",shared).put("secretField","must not leak"))
            .put(JSONObject().put("id","private").put("brand","私人"))
            .put(JSONObject().put("id","sensitive").put("brand","敏感").put("_sensitive",true).put("_sharedWith",shared))).toString()))
        server.start()
        val client=HttpClient.newHttpClient()
        fun request(token:String,path:String="/api/v1/family",body:String?=null):HttpResponse<String> {
            val builder=HttpRequest.newBuilder(URI("http://127.0.0.1:${server.boundPort}$path")).header("Authorization","Bearer $token")
            if(body!=null) builder.POST(HttpRequest.BodyPublishers.ofString(body))
            return client.send(builder.build(),HttpResponse.BodyHandlers.ofString())
        }
        try {
            val response=request(viewer.token);assertEquals(200,response.statusCode())
            assertEquals(1,JSONArray(response.body()).length());assertFalse(response.body().contains("secretField"));assertFalse(response.body().contains("敏感"))
            assertEquals(401,request(viewer.token,"/api/v1/backup/export").statusCode())
            val update="""{"op":"batch","refs":["entries:shared"],"patch":{"loc":"客厅"}}"""
            assertEquals(403,request(viewer.token,body=update).statusCode())
            assertEquals(200,request(editor.token,body=update).statusCode())
            assertEquals(403,request(editor.token,body=update.replace("entries:shared","entries:private")).statusCode())
            assertEquals(403,request(editor.token,body="""{"op":"batch","refs":["entries:shared"],"patch":{"_sensitive":false}}""").statusCode())
            val record=store.workbenchSnapshot().getJSONArray("entries").getJSONObject(0)
            assertEquals("客厅",record.getString("loc"));assertEquals(editor.member.id,record.getJSONArray("_audit").getJSONObject(0).getString("actor"))
            server.familyAccess.revoke(editor.member.id)
            assertEquals(401,request(editor.token).statusCode())
        } finally {server.stop()}
        assertNull(server.familyAccess.identify(viewer.token))
    }
}
