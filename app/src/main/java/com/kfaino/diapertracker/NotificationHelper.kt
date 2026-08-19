package com.kfaino.diapertracker

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.util.Calendar
import java.util.Locale

/**
 * 资产与订阅系统级通知提醒管理器
 */
object NotificationHelper {

    const val CHANNEL_ID = "collector_asset_reminders"
    const val ACTION_CHECK_REMINDERS = "com.kfaino.diapertracker.ACTION_CHECK_REMINDERS"

    /** 初始化通知渠道 (Android 8.0+) */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "资产与订阅提醒",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "周期订阅扣费预警、重要物品位置定期核对与保质期到期提醒"
                enableLights(true)
                enableVibration(true)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    /** 检查是否拥有发送通知权限 (Android 13+) */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /** 调度每日定时提醒 (通过 AlarmManager) */
    fun scheduleDailyReminder(context: Context) {
        val store = DataStore(context)
        if (!store.isNotificationEnabled()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_CHECK_REMINDERS
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(context, 1001, intent, flags)

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, store.getNotificationHour())
            set(Calendar.MINUTE, store.getNotificationMinute())
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                cal.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        } catch (_: Exception) {}
    }

    /** 执行提醒检查并推送通知 */
    fun checkAndSendReminders(context: Context): Int {
        createNotificationChannel(context)
        if (!hasNotificationPermission(context)) return 0

        val store = DataStore(context)
        if (!store.isNotificationEnabled()) return 0

        val entries = store.loadAll()
        val now = System.currentTimeMillis()
        var notificationCount = 0

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 1. 周期订阅扣费预警 (提前 3 天与当天提醒)
        for (sub in entries.filter { it.isSubscription }) {
            if (sub.subNextBillingDate > 0) {
                val diffMs = sub.subNextBillingDate - now
                val daysUntil = (diffMs / (24L * 60 * 60 * 1000)).toInt()
                if (daysUntil in 0..3) {
                    val timeDesc = if (daysUntil == 0) "今天" else if (daysUntil == 1) "明天" else "${daysUntil}天后"
                    val content = "【${sub.brand}】将于 $timeDesc 自动扣费 ¥${String.format(Locale.getDefault(), "%.2f", sub.price)}（${sub.subCycle}），请留意账户余额。"
                    sendNotification(
                        context,
                        nm,
                        id = sub.id.hashCode(),
                        title = "🔄 订阅扣费预警：${sub.brand}",
                        content = content
                    )
                    notificationCount++
                }
            }
        }

        // 2. VIP 重要贵重物品超期未核对提醒
        for (vip in entries.filter { (it.isImportant || it.reminderEnabled) && !it.isRetired }) {
            val lastCheck = if (vip.lastCheckedAt > 0) vip.lastCheckedAt else vip.ts
            val daysSince = ((now - lastCheck) / (24L * 60 * 60 * 1000)).toInt()
            if (daysSince >= vip.reminderIntervalDays) {
                val locDesc = if (vip.location.isNotBlank()) "【${vip.houseName} · ${vip.location}】" else "预定位置"
                val content = "您已超过 ${daysSince} 天未核对【${vip.brand}】，请打开应用确认物品是否仍在 $locDesc 处。"
                sendNotification(
                    context,
                    nm,
                    id = vip.id.hashCode(),
                    title = "🔑 重要物品核对提醒：${vip.brand}",
                    content = content
                )
                notificationCount++
            }
        }

        // 3. 临期保质期物品提醒 (提前 7 天)
        for (item in entries.filter { it.expiryDate > 0 && !it.isRetired }) {
            val diffMs = item.expiryDate - now
            val daysLeft = (diffMs / (24L * 60 * 60 * 1000)).toInt()
            if (daysLeft in 0..7) {
                val content = "【${item.brand}】还有 $daysLeft 天即将过期（${item.qty}${item.unit}），请尽快使用避免浪费。"
                sendNotification(
                    context,
                    nm,
                    id = item.id.hashCode() + 100,
                    title = "⏳ 物品临期提醒：${item.brand}",
                    content = content
                )
                notificationCount++
            }
        }

        return notificationCount
    }

    /** 触发单条通知构建与发送 */
    private fun sendNotification(
        context: Context,
        nm: NotificationManager,
        id: Int,
        title: String,
        content: String
    ) {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(context, id, launchIntent, flags)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(ContextCompat.getColor(context, R.color.primary))
            .build()

        nm.notify(id, notification)
    }

    /** 发送即时测试通知 */
    fun sendTestNotification(context: Context) {
        createNotificationChannel(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        sendNotification(
            context,
            nm,
            id = 9999,
            title = "🔔 Collecter 资产管家提醒测试",
            content = "通知通道已成功连通！系统将在每日指定时间自动为您监测订阅扣费、重要物品核对与物品保质期。"
        )
    }
}
