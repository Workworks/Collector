package com.kfaino.collector.desktop.storage

import java.io.File

/**
 * 桌面单机版独立数据目录解析器
 *
 * 遵循 capital-agent-system Standalone 规范：
 * - Windows: %LOCALAPPDATA%\CollecterStandalone\data
 * - Linux: ~/.local/share/collecter-standalone/data
 * - macOS: ~/Library/Application Support/CollecterStandalone/data
 *
 * 物理隔离于程序安装目录，卸载程序绝对不得触碰此数据目录。
 * 具备从早期 ~/.collector 的平滑迁移机制。
 */
object DesktopDataDirectory {

    fun resolve(): File {
        // 允许通过环境变量指定自定义数据目录（用于测试或便携模式）
        val custom = System.getenv("COLLECTER_DATA_DIR")
        if (!custom.isNullOrBlank()) {
            val dir = File(custom)
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

        val os = System.getProperty("os.name", "").lowercase()
        val userHome = System.getProperty("user.home", "")

        val targetDir = when {
            os.contains("win") -> {
                val localAppData = System.getenv("LOCALAPPDATA")
                if (!localAppData.isNullOrBlank()) {
                    File(localAppData, "CollecterStandalone\\data")
                } else {
                    File(userHome, "AppData\\Local\\CollecterStandalone\\data")
                }
            }
            os.contains("mac") || os.contains("darwin") -> {
                File(userHome, "Library/Application Support/CollecterStandalone/data")
            }
            else -> {
                // Linux / Unix (XDG)
                val xdgData = System.getenv("XDG_DATA_HOME")
                if (!xdgData.isNullOrBlank()) {
                    File(xdgData, "collecter-standalone/data")
                } else {
                    File(userHome, ".local/share/collecter-standalone/data")
                }
            }
        }

        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        // 迁移机制：如果新目录为空但老目录 ~/.collector/collector_data.json 存在，执行无缝迁移复制
        migrateLegacyIfPresent(targetDir, userHome)

        return targetDir
    }

    private fun migrateLegacyIfPresent(targetDir: File, userHome: String) {
        val targetDataFile = File(targetDir, "collector_data.json")
        if (targetDataFile.exists()) return

        val legacyDir = File(userHome, ".collector")
        val legacyDataFile = File(legacyDir, "collector_data.json")
        if (legacyDataFile.exists() && legacyDataFile.length() > 0) {
            try {
                legacyDataFile.copyTo(targetDataFile, overwrite = false)
                val legacyConfigFile = File(legacyDir, "config.json")
                if (legacyConfigFile.exists()) {
                    legacyConfigFile.copyTo(File(targetDir, "config.json"), overwrite = false)
                }
            } catch (e: Exception) {
                System.err.println("从历史数据目录迁移失败: ${e.message}")
            }
        }
    }
}
