package com.kfaino.collector.desktop.ui

import com.kfaino.collecter.core.BackupDocument
import com.kfaino.collector.desktop.storage.DesktopDataStore
import java.awt.Component
import javax.swing.*

object DesktopBackupActions {
    fun menu(parent: Component, store: DesktopDataStore) = JMenu("本地备份").apply {
        add(JMenuItem("导出加密备份…").apply { addActionListener { encrypted(parent,store,true) } })
        add(JMenuItem("WebDAV 历史版本…").apply { addActionListener {
            val url=store.getWebDavUrl();val user=store.getWebDavUsername();val pass=store.getWebDavPassword()
            work(parent,{com.kfaino.collecter.core.WebDavHistoryClient.list(url,user,pass)}) {versions->
                if(versions.length()==0) JOptionPane.showMessageDialog(parent,"没有历史版本")
                else {
                    val labels=(0 until versions.length()).map {versions.getJSONObject(it).let {v->v.optString("createdAt")+" · "+v.optString("reason")+" · "+v.getString("id")}}.toTypedArray()
                    val choice=JOptionPane.showInputDialog(parent,"仅恢复到本机，不覆盖远端最新备份","历史版本",JOptionPane.PLAIN_MESSAGE,null,labels,labels[0])
                    if(choice!=null) work(parent,{com.kfaino.collecter.core.WebDavHistoryClient.download(url,user,pass,versions.getJSONObject(labels.indexOf(choice)).getString("id"))}) {json->
                        if(JOptionPane.showConfirmDialog(parent,BackupDocument.preview(json),"历史恢复到本机",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION) work(parent,{check(store.importJson(json)){"恢复失败"};recordStatus(store,"历史版本已恢复到本机");"恢复成功"}) {JOptionPane.showMessageDialog(parent,it)}
                    }
                }
            }
        } })
        add(JMenuItem("预览恢复加密备份…").apply { addActionListener { encrypted(parent,store,false) } })
        add(JMenuItem("查看备份状态").apply { addActionListener {
            val file=java.io.File(store.dataDir,"workbench-backup-status.json")
            work(parent,{if(file.isFile) file.readText() else "尚无工作台备份记录"}) { JOptionPane.showMessageDialog(parent,it) }
        } })
        add(JMenuItem("同步冲突（只读）").apply { addActionListener {
            work(parent,{ (store.workbenchSnapshot().optJSONObject("_conflicts") ?: org.json.JSONObject()).toString(2) }) {text->
                JOptionPane.showMessageDialog(parent,JScrollPane(JTextArea(text+"\n只读：不自动选择或删除版本。",20,65).apply {isEditable=false}))
            }
        } })
        add(JMenuItem("导出完整备份…").apply { addActionListener {
            val chooser = JFileChooser().apply { selectedFile = java.io.File("Collecter-Backup.json") }
            if (chooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
                val file = chooser.selectedFile
                if (!file.exists() || JOptionPane.showConfirmDialog(parent, "替换已有备份文件？", "确认", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                    work(parent, { BackupDocument.atomicWrite(file, store.exportJson().toByteArray()); "备份已保存" }) {
                        JOptionPane.showMessageDialog(parent, it)
                    }
                }
            }
        } })
        add(JMenuItem("预览并恢复备份…").apply { addActionListener {
            val chooser = JFileChooser()
            if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
                work(parent, {
                    require(chooser.selectedFile.length() <= BackupDocument.MAX_BYTES) { "备份超过大小上限" }
                    chooser.selectedFile.readText().also { BackupDocument.parse(it) }
                }) { json ->
                    if (JOptionPane.showConfirmDialog(parent, BackupDocument.preview(json), "恢复预览", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                        work(parent, { check(store.importJson(json)) { "恢复失败，旧数据已保留" }; "恢复成功，请重新打开对应页面刷新" }) {
                            JOptionPane.showMessageDialog(parent, it)
                        }
                    }
                }
            }
        } })
    }

    private fun encrypted(parent:Component,store:DesktopDataStore,export:Boolean) {
        val chooser=JFileChooser().apply {selectedFile=java.io.File("Collecter-Backup.collecter")}
        val chosen=if(export) chooser.showSaveDialog(parent) else chooser.showOpenDialog(parent)
        if(chosen!=JFileChooser.APPROVE_OPTION) return
        val file=chooser.selectedFile
        if(export && file.exists() && JOptionPane.showConfirmDialog(parent,"替换已有文件？","确认",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION) return
        val input=JPasswordField(28)
        if(JOptionPane.showConfirmDialog(parent,arrayOf("输入至少 12 字符的备份密码。忘记密码无法恢复，请另处保管。",input),"加密备份",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION) return
        val password=input.password;input.text=""
        if(password.size !in 12..1024) {password.fill('\u0000');JOptionPane.showMessageDialog(parent,"密码长度需 12–1024 字符");return}
        if(export) {
            work(parent,{
                try { BackupDocument.atomicWrite(file,com.kfaino.collecter.core.EncryptedBackup.encrypt(store.exportJson(),password));recordStatus(store,"加密备份已保存");"备份已保存，请另处保管密码" }
                catch(e:Exception) {recordStatus(store,"加密导出失败；未确认有效备份");throw e}
                finally {password.fill('\u0000')}
            }) {JOptionPane.showMessageDialog(parent,it)}
        } else {
            work(parent,{
                try {require(file.length()<=com.kfaino.collecter.core.EncryptedBackup.MAX_BYTES);com.kfaino.collecter.core.EncryptedBackup.decrypt(file.readBytes(),password)}
                catch(e:Exception) {recordStatus(store,"加密恢复读取失败；检查密码和文件，本地记录未替换");throw e}
                finally {password.fill('\u0000')}
            }) {json->
                if(JOptionPane.showConfirmDialog(parent,BackupDocument.preview(json),"恢复预览",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION) {
                    work(parent,{check(store.importJson(json)){"恢复失败，旧数据保留"};recordStatus(store,"加密备份恢复成功");"恢复成功"}) {JOptionPane.showMessageDialog(parent,it)}
                }
            }
        }
    }
    private fun recordStatus(store:DesktopDataStore,message:String) {
        BackupDocument.atomicWrite(java.io.File(store.dataDir,"workbench-backup-status.json"),org.json.JSONObject().put("at",java.util.Date().toString()).put("message",message).toString(2).toByteArray())
    }

    private fun <T> work(parent: Component, task: () -> T, success: (T) -> Unit) {
        object : SwingWorker<T, Unit>() {
            override fun doInBackground() = task()
            override fun done() {
                try { success(get()) } catch (e: Exception) {
                    System.err.println("本地备份操作失败：$e")
                    JOptionPane.showMessageDialog(parent, e.cause?.message ?: e.message, "备份未完成", JOptionPane.ERROR_MESSAGE)
                }
            }
        }.execute()
    }
}
