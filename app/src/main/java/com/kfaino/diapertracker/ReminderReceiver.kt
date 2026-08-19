package com.kfaino.diapertracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 接收定时闹钟广播与开机广播，触发资产提醒扫描与重新调度
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                // 开机后重新调度定时闹钟
                NotificationHelper.scheduleDailyReminder(context)
            }
            NotificationHelper.ACTION_CHECK_REMINDERS -> {
                // 执行每日例行提醒核验与通知推送
                NotificationHelper.checkAndSendReminders(context)
                // 再次调度下一次闹钟
                NotificationHelper.scheduleDailyReminder(context)
            }
        }
    }
}
