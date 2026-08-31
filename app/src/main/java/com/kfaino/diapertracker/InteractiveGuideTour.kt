package com.kfaino.diapertracker

import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast

/**
 * 🎯 手把手全景互动引导教学与全功能单项跟手演练控制器 (Interactive Step-by-Step Guided Tour)
 * - 支持全景贯穿大漫游教学 (8 步引导)
 * - 支持全部细分功能点的单项手把手跟手演练 (委托 SingleFeatureTours)
 * - 结合 GuideOverlayView 实现像素级聚光灯镂空与呼吸光晕
 * - 教学期间严格屏蔽非引导控件误触，支持随时点击「✕ 退出教程」中断退出与完成打卡
 */
object InteractiveGuideTour {

    private var overlayView: GuideOverlayView? = null
    private var currentStep = 1
    private var activeTourKey: String? = null

    // 教程演示沙盒示例追踪与自动清理机制 (委托至 TourSandbox)
    fun registerDemoEntry(id: String) = TourSandbox.registerDemoEntry(id)
    fun registerDemoVoucher(id: String) = TourSandbox.registerDemoVoucher(id)
    fun registerDemoIdentityDoc(id: String) = TourSandbox.registerDemoIdentityDoc(id)
    fun registerDemoMedicine(id: String) = TourSandbox.registerDemoMedicine(id)
    fun registerDemoFood(id: String) = TourSandbox.registerDemoFood(id)
    fun registerDemoHonor(id: String) = TourSandbox.registerDemoHonor(id)

    /** 启动全景连续手把手教学 */
    fun startTour(activity: MainActivity) {
        activeTourKey = "full"
        currentStep = 1
        ensureOverlay(activity)
        executeFullStep(activity, currentStep)
    }

    /** 启动针对某一个特定功能点的单项手把手跟手演练教程 (中途随时可退出) */
    fun startFeatureTour(activity: MainActivity, featureKey: String) {
        activeTourKey = featureKey
        val overlay = ensureOverlay(activity)
        SingleFeatureTours.execute(
            activity = activity,
            featureKey = featureKey,
            overlay = overlay,
            onStop = { stopTour(activity) },
            fallbackTour = { startTour(activity) }
        )
    }

    /** 随时中断退出手把手教程 */
    fun stopTour(activity: MainActivity) {
        val decorView = activity.window.decorView as ViewGroup
        overlayView?.let {
            decorView.removeView(it)
            overlayView = null
        }
        activeTourKey = null
        TourSandbox.cleanup(activity)
        Toast.makeText(activity, "已退出手把手教学", Toast.LENGTH_SHORT).show()
    }

    private fun ensureOverlay(activity: MainActivity): GuideOverlayView {
        val decorView = activity.window.decorView as ViewGroup
        overlayView?.let { decorView.removeView(it) }

        val overlay = GuideOverlayView(activity).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        overlayView = overlay
        decorView.addView(overlay)
        return overlay
    }

