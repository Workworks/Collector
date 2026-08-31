package com.kfaino.diapertracker

import android.app.Activity
import android.content.res.Configuration

/**
 * 📱 Android 大屏 / 平板与折叠屏双栏响应式分流助手 (Tablet Layout Helper)
 */
object TabletLayoutHelper {

    fun isTabletOrExpandedScreen(activity: Activity): Boolean {
        val config = activity.resources.configuration
        val screenWidthDp = config.screenWidthDp
        return screenWidthDp >= 600 || (config.orientation == Configuration.ORIENTATION_LANDSCAPE && screenWidthDp >= 520)
    }

    fun getOptimalColumnCount(activity: Activity): Int {
        val widthDp = activity.resources.configuration.screenWidthDp
        return when {
            widthDp >= 900 -> 3
            widthDp >= 600 -> 2
            else -> 1
        }
    }
}