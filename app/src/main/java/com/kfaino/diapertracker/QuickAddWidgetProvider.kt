package com.kfaino.diapertracker

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.util.Locale

/**
 * 桌面小组件 2：资产概览与快捷记一笔小组件
 */
class QuickAddWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, QuickAddWidgetProvider::class.java))
            for (id in ids) {
                updateAppWidget(context, appWidgetManager, id)
            }
        }

        private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val store = DataStore(context)
            val entries = store.loadAll()
            val views = RemoteViews(context.packageName, R.layout.widget_quick_add)

            // 点击卡片进入主页
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingMainIntent = PendingIntent.getActivity(
                context,
                101,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_quick_root, pendingMainIntent)

            // 点击「+」按钮直接唤起记账弹窗
            val addIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("action_open_add_dialog", true)
            }
            val pendingAddIntent = PendingIntent.getActivity(
                context,
                102,
                addIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_quick_add, pendingAddIntent)

            // 计算在役资产净值与日均总成本
            val activeItems = entries.filter { !it.isRetired && !it.isSubscription }
            val totalWorth = activeItems.filter { it.isIn }.sumOf { it.price * it.qty }
            val totalDaily = activeItems.sumOf { it.getDailyCost() }

            views.setTextViewText(R.id.widget_total_worth, "¥${String.format(Locale.getDefault(), "%,.0f", totalWorth)}")
            views.setTextViewText(R.id.widget_daily_cost, "¥${String.format(Locale.getDefault(), "%.2f", totalDaily)}/天")

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
