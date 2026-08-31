package com.kfaino.collecter.core

import java.net.URL

object FamilyEndpoint {
    fun validate(address:String):URL {
        val url=URL(address)
        require(url.protocol in setOf("http","https") && url.path=="/api/v1/family" && url.userInfo==null && url.query==null && url.ref==null) { "请使用完整家庭接口地址，不包含账号、查询参数或片段" }
        requireTrustedTransport(url)
        return url
    }
    fun requireTrustedTransport(url:URL) {
        require(url.protocol in setOf("http","https") && url.userInfo==null) { "凭据连接必须使用 HTTP(S)，账号不能包含在地址中" }
        if(url.protocol=="http") {
            val host=url.host.lowercase()
            val parts=host.split('.').mapNotNull {it.toIntOrNull()}
            val privateV4=parts.size==4 && host.matches(Regex("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+")) && parts.all {it in 0..255} &&
                (parts[0]==10 || parts[0]==127 || (parts[0]==192 && parts[1]==168) || (parts[0]==172 && parts[1] in 16..31))
            require(host=="localhost" || privateV4) { "HTTP 仅允许私有 IPv4 地址或 localhost；公网请使用 HTTPS" }
        }
    }
}