    private fun executeFullStep(activity: MainActivity, step: Int) {
        currentStep = step
        val overlay = overlayView ?: return
        val totalSteps = 8

        when (step) {
            1 -> {
                activity.navigateToTab(0)
                activity.binding.root.postDelayed({
                    val target = activity.findViewById<View>(R.id.tab_subs) ?: activity.binding.navHome
                    overlay.showStep(
                        stepIndex = 1,
                        totalSteps = totalSteps,
                        title = "🏠 资产大盘与双模态资产池",
                        desc = "这里是你的资产全景大盘！实时计算在役总估值与每一件物品的日均使用消费（元/天）。顶部可以随时切换「物品」与「周期订阅」两大资产池。",
                        actionHint = "👉 请点击高亮的「周期订阅」体验切换，或点击下一步",
                        targetView = target,
                        onNext = { executeFullStep(activity, 2) },
                        onExit = { stopTour(activity) },
                        onTargetClick = {
                            Handler(Looper.getMainLooper()).postDelayed({ executeFullStep(activity, 2) }, 600)
                        }
                    )
                }, 200)
            }

            2 -> {
                val target = activity.binding.fabAdd
                overlay.showStep(
                    stepIndex = 2,
                    totalSteps = totalSteps,
                    title = "➕ 极速记账与 AI 免录助手",
                    desc = "点击底部中央「+」按钮，即可秒出记账面板，支持拍照发票 OCR、电商订单截图提取、自然语言一句话记账、4维分类与折旧估值计算。",
                    actionHint = "👉 请点击「+」按钮体验，或点击下一步",
                    targetView = target,
                    onNext = { executeFullStep(activity, 3) },
                    onPrev = { executeFullStep(activity, 1) },
                    onExit = { stopTour(activity) },
                    onTargetClick = {
                        Handler(Looper.getMainLooper()).postDelayed({ executeFullStep(activity, 3) }, 600)
                    }
                )
            }

            3 -> {
                activity.navigateToTab(0)
                activity.binding.root.postDelayed({
                    val toolsBar = activity.findViewById<View>(R.id.layout_tools_bar)
                    toolsBar?.visibility = View.VISIBLE
                    val target = activity.findViewById<View>(R.id.btn_ai_concierge_top) ?: activity.binding.navHome
                    overlay.showStep(
                        stepIndex = 3,
                        totalSteps = totalSteps,
                        title = "🤖 AI 资产智能管家与语义寻物",
                        desc = "支持一句话自然问答寻物（如：“我的相机电池放在哪？”）、资产财务穿透统计与临期风险追踪，端侧毫秒级解答！",
                        actionHint = "👉 请点击「🤖 智能管家」体验对话，或点击下一步",
                        targetView = target,
                        onNext = { executeFullStep(activity, 4) },
                        onPrev = { executeFullStep(activity, 2) },
                        onExit = { stopTour(activity) },
                        onTargetClick = {
                            Handler(Looper.getMainLooper()).postDelayed({ executeFullStep(activity, 4) }, 600)
                        }
                    )
                }, 200)
            }

            4 -> {
                activity.navigateToTab(0)
                activity.binding.root.postDelayed({
                    val toolsBar = activity.findViewById<View>(R.id.layout_tools_bar)
                    toolsBar?.visibility = View.VISIBLE
                    val target = activity.findViewById<View>(R.id.btn_kits_top) ?: activity.binding.navHome
                    overlay.showStep(
                        stepIndex = 4,
                        totalSteps = totalSteps,
                        title = "🎒 场景装备套装与打包核对模式",
                        desc = "自由组装外拍/出差/露营装备包，出行前一键开启打包核对防漏模式，支持动态装箱进度环与打卡确认！",
                        actionHint = "👉 请查看「🎒 装备套装」，或点击下一步",
                        targetView = target,
                        onNext = { executeFullStep(activity, 5) },
                        onPrev = { executeFullStep(activity, 3) },
                        onExit = { stopTour(activity) }
                    )
                }, 200)
            }

            5 -> {
                activity.navigateToTab(0)
                activity.binding.root.postDelayed({
                    val toolsBar = activity.findViewById<View>(R.id.layout_tools_bar)
                    toolsBar?.visibility = View.VISIBLE
                    val target = activity.findViewById<View>(R.id.btn_open_floorplan_top) ?: activity.binding.navHome
                    overlay.showStep(
                        stepIndex = 5,
                        totalSteps = totalSteps,
                        title = "🗺️ 空间平面图与全屋寻物",
                        desc = "可视化配置房间网格！由物找空间：点击物品位置在对应坐标闪烁金色呼吸光晕；由空间看物品：轻触房间滑出箱内清单与二维码！",
                        actionHint = "👉 请查看「空间地图」，或点击下一步",
                        targetView = target,
                        onNext = { executeFullStep(activity, 6) },
                        onPrev = { executeFullStep(activity, 4) },
                        onExit = { stopTour(activity) }
                    )
                }, 200)
            }

            6 -> {
                activity.navigateToTab(2) // 报表
                activity.binding.root.postDelayed({
                    val target = activity.findViewById<View>(R.id.overview_donut_chart) ?: activity.binding.navReport
                    overlay.showStep(
                        stepIndex = 6,
                        totalSteps = totalSteps,
                        title = "📊 动态环形图与断舍离健康雷达",
                        desc = "原生矢量 Canvas 环形图动态透视分类占比；闲置雷达自动挖掘超过 180 天未打卡物品，一键确认在位或挂闲鱼代售回血！",
                        actionHint = "👉 请查看报表中心，或点击下一步",
                        targetView = target,
                        onNext = { executeFullStep(activity, 7) },
                        onPrev = { executeFullStep(activity, 5) },
                        onExit = { stopTour(activity) }
                    )
                }, 250)
            }

            7 -> {
                activity.navigateToTab(3) // 我的
                activity.binding.root.postDelayed({
                    val target = activity.findViewById<View>(R.id.btn_more_settings) ?: activity.binding.navProfile
                    overlay.showStep(
                        stepIndex = 7,
                        totalSteps = totalSteps,
                        title = "🔐 隐私锁、WebDAV 云同步与桌面组件",
                        desc = "一键开启硬件级指纹/面容隐私锁；配置坚果云/Nextcloud 进行 WebDAV 静默云备份；长按手机桌面还可添加临期预警与资产小组件！",
                        actionHint = "👉 点击「更多设置」查看配置，或点击下一步",
                        targetView = target,
                        onNext = { executeFullStep(activity, 8) },
                        onPrev = { executeFullStep(activity, 6) },
                        onExit = { stopTour(activity) }
                    )
                }, 250)
            }

            8 -> {
                activity.navigateToTab(3)
                activity.binding.root.postDelayed({
                    val target = activity.findViewById<View>(R.id.btn_update_version) ?: activity.binding.navProfile
                    overlay.showStep(
                        stepIndex = 8,
                        totalSteps = totalSteps,
                        title = "🚀 检查与智能升级系统",
                        desc = "统一智能检测最新版本与增量热补丁！优先支持 ⚡ 免重装极速热更，秒级即时生效，必要时全量升级！",
                        actionHint = "👉 恭喜！您已完整掌握全部核心功能，点击完成教学！",
                        targetView = target,
                        onNext = { finishTour(activity) },
                        onPrev = { executeFullStep(activity, 7) },
                        onExit = { stopTour(activity) }
                    )
                }, 250)
            }
        }
    }

    private fun finishTour(activity: MainActivity) {
        val decorView = activity.window.decorView as ViewGroup
        overlayView?.let {
            decorView.removeView(it)
            overlayView = null
        }
        activeTourKey = null
        TourSandbox.cleanup(activity)

        ModernDialogHelper.showInfoDialog(
            context = activity,
            title = "🎉 恭喜完成全景手把手教学！",
            emoji = "🏆",
            message = "您已经完全掌握了 Collecter 的全部核心功能（AI智能管家、装备套装核对、空间寻物、收纳扫码、折旧算法、断舍离雷达、Web大屏、桌面组件与热更新）。\n\n随时可以在「我的 ➔ 功能全景与使用教程」中点击任意功能卡片随时重温跟手演练！",
            buttonText = "🚀 开始畅享使用"
        )
    }
}
