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
            "workbench" -> {
                onStop()
                activity.startActivity(android.content.Intent(activity,WorkbenchActivity::class.java))
            }
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

            "wardrobe_vault" -> {
                activity.navigateToTab(0)
                activity.binding.root.postDelayed({
                    val toolsBar = activity.findViewById<View>(R.id.layout_tools_bar)
                    toolsBar?.visibility = View.VISIBLE
                    val target = activity.findViewById<View>(R.id.btn_wardrobe_vault_top) ?: activity.binding.navHome

                    val demoWardrobeId = "demo_wardrobe_tour_" + System.currentTimeMillis()
                    val demoWardrobe = WardrobeRecord(
                        id = demoWardrobeId,
                        name = "波司登极寒鹅绒羽绒服 (教程示例)",
                        season = "winter",
                        category = "coat",
                        color = "米白色",
                        material = "90%白鹅绒 蓬松度800+",
                        storageLocation = "主卧大衣柜上层 · 真空压缩袋 #03",
                        purchasePrice = 1299.0,
                        purchaseDate = System.currentTimeMillis(),
                        wearCount = 6,
                        lastWornAt = System.currentTimeMillis(),
                        careNotes = "仅限专业干洗 · 不可烘干",
                        notes = "保暖御寒神器 (演练结束自动删除)"
                    )
                    TourSandbox.registerDemoWardrobe(demoWardrobeId)
                    store.addOrUpdateWardrobeRecord(demoWardrobe)

                    overlay.showStep(
                        stepIndex = 1,
                        totalSteps = 1,
                        title = "👗 换季衣橱与四季穿搭舱",
                        desc = "点击「👗 换季衣橱」进入衣橱收纳舱！四季胶囊衣橱分舱、真空压缩袋顶柜定位、穿着打卡与次均成本精算，科学管理换季与断舍离！",
                        actionHint = "👉 请点击「👗 换季衣橱」进入体验",
                        targetView = target,
                        onNext = {
                            onStop()
                            WardrobeVaultDialog.show(activity, store) {}
                        },
                        onExit = { onStop() },
                        onTargetClick = {
                            Handler(Looper.getMainLooper()).postDelayed({
                                onStop()
                                WardrobeVaultDialog.show(activity, store) {}
                            }, 300)
                        }
                    )
                }, 200)
            }

            "emergency_vault" -> {
                activity.navigateToTab(0)
                activity.binding.root.postDelayed({
                    val toolsBar = activity.findViewById<View>(R.id.layout_tools_bar)
                    toolsBar?.visibility = View.VISIBLE
                    val target = activity.findViewById<View>(R.id.btn_emergency_vault_top) ?: activity.binding.navHome

                    val demoEmergencyId = "demo_emergency_tour_" + System.currentTimeMillis()
                    val demoItem = EmergencyItem(
                        id = demoEmergencyId,
                        name = "手摇发电多波段收音机+强光手电 (教程示例)",
                        kitType = "earthquake",
                        category = "tool",
                        qty = 1.0,
                        unit = "台",
                        location = "玄关防盗门后收纳挂架 #01",
                        expiryDate = 0L,
                        rotationIntervalMonths = 6,
                        lastCheckedAt = System.currentTimeMillis(),
                        notes = "拔出安全插销，手摇发电 1 分钟可照明 15 分钟 (演练结束自动删除)"
                    )
                    TourSandbox.registerDemoEmergency(demoEmergencyId)
                    store.addOrUpdateEmergencyItem(demoItem)

                    overlay.showStep(
                        stepIndex = 1,
                        totalSteps = 1,
                        title = "🚨 家庭应急防灾与生命线舱",
                        desc = "点击「🚨 应急防灾」进入生命线舱！四大应急专包（地震/火灾/车载/暴雨）、黄金动线存放定位与物资时效轮换点检打卡！",
                        actionHint = "👉 请点击「🚨 应急防灾」进入体验",
                        targetView = target,
                        onNext = {
                            onStop()
                            EmergencyVaultDialog.show(activity, store) {}
                        },
                        onExit = { onStop() },
                        onTargetClick = {
                            Handler(Looper.getMainLooper()).postDelayed({
                                onStop()
                                EmergencyVaultDialog.show(activity, store) {}
                            }, 300)
                        }
                    )
                }, 200)
            }

            "universal_vault_center" -> {
                activity.navigateToTab(0)
                activity.binding.root.postDelayed({
                    val toolsBar = activity.findViewById<View>(R.id.layout_tools_bar)
                    toolsBar?.visibility = View.VISIBLE
                    val target = activity.findViewById<View>(R.id.btn_universal_vault_center_top) ?: activity.binding.navHome

                    overlay.showStep(
                        stepIndex = 1,
                        totalSteps = 1,
                        title = "🏛️ 全维度收纳大厅与生命线总控看板",
                        desc = "点击「🏛️ 全景收纳大厅」进入家庭总控看板！7 大专业收纳馆直通、全家临期与失效时效红绿灯雷达与 100 分制健康指数！",
                        actionHint = "👉 请点击「🏛️ 全景收纳大厅」进入体验",
                        targetView = target,
                        onNext = {
                            onStop()
                            UniversalVaultCenterDialog.show(activity, store) {}
                        },
                        onExit = { onStop() },
                        onTargetClick = {
                            Handler(Looper.getMainLooper()).postDelayed({
                                onStop()
                                UniversalVaultCenterDialog.show(activity, store) {}
                            }, 300)
                        }
                    )
                }, 200)
            }

            "tool_maintenance_vault" -> {
                activity.navigateToTab(0)
                activity.binding.root.postDelayed({
                    val toolsBar = activity.findViewById<View>(R.id.layout_tools_bar)
                    toolsBar?.visibility = View.VISIBLE
                    val target = activity.findViewById<View>(R.id.btn_tool_vault_top) ?: activity.binding.navHome

                    val demoToolId = "demo_tool_tour_" + System.currentTimeMillis()
                    val demoTool = ToolMaintenanceRecord(
                        id = demoToolId,
                        name = "博世 12V 锂电冲击钻 (教程示例)",
                        category = "power_tool",
                        spec = "12V 锂电 / 10mm 夹头 (配8mm钻头)",
                        qty = 1.0,
                        unit = "套",
                        location = "阳台工具收纳柜 #01",
                        maintenanceIntervalDays = 180,
                        lastMaintainedAt = System.currentTimeMillis(),
                        notes = "钻水泥墙需切换为冲击档；演练结束后自动清理"
                    )
                    TourSandbox.registerDemoTool(demoToolId)
                    store.addOrUpdateToolRecord(demoTool)

                    overlay.showStep(
                        stepIndex = 1,
                        totalSteps = 1,
                        title = "🔧 家庭工具五金与设备维保配件舱",
                        desc = "点击「🔧 工具五金」进入配件舱！电动/手工工具分类、螺丝五金与钻头规格速查、净水新风耗材维保排期与一键打卡！",
                        actionHint = "👉 请点击「🔧 工具五金」进入体验",
                        targetView = target,
                        onNext = {
                            onStop()
                            ToolMaintenanceDialog.show(activity, store) {}
                        },
                        onExit = { onStop() },
                        onTargetClick = {
                            Handler(Looper.getMainLooper()).postDelayed({
                                onStop()
                                ToolMaintenanceDialog.show(activity, store) {}
                            }, 300)
                        }
                    )
                }, 200)
            }

            "plant_care_vault" -> {
                activity.navigateToTab(0)
                activity.binding.root.postDelayed({
                    val toolsBar = activity.findViewById<View>(R.id.layout_tools_bar)
                    toolsBar?.visibility = View.VISIBLE
                    val target = activity.findViewById<View>(R.id.btn_plant_vault_top) ?: activity.binding.navHome

                    val demoPlantId = "demo_plant_tour_" + System.currentTimeMillis()
                    val demoPlant = PlantCareRecord(
                        id = demoPlantId,
                        name = "南阳台白锦龟背竹 (教程示例)",
                        species = "龟背竹 (Monstera Borsigiana)",
                        location = "南阳台花架 #02",
                        lightDemand = "semi_shade",
                        waterIntervalDays = 7,
                        lastWateredAt = System.currentTimeMillis(),
                        fertilizeIntervalDays = 30,
                        lastFertilizedAt = System.currentTimeMillis(),
                        careTips = "喜明亮散光，表土干透浇透；演练结束后自动清理"
                    )
                    TourSandbox.registerDemoPlant(demoPlantId)
                    store.addOrUpdatePlantRecord(demoPlant)

                    overlay.showStep(
                        stepIndex = 1,
                        totalSteps = 1,
                        title = "🪴 家庭绿植花卉与水肥养护舱",
                        desc = "点击「🪴 绿植水肥」进入绿植水肥日历！光照习性档案、浇水施肥倒计时排期与一键养护打卡！",
                        actionHint = "👉 请点击「🪴 绿植水肥」进入体验",
                        targetView = target,
                        onNext = {
                            onStop()
                            PlantCareDialog.show(activity, store) {}
                        },
                        onExit = { onStop() },
                        onTargetClick = {
                            Handler(Looper.getMainLooper()).postDelayed({
                                onStop()
                                PlantCareDialog.show(activity, store) {}
                            }, 300)
                        }
                    )
                }, 200)
            }

            "pet_care_vault" -> {
                activity.navigateToTab(0)
                activity.binding.root.postDelayed({
                    val toolsBar = activity.findViewById<View>(R.id.layout_tools_bar)
                    toolsBar?.visibility = View.VISIBLE
                    val target = activity.findViewById<View>(R.id.btn_pet_vault_top) ?: activity.binding.navHome

                    val demoPetId = "demo_pet_tour_" + System.currentTimeMillis()
                    val demoPet = PetCareRecord(
                        id = demoPetId,
                        name = "布丁 (教程示例)",
                        species = "英短银渐层",
                        birthDate = System.currentTimeMillis() - 365L * 24 * 3600 * 1000,
                        weightKg = 4.2,
                        microchipId = "900215000987654",
                        dewormIntervalDays = 30,
                        lastDewormedAt = System.currentTimeMillis(),
                        vaccineIntervalDays = 365,
                        lastVaccinatedAt = System.currentTimeMillis(),
                        foodBrand = "渴望鸡肉猫粮 · 阳台储粮桶 #01",
                        notes = "已绝育；演练结束后自动清理"
                    )
                    TourSandbox.registerDemoPet(demoPetId)
                    store.addOrUpdatePetRecord(demoPet)

                    overlay.showStep(
                        stepIndex = 1,
                        totalSteps = 1,
                        title = "🐾 家庭萌宠档案与健康耗材舱",
                        desc = "点击「🐾 家庭萌宠生活与疫苗驱虫舱」进入！物种档案、芯片号脱敏速查、驱虫疫苗倒计时与一键打卡！",
                        actionHint = "👉 请点击「🐾 家庭萌宠生活与疫苗驱虫舱」进入体验",
                        targetView = target,
                        onNext = {
                            onStop()
                            PetCareDialog.show(activity, store) {}
                        },
                        onExit = { onStop() },
                        onTargetClick = {
                            Handler(Looper.getMainLooper()).postDelayed({
                                onStop()
                                PetCareDialog.show(activity, store) {}
                            }, 300)
                        }
                    )
                }, 200)
            }

            "book_vault" -> {
                activity.navigateToTab(0)
                activity.binding.root.postDelayed({
                    val toolsBar = activity.findViewById<View>(R.id.layout_tools_bar)
                    toolsBar?.visibility = View.VISIBLE
                    val target = activity.findViewById<View>(R.id.btn_book_vault_top) ?: activity.binding.navHome

                    val demoBookId = "demo_book_tour_" + System.currentTimeMillis()
                    val demoBook = BookRecord(
                        id = demoBookId,
                        title = "置身事内 (教程示例)",
                        author = "兰小欢",
                        category = "社科人文",
                        bookshelfLocation = "书房A柜第2层",
                        totalPages = 340,
                        currentPages = 120,
                        readingStatus = "reading",
                        rating = 5.0f,
                        summaryNotes = "理解中国经济发展的微观基础与地方政府行为激励；演练结束后自动清理"
                    )
                    TourSandbox.registerDemoBook(demoBookId)
                    store.addOrUpdateBookRecord(demoBook)

                    overlay.showStep(
                        stepIndex = 1,
                        totalSteps = 1,
                        title = "📚 书房藏书与阅读收纳舱",
                        desc = "点击「📚 书房藏书」进入！藏书格子定位、阅读进度百分比计算、外借流转与书摘评分！",
                        actionHint = "👉 请点击「📚 书房藏书」进入体验",
                        targetView = target,
                        onNext = {
                            onStop()
                            BookVaultDialog.show(activity, store) {}
                        },
                        onExit = { onStop() },
                        onTargetClick = {
                            Handler(Looper.getMainLooper()).postDelayed({
                                onStop()
                                BookVaultDialog.show(activity, store) {}
                            }, 300)
                        }
                    )
                }, 200)
            }

            "beverage_vault" -> {
                activity.navigateToTab(0)
                activity.binding.root.postDelayed({
                    val toolsBar = activity.findViewById<View>(R.id.layout_tools_bar)
                    toolsBar?.visibility = View.VISIBLE
                    val target = activity.findViewById<View>(R.id.btn_beverage_vault_top) ?: activity.binding.navHome

                    val demoBevId = "demo_bev_tour_" + System.currentTimeMillis()
                    val demoBev = BeverageTeaRecord(
                        id = demoBevId,
                        name = "飞天茅台 53度 (教程示例)",
                        category = "烈酒名酿",
                        vintageYear = 2018,
                        originRegion = "贵州茅台镇",
                        storageLocation = "餐边柜恒温酒柜 #02",
                        qty = 2.0,
                        unit = "瓶",
                        bestDrinkingYear = 2028,
                        rating = 5.0f,
                        tastingNotes = "酱香幽雅，空杯留香；教程演练结束后自动安全清理"
                    )
                    TourSandbox.registerDemoBeverage(demoBevId)
                    store.addOrUpdateBeverageRecord(demoBev)

                    overlay.showStep(
                        stepIndex = 1,
                        totalSteps = 1,
                        title = "🍷 茶窖珍藏与适饮时效舱",
                        desc = "点击「🍷 茶窖名酿」进入！名茶名酒陈化年份精算、最佳适饮黄金期、开瓶保鲜与库存打卡！",
                        actionHint = "👉 请点击「🍷 茶窖名酿」进入体验",
                        targetView = target,
                        onNext = {
                            onStop()
                            BeverageTeaDialog.show(activity, store) {}
                        },
                        onExit = { onStop() },
                        onTargetClick = {
                            Handler(Looper.getMainLooper()).postDelayed({
                                onStop()
                                BeverageTeaDialog.show(activity, store) {}
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
                        title = "🌐 局域网 Web 大屏与双机 P2P 增量对撞",
                        desc = "点击启动局域网微服务，电脑或 iPad 浏览器直接输入提示的 IP 网址（8848端口），进入大屏控制台；同一 Wi-Fi 下还能自动发现附近手机与桌面端设备，一键发起双向增量对撞合并！",
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

            "global_search" -> {
                activity.navigateToTab(0)
                activity.binding.root.postDelayed({
                    val target = activity.findViewById<View>(R.id.btn_search_items) ?: activity.binding.navHome
                    overlay.showStep(
                        stepIndex = 1,
                        totalSteps = 1,
                        title = "🔍 全库与 12 馆跨维极速联合检索",
                        desc = "点击右上角搜索图标，输入任意关键词即可秒级穿透资产主库及全部 12 个专业收纳馆（卡券、证照、药箱、食材、应急物资、工具、绿植、宠物等）！",
                        actionHint = "👉 点击放大镜图标体验全库联合检索",
                        targetView = target,
                        onNext = {
                            onStop()
                            GlobalSearchDialog.show(activity, store)
                        },
                        onExit = { onStop() },
                        onTargetClick = {
                            Handler(Looper.getMainLooper()).postDelayed({
                                onStop()
                                GlobalSearchDialog.show(activity, store)
                            }, 300)
                        }
                    )
                }, 200)
            }

            "today_alerts" -> {
                activity.navigateToTab(0)
                activity.binding.root.postDelayed({
                    val toolsBar = activity.findViewById<View>(R.id.layout_tools_bar)
                    toolsBar?.visibility = View.VISIBLE
                    val target = activity.findViewById<View>(R.id.card_today_alerts_banner)
                        ?: activity.findViewById<View>(R.id.btn_universal_vault_center_top)
                        ?: activity.binding.navHome
                    overlay.showStep(
                        stepIndex = 1,
                        totalSteps = 1,
                        title = "⚠️ 今日时效待办与 12 馆生命线看板",
                        desc = "首页动态聚合全家临期卡券、到期药品、冷冻生鲜鲜度、绿植水肥与萌宠驱虫排期，最紧迫事项自动置顶提醒，点击卡片一秒直达全景收纳大厅！",
                        actionHint = "👉 点击体验今日时效看板与收纳大厅",
                        targetView = target,
                        onNext = {
                            onStop()
                            UniversalVaultCenterDialog.show(activity, store) {}
                        },
                        onExit = { onStop() }
                    )
                }, 200)
            }

            "all_vaults_export" -> {
                activity.navigateToTab(3)
                activity.binding.root.postDelayed({
                    val target = activity.findViewById<View>(R.id.btn_backup_restore) ?: activity.binding.navProfile
                    overlay.showStep(
                        stepIndex = 1,
                        totalSteps = 1,
                        title = "📊 12 馆时效物资联合报表导出",
                        desc = "在「数据备份与多格式导出」中点击「导出【12 馆时效联合报表】」，系统瞬间汇总全部 12 馆物资生成带 UTF-8 BOM 的标准 CSV 表格，微信一键分享！",
                        actionHint = "👉 点击「数据备份与导出」了解联合导出",
                        targetView = target,
                        onNext = { onStop() },
                        onExit = { onStop() }
                    )
                }, 250)
            }

            "storage_cleanup" -> {
                activity.navigateToTab(3)
                activity.binding.root.postDelayed({
                    val target = activity.findViewById<View>(R.id.btn_backup_restore) ?: activity.binding.navProfile
                    overlay.showStep(
                        stepIndex = 1,
                        totalSteps = 1,
                        title = "🧹 图片沙盒孤立文件清理与批量重压缩",
                        desc = "自动扫描未被任何物品引用的孤立废弃图片一键清理，更提供后台无损批量重压缩引擎，为全量图片减重 15%~20% 释放手机空间！",
                        actionHint = "👉 点击体验存储空间管理",
                        targetView = target,
                        onNext = {
                            onStop()
                            StorageCleanupDialog.show(activity, store)
                        },
                        onExit = { onStop() }
                    )
                }, 250)
            }

            "vault_alert_widget" -> {
                activity.navigateToTab(0)
                activity.binding.root.postDelayed({
                    val target = activity.binding.navHome
                    overlay.showStep(
                        stepIndex = 1,
                        totalSteps = 1,
                        title = "📱 12 馆时效预警桌面小组件 (2×2)",
                        desc = "长按手机桌面空白处添加「⚠️ 今日待处理」小组件，无需打开 App 即可一眼查看最紧急的 4 条待办提醒（临期卡券、到期药品、缺水绿植等），随数据变更自动秒级同步！",
                        actionHint = "👉 点击完成了解桌面小组件",
                        targetView = target,
                        onNext = { onStop() },
                        onExit = { onStop() }
                    )
                }, 200)
            }

            else -> {
                fallbackTour()
            }
        }
    }
}
