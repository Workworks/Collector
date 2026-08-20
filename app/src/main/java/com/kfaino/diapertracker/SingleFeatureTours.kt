package com.kfaino.diapertracker

import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast

/**
 * 🎯 28 个单项功能点手把手跟手演练分发器 (Single-Feature Guided Tours Dispatcher)
 * 支持中途随时安全退出，并在退出时自动回滚清理沙盒演示数据。
 */
object SingleFeatureTours {

    fun execute(
        activity: MainActivity,
        featureKey: String,
        overlay: GuideOverlayView,
        onStop: () -> Unit,
        fallbackTour: () -> Unit
    ) {
        val store = DataStore(activity)

        when (featureKey) {
            "life_capsule" -> {
                activity.navigateToTab(0)
                activity.binding.root.postDelayed({
                    val target = activity.findViewById<View>(R.id.item_capsule_badge)
                        ?: activity.findViewById<View>(R.id.rv_asset_list)
                        ?: activity.binding.navHome
                    overlay.showStep(
                        stepIndex = 1,
                        totalSteps = 1,
                        title = "🎞️ 物品时光胶囊与生活画册回忆录",
                        desc = "点击卡片上的「🎞️ 回忆」或更多菜单中的「时光胶囊」，您可以为该物品记录生活高光故事、发生日期与 1~5 星真香体验，更可一键导出 1080P 高清拍立得长图海报！",
                        actionHint = "👉 点击高亮卡片体验时光胶囊回忆流与画册生成",
                        targetView = target,
                        onNext = {
                            onStop()
                            val firstEntry = store.loadAll().firstOrNull()
                            if (firstEntry != null) {
                                LifeCapsuleDialog.showCapsuleDialog(activity, store, firstEntry) {}
                            } else {
                                Toast.makeText(activity, "请先记一笔物品以体验时光胶囊", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onExit = { onStop() }
                    )
                }, 200)
            }

            "digital_vault" -> {
                activity.navigateToTab(0)
                activity.binding.root.postDelayed({
                    val target = activity.findViewById<View>(R.id.tab_digital_assets) ?: activity.binding.navHome
                    overlay.showStep(
                        stepIndex = 1,
                        totalSteps = 1,
                        title = "📷 数字相册与电子资产专属展厅",
                        desc = "点击「📷 数字相册」切换至数字资产专属展厅！支持珍贵照片回忆相册、软件授权Key、域名与数字资料库集中归档、容量统计与备份追踪！",
                        actionHint = "👉 请点击「📷 数字相册」或点击完成",
                        targetView = target,
                        onNext = {
                            onStop()
                            DigitalAssetManagerDialog.showDigitalVaultDialog(activity, store) {}
                        },
                        onExit = { onStop() },
                        onTargetClick = {
                            Handler(Looper.getMainLooper()).postDelayed({
                                onStop()
                                DigitalAssetManagerDialog.showDigitalVaultDialog(activity, store) {}
                            }, 300)
                        }
                    )
                }, 200)
            }

            "lending_hub" -> {
                activity.navigateToTab(0)
                activity.binding.root.postDelayed({
                    val toolsBar = activity.findViewById<View>(R.id.layout_tools_bar)
                    toolsBar?.visibility = View.VISIBLE
                    val target = activity.findViewById<View>(R.id.btn_lending_hub_top) ?: activity.binding.navHome
                    overlay.showStep(
                        stepIndex = 1,
                        totalSteps = 1,
                        title = "📤 实物外借与借还流转管理",
                        desc = "点击「📤 借还流转」进入外借中心！支持借用人档案、归还期限倒计时、一键生成 1080P 电子借条与微信温馨催还海报，支持归还成色打卡！",
                        actionHint = "👉 请点击「📤 借还流转」进入外借中心",
                        targetView = target,
                        onNext = {
                            onStop()
                            LendingManagerDialog.showLendingHubDialog(activity, store) {}
                        },
                        onExit = { onStop() },
                        onTargetClick = {
                            Handler(Looper.getMainLooper()).postDelayed({
                                onStop()
                                LendingManagerDialog.showLendingHubDialog(activity, store) {}
                            }, 300)
                        }
                    )
                }, 200)
            }

            "voucher_vault" -> {
                activity.navigateToTab(0)
                activity.binding.root.postDelayed({
                    val toolsBar = activity.findViewById<View>(R.id.layout_tools_bar)
                    toolsBar?.visibility = View.VISIBLE
                    val target = activity.findViewById<View>(R.id.btn_voucher_vault_top) ?: activity.binding.navHome
                    overlay.showStep(
                        stepIndex = 1,
                        totalSteps = 1,
                        title = "🎟️ 时效权益与卡券票据收纳馆",
                        desc = "点击「🎟️ 时效卡券」进入卡券馆！优惠券、代金券、洗车/理发次卡与会员专属权益统一归拢，3天内临期自动置顶强预警，支持次卡一键扣减与券码复制！",
                        actionHint = "👉 请点击「🎟️ 时效卡券」进入收纳馆",
                        targetView = target,
                        onNext = {
                            onStop()
                            VoucherVaultDialog.showVoucherVaultDialog(activity, store) {}
                        },
                        onExit = { onStop() },
                        onTargetClick = {
                            Handler(Looper.getMainLooper()).postDelayed({
                                onStop()
                                VoucherVaultDialog.showVoucherVaultDialog(activity, store) {}
                            }, 300)
                        }
                    )
                }, 200)
            }

            "family_vault" -> {
                activity.navigateToTab(0)
                activity.binding.root.postDelayed({
                    val toolsBar = activity.findViewById<View>(R.id.layout_tools_bar)
                    toolsBar?.visibility = View.VISIBLE
                    val target = activity.findViewById<View>(R.id.btn_family_vault_top) ?: activity.binding.navHome
                    overlay.showStep(
                        stepIndex = 1,
                        totalSteps = 1,
                        title = "🪪 家庭多成员证照安全夹",
                        desc = "点击「🪪 证照安全夹」进入证照夹！全家身份证、护照、户口本分类归档，证号一键脱敏复制，到期换证提前预警！",
                        actionHint = "👉 请点击「🪪 证照安全夹」进入证照夹",
                        targetView = target,
                        onNext = {
                            onStop()
                            FamilyVaultDialog.showFamilyVaultDialog(activity, store) {}
                        },
                        onExit = { onStop() },
                        onTargetClick = {
                            Handler(Looper.getMainLooper()).postDelayed({
                                onStop()
                                FamilyVaultDialog.showFamilyVaultDialog(activity, store) {}
                            }, 300)
                        }
                    )
                }, 200)
            }

            "medicine_vault" -> {
                activity.navigateToTab(0)
                activity.binding.root.postDelayed({
                    val toolsBar = activity.findViewById<View>(R.id.layout_tools_bar)
                    toolsBar?.visibility = View.VISIBLE
                    val target = activity.findViewById<View>(R.id.btn_family_medicine_top) ?: activity.binding.navHome
                    overlay.showStep(
                        stepIndex = 1,
                        totalSteps = 1,
                        title = "💊 家庭智能健康药箱",
                        desc = "点击「💊 家庭药箱」进入家庭药箱！支持按病症（🤒发烧/🤧感冒/🤢肠胃/🩹外伤）对症速查，开封倒计时打卡，过期药品显著飘红【🚫 严禁服用】！",
                        actionHint = "👉 请点击「💊 家庭药箱」进入药箱",
                        targetView = target,
                        onNext = {
                            onStop()
                            FamilyMedicineDialog.showMedicineVaultDialog(activity, store) {}
                        },
                        onExit = { onStop() },
                        onTargetClick = {
                            Handler(Looper.getMainLooper()).postDelayed({
                                onStop()
                                FamilyMedicineDialog.showMedicineVaultDialog(activity, store) {}
                            }, 300)
                        }
                    )
                }, 200)
            }

            "food_vault" -> {
                activity.navigateToTab(0)
                activity.binding.root.postDelayed({
                    val toolsBar = activity.findViewById<View>(R.id.layout_tools_bar)
                    toolsBar?.visibility = View.VISIBLE
                    val target = activity.findViewById<View>(R.id.btn_food_vault_top) ?: activity.binding.navHome

                    val demoFoodId = "demo_food_tour_" + System.currentTimeMillis()
                    val demoFood = FoodRecord(
                        id = demoFoodId,
                        name = "安格斯原切雪花牛排 (教程示例)",
                        zone = "freezer",
                        qty = 2.0,
                        unit = "袋",
                        location = "冰箱冷冻二层",
                        expiryDate = System.currentTimeMillis() + 2L * 24 * 60 * 60 * 1000,
                        notes = "适合提前冷藏解冻香煎 (演练结束自动删除)"
                    )
                    TourSandbox.registerDemoFood(demoFoodId)
                    store.addOrUpdateFood(demoFood)

                    overlay.showStep(
                        stepIndex = 1,
                        totalSteps = 1,
                        title = "🥦 冰箱冷冻与食材生鲜鲜度库",
                        desc = "点击「🥦 食材鲜度库」进入保鲜展厅！支持冷冻/冷藏/调味温区分区管理，开封保鲜倒计时，点击「🍳 今晚清库存」一秒找出临期食材！",
                        actionHint = "👉 请点击「🥦 食材鲜度库」进入体验",
                        targetView = target,
                        onNext = {
                            onStop()
                            FoodVaultDialog.show(activity, store) {}
                        },
                        onExit = { onStop() },
                        onTargetClick = {
                            Handler(Looper.getMainLooper()).postDelayed({
                                onStop()
                                FoodVaultDialog.show(activity, store) {}
                            }, 300)
                        }
                    )
                }, 200)
            }

            "honor_vault" -> {
                activity.navigateToTab(0)
                activity.binding.root.postDelayed({
                    val toolsBar = activity.findViewById<View>(R.id.layout_tools_bar)
                    toolsBar?.visibility = View.VISIBLE
                    val target = activity.findViewById<View>(R.id.btn_honor_vault_top) ?: activity.binding.navHome

                    val demoHonorId = "demo_honor_tour_" + System.currentTimeMillis()
                    val demoHonor = HonorCredential(
                        id = demoHonorId,
                        member = "本人",
                        category = "career",
                        title = "国家计算机技术与软件资格认证 (教程示例)",
                        certNumber = "2024098877665544",
                        issuer = "中华人民共和国人力资源和社会保障部",
                        issueDate = System.currentTimeMillis(),
                        scoreOrLevel = "优秀",
                        notes = "一次性高分通过 (演练结束自动删除)"
                    )
                    TourSandbox.registerDemoHonor(demoHonorId)
                    store.addOrUpdateHonorCredential(demoHonor)

                    overlay.showStep(
                        stepIndex = 1,
                        totalSteps = 1,
                        title = "🏆 全家成长履历与荣誉勋章馆",
                        desc = "点击「🏆 荣誉勋章馆」进入荣誉殿堂！全家学历学位、职业职称、考级奖状分类归档，支持证书号脱敏一键复制与 1080P 人生高光足迹长卷导出！",
                        actionHint = "👉 请点击「🏆 荣誉勋章馆」进入体验",
                        targetView = target,
                        onNext = {
                            onStop()
                            HonorVaultDialog.show(activity, store) {}
                        },
                        onExit = { onStop() },
                        onTargetClick = {
                            Handler(Looper.getMainLooper()).postDelayed({
                                onStop()
                                HonorVaultDialog.show(activity, store) {}
                            }, 300)
                        }
                    )
                }, 200)
            }

            "ai_concierge" -> {
                activity.navigateToTab(0)
                activity.binding.root.postDelayed({
                    val toolsBar = activity.findViewById<View>(R.id.layout_tools_bar)
                    toolsBar?.visibility = View.VISIBLE
                    val target = activity.findViewById<View>(R.id.btn_ai_concierge_top) ?: activity.binding.navHome
                    overlay.showStep(
                        stepIndex = 1,
                        totalSteps = 1,
                        title = "🤖 AI 资产智能管家与空间收纳顾问",
                        desc = "点击「🤖 智能管家」，支持自然语言寻物、全屋空间负荷体检诊断、环境安全风险排查与资产价值穿透，100% 离线端侧毫秒级响应！",
                        actionHint = "👉 请点击高亮的「🤖 智能管家」体验对话，或点击完成",
                        targetView = target,
                        onNext = {
                            onStop()
                            AiConciergeHelper.showConciergeDialog(activity, store)
                        },
                        onExit = { onStop() },
                        onTargetClick = {
                            Handler(Looper.getMainLooper()).postDelayed({
                                onStop()
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
                        title = "🎒 场景装备套装与出行归巢归位舱",
                        desc = "自由组装出差、露营、摄影等场景装备包。支持【去程装箱】与【返程清点】，到家后一键调出【物归原位路线图】批量归位打卡！",
                        actionHint = "👉 请点击「🎒 装备套装」开始管理或核对",
                        targetView = target,
                        onNext = {
                            onStop()
                            KitManager.showKitListDialog(activity, store)
                        },
                        onExit = { onStop() },
                        onTargetClick = {
                            Handler(Looper.getMainLooper()).postDelayed({
                                onStop()
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
                            onStop()
                            activity.showAddDialog()
                        },
                        onExit = { onStop() },
                        onTargetClick = {
                            Handler(Looper.getMainLooper()).postDelayed({
                                onStop()
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
                        title = "🗺️ 空间平面图与箱盒 X 光透视舱",
                        desc = "进入空间平面图，可视化俯瞰全屋空间。点击任意房间滑出【箱盒 X 光透视舱】（透视资产总值与空间负荷指数），支持一键【🚚 整箱搬家】与【🏷️ 便签工坊】！",
                        actionHint = "👉 请点击「空间地图」直达空间画布",
                        targetView = target,
                        onNext = {
                            onStop()
                            FloorPlanDialog.show(activity, store, isSelectMode = false)
                        },
                        onExit = { onStop() },
                        onTargetClick = {
                            Handler(Looper.getMainLooper()).postDelayed({
                                onStop()
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
                        title = "🏷️ 智能收纳便签与扫码寻物",
                        desc = "在空间平面图中点击可唤起「智能收纳便签工坊」（支持箱盒清单、生鲜保鲜、药箱用法、线缆规格与防丢联系卡 5 大模板），一键蓝牙热敏直印或保存相册；点击「扫码寻物」即可秒级对准便签扫码查箱或匹配商品！",
                        actionHint = "👉 请点击「扫码寻物」启动离线扫码",
                        targetView = target,
                        onNext = {
                            onStop()
                            activity.startQrScanner()
                        },
                        onExit = { onStop() },
                        onTargetClick = {
                            Handler(Looper.getMainLooper()).postDelayed({
                                onStop()
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
                            onStop()
                            InventoryAuditDialog.startAudit(activity, store) { activity.refreshCurrentFragment() }
                        },
                        onExit = { onStop() },
                        onTargetClick = {
                            Handler(Looper.getMainLooper()).postDelayed({
                                onStop()
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
                            onStop()
                            LanSyncHelper.showLanSyncDialog(activity, store) { activity.refreshCurrentFragment() }
                        },
                        onExit = { onStop() },
                        onTargetClick = {
                            Handler(Looper.getMainLooper()).postDelayed({
                                onStop()
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
                            onStop()
                            LedgerManager.showLedgerPicker(activity) { activity.refreshCurrentFragment() }
                        },
                        onExit = { onStop() },
                        onTargetClick = {
                            Handler(Looper.getMainLooper()).postDelayed({
                                onStop()
                                LedgerManager.showLedgerPicker(activity) { activity.refreshCurrentFragment() }
                            }, 300)
                        }
                    )
                }, 200)
            }

            "chart_radar" -> {
                activity.navigateToTab(2)
                activity.binding.root.postDelayed({
                    val target = activity.findViewById<View>(R.id.overview_donut_chart) ?: activity.binding.navReport
                    overlay.showStep(
                        stepIndex = 1,
                        totalSteps = 1,
                        title = "📊 动态环形图与断舍离回血决策舱",
                        desc = "轻触环形图扇区动态高亮分类占比；点击「断舍离雷达」直达【♻️ 闲置断舍离与回血决策舱】，智能估算沉睡资产预计回血总额，一键生成 AI 闲鱼/转转出清文案！",
                        actionHint = "👉 点击扇区体验交互，或点击完成",
                        targetView = target,
                        onNext = { onStop() },
                        onExit = { onStop() }
                    )
                }, 250)
            }

            "privacy_sync" -> {
                activity.navigateToTab(3)
                activity.binding.root.postDelayed({
                    val target = activity.findViewById<View>(R.id.btn_more_settings) ?: activity.binding.navProfile
                    overlay.showStep(
                        stepIndex = 1,
                        totalSteps = 1,
                        title = "🔐 指纹面容隐私锁与 WebDAV 私有云同步",
                        desc = "在「更多设置」中可一键开启系统硬件级生物识别隐私锁，保护资产隐私；配置坚果云/Nextcloud 实现 WebDAV 静默云备份与换机恢复！",
                        actionHint = "👉 点击「更多设置」体验隐私与同步配置",
                        targetView = target,
                        onNext = { onStop() },
                        onExit = { onStop() },
                        onTargetClick = {
                            Handler(Looper.getMainLooper()).postDelayed({ onStop() }, 300)
                        }
                    )
                }, 250)
            }

            "hot_patch" -> {
                activity.navigateToTab(3)
                activity.binding.root.postDelayed({
                    val target = activity.findViewById<View>(R.id.btn_update_version) ?: activity.binding.navProfile
                    overlay.showStep(
                        stepIndex = 1,
                        totalSteps = 1,
                        title = "🚀 检查与智能升级系统",
                        desc = "统一智能检测最新版本与增量热补丁！优先支持 ⚡ 免重装极速热更（秒级即时生效），必要时全量 APK 升级！",
                        actionHint = "👉 请点击「检查与智能升级」检测可用更新",
                        targetView = target,
                        onNext = {
                            onStop()
                            UpdateManager.checkUpdate(activity, isManual = true)
                        },
                        onExit = { onStop() },
                        onTargetClick = {
                            Handler(Looper.getMainLooper()).postDelayed({
                                onStop()
                                UpdateManager.checkUpdate(activity, isManual = true)
                            }, 300)
                        }
                    )
                }, 250)
            }

            "bluetooth_nfc" -> {
                activity.navigateToTab(0)
                activity.binding.root.postDelayed({
                    val toolsBar = activity.findViewById<View>(R.id.layout_tools_bar)
                    toolsBar?.visibility = View.VISIBLE
                    val target = activity.findViewById<View>(R.id.btn_open_floorplan_top) ?: activity.binding.navHome
                    overlay.showStep(
                        stepIndex = 1,
                        totalSteps = 1,
                        title = "🖨️ 蓝牙标签机直连与 NFC 碰一碰寻物",
                        desc = "在空间平面图中点击任意房间的「生成二维码标签」，即可通过蓝牙直连 58/80mm 热敏便携打印机打印物理贴纸，或一键将收纳箱写入 NFC 贴纸，手机碰一碰零点击识别展开！",
                        actionHint = "👉 点击「空间地图」体验硬件扩展",
                        targetView = target,
                        onNext = {
                            onStop()
                            FloorPlanDialog.show(activity, store, isSelectMode = false)
                        },
                        onExit = { onStop() },
                        onTargetClick = {
                            Handler(Looper.getMainLooper()).postDelayed({
                                onStop()
                                FloorPlanDialog.show(activity, store, isSelectMode = false)
                            }, 300)
                        }
                    )
                }, 200)
            }

            "date_picker" -> {
                activity.navigateToTab(0)
                activity.binding.root.postDelayed({
                    val target = activity.binding.fabAdd
                    overlay.showStep(
                        stepIndex = 1,
                        totalSteps = 1,
                        title = "📅 现代化全景日期选择器",
                        desc = "在记账时点击购入日期或生产日期，可一秒展开 1980~2035 跨年代网格直达，支持「昨天」、「1个月前」、「1年前」、「3年前」快捷时间标签与相对天数直观感知！",
                        actionHint = "👉 点击「+」打开记账窗口体验日期选择器",
                        targetView = target,
                        onNext = {
                            onStop()
                            activity.showAddDialog()
                        },
                        onExit = { onStop() },
                        onTargetClick = {
                            Handler(Looper.getMainLooper()).postDelayed({ onStop() }, 300)
                        }
                    )
                }, 200)
            }

            "safety_stock" -> {
                activity.navigateToTab(0)
                activity.binding.root.postDelayed({
                    val target = activity.findViewById<View>(R.id.btn_ai_concierge_top) ?: activity.binding.navHome
                    val toolsBar = activity.findViewById<View>(R.id.layout_tools_bar)
                    toolsBar?.visibility = View.VISIBLE
                    overlay.showStep(
                        stepIndex = 1,
                        totalSteps = 1,
                        title = "⚠️ 耗材安全库存预警与采购单",
                        desc = "为生活耗材设定最低安全库存线。当库存触底时，首页顶部自动浮现告急卡片，支持「一键生成采购单」汇总缺货差额并复制到剪贴板！",
                        actionHint = "👉 点击完成了解耗材管理",
                        targetView = target,
                        onNext = { onStop() },
                        onExit = { onStop() }
                    )
                }, 200)
            }

            "csv_export" -> {
                activity.navigateToTab(3)
                activity.binding.root.postDelayed({
                    val target = activity.findViewById<View>(R.id.btn_backup_restore) ?: activity.binding.navProfile
                    overlay.showStep(
                        stepIndex = 1,
                        totalSteps = 1,
                        title = "📊 Excel 兼容 CSV 资产总表导出",
                        desc = "一键导出包含全部品名、分类、折旧、单价、拥有天数与日均成本的 CSV 表格，自带 UTF-8 BOM 编码，完美兼容电脑 Excel 与微信直接打开！",
                        actionHint = "👉 点击「数据备份与导出」体验 CSV 导出",
                        targetView = target,
                        onNext = { onStop() },
                        onExit = { onStop() }
                    )
                }, 250)
            }

            else -> {
                fallbackTour()
            }
        }
    }
}
