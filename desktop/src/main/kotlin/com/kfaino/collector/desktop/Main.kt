package com.kfaino.collector.desktop

import com.formdev.flatlaf.FlatDarkLaf
import com.kfaino.collector.desktop.storage.DesktopDataStore
import com.kfaino.collector.desktop.ui.MainWindow
import java.awt.EventQueue
import javax.swing.UIManager

fun main() {
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

    EventQueue.invokeLater {
        val store = DesktopDataStore()
        val window = MainWindow(store)
        window.isVisible = true
    }
}
