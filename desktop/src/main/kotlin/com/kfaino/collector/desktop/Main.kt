package com.kfaino.collector.desktop

import com.formdev.flatlaf.FlatDarkLaf
import com.kfaino.collector.desktop.storage.DesktopDataStore
import com.kfaino.collector.desktop.ui.MainWindow
import java.awt.EventQueue
import javax.swing.UIManager

fun main(args: Array<String>) {
    // 针对 macOS 的原生菜单栏与标题栏沉浸优化
    System.setProperty("apple.laf.useScreenMenuBar", "true")
    System.setProperty("apple.awt.application.name", "Collecter")
    System.setProperty("apple.awt.application.appearance", "system")

    try {
        FlatDarkLaf.setup()
        UIManager.put("Button.arc", 12)
        UIManager.put("Component.arc", 12)
        UIManager.put("TextComponent.arc", 10)
        UIManager.put("ScrollBar.showButtons", false)
        UIManager.put("ScrollBar.width", 10)
    } catch (e: Exception) {
        e.printStackTrace()
    }

    val store = DesktopDataStore()

    // 启动嵌入式轻量 HTTP 引擎 (Port 8848)，供 Native WebView2 宿主外壳和局域网设备通信
    val server = com.kfaino.collector.desktop.server.EmbeddedWebServer(store, allowLan = "--lan" in args)
    server.start()
    Runtime.getRuntime().addShutdownHook(Thread { server.stop() })

    if ("--lan" in args) {
        javax.swing.JOptionPane.showMessageDialog(null,
            "已显式启用局域网访问。仅在可信网络使用（HTTP 不加密）。\n用户名：collecter\n访问密钥：${server.accessToken}\n关闭应用将停止服务并使密钥失效。",
            "局域网配对", javax.swing.JOptionPane.WARNING_MESSAGE)
    }

    if ("--native" in args) {
        val jarDir = java.io.File(object {}.javaClass.protectionDomain.codeSource.location.toURI()).parentFile
        val host = java.io.File(jarDir, "CollecterWindow.exe")
        try {
            check(host.isFile) { "未找到 Native 宿主：$host；请使用完整安装包，开发调试可省略 --native" }
            val command = mutableListOf(host.absolutePath)
            if ("--smoke-test" in args) command.add("--smoke-test")
            val process = ProcessBuilder(command).directory(jarDir).start()
            Runtime.getRuntime().addShutdownHook(Thread { if (process.isAlive) process.destroy() })
            val code = process.waitFor()
            check(code == 0) { "Native 宿主退出失败：$code" }
        } finally { server.stop() }
        return
    }

    EventQueue.invokeLater {
        val window = MainWindow(store)
        window.jMenuBar = javax.swing.JMenuBar().apply {
            add(com.kfaino.collector.desktop.ui.DesktopBackupActions.menu(window, store))
            add(javax.swing.JMenu("整理工作台").apply {
                add(javax.swing.JMenuItem("收集、找回与生命周期…").apply { addActionListener {
                    com.kfaino.collector.desktop.ui.DesktopWorkbench(store,server.familyAccess,server.boundPort,window).isVisible=true
                } })
            })
            add(javax.swing.JMenu("本机服务").apply {
                add(javax.swing.JMenuItem("查看本次配对信息").apply { addActionListener {
                    javax.swing.JOptionPane.showMessageDialog(window,
                        "地址：http://127.0.0.1:${server.boundPort}/\n用户名：collecter\n访问密钥：${server.accessToken}\n默认仅本机；需局域网时用 --lan 显式启动。")
                } })
            })
        }
        window.isVisible = true
    }
}
