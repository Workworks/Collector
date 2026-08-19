package com.kfaino.diapertracker

import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * 手把手全景互动引导教学控制器 (Interactive Step-by-Step Guided Tour)
 * - 贯穿 7 大核心业务流程
 * - 结合 GuideOverlayView 实现严格防误触与聚光灯聚焦
 * - 教学期间严格屏蔽其他无关按钮点击，支持随时中断退出与完成祝贺
 */
object InteractiveGuideTour {

    private var overlayView: GuideOverlayView? = null
    private var currentStep = 1
    private const val TOTAL_STEPS = 7

    fun startTour(activity: MainActivity) {
        currentStep = 1
        val decorView = activity.window.decorView as ViewGroup

        // 如果已有遮罩先移除
        overlayView?.let { decorView.removeView(it) }

        val overlay = GuideOverlayView(activity).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        overlayView = overlay
        decorView.addView(overlay)

        executeStep(activity, currentStep)
    }

    fun stopTour(activity: MainActivity) {
        val decorView = activity.window.decorView as ViewGroup
        overlayView?.let {
            decorView.removeView(it)
            overlayView = null
        }
        Toast.makeText(activity, "已退出手把手教学", Toast.LENGTH_SHORT).show()
    }

    private fun executeStep(activity: MainActivity, step: Int) {
        currentStep = step
        val overlay = overlayView ?: return

        when (step) {
            1 -> {
                // Step 1: 首页资产大盘与双模态切换
                activity.navigateToTab(0)
                activity.binding.root.postDelayed({
                    val target = activity.findViewById<View>(R.id.tab_subs) ?: activity.binding.navHome
                    overlay.showStep(
                        stepIndex = 1,
                        totalSteps = TOTAL_STEPS,
                        title = "🏠 资产大盘与双模态资产池",
                        desc = "这里是你的资产全景大盘！实时计算在役总估值与每一件物品的日均使用消费（元/天）。顶部可以随时切换「物品」与「周期订阅」两大资产池。",
                        actionHint = "👉 请点击高亮的「周期订阅」体验切换，或点击下一步",
                        targetView = target,
                        onNext = { executeStep(activity, 2) },
                        onExit = { stopTour(activity) },
                        onTargetClick = {
                            Handler(Looper.getMainLooper()).postDelayed({ executeStep(activity, 2) }, 600)
                        }
                    )
                }, 200)
            }

            2 -> {
                // Step 2: 核心记账入口 '+'
                val target = activity.binding.fabAdd
                overlay.showStep(
                    stepIndex = 2,
                    totalSteps = TOTAL_STEPS,
                    title = "➕ 极速记账与资产录入",
                    desc = "无论你在哪个页面，点击底部中央发光的「+」按钮，就能一秒弹出记账窗口，支持输入单价、数量、4维管理分类、实物照片与折旧计算。",
                    actionHint = "👉 请点击下方的「+」按钮体验，或点击下一步",
                    targetView = target,
                    onNext = { executeStep(activity, 3) },
                    onPrev = { executeStep(activity, 1) },
                    onExit = { stopTour(activity) },
                    onTargetClick = {
                        Handler(Looper.getMainLooper()).postDelayed({ executeStep(activity, 3) }, 800)
                    }
                )
            }

            3 -> {
                // Step 3: 首页顶栏「扫一扫」
                activity.navigateToTab(0)
                activity.binding.root.postDelayed({
                    val target = activity.findViewById<View>(R.id.btn_scan_qr_top)
                    overlay.showStep(
                        stepIndex = 3,
                        totalSteps = TOTAL_STEPS,
                        title = "📷 智能扫一扫 (收纳箱与商品条码)",
                        desc = "离线扫描收纳箱二维码标签，秒级滑出箱内所有物品清单！扫描商品条形码（EAN-13 等）自动匹配在库资产或极速快速录入。",
                        actionHint = "👉 请点击顶栏「📷 扫一扫」按钮体验，或点击下一步",
                        targetView = target,
                        onNext = { executeStep(activity, 4) },
                        onPrev = { executeStep(activity, 2) },
                        onExit = { stopTour(activity) },
                        onTargetClick = {
                            Handler(Looper.getMainLooper()).postDelayed({ executeStep(activity, 4) }, 800)
                        }
                    )
                }, 200)
            }

            4 -> {
                // Step 4: 空间平面图与寻物地图
                activity.navigateToTab(3) // 先切到「我的」
                activity.binding.root.postDelayed({
                    val target = activity.findViewById<View>(R.id.btn_floorplan_manage)
                    overlay.showStep(
                        stepIndex = 4,
                        totalSteps = TOTAL_STEPS,
                        title = "🗺️ 空间平面图与寻物双向穿梭",
                        desc = "可视化配置房间网格！由物找空间：首页点击位置即可弹出平面图并在对应坐标闪烁金色呼吸光晕；由空间看物品：轻触房间滑出在库物品抽屉，还能一键生成收纳箱二维码标签打印贴箱！",
                        actionHint = "👉 请点击「空间平面图」选项体验，或点击下一步",
                        targetView = target,
                        onNext = { executeStep(activity, 5) },
                        onPrev = { executeStep(activity, 3) },
                        onExit = { stopTour(activity) },
                        onTargetClick = {
                            Handler(Looper.getMainLooper()).postDelayed({ executeStep(activity, 5) }, 800)
                        }
                    )
                }, 250)
            }

            5 -> {
                // Step 5: 报表中心与动态分类环形图
                activity.navigateToTab(2) // 切到「报表」
                activity.binding.root.postDelayed({
                    val target = activity.findViewById<View>(R.id.overview_donut_chart) ?: activity.binding.navReport
                    overlay.showStep(
                        stepIndex = 5,
                        totalSteps = TOTAL_STEPS,
                        title = "📊 动态环形图与资产分类分布",
                        desc = "报表中心采用原生矢量 Canvas 绘制分类资产占比环形图，触碰任意扇区即可放大高亮，中心联动展示该分类金额与占比。",
                        actionHint = "👉 请查看动态环形图，或点击下一步",
                        targetView = target,
                        onNext = { executeStep(activity, 6) },
                        onPrev = { executeStep(activity, 4) },
                        onExit = { stopTour(activity) }
                    )
                }, 250)
            }

            6 -> {
                // Step 6: 闲置资产与断舍离健康雷达
                activity.navigateToTab(2)
                activity.binding.root.postDelayed({
                    val target = activity.findViewById<View>(R.id.card_asset_health)
                    overlay.showStep(
                        stepIndex = 6,
                        totalSteps = TOTAL_STEPS,
                        title = "🧹 闲置资产与断舍离健康雷达",
                        desc = "系统通过 100 分制智能评估资产流转健康度！雷达自动挖掘超过 180 天未打卡或临期闲置物品，支持一键确认在位或一键挂闲鱼/转转代售回血。",
                        actionHint = "👉 请查看闲置雷达，或点击下一步",
                        targetView = target,
                        onNext = { executeStep(activity, 7) },
                        onPrev = { executeStep(activity, 5) },
                        onExit = { stopTour(activity) }
                    )
                }, 200)
            }

            7 -> {
                // Step 7: 生物识别隐私锁与 WebDAV 私有云同步
                activity.navigateToTab(3)
                activity.binding.root.postDelayed({
                    val target = activity.findViewById<View>(R.id.btn_more_settings)
                    overlay.showStep(
                        stepIndex = 7,
                        totalSteps = TOTAL_STEPS,
                        title = "🔐 隐私锁、云同步与桌面小组件",
                        desc = "在更多设置中可一键开启指纹/面容隐私锁，保护贵重物品位置；配置坚果云/Nextcloud 进行 WebDAV 静默云备份；长按手机桌面还可添加临期预警与资产小组件！",
                        actionHint = "👉 恭喜！你已完整了解所有功能，点击完成教学！",
                        targetView = target,
                        onNext = {
                            finishTour(activity)
                        },
                        onPrev = { executeStep(activity, 6) },
                        onExit = { stopTour(activity) }
                    )
                }, 200)
            }
        }
    }

    private fun finishTour(activity: MainActivity) {
        val decorView = activity.window.decorView as ViewGroup
        overlayView?.let {
            decorView.removeView(it)
            overlayView = null
        }

        MaterialAlertDialogBuilder(activity)
            .setTitle("🎉 恭喜完成手把手教学！")
            .setMessage("你已经完全掌握了 Collecter 的全部核心功能（空间寻物、收纳扫码、折旧算法、断舍离雷达、桌面小组件与云同步）。\n\n随时可以在「我的 ➔ 功能全景与使用教程」中重温教程！")
            .setPositiveButton("🚀 开始畅享使用", null)
            .show()
    }
}
