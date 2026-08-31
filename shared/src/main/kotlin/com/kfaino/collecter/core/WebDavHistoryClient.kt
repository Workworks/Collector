package com.kfaino.collecter.core

import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

/** Read-only history access. Restoring locally never silently replaces the server's latest backup. */
object WebDavHistoryClient {
    private fun read(base:String,user:String,password:String,query:String,limit:Int):String {
        require(user.isNotBlank() && password.isNotBlank()) { "请先配置 WebDAV 账号" }
        val clean=base.trim().trimEnd('/')
        val target=if(clean.endsWith("Collecter_Backup.json")) clean else "$clean/Collecter_Backup.json"
        val url=URL("$target?$query")
        FamilyEndpoint.requireTrustedTransport(url)
        require(url.protocol in setOf("http","https") && url.userInfo==null)
        val conn=url.openConnection() as HttpURLConnection
        try {
            conn.instanceFollowRedirects=false;conn.connectTimeout=15000;conn.readTimeout=20000
            conn.setRequestProperty("Authorization","Basic "+Base64.getEncoder().encodeToString("$user:$password".toByteArray(Charsets.UTF_8)))
            require(conn.responseCode==200) { "历史读取失败 HTTP ${conn.responseCode}" }
            require(conn.getHeaderField("X-Collecter-Backup")=="history-etag-v1") { "此 WebDAV 服务未提供 Collecter 历史接口" }
            return conn.inputStream.use { input->
                val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
                while(true) {val n=input.read(buffer);if(n<0) break;require(out.size().toLong()+n<=limit){"历史内容超过大小限制"};out.write(buffer,0,n)}
                out.toString("UTF-8")
            }
        } finally {conn.disconnect()}
    }
    fun list(base:String,user:String,password:String)=JSONArray(read(base,user,password,"history",1024*1024))
    fun download(base:String,user:String,password:String,revision:String):String {
        require(revision.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}"))) { "无效的历史编号" }
        return read(base,user,password,"revision=$revision",BackupDocument.MAX_BYTES).also { BackupDocument.parse(it) }
    }
}
