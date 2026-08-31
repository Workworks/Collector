package com.kfaino.diapertracker

import android.content.SharedPreferences

/**
 * ⚙️ 系统通用与功能配置持久化仓储 (Settings Store)
 * 封装深浅主题、通知提醒、触感震动、生物识别隐私锁、WebDAV 云凭据、备份提醒与简易模式配置。
 * 作为 DataStore 门面下沉的专用仓储，严格保持 SharedPreferences Key 完全不变。
 */
class SettingsStore(private val prefs: SharedPreferences) {

    companion object {
        const val KEY_THEME = "app_theme_mode"
        const val KEY_GITHUB_REPO = "github_repo"
        const val KEY_REMINDERS_ENABLED = "reminders_enabled"
        const val KEY_REMINDER_HOUR = "reminder_hour"
        const val KEY_REMINDER_MINUTE = "reminder_minute"
        const val KEY_HAPTIC_ENABLED = "haptic_feedback_enabled"
        const val KEY_NEXT_BACKUP_PROMPT = "next_backup_prompt_time"
        const val KEY_LAST_BACKUP_TIME = "last_backup_time"
        const val KEY_BIOMETRIC_LOCK = "biometric_lock_enabled"
        const val KEY_WEBDAV_URL = "webdav_server_url"
        const val KEY_WEBDAV_USER = "webdav_username"
        const val KEY_WEBDAV_PASS = "webdav_password"
        const val KEY_SIMPLE_MODE = "app_simple_mode_enabled"

        const val DEFAULT_WEBDAV_URL = "https://dav.jianguoyun.com/dav/"
        const val DEFAULT_GITHUB_REPO = "Workworks/Collector"
    }

    // ==================== 主题设置 ====================

    fun getThemeMode(): Int = prefs.getInt(KEY_THEME, DataStore.THEME_SYSTEM)

    fun setThemeMode(mode: Int) {
        prefs.edit().putInt(KEY_THEME, mode).apply()
        DataStore.applyThemeMode(mode)
    }

    // ==================== GitHub 仓库设置 ====================

    fun getGithubRepo(): String = prefs.getString(KEY_GITHUB_REPO, DEFAULT_GITHUB_REPO) ?: DEFAULT_GITHUB_REPO

    fun setGithubRepo(repo: String) {
        prefs.edit().putString(KEY_GITHUB_REPO, repo.trim()).apply()
    }

    // ==================== 通知提醒设置 ====================

    fun isNotificationEnabled(): Boolean = prefs.getBoolean(KEY_REMINDERS_ENABLED, true)

    fun setNotificationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REMINDERS_ENABLED, enabled).apply()
    }

    fun getNotificationHour(): Int = prefs.getInt(KEY_REMINDER_HOUR, 9)

    fun getNotificationMinute(): Int = prefs.getInt(KEY_REMINDER_MINUTE, 0)

    fun setNotificationTime(hour: Int, minute: Int) {
        prefs.edit()
            .putInt(KEY_REMINDER_HOUR, hour)
            .putInt(KEY_REMINDER_MINUTE, minute)
            .apply()
    }

    // ==================== 触感震动反馈配置 ====================

    fun isHapticFeedbackEnabled(): Boolean = prefs.getBoolean(KEY_HAPTIC_ENABLED, true)

    fun setHapticFeedbackEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTIC_ENABLED, enabled).apply()
    }

    // ==================== 备份提醒持久化控制 ====================

    fun getNextBackupPromptTime(): Long = prefs.getLong(KEY_NEXT_BACKUP_PROMPT, 0L)

    fun snoozeBackupPrompt(days: Int = 3) {
        val nextTime = System.currentTimeMillis() + days.toLong() * 24L * 60 * 60 * 1000
        prefs.edit().putLong(KEY_NEXT_BACKUP_PROMPT, nextTime).apply()
    }

    fun recordBackupDone() {
        val now = System.currentTimeMillis()
        prefs.edit()
            .putLong(KEY_LAST_BACKUP_TIME, now)
            .putLong(KEY_NEXT_BACKUP_PROMPT, now + 7L * 24 * 60 * 60 * 1000)
            .apply()
    }

    // ==================== 生物识别指纹应用锁 ====================

    fun isBiometricLockEnabled(): Boolean = prefs.getBoolean(KEY_BIOMETRIC_LOCK, false)

    fun setBiometricLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_LOCK, enabled).apply()
    }

    // ==================== WebDAV 私有云配置 ====================

    fun getWebDavUrl(): String = prefs.getString(KEY_WEBDAV_URL, DEFAULT_WEBDAV_URL) ?: DEFAULT_WEBDAV_URL

    fun setWebDavUrl(url: String) {
        prefs.edit().putString(KEY_WEBDAV_URL, url.trim()).apply()
    }

    fun getWebDavUsername(): String = prefs.getString(KEY_WEBDAV_USER, "") ?: ""

    fun setWebDavUsername(user: String) {
        prefs.edit().putString(KEY_WEBDAV_USER, user.trim()).apply()
    }

    fun getWebDavPassword(): String = prefs.getString(KEY_WEBDAV_PASS, "") ?: ""

    fun setWebDavPassword(pass: String) {
        prefs.edit().putString(KEY_WEBDAV_PASS, pass).apply()
    }

    // ==================== 简易库存模式 (Simplified Mode) ====================

    fun isSimpleMode(): Boolean = prefs.getBoolean(KEY_SIMPLE_MODE, false)

    fun setSimpleMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SIMPLE_MODE, enabled).apply()
    }

    // ==================== 📸 截图无感自动收纳与 OCR ====================

    fun isScreenshotCaptureEnabled(): Boolean = prefs.getBoolean("screenshot_auto_capture_enabled", false)

    fun setScreenshotCaptureEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("screenshot_auto_capture_enabled", enabled).apply()
    }
}
