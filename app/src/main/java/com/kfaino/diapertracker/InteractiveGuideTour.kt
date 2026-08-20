package com.kfaino.diapertracker

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * 🎯 手把手全景互动引导教学与各功能单项跟手演练控制器 (Interactive Step-by-Step Guided Tour)
 * - 支持全景贯穿大漫游教学
 * - 支持全部 22+ 个功能点的单项手把手跟手演练
 * - 结合 GuideOverlayView 实现像素级聚光灯镂空与呼吸光晕
 * - 教学期间严格屏蔽非引导控件误触，支持随时点击「✕ 退出教程」中断退出与完成打卡
 */
object InteractiveGuideTour {

    private var overlayView: GuideOverlayView? = null
    private var currentStep = 1
    private var activeTourKey: String? = null

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
        ensureOverlay(activity)
        executeFeatureTour(activity, featureKey)
    }

    /** 随时中断退出手把手教程 */
    fun stopTour(activity: MainActivity) {
        val decorView = activity.window.decorView as ViewGroup
        overlayView?.let {
            decorView.removeView(it)
            overlayView = null
        }
        activeTourKey = null
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

    private fun executeFeatureTour(activity: MainActivity, featureKey: String) {
        val overlay = overlayView ?: return
        val store = DataStore(activity)

        when (featureKey) {
            "ai_concierge" -> {
                activity.navigateToTab(0)
                activity.binding.root.postDelayed({
                    val toolsBar = activity.findViewById<View>(R.id.layout_tools_bar)
                    toolsBar?.visibility = View.VISIBLE
                    val target = activity.findViewById<View>(R.id.btn_ai_concierge_top) ?: activity.binding.navHome
                    overlay.showStep(
                        stepIndex = 1,
                        totalSteps = 1,
                        title = "🤖 AI 资产智能管家与语义寻物",
                        desc = "点击上方高亮的「🤖 智能管家」，您可以直接使用自然语言寻物（例如：“我的充电线放在哪？”）或询问“数码设备总价值多少钱？”、“哪些耗材快用完了？”，AI 端侧毫秒级解答！",
                        actionHint = "👉 请点击高亮的「🤖 智能管家」体验对话，或点击完成",
                        targetView = target,
                        onNext = {
                            stopTour(activity)
                            AiConciergeHelper.showConciergeDialog(activity, store)
                        },
                        onExit = { stopTour(activity) },
                        onTargetClick = {
                            Handler(Looper.getMainLooper()).postDelayed({
                                stopTour(activity)
                                AiConciergeHelper.showConciergeDialog(activity, store)
                            }, 300)
                        }
                    )
                }, 200)
            }

            "kit_manager" -> {
                activity.navigateToTab(0)
                activity.binding.root.postDelayed({
                    val toolsBar = activity.findViewById<View>(R.id.layout_tools_bar)
                    toolsBar?.visibility = View.VISIBLE
                    val target = activity.findViewById<View>(R.id.btn_kits_top) ?: activity.binding.navHome
                    overlay.showStep(
                        stepIndex = 1,
                        totalSteps = 1,
                        title = "🎒 场景装备套装与打包核对",
                        desc = "点击「🎒 装备套装」，您可以自由组装“摄影外拍包”、“3天出差包”或“露营包”。出行前一键开启装箱核对模式，显示动态进度环逐项装箱防漏！",
                        actionHint = "👉 请点击「🎒 装备套装」开始管理或核对",
                        targetView = target,
                        onNext = {
                            stopTour(activity)
                            KitManager.showKitListDialog(activity, store)
                        },
                        onExit = { stopTour(activity) },
                        onTargetClick = {
                            Handler(Looper.getMainLooper()).postDelayed({
                                stopTour(activity)
                                KitManager.showKitListDialog(activity, store)
                            }, 300)
                        }
                    )
                }, 200)
            }

            "ecommerce" -> {
                activity.navigateToTab(0)
                activity.binding.root.postDelayed({
                    val target = activity.binding.fabAdd
                    overlay.showStep(
                        stepIndex = 1,
                        totalSteps = 1,
                        title = "🛒 电商订单截图 OCR 与淘口令无感桥接",
                        desc = "1. 截图识物：在记账窗口点击「⚡ 拍照/选图识物」，上传淘宝或京东订单截图，AI 自动提取品名单价与规格；\n2. 剪贴板桥接：在淘宝/京东复制分享链接，切回 Collecter 自动弹出智能入库提示！",
                        actionHint = "👉 点击「+」打开记账窗口体验电商 OCR 识图",
                        targetView = target,
                        onNext = {
                            stopTour(activity)
                            activity.showAddDialog()
                        },
                        onExit = { stopTour(activity) },
                        onTargetClick = {
                            Handler(Looper.getMainLooper()).postDelayed({
                                stopTour(activity)
                            }, 300)
                        }
                    )
                }, 200)
            }

            "floorplan" -> {
                activity.navigateToTab(0)
                activity.binding.root.postDelayed({
                    val toolsBar = activity.findViewById<View>(R.id.layout_tools_bar)
                    toolsBar?.visibility = View.VISIBLE
                    val target = activity.findViewById<View>(R.id.btn_open_floorplan_top) ?: activity.binding.navHome
                    overlay.showStep(
                        stepIndex = 1,
                        totalSteps = 1,
                        title = "🗺️ 空间平面图与寻物地图",
                        desc = "进入空间平面图，可视化俯瞰全屋房间布局。点击任意房间滑出箱内清单，长按房间可自定义图标，支持双向穿梭与金色呼吸光晕精准寻物！",
                        actionHint = "👉 请点击「空间地图」直达空间画布",
                        targetView = target,
                        onNext = {
                            stopTour(activity)
                            FloorPlanDialog.show(activity, store, isSelectMode = false)
                        },
                        onExit = { stopTour(activity) },
                        onTargetClick = {
                            Handler(Looper.getMainLooper()).postDelayed({
                                stopTour(activity)
                                FloorPlanDialog.show(activity, store, isSelectMode = false)
                            }, 300)
                        }
                    )
                }, 200)
            }

            "qr_scan" -> {
                activity.navigateToTab(0)
                activity.binding.root.postDelayed({
                    val toolsBar = activity.findViewById<View>(R.id.layout_tools_bar)
                    toolsBar?.visibility = View.VISIBLE
                    val target = activity.findViewById<View>(R.id.btn_scan_qr_top) ?: activity.binding.navHome
                    overlay.showStep(
                        stepIndex = 1,
                        totalSteps = 1,
                        title = "📷 智能扫一扫 (收纳箱与商品条码)",
                        desc = "点击「扫码寻物」，对准贴在收纳箱上的二维码标签，秒级识别并滑出箱内全部在库物品；扫描商品条形码自动匹配或新建记账！",
                        actionHint = "👉 请点击「扫码寻物」启动离线扫码",
                        targetView = target,
                        onNext = {
                            stopTour(activity)
                            activity.startQrScanner()
                        },
                        onExit = { stopTour(activity) },
                        onTargetClick = {
                            Handler(Looper.getMainLooper()).postDelayed({
                                stopTour(activity)
                            }, 300)
                        }
                    )
                }, 200)
            }

            "audit" -> {
                activity.navigateToTab(0)
                activity.binding.root.postDelayed({
                    val toolsBar = activity.findViewById<View>(R.id.layout_tools_bar)
                    toolsBar?.visibility = View.VISIBLE
                    val target = activity.findViewById<View>(R.id.btn_inventory_audit_top) ?: activity.binding.navHome
                    overlay.showStep(
                        stepIndex = 1,
                        totalSteps = 1,
                        title = "📋 空间实物沉浸式大盘点",
                        desc = "开启实物巡检盲盘模式，逐项核对在位情况，自动生成《实物资产盘点报告》（完全吻合数、数量调整、缺失清单），一键刷新打卡时间！",
                        actionHint = "👉 请点击「实物盘点」开启沉浸巡检",
                        targetView = target,
                        onNext = {
                            stopTour(activity)
                            InventoryAuditDialog.startAudit(activity, store) { activity.refreshCurrentFragment() }
                        },
                        onExit = { stopTour(activity) },
                        onTargetClick = {
                            Handler(Looper.getMainLooper()).postDelayed({
                                stopTour(activity)
                                InventoryAuditDialog.startAudit(activity, store) { activity.refreshCurrentFragment() }
                            }, 300)
                        }
                    )
                }, 200)
            }

            "lan_sync" -> {
                activity.navigateToTab(0)
                activity.binding.root.postDelayed({
                    val toolsBar = activity.findViewById<View>(R.id.layout_tools_bar)
                    toolsBar?.visibility = View.VISIBLE
                    val target = activity.findViewById<View>(R.id.btn_lan_sync_top) ?: activity.binding.navHome
                    overlay.showStep(
                        stepIndex = 1,
                        totalSteps = 1,
                        title = "🌐 局域网免装 Web 网页大屏控制台",
                        desc = "点击启动局域网微服务，电脑或 iPad 浏览器直接输入提示的 IP 网址（8848端口），即可进入黑曜石大屏控制台，支持大屏表格、批量操作与电商导入！",
                        actionHint = "👉 请点击「局域网大屏」启动微服务",
                        targetView = target,
                        onNext = {
                            stopTour(activity)
                            LanSyncHelper.showLanSyncDialog(activity, store) { activity.refreshCurrentFragment() }
                        },
                        onExit = { stopTour(activity) },
                        onTargetClick = {
                            Handler(Looper.getMainLooper()).postDelayed({
                                stopTour(activity)
                                LanSyncHelper.showLanSyncDialog(activity, store) { activity.refreshCurrentFragment() }
                            }, 300)
                        }
                    )
                }, 200)
            }

            "ledgers" -> {
                activity.navigateToTab(0)
                activity.binding.root.postDelayed({
                    val target = activity.findViewById<View>(R.id.layout_ledger_switcher) ?: activity.binding.navHome
                    overlay.showStep(
                        stepIndex = 1,
                        totalSteps = 1,
                        title = "📚 多账本独立空间与资产隔离",
                        desc = "点击左上角账本标题，可以在「个人资产」、「家庭仓储」、「公司办公物资」之间秒速切换，各账本流水与统计完全独立核算！",
                        actionHint = "👉 请点击账本名称切换或新建账本",
                        targetView = target,
                        onNext = {
                            stopTour(activity)
                            LedgerManager.showLedgerPicker(activity) { activity.refreshCurrentFragment() }
                        },
                        onExit = { stopTour(activity) },
                        onTargetClick = {
                            Handler(Looper.getMainLooper()).postDelayed({
                                stopTour(activity)
                                LedgerManager.showLedgerPicker(activity) { activity.refreshCurrentFragment() }
                            }, 300)
                        }
                    )
                }, 200)
            }

            "chart_radar" -> {
                activity.navigateToTab(2) // 切到「报表」
                activity.binding.root.postDelayed({
                    val target = activity.findViewById<View>(R.id.overview_donut_chart) ?: activity.binding.navReport
                    overlay.showStep(
                        stepIndex = 1,
                        totalSteps = 1,
                        title = "📊 动态环形图与断舍离健康雷达",
                        desc = "轻触环形图扇区即可动态高亮分类占比；下滑查看「断舍离健康雷达」，自动识别 180 天未动闲置物品，一键确认在位或挂闲鱼代售回血！",
                        actionHint = "👉 点击扇区体验交互，或点击完成",
                        targetView = target,
                        onNext = { stopTour(activity) },
                        onExit = { stopTour(activity) }
                    )
                }, 250)
            }

            "privacy_sync" -> {
                activity.navigateToTab(3) // 切到「我的」
                activity.binding.root.postDelayed({
                    val target = activity.findViewById<View>(R.id.btn_more_settings) ?: activity.binding.navProfile
                    overlay.showStep(
                        stepIndex = 1,
                        totalSteps = 1,
                        title = "🔐 指纹面容隐私锁与 WebDAV 私有云同步",
                        desc = "在「更多设置」中可一键开启系统硬件级生物识别隐私锁，保护资产隐私；配置坚果云/Nextcloud 实现 WebDAV 静默云备份与换机恢复！",
                        actionHint = "👉 点击「更多设置」体验隐私与同步配置",
                        targetView = target,
                        onNext = { stopTour(activity) },
                        onExit = { stopTour(activity) },
                        onTargetClick = {
                            Handler(Looper.getMainLooper()).postDelayed({ stopTour(activity) }, 300)
                        }
                    )
                }, 250)
            }

            "hot_patch" -> {
                activity.navigateToTab(3)
                activity.binding.root.postDelayed({
                    val target = activity.findViewById<View>(R.id.btn_hot_patch_update) ?: activity.binding.navProfile
                    overlay.showStep(
                        stepIndex = 1,
                        totalSteps = 1,
                        title = "🎮 类似游戏极速热更新补丁",
                        desc = "无需每次下载几十 MB 全量安装包，仅下载几百 KB 增量补丁，底层沙盒引擎动态加载，免系统安装弹窗，瞬间无感生效体验最新特性！",
                        actionHint = "👉 请点击「极速热更新」检查可用补丁",
                        targetView = target,
                        onNext = {
                            stopTour(activity)
                            HotUpdateManager.checkHotPatch(activity) { info, err ->
                                if (info != null) {
                                    HotUpdateDialog.show(activity, info)
                                } else {
                                    Toast.makeText(activity, err ?: "当前已是最新状态，暂无可用热补丁", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onExit = { stopTour(activity) },
                        onTargetClick = {
                            Handler(Looper.getMainLooper()).postDelayed({
                                stopTour(activity)
                                HotUpdateManager.checkHotPatch(activity) { info, err ->
                                    if (info != null) {
                                        HotUpdateDialog.show(activity, info)
                                    } else {
                                        Toast.makeText(activity, err ?: "当前已是最新状态，暂无可用热补丁", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }, 300)
                        }
                    )
                }, 250)
            }

            else -> {
                startTour(activity)
            }
        }
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
                    val target = activity.findViewById<View>(R.id.btn_hot_patch_update) ?: activity.binding.navProfile
                    overlay.showStep(
                        stepIndex = 8,
                        totalSteps = totalSteps,
                        title = "🎮 极速热更新补丁系统",
                        desc = "告别漫长的大包下载，仅下载几百 KB 增量补丁，底层沙盒引擎动态加载，免系统安装弹窗，瞬间无感生效体验最新特性！",
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

        MaterialAlertDialogBuilder(activity)
            .setTitle("🎉 恭喜完成全景手把手教学！")
            .setMessage("您已经完全掌握了 Collecter 的全部核心功能（AI智能管家、装备套装核对、空间寻物、收纳扫码、折旧算法、断舍离雷达、Web大屏、桌面组件与热更新）。\n\n随时可以在「我的 ➔ 功能全景与使用教程」中点击任意功能卡片随时重温跟手演练！")
            .setPositiveButton("🚀 开始畅享使用", null)
            .show()
    }
}
