package com.kfaino.diapertracker

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 桌面小组件 1：临期保质期与周期订阅预警小组件
 */
class ExpiringAndSubWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, ExpiringAndSubWidgetProvider::class.java))
            for (id in ids) {
                updateAppWidget(context, appWidgetManager, id)
            }
        }

        private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val store = DataStore(context)
            val entries = store.loadAll()
            val views = RemoteViews(context.packageName, R.layout.widget_expiring_sub)

            // 点击整个小组件跳转至应用首页
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            // 1. 获取最近过期的在役物品
            val nowMs = System.currentTimeMillis()
            val expiringItems = entries.filter {
                !it.isRetired && it.assetType == "expiring" && it.expiryDate > 0
            }.sortedBy { it.expiryDate }

            val nearestExpiring = expiringItems.firstOrNull()
            if (nearestExpiring != null) {
                val diffDays = ((nearestExpiring.expiryDate - nowMs) / (24L * 60 * 60 * 1000)).toInt()
                views.setTextViewText(R.id.widget_expiring_item_name, "⏳ ${nearestExpiring.brand}")
                if (diffDays < 0) {
                    views.setTextViewText(R.id.widget_expiring_days_badge, "已过期 ${-diffDays}天")
                } else if (diffDays <= 7) {
                    views.setTextViewText(R.id.widget_expiring_days_badge, "剩 ${diffDays}天")
                } else {
                    views.setTextViewText(R.id.widget_expiring_days_badge, "剩 ${diffDays}天")
                }
            } else {
                views.setTextViewText(R.id.widget_expiring_item_name, "⏳ 暂无临期物品")
                views.setTextViewText(R.id.widget_expiring_days_badge, "安全")
            }

            // 2. 获取最近要扣费的周期订阅
            val subs = entries.filter { it.isSubscription && it.subNextBillingDate > 0 }.sortedBy { it.subNextBillingDate }
            val nearestSub = subs.firstOrNull()
            if (nearestSub != null) {
                val diffDays = ((nearestSub.subNextBillingDate - nowMs) / (24L * 60 * 60 * 1000)).toInt()
                views.setTextViewText(R.id.widget_sub_name, "🔄 ${nearestSub.brand}")
                views.setTextViewText(R.id.widget_sub_days_badge, if (diffDays <= 0) "今日扣费" else "${diffDays}天后扣费")
            } else {
                views.setTextViewText(R.id.widget_sub_name, "🔄 暂无近期扣费订阅")
                views.setTextViewText(R.id.widget_sub_days_badge, "无")
            }

            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            views.setTextViewText(R.id.widget_update_time, timeStr)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
