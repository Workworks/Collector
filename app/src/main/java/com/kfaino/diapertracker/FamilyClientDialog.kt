package com.kfaino.diapertracker

import android.app.Activity
import android.widget.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Explicit connection only; member secrets remain in this dialog session and never in backups. */
object FamilyClientDialog {
    fun show(activity:Activity) {
        val address=EditText(activity).apply {hint="http://桌面IP:8848/api/v1/family"}
        val secret=EditText(activity).apply {hint="桌面签发的成员密钥";inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD}
        val form=LinearLayout(activity).apply {orientation=LinearLayout.VERTICAL;addView(address);addView(secret)}
        MaterialAlertDialogBuilder(activity).setTitle("连接家庭工作台").setMessage("仅可信局域网使用 HTTP；不自动保存密钥。仅显示桌面端明确授权的非敏感记录。")
            .setView(form).setNegativeButton("取消",null).setPositiveButton("连接") { _,_->
                val url=address.text.toString().trim();val token=secret.text.toString();secret.text.clear()
                fetch(activity,url,token)
            }.show()
    }
    private fun fetch(activity:Activity,url:String,token:String) {
        network(activity,{JSONArray(request(url,token,null))}) { array->
            val records=(0 until array.length()).map {array.getJSONObject(it)}
            MaterialAlertDialogBuilder(activity).setTitle("已共享 ${records.size} 条").setItems(records.map { com.kfaino.collecter.core.CollectionWorkbench.title(it.getJSONObject("record")) }.toTypedArray()) { _,i->
                val selected=records[i]
                MaterialAlertDialogBuilder(activity).setTitle("共享记录").setMessage(selected.getJSONObject("record").toString(2))
                    .setNegativeButton("关闭",null).setPositiveButton("编辑位置（需编辑者权限）") { _,_->
                        val field=EditText(activity).apply {setText(selected.getJSONObject("record").optString("loc"))}
                        MaterialAlertDialogBuilder(activity).setTitle("修改共享物品位置").setView(field).setNegativeButton("取消",null).setPositiveButton("保存") { _,_->
                            val cmd=JSONObject().put("op","batch").put("refs",JSONArray(listOf(selected.getString("reference")))).put("patch",JSONObject().put("loc",field.text.toString()))
                            network(activity,{request(url,token,cmd)}) {fetch(activity,url,token)}
                        }.show()
                    }.show()
            }.setNegativeButton("断开",null).setPositiveButton("刷新") { _,_->fetch(activity,url,token)}.show()
        }
    }
    fun request(address:String,token:String,command:JSONObject?):String {
        val url=com.kfaino.collecter.core.FamilyEndpoint.validate(address)
        require(token.length in 32..100) { "成员密钥格式无效" }
        val conn=url.openConnection() as HttpURLConnection
        try {
            conn.instanceFollowRedirects=false;conn.connectTimeout=10000;conn.readTimeout=15000
            conn.setRequestProperty("Authorization","Bearer $token")
            conn.requestMethod=if(command==null) "GET" else "POST"
            if(command!=null) { conn.doOutput=true;conn.setRequestProperty("Content-Type","application/json");conn.outputStream.use {it.write(command.toString().toByteArray())} }
            require(conn.responseCode in 200..299) { "访问失败 HTTP ${conn.responseCode}；检查角色、共享范围或密钥是否已撤销" }
            return conn.inputStream.use { input->
                val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
                while(true) {val n=input.read(buffer);if(n<0) break;require(out.size()+n<=4*1024*1024){"共享列表过大，请桌面减少共享范围"};out.write(buffer,0,n)}
                out.toString("UTF-8")
            }
        } finally {conn.disconnect()}
    }
    private fun <T> network(activity:Activity,task:()->T,success:(T)->Unit) {
        Thread {
            try {val value=task();activity.runOnUiThread {if(!activity.isFinishing && !activity.isDestroyed) success(value)}}
            catch(e:Exception) {android.util.Log.e("FamilyClient","家庭访问失败",e);activity.runOnUiThread {if(!activity.isFinishing && !activity.isDestroyed) Toast.makeText(activity,e.message,Toast.LENGTH_LONG).show()}}
        }.start()
    }
}
