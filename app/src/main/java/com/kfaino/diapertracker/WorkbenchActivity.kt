package com.kfaino.diapertracker

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kfaino.collecter.core.*
import org.json.JSONObject
import org.json.JSONArray
import java.time.LocalDate
import java.time.ZoneId

/** An explicit workbench over the existing collections, not a new isolated vault. */
class WorkbenchActivity : AppCompatActivity() {
    private val repository by lazy { WorkbenchRepository(this) }
    private lateinit var query: EditText
    private lateinit var location: EditText
    private lateinit var list: ListView
    private lateinit var summary: TextView
    private lateinit var progress: ProgressBar
    private val running=java.util.concurrent.atomic.AtomicInteger()
    private var hits=emptyList<CollectionWorkbench.Hit>()
    private var password: CharArray?=null
    private val saveEncrypted=registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val pass=password; password=null
        if(uri!=null && pass!=null) background({
            try {
                val bytes=EncryptedBackup.encrypt(DataStore(this).exportBackupJson(),pass)
                contentResolver.openOutputStream(uri,"wt")!!.use { it.write(bytes) }
                backupStatus("加密备份已保存；密码无法代为找回")
                "加密备份已保存，请另处保管密码"
            } catch(e:Exception) { backupStatus("加密导出失败；未确认生成有效备份");throw e }
            finally { pass.fill('\u0000') }
        }) { toast(it) } else pass?.fill('\u0000')
    }
    private val openEncrypted=registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val pass=password; password=null
        if(uri!=null && pass!=null) background({
            try {
                val out=java.io.ByteArrayOutputStream()
                contentResolver.openInputStream(uri)!!.use { stream ->
                    val buffer=ByteArray(8192)
                    while(true) { val n=stream.read(buffer); if(n<0) break; require(out.size().toLong()+n<=EncryptedBackup.MAX_BYTES); out.write(buffer,0,n) }
                }
                EncryptedBackup.decrypt(out.toByteArray(),pass)
            } catch(e:Exception) { backupStatus("加密恢复读取失败；检查密码和文件完整性，本地记录未替换");throw e }
            finally { pass.fill('\u0000') }
        }) { json ->
            MaterialAlertDialogBuilder(this).setTitle("加密备份恢复预览").setMessage(BackupDocument.preview(json))
                .setNegativeButton("取消",null).setPositiveButton("确认替换所含集合") { _,_->
                    background({ check(DataStore(this).importBackupJson(json)) { "恢复失败，原数据已保留" }; backupStatus("恢复成功"); "恢复成功" }) { toast(it); refresh() }
                }.show()
        } else pass?.fill('\u0000')
    }
    override fun onDestroy() { password?.fill('\u0000'); password=null; super.onDestroy() }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title="收集与物品工作台"
        val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(20,12,20,12) }
        summary=TextView(this).apply { text="搜索、整理与生命周期；原件保留" }; root.addView(summary)
        progress=ProgressBar(this).apply {visibility=android.view.View.GONE};root.addView(progress)
        query=EditText(this).apply { hint="关键词，可用空格组合" }; location=EditText(this).apply { hint="位置筛选（可留空）" }
        root.addView(query); root.addView(location)
        fun row(vararg actions: Pair<String,()->Unit>) { root.addView(LinearLayout(this).apply { for((label,action) in actions) addView(Button(this@WorkbenchActivity).apply { text=label; setOnClickListener { action() } },LinearLayout.LayoutParams(0,-2,1f)) }) }
        row("搜索" to { refresh() },"保存搜索" to { execute(JSONObject().put("op","save-search").put("query",query.text).put("location",location.text)) },"已存搜索" to { savedSearches() })
        row("收集" to { collect() },"批量整理" to { edit() },"详情/关联" to { details() })
        row("生命周期" to { lifecycle() },"备份/家庭" to { backupMenu() })
        list=ListView(this).apply { choiceMode=ListView.CHOICE_MODE_MULTIPLE }
        root.addView(list,LinearLayout.LayoutParams(-1,0,1f)); setContentView(root)
        refresh()
    }
    private fun refresh() {
        val q=query.text.toString(); val loc=location.text.toString()
        background({ CollectionWorkbench.search(repository.snapshot(),q,loc) }) { result ->
            hits=result; list.adapter=ArrayAdapter(this,android.R.layout.simple_list_item_multiple_choice,hits.map { "${CollectionWorkbench.title(it.record)} · ${it.record.optString("loc",it.record.optString("location"))} · ${it.record.optString("_lifeState","active")}" })
            summary.text="找到 ${hits.size} 条 · 勾选后可批量操作"
        }
    }
    private fun selected()=hits.indices.filter { list.isItemChecked(it) }.map { hits[it].reference }
    private fun command(op:String)=JSONObject().put("op",op).put("refs",JSONArray(selected()))
    private fun execute(command:JSONObject) { background({ repository.execute(command); "已保存" }) { toast(it); refresh() } }
    private fun input(title:String,hint:String="",accept:(String)->Unit) {
        val field=EditText(this).apply { this.hint=hint; minLines=2 }
        MaterialAlertDialogBuilder(this).setTitle(title).setView(field).setNegativeButton("取消",null).setPositiveButton("确认") { _,_->accept(field.text.toString()) }.show()
    }
    private fun collect()=input("收下文字、链接或 OCR 原文","原文不会被提取建议覆盖") { text ->
        background({ CollectionWorkbench.duplicates(repository.snapshot(),text).size }) { duplicates ->
            val save={ execute(JSONObject().put("op","collect").put("text",text)) }
            if(duplicates==0) save() else MaterialAlertDialogBuilder(this).setTitle("发现 $duplicates 条相同原文").setMessage("可取消检查已有内容，也可保留另一份。不会自动删除重复记录。")
                .setNegativeButton("取消",null).setPositiveButton("仍然收下") { _,_->save() }.show()
        }
    }
    private fun edit() {
        if(selected().isEmpty()) { toast("请先勾选记录"); return }
        val actions=arrayOf("位置","标签","责任人","提取建议（预览后应用）","共享范围与敏感标记")
        MaterialAlertDialogBuilder(this).setTitle("整理 ${selected().size} 条").setItems(actions) { _,which->
            when(which) {
                0,1,2 -> input(actions[which],if(which==1) "多个标签以逗号分隔" else "") { value ->
                    val key=listOf("loc","tags","_responsible")[which]
                    val patch=JSONObject().put(key,if(which==1) JSONArray(value.split(',','，').map(String::trim).filter(String::isNotEmpty)) else value)
                    execute(command("batch").put("patch",patch))
                }
                3 -> {
                    val chosen=hits.filter { it.reference in selected() }
                    val preview=chosen.joinToString("\n") { CollectionWorkbench.title(it.record)+"："+CollectionWorkbench.suggestions(listOf("original","ocr","content").joinToString("\n") { key->it.record.optString(key) }) }
                    MaterialAlertDialogBuilder(this).setTitle("仅保存辅助字段，原文不变").setMessage(preview).setNegativeButton("取消",null).setPositiveButton("应用建议") { _,_->execute(command("suggest")) }.show()
                }
                4 -> input("共享给成员 ID（逗号分隔）","留空设为私人；敏感资料不可共享") { members ->
                    val patch=JSONObject().put("_sharedWith",JSONArray(members.split(',','，').map(String::trim).filter(String::isNotEmpty)))
                    MaterialAlertDialogBuilder(this).setTitle("这些记录是否敏感？").setMessage("标记敏感后，家庭接口不会展示；不等于本机文件加密。")
                        .setNegativeButton("取消",null).setNeutralButton("普通") { _,_->execute(command("batch").put("patch",patch.put("_sensitive",false))) }
                        .setPositiveButton("敏感") { _,_->execute(command("batch").put("patch",patch.put("_sensitive",true))) }.show()
                }
            }
        }.show()
    }
    private fun details() {
        val ref=selected().singleOrNull() ?: run { toast("请勾选一条查看详情");return }
        val item=hits.first { it.reference==ref }
        val root=repository.snapshot()
        val related=WorkspaceRecords.related(root.optJSONArray("links") ?: JSONArray(),ref)
        val indexed=CollectionWorkbench.records(root).associateBy { it.reference }
        val names=related.map { indexed[it]?.let { hit->CollectionWorkbench.title(hit.record) } ?: "已删除：$it" }
        MaterialAlertDialogBuilder(this).setTitle(CollectionWorkbench.title(item.record)).setMessage(item.record.toString(2)+"\n关联资料：\n"+names.joinToString("\n"))
            .setPositiveButton("建立关联") { _,_->
                val targets=CollectionWorkbench.records(root).filter { it.reference!=ref }
                MaterialAlertDialogBuilder(this).setTitle("选择资料").setItems(targets.map { CollectionWorkbench.title(it.record) }.toTypedArray()) { _,i->execute(JSONObject().put("op","link").put("left",ref).put("right",targets[i].reference)) }.show()
            }.setNegativeButton("关闭",null).show()
        background({ repository.execute(JSONObject().put("op","open").put("refs",JSONArray(listOf(ref)))); Unit }) {}
    }
    private fun lifecycle() {
        if(selected().isEmpty()) { toast("请先勾选记录");return }
        val names=arrayOf("购买","维护","借出","归还","转卖","报废")
        MaterialAlertDialogBuilder(this).setTitle("记录生命周期").setItems(names) { _,i->
            input("${names[i]}：责任人/借用人") { person -> input("处理说明") { note ->
                val cmd=command("life").put("action",listOf("purchase","maintenance","lend","return","sell","retire")[i]).put("person",person).put("note",note)
                if(i==1) input("下次维护日期","YYYY-MM-DD；留空不提醒") { date ->
                    background({ val next=if(date.isBlank()) 0 else LocalDate.parse(date.trim()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(); repository.execute(cmd.put("nextAt",next)); "维护已记录" }) { toast(it);refresh() }
                } else execute(cmd)
            } }
        }.show()
    }
    private fun savedSearches() {
        val array=repository.snapshot().optJSONArray("saved_searches") ?: JSONArray()
        MaterialAlertDialogBuilder(this).setTitle("保存的搜索").setItems((0 until array.length()).map { array.getJSONObject(it).let { s->s.optString("query")+" · "+s.optString("location") } }.toTypedArray()) { _,i->
            val saved=array.getJSONObject(i); query.setText(saved.optString("query"));location.setText(saved.optString("location"));refresh()
        }.show()
    }
    private fun backupStatus(message:String) { getSharedPreferences("workbench_status",MODE_PRIVATE).edit().putString("backup",java.util.Date().toString()+"\n"+message).apply() }
    private fun backupMenu() {
        MaterialAlertDialogBuilder(this).setTitle("备份与家庭").setItems(arrayOf("导出加密备份","预览恢复加密备份","本机备份状态","连接家庭工作台","同步冲突（只读）","WebDAV 历史版本")) { _,i->
            when(i) {
                0,1 -> {
                    val field=EditText(this).apply { inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD;hint="至少 12 字符；忘记密码无法恢复" }
                    MaterialAlertDialogBuilder(this).setTitle("备份密码").setView(field).setNegativeButton("取消",null).setPositiveButton("继续") { _,_->
                        val pass=field.text.toString().toCharArray();field.text.clear()
                        if(pass.size !in 12..1024) { pass.fill('\u0000');toast("密码长度需 12–1024 字符") }
                        else { password=pass; if(i==0) saveEncrypted.launch("Collecter-${System.currentTimeMillis()}.collecter") else openEncrypted.launch(arrayOf("application/octet-stream","*/*")) }
                    }.show()
                }
                2 -> MaterialAlertDialogBuilder(this).setTitle("本机备份状态").setMessage(getSharedPreferences("workbench_status",MODE_PRIVATE).getString("backup","尚无本工作台备份记录")+"\nWebDAV 状态请在原备份入口查看；冲突时先核对远端，不自动覆盖。").setPositiveButton("关闭",null).show()
                3 -> FamilyClientDialog.show(this)
                4 -> MaterialAlertDialogBuilder(this).setTitle("同步保留的冲突").setMessage((repository.snapshot().optJSONObject("_conflicts") ?: JSONObject()).toString(2)+"\n此处只读，不自动删除或选择版本。WebDAV 版本冲突需先下载核对。").setPositiveButton("关闭",null).show()
                5 -> history()
            }
        }.show()
    }
    private fun history() {
        val store=DataStore(this);val url=store.getWebDavUrl();val user=store.getWebDavUsername();val pass=store.getWebDavPassword()
        background({WebDavHistoryClient.list(url,user,pass)}) { versions->
            if(versions.length()==0) {toast("没有历史版本");return@background}
            MaterialAlertDialogBuilder(this).setTitle("WebDAV 历史：只恢复到本机").setItems((0 until versions.length()).map {versions.getJSONObject(it).let {v->v.optString("createdAt")+" · "+v.optString("reason")}}.toTypedArray()) { _,i->
                background({WebDavHistoryClient.download(url,user,pass,versions.getJSONObject(i).getString("id"))}) {json->
                    MaterialAlertDialogBuilder(this).setTitle("历史恢复预览").setMessage(BackupDocument.preview(json)+"\n仅替换本机所含集合，不覆盖远端最新备份。")
                        .setNegativeButton("取消",null).setPositiveButton("恢复到本机") { _,_->background({check(store.importBackupJson(json)){"本机恢复失败"};backupStatus("历史版本已恢复到本机");"已恢复到本机"}) {toast(it);refresh()}}.show()
                }
            }.setNegativeButton("关闭",null).show()
        }
    }
    private fun toast(text:String)=Toast.makeText(this,text,Toast.LENGTH_LONG).show()
    private fun <T> background(task:()->T, success:(T)->Unit) {
        running.incrementAndGet();progress.visibility=android.view.View.VISIBLE
        Thread {
            try { val result=task();runOnUiThread { if(!isFinishing && !isDestroyed) success(result) } }
            catch(e:Exception) { android.util.Log.e("Workbench","操作失败",e);runOnUiThread { if(!isFinishing && !isDestroyed) toast("未完成：${e.cause?.message ?: e.message}") } }
            finally { if(running.decrementAndGet()==0) runOnUiThread {if(!isDestroyed) progress.visibility=android.view.View.GONE} }
        }.start()
    }
}
