package com.kfaino.diapertracker

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews

/**
 * 桌面小组件 3：12 馆综合时效预警（2×2 格子）
 * - 聚合全部 12 个专业收纳馆最紧迫的临期/待处理事项
 * - 点击跳转主 App（MainActivity）
 * - 依赖 VaultAlertAggregator 统一聚合逻辑
 */
class VaultAlertWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, id)
        }
    }

    companion object {
        private const val TAG = "VaultAlertWidget"

        fun updateAllWidgets(context: Context) {
            try {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(
                    ComponentName(context, VaultAlertWidgetProvider::class.java)
                )
                for (id in ids) updateAppWidget(context, manager, id)
            } catch (e: Exception) {
                android.util.Log.w(TAG, "刷新 VaultAlert 桌面小组件失败", e)
            }
        }

        private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            try {
                val store = DataStore(context)
                val alerts = VaultAlertAggregator.getUrgentAlerts(context, store)
                val views = RemoteViews(context.packageName, R.layout.widget_vault_alert)

                // 点击跳转主 App
                val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                if (launchIntent != null) {
                    val pendingIntent = PendingIntent.getActivity(
                        context, 0, launchIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.tv_widget_vault_alert_title, pendingIntent)
                }

                val alertViewIds = listOf(
                    R.id.tv_vault_alert_1,
                    R.id.tv_vault_alert_2,
                    R.id.tv_vault_alert_3,
                    R.id.tv_vault_alert_4
                )

                if (alerts.isEmpty()) {
                    alertViewIds.forEach { views.setViewVisibility(it, View.GONE) }
                    views.setViewVisibility(R.id.tv_vault_alert_more, View.GONE)
                    views.setViewVisibility(R.id.tv_vault_alert_empty, View.VISIBLE)
                } else {
                    views.setViewVisibility(R.id.tv_vault_alert_empty, View.GONE)
                    val display = alerts.take(4)
                    for (i in alertViewIds.indices) {
                        if (i < display.size) {
                            views.setTextViewText(alertViewIds[i], "${display[i].emoji} ${display[i].label}")
                            views.setViewVisibility(alertViewIds[i], View.VISIBLE)
                        } else {
                            views.setViewVisibility(alertViewIds[i], View.GONE)
                        }
                    }
                    if (alerts.size > 4) {
                        views.setTextViewText(R.id.tv_vault_alert_more, "+ ${alerts.size - 4} 项更多待处理")
                        views.setViewVisibility(R.id.tv_vault_alert_more, View.VISIBLE)
                    } else {
                        views.setViewVisibility(R.id.tv_vault_alert_more, View.GONE)
                    }
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                android.util.Log.w(TAG, "更新单个 VaultAlert Widget 失败: appWidgetId=$appWidgetId", e)
            }
        }
    }
}
