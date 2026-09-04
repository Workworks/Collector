package com.kfaino.collector.desktop.ui

import com.kfaino.collecter.core.*
import com.kfaino.collector.desktop.server.FamilyAccess
import com.kfaino.collector.desktop.storage.DesktopDataStore
import org.json.JSONArray
import org.json.JSONObject
import java.awt.BorderLayout
import java.awt.GridLayout
import java.awt.datatransfer.DataFlavor
import java.awt.image.BufferedImage
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import javax.swing.*
import javax.swing.table.DefaultTableModel

class DesktopWorkbench(private val store:DesktopDataStore,private val family:FamilyAccess,private val port:Int,parent:JFrame):JDialog(parent,"Collecter · 整理工作台",false) {
    private val query=JTextField()
    private val locationField=JTextField()
    private val status=JLabel("拖入文件仅复制到本机，不自动上传。Ctrl+F 搜索。")
    private val model=object:DefaultTableModel(arrayOf("名称","集合","位置","生命周期","关联数"),0) { override fun isCellEditable(row:Int,column:Int)=false }
    private val table=JTable(model)
    private var hits=emptyList<CollectionWorkbench.Hit>()
    init {
        setSize(1080,700);setLocationRelativeTo(parent);defaultCloseOperation=DISPOSE_ON_CLOSE
        val top=JPanel(BorderLayout())
        top.add(JPanel(GridLayout(2,2)).apply { add(JLabel("关键词"));add(query);add(JLabel("位置"));add(locationField) },BorderLayout.NORTH)
        top.add(JPanel().apply {
            fun button(label:String,action:()->Unit) { add(JButton(label).apply { addActionListener { action() } }) }
            button("搜索",::refresh);button("保存搜索") { execute(JSONObject().put("op","save-search").put("query",query.text).put("location",locationField.text)) }
            button("已存搜索",::saved);button("收集",::collect);button("导入文件",::chooseFiles)
            button("批量整理",::edit);button("详情/关联",::details);button("生命周期",::life);button("家庭权限",::members)
        },BorderLayout.CENTER)
        add(top,BorderLayout.NORTH);add(JScrollPane(table),BorderLayout.CENTER);add(status,BorderLayout.SOUTH)
        query.addActionListener { refresh() };locationField.addActionListener { refresh() }
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control F"),"search")
        rootPane.actionMap.put("search",object:AbstractAction(){override fun actionPerformed(e:java.awt.event.ActionEvent){query.requestFocusInWindow();query.selectAll()}})
        table.transferHandler=object:TransferHandler() {
            override fun canImport(support:TransferSupport)=support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
            override fun importData(support:TransferSupport):Boolean {
                if(!canImport(support)) return false
                return try {
                    val files=(support.transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<*>).filterIsInstance<File>()
                    confirmFiles(files);true
                } catch(e:Exception) { showFailure(e);false }
            }
        }
        refresh()
    }
    private fun selected()=table.selectedRows.map { hits[table.convertRowIndexToModel(it)].reference }
    private fun command(op:String)=JSONObject().put("op",op).put("refs",JSONArray(selected()))
    private fun refresh() {
        val q=query.text;val loc=locationField.text
        work({ val root=store.workbenchSnapshot();CollectionWorkbench.search(root,q,loc) to (root.optJSONArray("links") ?: JSONArray()) }) { (result,links)->
            hits=result;model.rowCount=0
            for(hit in hits) model.addRow(arrayOf(CollectionWorkbench.title(hit.record),hit.reference.substringBefore(':'),hit.record.optString("loc",hit.record.optString("location")),hit.record.optString("_lifeState","active"),WorkspaceRecords.related(links,hit.reference).size))
            status.text="${hits.size} 条 · 可多选后批量处理 · 文件拖入前会确认"
        }
    }
    private fun execute(cmd:JSONObject) { work({store.executeWorkbench(cmd);Unit}) {refresh()} }
    private fun ask(title:String,initial:String=""):String?=JOptionPane.showInputDialog(this,title,initial)
    private fun collect() {
        val text=ask("收下文字或链接；原文保留") ?: return
        work({CollectionWorkbench.duplicates(store.workbenchSnapshot(),text).size}) { count->
            if(count==0 || JOptionPane.showConfirmDialog(this,"已有 $count 条相同原文，仍然收下？","重复提示",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION) execute(JSONObject().put("op","collect").put("text",text))
        }
    }
    private fun saved() {
        val all=store.workbenchSnapshot().optJSONArray("saved_searches") ?: JSONArray()
        val choices=(0 until all.length()).map { all.getJSONObject(it) }
        if(choices.isEmpty()) { JOptionPane.showMessageDialog(this,"还没有保存的搜索");return }
        val labels=choices.map { it.optString("query")+" · "+it.optString("location") }.toTypedArray()
        val value=JOptionPane.showInputDialog(this,"选择查询","保存的搜索",JOptionPane.PLAIN_MESSAGE,null,labels,labels.first()) ?: return
        val item=choices[labels.indexOf(value)];query.text=item.optString("query");locationField.text=item.optString("location");refresh()
    }
    private fun edit() {
        val refs=selected();if(refs.isEmpty()) { JOptionPane.showMessageDialog(this,"请先选择记录");return }
        val choices=arrayOf("位置","标签","责任人","备注","提取建议","共享成员","敏感标记")
        val selected=JOptionPane.showInputDialog(this,"批量修改 ${refs.size} 条","整理",JOptionPane.PLAIN_MESSAGE,null,choices,choices.first()) ?: return
        val index=choices.indexOf(selected)
        if(index==4) {
            val preview=hits.filter { it.reference in refs }.joinToString("\n") { hit->CollectionWorkbench.title(hit.record)+"："+CollectionWorkbench.suggestions(listOf("original","ocr","content").joinToString("\n"){hit.record.optString(it)}) }
            if(JOptionPane.showConfirmDialog(this,preview+"\n确认保存为辅助字段，原文不变？","建议预览",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION) execute(JSONObject().put("op","suggest").put("refs",JSONArray(refs)))
            return
        }
        val key=listOf("loc","tags","_responsible","notes","","_sharedWith","_sensitive")[index]
        val value:Any=if(index==6) {
            val decision=JOptionPane.showConfirmDialog(this,"标记为敏感？是：不进入家庭视图；否：取消敏感标记。\n不等于本机文件加密。","敏感资料",JOptionPane.YES_NO_CANCEL_OPTION)
            if(decision !in listOf(JOptionPane.YES_OPTION,JOptionPane.NO_OPTION)) return
            decision==JOptionPane.YES_OPTION
        } else {
            val input=ask(if(index==5) "成员 ID 以逗号分隔，留空取消共享；成员 ID 在家庭权限中查看" else "${choices[index]}（多个标签以逗号分隔）") ?: return
            if(index in listOf(1,5)) JSONArray(input.split(',','，').map(String::trim).filter(String::isNotEmpty)) else input
        }
        execute(JSONObject().put("op","batch").put("refs",JSONArray(refs)).put("patch",JSONObject().put(key,value)))
    }
    private fun details() {
        val ref=selected().singleOrNull() ?: run { JOptionPane.showMessageDialog(this,"请选择一条记录");return }
        val root=store.workbenchSnapshot();val records=CollectionWorkbench.records(root)
        val record=records.firstOrNull { it.reference==ref } ?: return
        val related=WorkspaceRecords.related(root.optJSONArray("links") ?: JSONArray(),ref)
        val detail=record.record.toString(2)+"\n关联资料：\n"+records.filter { it.reference in related }.joinToString("\n"){CollectionWorkbench.title(it.record)+"\n"+it.record.toString(2)}
        val text=JTextArea(detail,22,80).apply { isEditable=false;lineWrap=true;wrapStyleWord=true }
        val choice=JOptionPane.showOptionDialog(this,JScrollPane(text),"记录与关联",JOptionPane.DEFAULT_OPTION,JOptionPane.PLAIN_MESSAGE,null,arrayOf("关闭","建立关联"),"关闭")
        if(choice==1) {
            val targets=records.filter { it.reference!=ref }
            val labels=targets.map { CollectionWorkbench.title(it.record)+" ["+it.reference+"]" }.toTypedArray()
            val picked=JOptionPane.showInputDialog(this,"选择关联资料","关联",JOptionPane.PLAIN_MESSAGE,null,labels,labels.firstOrNull())
            if(picked!=null) execute(JSONObject().put("op","link").put("left",ref).put("right",targets[labels.indexOf(picked)].reference))
        }
        execute(JSONObject().put("op","open").put("refs",JSONArray(listOf(ref))))
    }
    private fun life() {
        val refs=selected();if(refs.isEmpty()) return
        val labels=arrayOf("购买","维护","借出","归还","转卖","报废")
        val action=JOptionPane.showInputDialog(this,"选择实际发生的事件","生命周期",JOptionPane.PLAIN_MESSAGE,null,labels,labels.first()) ?: return
        val person=ask("责任人或借用人") ?: return;val note=ask("本次处理说明") ?: return
        val index=labels.indexOf(action)
        val cmd=JSONObject().put("op","life").put("refs",JSONArray(refs)).put("action",listOf("purchase","maintenance","lend","return","sell","retire")[index]).put("person",person).put("note",note)
        if(index==1) {
            val date=ask("下次维护日期 YYYY-MM-DD，留空不提醒") ?: return
            try { cmd.put("nextAt",if(date.isBlank()) 0L else LocalDate.parse(date.trim()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()) }
            catch(e:Exception) { showFailure(e);return }
        }
        execute(cmd)
    }
    private fun chooseFiles() { val chooser=JFileChooser().apply { isMultiSelectionEnabled=true };if(chooser.showOpenDialog(this)==JFileChooser.APPROVE_OPTION) confirmFiles(chooser.selectedFiles.toList()) }
    private fun confirmFiles(files:List<File>) {
        if(JOptionPane.showConfirmDialog(this,"复制 ${files.size} 个文件到本机收集箱？原文件保留，不自动共享或上传。","导入确认",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION) return
        work({ importFiles(store,files) }) {refresh()}
    }
    private fun members() {
        val choices=arrayOf("签发成员密钥","查看/撤销成员")
        val option=JOptionPane.showOptionDialog(this,"仅共享明确授权且非敏感的记录；本地令牌在 12 小时或应用退出后失效。\n跨设备需显式 --lan 启动；HTTP 只在可信网络使用。","家庭权限",JOptionPane.DEFAULT_OPTION,JOptionPane.WARNING_MESSAGE,null,choices,choices[0])
        if(option==0) {
            val name=ask("成员显示名") ?: return
            val roles=arrayOf("viewer","editor")
            val role=JOptionPane.showInputDialog(this,"viewer 只读；editor 可整理和记录维护/借还","角色",JOptionPane.PLAIN_MESSAGE,null,roles,roles[0]) as? String ?: return
            try {
                val grant=family.issue(name,role)
                val address=localFamilyAddress(port)
                val payload="collecter://family?url="+java.net.URLEncoder.encode(address,"UTF-8")+"&token="+java.net.URLEncoder.encode(grant.token,"UTF-8")
                val text=JTextArea("成员 ID：${grant.member.id}\n角色：$role\n接口：$address\n访问密钥：${grant.token}\n先在记录中授权此成员 ID；二维码和密钥仅交给该成员。",7,58).apply { isEditable=false;lineWrap=true;wrapStyleWord=true }
                val panel=JPanel(BorderLayout(12,12)).apply { add(JLabel(ImageIcon(qrImage(payload,260))),BorderLayout.WEST);add(JScrollPane(text),BorderLayout.CENTER) }
                JOptionPane.showMessageDialog(this,panel,"用 Collecter 扫码连接",JOptionPane.INFORMATION_MESSAGE)
            } catch(e:Exception) {showFailure(e)}
        } else if(option==1) {
            val members=family.members();if(members.isEmpty()) {JOptionPane.showMessageDialog(this,"没有有效成员");return}
            val labels=members.map { "${it.name} · ${it.role} · ${it.id}" }.toTypedArray()
            val chosen=JOptionPane.showInputDialog(this,"选择要撤销的成员","撤销授权",JOptionPane.PLAIN_MESSAGE,null,labels,labels[0]) ?: return
            if(JOptionPane.showConfirmDialog(this,"立即撤销该成员密钥？","确认",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION) family.revoke(members[labels.indexOf(chosen)].id)
        }
    }
    private fun showFailure(e:Exception) { System.err.println("工作台操作失败：${e.message}");JOptionPane.showMessageDialog(this,e.cause?.message ?: e.message,"未完成，未确认保存",JOptionPane.ERROR_MESSAGE) }
    private fun <T> work(task:()->T,success:(T)->Unit) { object:SwingWorker<T,Unit>() {
        override fun doInBackground()=task()
        override fun done() { try { val value=get();if(isDisplayable) success(value) } catch(e:Exception) {showFailure(e)} }
    }.execute() }
    companion object {
        internal fun localFamilyAddress(port:Int):String {
            val ip=java.net.NetworkInterface.getNetworkInterfaces().toList().asSequence()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.toList().asSequence() }
                .filterIsInstance<java.net.Inet4Address>()
                .map { it.hostAddress }
                .firstOrNull { it.startsWith("10.") || it.startsWith("192.168.") || Regex("172\\.(1[6-9]|2\\d|3[01])\\.").containsMatchIn(it) }
                ?: "127.0.0.1"
            return "http://$ip:$port/api/v1/family"
        }
        internal fun qrImage(text:String,size:Int):BufferedImage {
            val matrix=com.google.zxing.MultiFormatWriter().encode(text,com.google.zxing.BarcodeFormat.QR_CODE,size,size)
            return BufferedImage(size,size,BufferedImage.TYPE_INT_RGB).apply {
                for(y in 0 until size) for(x in 0 until size) setRGB(x,y,if(matrix[x,y]) 0x000000 else 0xFFFFFF)
            }
        }
        fun importFiles(store:DesktopDataStore,files:List<File>) {
            require(files.size in 1..100)
            synchronized(store) {
                var root=store.workbenchSnapshot()
                var total=0L
                val folder=File(store.dataDir,"workbench-files").apply {mkdirs()}
                require(!java.nio.file.Files.isSymbolicLink(folder.toPath()))
                val created=mutableListOf<File>()
                try {
                for(file in files) {
                    require(file.isFile && file.length() <= BackupDocument.MAX_ASSET_BYTES) { "单文件最多 16 MiB" }
                    val bytes=file.inputStream().use { it.readNBytes(BackupDocument.MAX_ASSET_BYTES+1) }
                    require(bytes.size <= BackupDocument.MAX_ASSET_BYTES)
                    total+=bytes.size;require(total<=BackupDocument.MAX_BYTES/2) { "单次附件最多 32 MiB" }
                    val target=File(folder,BackupDocument.sha256(bytes))
                    if(!target.exists()) { BackupDocument.atomicWrite(target,bytes);created.add(target) }
                    require(target.isFile && !java.nio.file.Files.isSymbolicLink(target.toPath()))
                    root=CollectionWorkbench.apply(root,JSONObject().put("op","collect").put("text",file.name).put("photo",target.absolutePath))
                }
                BackupDocument.attachFiles(root,listOf(store.dataDir))
                check(store.importJson(root.toString())) { "文件记录保存失败；原文件未删除" }
                } catch(failure:Exception) {
                    for(target in created) {
                        try { java.nio.file.Files.deleteIfExists(target.toPath()) }
                        catch(cleanup:Exception) { failure.addSuppressed(cleanup);System.err.println("附件回滚失败：${target.name}：${cleanup.message}") }
                    }
                    throw failure
                }
            }
        }
    }
}
