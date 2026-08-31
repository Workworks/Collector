package com.kfaino.diapertracker

import java.util.Calendar
import java.util.UUID

/** 物品外借流转与归还记录 */
data class LendingRecord(
    val id: String = UUID.randomUUID().toString(),
    val borrowerName: String = "",                  // 借用人姓名 (如 "张三", "老王", "表弟")
    val borrowerContact: String = "",               // 借用人联系方式 (手机/微信)
    val lentDate: Long = System.currentTimeMillis(),// 借出交接日期
    val expectedReturnDate: Long = 0L,              // 约定预计归还日期 (0表示未指定)
    val actualReturnDate: Long = 0L,                // 实际归还日期 (0表示尚未归还)
    val deposit: Double = 0.0,                      // 押金/租金 (元)
    val notes: String = "",                         // 借出配件与交接说明 (如 "含原装充电器、保护包")
    val photoPath: String = "",                     // 交接现场实拍留存照片
    val status: String = "lent",                    // "lent" (借出中), "returned" (已归还), "overdue" (已逾期)
    val returnConditionRating: Int = 5              // 归还时物品成色/完好度 (1~5星)
)

/** 物品时光胶囊与生活回忆瞬间 */
data class ItemMemoryMoment(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",                         // 时光事件标题 (如 "带它登顶泰山看日出", "录制第一支吉他弹唱")
    val story: String = "",                         // 回忆故事 / 心得随笔
    val photoPath: String = "",                     // 故事现场实拍/相片留存
    val date: Long = System.currentTimeMillis(),    // 发生日期
    val moodEmoji: String = "✨",                   // 心情/氛围 Emoji (如 "🏔️", "❤️", "🎂", "🎸", "🚗")
    val rating: Int = 5                             // 心动/真香指数 (1~5星)
)

/** 物品挪动与位置变迁历史记录 */
data class LocationMovement(
    val location: String,                           // 放置地点名称 (如 "主卧衣柜二层")
    val houseName: String = "我的家",                // 所属空间名称 (如 "🏠 自己的家")
    val roomName: String = "主卧",                   // 所属房间名称 (如 "主卧")
    val pinX: Float = -1f,                          // 平面图相对坐标 X (0.0 - 1.0)
    val pinY: Float = -1f,                          // 平面图相对坐标 Y (0.0 - 1.0)
    val movedAt: Long = System.currentTimeMillis(), // 移动时间戳
    val note: String = ""                           // 挪动备注 (如 "从客厅茶几移入")
)

/** 一条出入库/记账记录（集成折旧拥有天数、在役/退役待办归置、订阅型资产体系） */
data class Entry(
    val id: String = UUID.randomUUID().toString(),
    val category: String = "通用",       // 分类: 数码, 日用品, 零食, 耗材等
    val brand: String = "物品",          // 品牌/物品名称
    val qty: Int = 1,           // 数量
    val price: Double = 0.0,    // 购买原价 / 单价（元/单位）
    val currentValuation: Double = 0.0, // 当前二手/折旧估值 (元，若为0则默认按原价)
    val purchaseDate: Long = System.currentTimeMillis(), // 购入日期 (用于计算拥有天数和日均成本)
    val ts: Long = System.currentTimeMillis(),           // 记录时间戳
    val isIn: Boolean = true,   // true=增加(入库/购入), false=减少(出库/消耗)
    val notes: String = "",     // 备注
    val unit: String = "件",    // 数量单位: 件, 台, 片, 包, 个, 箱, 瓶等

    // 空间与位置体系 (选填)
    val location: String = "",                      // 当前放置位置 (如 "主卧衣柜二层")
    val houseId: String = "default_house",          // 所属空间 ID
    val houseName: String = "我的家",                // 所属空间名称
    val roomName: String = "",                      // 所属房间名称 (如 "主卧")
    val pinX: Float = -1f,                          // 平面图标记 X (0.0 ~ 1.0)
    val pinY: Float = -1f,                          // 平面图标记 Y (0.0 ~ 1.0)
    val locationHistory: List<LocationMovement> = emptyList(), // 历史移动轨迹

    // 重要物品防丢与订阅提醒体系
    val isImportant: Boolean = false,               // 是否为重要贵重物品 (钥匙/身份证/房产证/贵重首饰等)
    val reminderEnabled: Boolean = false,           // 是否开启定期核对订阅提醒
    val reminderIntervalDays: Int = 1,              // 提醒间隔天数 (1=每天, 3=每3天, 7=每周, 15=半月, 30=每月)
    val reminderTime: String = "09:00",             // 每日提醒时间
    val lastCheckedAt: Long = 0L,                   // 上次核对位置确认时间戳

    // 3. 物品在役 / 退役与待办归置系统
    val isRetired: Boolean = false,                 // false=在役, true=已退役 (如闲鱼代售、封存)
    val retiredAt: Long = 0L,                       // 退役时间戳
    val retiredAction: String = "",                 // 待办归置渠道 (如 "挂闲鱼代售", "挂转转代售", "赠送亲友", "封箱入库", "环保回收", "报废")
    val retiredSoldPrice: Double = 0.0,             // 二手出掉回血金额 (如有)
    val retiredNote: String = "",                   // 退役归置备注

    // 2. 按期订阅型资产体系 (如 iCloud, ChatGPT, Netflix, 宽带, 健身年卡)
    val isSubscription: Boolean = false,            // 是否为订阅型资产
    val subCycle: String = "按月",                  // 订阅周期: "按月", "按年", "按季", "按周"
    val subNextBillingDate: Long = 0L,              // 下次扣费日期
    val subAutoRenew: Boolean = true,               // 是否自动续费

    // 4. 纠正需求：细分物品类型 (折旧资产, 保质期物品, 长期使用耐用品, 日常消耗品)
    val assetType: String = "consumable",           // "depreciating" (折旧资产), "expiring" (保质期物品), "durable" (长期使用), "consumable" (日常消耗品)
    val manufactureDate: Long = 0L,                 // 生产日期
    val expiryDate: Long = 0L,                      // 到期日期

    // 5. 实物照片与购买发票/保修卡留存 (沙盒私有路径)
    val photoPath: String = "",                     // 物品实物照片文件名/相对路径
    val receiptPath: String = "",                   // 购买发票/凭证/保修卡照片文件名/相对路径

    // 6. 安全库存预警体系
    val minStockThreshold: Int = 0,                 // 最低安全库存预警阈值 (0=不预警, >0=当在库数量<=该值时触发补货清单)

    // 7. 数字与电子资产体系 (Digital Assets & Electronic Albums)
    val isDigital: Boolean = false,                 // 是否为数字/电子资产 (相册集/软件Key/域名/数字藏品/教程)
    val digitalType: String = "album",             // "album" (照片相册/回忆集), "software" (软件/游戏/授权Key), "domain" (域名/网站), "doc" (电子书/课程/资料包)
    val digitalUrl: String = "",                    // 访问链接 / 网盘存储路径 / 本地路径
    val digitalSize: String = "",                   // 容量大小 (如 "128 GB", "4.2 MB")
    val digitalLicenseKey: String = "",             // 激活码 / 许可证 / 授权凭证 (可快速一键复制)
    val backupStatus: String = "local",             // "local" (仅本地), "synced" (已备份网盘/NAS), "unbacked" (未备份)

    // 8. 物品时光胶囊与回忆录 (Life Memory Moments)
    val memoryMoments: List<ItemMemoryMoment> = emptyList(), // 时光回忆里程碑列表

    // 9. 实物外借与共享流转体系 (Asset Lending & Circulation)
    val isLentOut: Boolean = false,                 // 是否处于外借状态
    val currentBorrower: String = "",               // 当前借用人姓名 (如 "张三", "老王")
    val currentBorrowerContact: String = "",        // 当前借用人联系方式 (手机/微信)
    val currentLentDate: Long = 0L,                 // 当前借出时间戳
    val expectedReturnDate: Long = 0L,              // 约定预计归还日期 (0表示未指定)
    val currentDeposit: Double = 0.0,               // 当前已付押金 (元)
    val lendingHistory: List<LendingRecord> = emptyList(), // 历史流转与借还记录

    // 10. 全家耐用资产维保与年检周期体系 (Asset Maintenance & Service Cycles)
    val maintenanceIntervalMonths: Int = 0,        // 定期维保周期 (月数, 0=无维保, 3=每季, 6=每半年, 12=每年)
    val lastMaintainedAt: Long = 0L,               // 上次完成维保时间戳
    val maintenanceNotes: String = ""              // 维保备忘/耗材型号 (如 "RO反渗透滤芯 400G")
) {
    /** 是否配置了定期维保周期 */
    fun isMaintenanceEnabled(): Boolean = maintenanceIntervalMonths > 0

    /** 获取下次维保计划日期 */
    fun getNextMaintenanceDate(): Long {
        if (maintenanceIntervalMonths <= 0) return 0L
        val baseDate = if (lastMaintainedAt > 0L) lastMaintainedAt else purchaseDate
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = baseDate
        cal.add(java.util.Calendar.MONTH, maintenanceIntervalMonths)
        return cal.timeInMillis
    }

    /** 获取距离下次维保的剩余天数 (负数表示已逾期超期) */
    fun getMaintenanceRemainingDays(): Int {
        val nextDate = getNextMaintenanceDate()
        if (nextDate <= 0L) return 9999
        val diff = nextDate - System.currentTimeMillis()
        return (diff / (24L * 60 * 60 * 1000)).toInt()
    }

    /** 是否处于逾期未还状态 */
    fun isLendingOverdue(): Boolean {
        if (!isLentOut || expectedReturnDate <= 0L) return false
        return System.currentTimeMillis() > expectedReturnDate
    }

    /** 获取借出流转状态文本 */
    fun getLendingStatusText(): String {
        if (!isLentOut) return "🟢 在库"
        val now = System.currentTimeMillis()
        if (expectedReturnDate > 0L) {
            val diffMs = expectedReturnDate - now
            val days = (diffMs / (24L * 60 * 60 * 1000)).toInt()
            return if (days < 0) {
                "⚠️ 借给 $currentBorrower · 逾期 ${kotlin.math.abs(days)} 天"
            } else if (days == 0) {
                "⏰ 借给 $currentBorrower · 今日到期"
            } else {
                "📤 借给 $currentBorrower · 剩 $days 天"
            }
        }
        val daysLent = if (currentLentDate > 0) ((now - currentLentDate) / (24L * 60 * 60 * 1000)).toInt().coerceAtLeast(1) else 1
        return "📤 借给 $currentBorrower · 已借 $daysLent 天"
    }

    /** 获取当前活跃的借出记录 */
    fun getCurrentLendingRecord(): LendingRecord? {
        if (!isLentOut) return null
        return lendingHistory.firstOrNull { it.status == "lent" || it.actualReturnDate == 0L }
            ?: lendingHistory.lastOrNull()
    }

    /** 获取数字资产类型描述 */
    fun getDigitalTypeDisplayName(): String {
        return when (digitalType) {
            "album" -> "📷 照片相册集"
            "software" -> "🔑 软件/游戏授权"
            "domain" -> "🌐 域名/网站"
            "doc" -> "📚 文档/课程资料"
            else -> "💾 数字资产"
        }
    }

    /** 获取时光胶囊总回忆数 */
    fun getMemoryCount(): Int = memoryMoments.size

    /** 获取平均心动真香评分 */
    fun getAverageRating(): Float {
        if (memoryMoments.isEmpty()) return 5.0f
        return memoryMoments.map { it.rating }.average().toFloat()
    }
    /** 是否处于低库存缺货预警状态 */
    fun isLowStock(): Boolean {
        return isIn && !isRetired && minStockThreshold > 0 && qty <= minStockThreshold
    }
    /** 获取到期状态描述 */
    fun getExpiryStatusText(): String {
        if (expiryDate <= 0L) return ""
        val now = System.currentTimeMillis()
        val diffMs = expiryDate - now
        val days = (diffMs / (24L * 60 * 60 * 1000)).toInt()
        return if (days < 0) {
            "🔴 已过期 ${Math.abs(days)} 天"
        } else {
            "⏳ 剩 $days 天过期"
        }
    }

    /** 获取类型中文描述 */
    fun getAssetTypeDisplayName(): String {
        return when (assetType) {
            "depreciating" -> "折旧资产"
            "expiring" -> "保质期"
            "durable" -> "长期持有"
            else -> "消耗品"
        }
    }
    /** 计算拥有天数 */
    fun getDaysOwned(): Int {
        val endTime = if (isRetired && retiredAt > 0) retiredAt else System.currentTimeMillis()
        val days = ((endTime - purchaseDate) / (24L * 60 * 60 * 1000)).toInt()
        return days.coerceAtLeast(1)
    }

    /** 计算日均使用成本 (元/天) */
    fun getDailyCost(): Double {
        val days = getDaysOwned().toDouble()
        val totalCost = price * qty
        return if (isRetired && retiredSoldPrice > 0) {
            (totalCost - retiredSoldPrice).coerceAtLeast(0.0) / days
        } else if (currentValuation > 0 && currentValuation < totalCost) {
            (totalCost - currentValuation).coerceAtLeast(0.0) / days
        } else {
            totalCost / days
        }
    }
}

/** 空间房间模型 */
data class HouseRoom(
    val id: String = UUID.randomUUID().toString(),
    val name: String,             // 房间名称 (如 "客厅", "主卧", "玄关", "储物间")
    val icon: String = "🛋️",
    val colorHex: String = "#10B981",
    val xPct: Float = 0.1f,       // 房间左上角 X (0.0 ~ 1.0)
    val yPct: Float = 0.1f,       // 房间左上角 Y (0.0 ~ 1.0)
    val widthPct: Float = 0.35f,  // 房间宽 (0.0 ~ 1.0)
    val heightPct: Float = 0.35f  // 房间高 (0.0 ~ 1.0)
)

/** 家庭/空间模型 */
data class HouseSpace(
    val id: String = UUID.randomUUID().toString(),
    val name: String,             // 空间名称 (如 "🏠 自己的家", "🏡 父母家", "🏢 办公室", "🚗 汽车")
    val type: String = "住宅",     // 空间类型
    val rooms: List<HouseRoom> = defaultRooms(),
    val isDefault: Boolean = false
) {
    companion object {
        fun defaultRooms(): List<HouseRoom> = listOf(
            HouseRoom(name = "玄关", icon = "🚪", colorHex = "#3B82F6", xPct = 0.05f, yPct = 0.05f, widthPct = 0.28f, heightPct = 0.25f),
            HouseRoom(name = "客厅", icon = "🛋️", colorHex = "#10B981", xPct = 0.36f, yPct = 0.05f, widthPct = 0.58f, heightPct = 0.42f),
            HouseRoom(name = "厨房", icon = "🍳", colorHex = "#F59E0B", xPct = 0.05f, yPct = 0.33f, widthPct = 0.28f, heightPct = 0.30f),
            HouseRoom(name = "主卧", icon = "🛏️", colorHex = "#8B5CF6", xPct = 0.05f, yPct = 0.66f, widthPct = 0.43f, heightPct = 0.29f),
            HouseRoom(name = "次卧", icon = "🛌", colorHex = "#EC4899", xPct = 0.51f, yPct = 0.50f, widthPct = 0.43f, heightPct = 0.22f),
            HouseRoom(name = "储物间", icon = "📦", colorHex = "#06B6D4", xPct = 0.51f, yPct = 0.75f, widthPct = 0.43f, heightPct = 0.20f)
        )
    }
}

/** 某分类下的品牌/物品汇总 */
data class BrandSummary(
    val name: String,
    val count: Int,             // 当前库存（增加-减少）
    val amount: Double,         // 累计花费（仅增加）
    val avgPrice: Double,       // 平均单价
    val unit: String = "件",    // 常用单位
    val location: String = "",  // 当前最新放置位置
    val houseName: String = "", // 所属空间
    val isImportant: Boolean = false,
    val isRetired: Boolean = false,
    val retiredAction: String = "",
    val daysOwned: Int = 1,
    val dailyCost: Double = 0.0,
    val isSubscription: Boolean = false,
    val subCycle: String = "按月",
    val subNextBillingDate: Long = 0L,
    val currentValuation: Double = 0.0
)

/** 分类汇总 */
data class CategoryGroup(
    val name: String,           // 分类名
    val brands: List<BrandSummary>,
    val totalCount: Int,
    val totalAmount: Double,
    val unit: String = "件"
)

/** 月度统计模型（账本报表） */
data class MonthStat(
    val year: Int,
    val month: Int,            // 1-12
    val addCount: Int,         // 增加数量
    val addAmount: Double,     // 增加金额 (本月总投入)
    val reduceCount: Int,      // 减少数量 (本月总消耗)
    val entryCount: Int,       // 总笔数
    val topItems: List<String> = emptyList() // 当月主要记账项目
)

/** 多账本独立空间数据模型 */
data class Ledger(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "默认账本",
    val icon: String = "🏠",
    val desc: String = "个人与家庭资产",
    val createdAt: Long = System.currentTimeMillis()
)

// =========================================================================
// 🎟️ 第一性原理收纳：时效权益与卡券票据收纳模型 (Voucher & Privilege)
// =========================================================================

data class VoucherRecord(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",                         // 券名/权益名称 (如 "美团外卖满30减10", "途虎洗车次卡", "星巴克大杯兑换券")
    val type: String = "coupon",                    // "coupon" (满减/立减优惠券), "times_card" (计次卡), "cash_voucher" (代金券), "privilege" (会员月度权益)
    val valueAmount: Double = 0.0,                  // 面额金额 (如 10.0 元, 50.0 元)
    val minSpend: Double = 0.0,                     // 门槛要求 (如 满30元 可用, 0为无门槛)
    val remainingTimes: Int = 1,                    // 次卡剩余可用次数
    val totalTimes: Int = 1,                        // 次卡总次数
    val startDate: Long = System.currentTimeMillis(), // 生效时间
    val expiryDate: Long = 0L,                      // 到期时间 (0表示长期有效)
    val code: String = "",                          // 核销码/券码/兑换码
    val platform: String = "",                      // 适用平台/商家 (如 "美团", "京东", "山姆", "线下门店")
    val photoPath: String = "",                     // 卡券截图/条码凭证照片
    val notes: String = "",                         // 使用规则与限制说明
    val isUsed: Boolean = false,                    // 是否已用完/已核销
    val usedAt: Long = 0L                           // 核销归档时间戳
) {
    /** 是否在 3 天内即将到期 */
    fun isExpiringSoon(): Boolean {
        if (isUsed || expiryDate <= 0L) return false
        val diffMs = expiryDate - System.currentTimeMillis()
        return diffMs in 0..(3L * 24 * 60 * 60 * 1000)
    }

    /** 是否已过期作废 */
    fun isExpired(): Boolean {
        if (isUsed || expiryDate <= 0L) return false
        return System.currentTimeMillis() > expiryDate
    }

    /** 获取类型中文名称 */
    fun getTypeDisplayName(): String {
        return when (type) {
            "coupon" -> "🎟️ 满减优惠券"
            "times_card" -> "🎫 计次服务卡"
            "cash_voucher" -> "💰 无门槛代金券"
            "privilege" -> "👑 会员专属权益"
            else -> "🎟️ 权益券"
        }
    }

    /** 获取面额显示文本 */
    fun getDisplayValue(): String {
        return if (type == "times_card") {
            "余 $remainingTimes/$totalTimes 次"
        } else if (valueAmount > 0) {
            "¥${if (valueAmount % 1.0 == 0.0) valueAmount.toInt().toString() else String.format("%.2f", valueAmount)}"
        } else {
            "专享权益"
        }
    }
}

// =========================================================================
// 🪪 第一性原理收纳：家庭多成员证照与敏感凭证模型 (Family Identity & Safe)
// =========================================================================

data class IdentityDocument(
    val id: String = UUID.randomUUID().toString(),
    val member: String = "本人",                    // 成员归属: "本人", "伴侣", "孩子", "父亲", "母亲", "长辈"
    val docType: String = "id_card",                // "id_card" (身份证), "passport" (护照), "hk_macau_pass" (港澳通行证), "driver_license" (驾驶证), "household" (户口本), "marriage" (结婚证), "property" (房产证), "contract" (合同), "other" (其他)
    val docNumber: String = "",                     // 证件号码 / 统一信用代码
    val nameOnDoc: String = "",                     // 证件姓名
    val issueDate: Long = 0L,                       // 签发日期
    val expiryDate: Long = 0L,                      // 有效期截止日 (0表示长期有效)
    val frontPhotoPath: String = "",                // 正面扫描/实拍照
    val backPhotoPath: String = "",                 // 反面扫描/国徽面照
    val issuingAuthority: String = "",              // 签发机关
    val notes: String = ""                          // 备注
) {
    /** 是否在 180 天 (半年) 内即将到期需换证 */
    fun isExpiringSoon(): Boolean {
        if (expiryDate <= 0L) return false
        val diffMs = expiryDate - System.currentTimeMillis()
        return diffMs in 0..(180L * 24 * 60 * 60 * 1000)
    }

    /** 是否已过期 */
    fun isExpired(): Boolean {
        if (expiryDate <= 0L) return false
        return System.currentTimeMillis() > expiryDate
    }

    /** 获取脱敏证件号 (例如 110101********1234) */
    fun getMaskedNumber(): String {
        if (docNumber.length <= 6) return docNumber
        val prefix = docNumber.take(4)
        val suffix = docNumber.takeLast(4)
        return "$prefix${"*".repeat((docNumber.length - 8).coerceAtLeast(4))}$suffix"
    }

    /** 获取证件类型中文名 */
    fun getDocTypeDisplayName(): String {
        return when (docType) {
            "id_card" -> "🪪 居民身份证"
            "passport" -> "🛂 出入境护照"
            "hk_macau_pass" -> "🧳 港澳通行证"
            "driver_license" -> "🚗 机动车驾驶证"
            "household" -> "👨‍👩‍👧 居民户口簿"
            "marriage" -> "💍 结婚证/公证书"
            "property" -> "🏠 不动产权证"
            "insurance" -> "🛡️ 商业保单契约"
            "contract" -> "📄 电子劳动/租赁合同"
            else -> "📑 重要证照凭证"
        }
    }
}

// =========================================================================
// 💊 第一性原理收纳：家庭智能健康药箱模型 (Medicine & Scenario Vault)
// =========================================================================

data class MedicineRecord(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",                          // 药品名称 (如 "布洛芬缓释胶囊", "氯雷他定片", "左氧氟沙星滴眼液")
    val category: String = "fever",                 // "fever" (发热镇痛), "cold" (感冒咳嗽), "digest" (肠胃消化), "trauma" (外伤消炎), "allergy" (抗过敏), "chronic" (慢病常备), "other" (其他)
    val form: String = "片剂",                      // 剂型: "片剂", "胶囊", "颗粒/冲剂", "口服液", "外用喷剂/眼药水", "敷料/贴膏"
    val qty: Int = 1,                               // 余量
    val unit: String = "盒",                        // 单位: 盒, 瓶, 支, 板, 袋
    val location: String = "家庭急救药箱",          // 存放位置
    val dosage: String = "",                        // 用法用量 (如 "成人一次 1 粒，一日 2 次，饭后温水服用")
    val targetAudience: String = "全家通用",        // 适用人群: "全家通用", "成人专用", "儿童专用"
    val expiryDate: Long = 0L,                      // 未开封保质期
    val isOpened: Boolean = false,                  // 是否已开封
    val openedAt: Long = 0L,                        // 开封时间戳
    val openedValidityDays: Int = 0,                // 开封后有效期天数 (如滴眼液开封 28 天到期, 0表示按原保质期)
    val photoPath: String = "",                     // 包装盒/说明书照片
    val contraindications: String = ""              // 禁忌与注意事项 (如 "服药期间严禁饮酒")
) {
    /** 计算实际有效截止时间戳 (综合原保质期与开封后时效) */
    fun getEffectiveExpiryDate(): Long {
        if (isOpened && openedValidityDays > 0 && openedAt > 0L) {
            val openedExpire = openedAt + (openedValidityDays.toLong() * 24 * 60 * 60 * 1000)
            return if (expiryDate > 0L) Math.min(expiryDate, openedExpire) else openedExpire
        }
        return expiryDate
    }

    /** 是否已过期 */
    fun isExpired(): Boolean {
        val eff = getEffectiveExpiryDate()
        if (eff <= 0L) return false
        return System.currentTimeMillis() > eff
    }

    /** 是否临近到期 (30天内) */
    fun isExpiringSoon(): Boolean {
        val eff = getEffectiveExpiryDate()
        if (eff <= 0L) return false
        val diffMs = eff - System.currentTimeMillis()
        return diffMs in 0..(30L * 24 * 60 * 60 * 1000)
    }

    /** 获取过期状态描述 */
    fun getExpiryStatusText(): String {
        val eff = getEffectiveExpiryDate()
        if (eff <= 0L) return "🟢 长期有效"
        val now = System.currentTimeMillis()
        val diffMs = eff - now
        val days = (diffMs / (24L * 60 * 60 * 1000)).toInt()
        return if (days < 0) {
            "🔴 已过期 ${Math.abs(days)} 天 (🚫 严禁服用)"
        } else if (days <= 30) {
            "⏳ 仅剩 $days 天到期 (请尽快使用)"
        } else {
            "🟢 剩余 $days 天"
        }
    }

    /** 获取分类中文名称 */
    fun getCategoryDisplayName(): String {
        return when (category) {
            "fever" -> "🤒 发烧镇痛"
            "cold" -> "🤧 感冒咳嗽"
            "digest" -> "🤢 肠胃消化"
            "trauma" -> "🩹 外伤消炎"
            "allergy" -> "🌿 抗过敏"
            "chronic" -> "💊 慢病常备"
            else -> "📦 其他常备"
        }
    }
}

// =========================================================================
// 🥦 第一性原理收纳：冰箱冷冻与食材生鲜鲜度库模型 (Food & Fresh Vault)
// =========================================================================

data class FoodRecord(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",                          // 食材名称 (如 "安格斯原切牛排", "鲜牛奶", "有机菠菜", "自制冷冻水饺")
    val zone: String = "freezer",                   // 分区温区: "freezer" (❄️ 冷冻室), "fridge" (🧊 冷藏室), "pantry" (🫙 干货调味), "room" (🍞 常温果蔬)
    val qty: Double = 1.0,                          // 数量
    val unit: String = "份",                        // 单位: 份, 袋, 盒, 瓶, kg, g, 斤
    val location: String = "冰箱冷冻二层",          // 具体位置
    val purchaseDate: Long = System.currentTimeMillis(), // 买入/冷冻日期
    val expiryDate: Long = 0L,                      // 保质期截止日 (0表示长期)
    val isOpened: Boolean = false,                  // 是否已开封
    val openedAt: Long = 0L,                        // 开封时间戳
    val openedValidityDays: Int = 0,                // 开封后建议几天内吃完 (如鲜奶开封 3 天, 0表示按原保质期)
    val photoPath: String = "",                     // 照片
    val notes: String = "",                         // 烹饪灵感/解冻提醒/备注
    val isConsumed: Boolean = false                 // 是否已吃完/已出清
) {
    /** 计算实际有效截止时间戳 (综合原保质期与开封后保鲜期) */
    fun getEffectiveExpiryDate(): Long {
        if (isOpened && openedValidityDays > 0 && openedAt > 0L) {
            val openedExpire = openedAt + (openedValidityDays.toLong() * 24 * 60 * 60 * 1000)
            return if (expiryDate > 0L) Math.min(expiryDate, openedExpire) else openedExpire
        }
        return expiryDate
    }

    /** 是否已过期 */
    fun isExpired(): Boolean {
        val eff = getEffectiveExpiryDate()
        if (eff <= 0L) return false
        return System.currentTimeMillis() > eff
    }

    /** 是否临期急需消灭 (3 天内) */
    fun isExpiringSoon(): Boolean {
        val eff = getEffectiveExpiryDate()
        if (eff <= 0L) return false
        val diffMs = eff - System.currentTimeMillis()
        return diffMs in 0..(3L * 24 * 60 * 60 * 1000)
    }

    /** 剩余保鲜天数 */
    fun getRemainingDays(): Int {
        val eff = getEffectiveExpiryDate()
        if (eff <= 0L) return 9999
        val diffMs = eff - System.currentTimeMillis()
        return (diffMs / (24L * 60 * 60 * 1000)).toInt()
    }

    /** 获取鲜度状态文本描述 */
    fun getFreshnessStatusText(): String {
        val eff = getEffectiveExpiryDate()
        if (eff <= 0L) return "🟢 长期在库"
        val days = getRemainingDays()
        return if (days < 0) {
            "🔴 已过期 ${Math.abs(days)} 天 (⚠️ 请勿食用)"
        } else if (days <= 3) {
            "⏳ 仅剩 $days 天 (🍳 建议今晚消灭)"
        } else if (days <= 7) {
            "🟡 剩余 $days 天 (保鲜中)"
        } else {
            "🟢 极鲜 (剩余 $days 天)"
        }
    }

    /** 获取分区温区中文名称 */
    fun getZoneDisplayName(): String {
        return when (zone) {
            "freezer" -> "❄️ 冷冻室"
            "fridge" -> "🧊 冷藏室"
            "pantry" -> "🫙 干货调味"
            "room" -> "🍞 常温果蔬"
            else -> "📦 食品储藏"
        }
    }
}

// =========================================================================
// 🏆 第一性原理收纳：全家成长履历与职业荣誉考级勋章馆模型 (Honor & Credentials)
// =========================================================================

data class HonorCredential(
    val id: String = UUID.randomUUID().toString(),
    val member: String = "本人",                    // 成员归属: "本人", "伴侣", "孩子", "父亲", "母亲"
    val category: String = "career",                // "degree" (🎓 学历学位), "career" (💼 职业资质/职称), "exam" (🏅 考级认证), "competition" (🏆 竞赛获奖/荣誉), "medal" (🎖️ 赛事勋章)
    val title: String = "",                         // 证书/荣誉名称 (如 "软件设计师 (中级)", "英皇钢琴八级", "全国少儿英语竞赛一等奖")
    val certNumber: String = "",                    // 证书编号/统一序列号
    val issuer: String = "",                        // 颁发机构/发证部门 (如 "中华人民共和国人力资源和社会保障部")
    val issueDate: Long = 0L,                       // 获得/发证时间戳
    val expiryDate: Long = 0L,                      // 有效期截止日 / 复审年审日期 (0表示终身有效)
    val scoreOrLevel: String = "",                  // 成绩/等级/名次 (如 "优秀", "一等奖", "92分")
    val photoPath: String = "",                     // 证书原件/奖状扫描照
    val verifyUrl: String = "",                     // 官方查验网址/验证链接
    val notes: String = ""                          // 成长足迹故事/备注
) {
    /** 是否需要在 90 天内复审/换证 */
    fun isExpiringSoon(): Boolean {
        if (expiryDate <= 0L) return false
        val diffMs = expiryDate - System.currentTimeMillis()
        return diffMs in 0..(90L * 24 * 60 * 60 * 1000)
    }

    /** 是否已过复审换证有效期 */
    fun isExpired(): Boolean {
        if (expiryDate <= 0L) return false
        return System.currentTimeMillis() > expiryDate
    }

    /** 获取脱敏证书编号 (如 202401********8899) */
    fun getMaskedCertNumber(): String {
        if (certNumber.length <= 6) return certNumber
        val prefix = certNumber.take(4)
        val suffix = certNumber.takeLast(4)
        return "$prefix${"*".repeat((certNumber.length - 8).coerceAtLeast(4))}$suffix"
    }

    /** 获取分类中文名称 */
    fun getCategoryDisplayName(): String {
        return when (category) {
            "degree" -> "🎓 学历学位"
            "career" -> "💼 职业资格与职称"
            "exam" -> "🏅 技能考级认证"
            "competition" -> "🏆 竞赛荣誉与奖状"
            "medal" -> "🎖️ 赛事勋章与奖牌"
            else -> "📑 荣誉证书"
        }
    }
}

// =========================================================================
// 👗 第一性原理收纳：换季衣橱、四季穿搭与封箱收纳舱模型 (Wardrobe & Seasonal Closet)
// =========================================================================

data class WardrobeRecord(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",                         // 衣物/单品名称 (如 "波司登极寒鹅绒羽绒服", "Acne Studios 羊绒围巾", "无印良品纯棉衬衫")
    val season: String = "winter",                 // 适用季节: "spring_autumn" (🌸 春秋), "summer" (☀️ 夏季), "winter" (🍂 冬季), "all_season" (♾️ 四季通用)
    val category: String = "coat",                 // 类别: "coat" (🧥 外套/羽绒), "top" (👕 上衣/衬衫), "pants" (👖 裤装/裙装), "shoes" (👟 鞋履), "accessory" (🧣 配件/包袋), "bedding" (🛏️ 换季被褥)
    val color: String = "",                        // 颜色系 (如 "米白", "藏青", "卡其", "纯黑")
    val material: String = "",                     // 面料材质 (如 "90%白鹅绒", "100%山羊绒", "重磅真丝", "精梳棉")
    val careNotes: String = "",                    // 洗护与保养说明 (如 "仅限专业干洗", "不可烘干", "防虫袋悬挂")
    val storageLocation: String = "",              // 存放收纳位置 (如 "主卧大衣柜上层", "真空压缩袋 #03", "榻榻米底抽")
    val purchasePrice: Double = 0.0,               // 购入单价
    val purchaseDate: Long = 0L,                   // 购入日期
    val wearCount: Int = 0,                        // 穿着打卡次数
    val lastWornAt: Long = 0L,                     // 最近一次穿着/出镜日期
    val isSealed: Boolean = false,                 // 是否处于封箱/真空收纳防尘状态
    val sealedAt: Long = 0L,                       // 封箱归档时间
    val photoPath: String = "",                    // 实物穿搭照/平铺照私有沙盒路径
    val notes: String = ""                         // 搭配灵感与备忘
) {
    /** 计算穿戴次均成本 (元/次) */
    fun getCostPerWear(): Double {
        if (purchasePrice <= 0.0) return 0.0
        val count = wearCount.coerceAtLeast(1)
        return purchasePrice / count
    }

    /** 是否长期沉睡未穿 (超过 180 天未打卡且非封箱状态) */
    fun isSleeping(): Boolean {
        if (isSealed || lastWornAt <= 0L) return false
        val diffMs = System.currentTimeMillis() - lastWornAt
        return diffMs > 180L * 24 * 60 * 60 * 1000
    }

    /** 季节展示名称 */
    fun getSeasonDisplayName(): String {
        return when (season) {
            "spring_autumn" -> "🌸 春秋季"
            "summer" -> "☀️ 夏季"
            "winter" -> "🍂 冬季"
            else -> "♾️ 四季通用"
        }
    }

    /** 品类展示名称 */
    fun getCategoryDisplayName(): String {
        return when (category) {
            "coat" -> "🧥 外套羽绒"
            "top" -> "👕 上衣衬衫"
            "pants" -> "👖 裤装裙装"
            "shoes" -> "👟 鞋靴箱包"
            "accessory" -> "🧣 配件饰品"
            "bedding" -> "🛏️ 床品被褥"
            else -> "👗 经典服饰"
        }
    }
}

// =========================================================================
// 🚨 第一性原理收纳：家庭应急防灾、避难包与生命线收纳模型 (Emergency & Survival Vault)
// =========================================================================

data class EmergencyItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",                         // 物资名称 (如 "手摇发电多波段收音机", "高能压缩饼干 (500g)", "火灾逃生过滤式自救呼吸器", "车载多功能破窗安全带割刀")
    val kitType: String = "earthquake",            // 场景包: "earthquake" (🏠 地震避险72h), "fire" (🔥 火灾高层逃生), "car" (🚗 车载应急救援), "flood" (🌧️ 暴雨防汛与极端天气), "general" (🛡️ 常备应急急救)
    val category: String = "tool",                 // 类别: "food_water" (🍞 食品饮水), "medical" (🩹 急救医疗), "tool" (🔦 求救工具), "protection" (🦺 防护保暖)
    val qty: Double = 1.0,                         // 数量
    val unit: String = "件",                       // 计量单位 (瓶/盒/件/包)
    val location: String = "",                     // 存放收纳位置 (如 "玄关防盗门后收纳挂架", "主卧床头储物格", "后备箱右侧储物网兜")
    val expiryDate: Long = 0L,                     // 保质截止日 / 滤毒罐失效期 (0 表示长期有效)
    val rotationIntervalMonths: Int = 0,           // 轮换检查周期月数 (如 电池/饮用水 6个月检查一次，0表示不设限)
    val lastCheckedAt: Long = 0L,                  // 最近一次点检/测试日期
    val notes: String = "",                        // 使用要领与操作备忘
    val photoPath: String = ""                     // 物资实物照片私有沙盒路径
) {
    /** 是否在 30 天内即将到期/失效 */
    fun isExpiringSoon(): Boolean {
        if (expiryDate <= 0L) return false
        val diffMs = expiryDate - System.currentTimeMillis()
        return diffMs in 0..(30L * 24 * 60 * 60 * 1000)
    }

    /** 是否已过期/失效（必须立即轮换替换） */
    fun isExpired(): Boolean {
        if (expiryDate <= 0L) return false
        return System.currentTimeMillis() > expiryDate
    }

    /** 是否超过轮换周期需要检查测试 */
    fun isNeedsCheck(): Boolean {
        if (rotationIntervalMonths <= 0) return false
        val checkBase = if (lastCheckedAt > 0L) lastCheckedAt else 0L
        if (checkBase == 0L) return true
        val intervalMs = rotationIntervalMonths.toLong() * 30L * 24 * 60 * 60 * 1000
        return (System.currentTimeMillis() - checkBase) > intervalMs
    }

    /** 场景包名称展示 */
    fun getKitTypeDisplayName(): String {
        return when (kitType) {
            "earthquake" -> "🏠 地震避险72h包"
            "fire" -> "🔥 火灾逃生包"
            "car" -> "🚗 车载应急救援包"
            "flood" -> "🌧️ 暴雨防汛极端包"
            else -> "🛡️ 常备应急急救包"
        }
    }

    /** 类别名称展示 */
    fun getCategoryDisplayName(): String {
        return when (category) {
            "food_water" -> "🍞 食品与水"
            "medical" -> "🩹 急救医疗"
            "protection" -> "🦺 防护保暖"
            else -> "🔦 工具照明"
        }
    }
}

// =========================================================================
// 🔧 第一性原理收纳：家庭工具、五金配件与设备维保耗材模型 (Tools & Maintenance Vault)
// =========================================================================

data class ToolMaintenanceRecord(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",                         // 工具/五金/配件名称 (如 "博世 12V 锂电冲击钻", "M6*60 不锈钢膨胀螺丝", "沁园 RO 净水器 400G 滤芯", "生料带 10米")
    val category: String = "power_tool",           // "power_tool" (⚡ 电动工具), "hand_tool" (🔨 手动工具), "hardware" (🔩 五金耗材), "maintenance" (🛠️ 设备维保)
    val spec: String = "",                         // 规格型号 / 适配钻头 (如 "12V 锂电 / 10mm 夹头", "M6*60 (适配8mm钻头)", "RO-400G 复合膜", "宽18mm*厚0.1mm")
    val qty: Double = 1.0,                         // 数量
    val unit: String = "件",                       // 单位 (件/套/只/卷/盒)
    val location: String = "",                     // 存放位置 (如 "阳台五金柜 #01", "地下室工具箱 #02", "水槽下方滤芯备件格")
    val maintenanceIntervalDays: Int = 0,          // 维保/更换周期天数 (如滤芯 180 天, 0 表示长期工具无需周期更换)
    val lastMaintainedAt: Long = 0L,               // 最近维保/更换/点检时间戳
    val notes: String = "",                        // 使用技巧、安全注意与操作备忘
    val photoPath: String = ""                     // 实物照片私有沙盒路径
) {
    /** 计算下一次维保截止时间戳 (0 表示不适用) */
    fun getNextMaintenanceDate(): Long {
        if (maintenanceIntervalDays <= 0) return 0L
        val base = if (lastMaintainedAt > 0L) lastMaintainedAt else 0L
        if (base == 0L) return 0L
        return base + (maintenanceIntervalDays.toLong() * 24 * 60 * 60 * 1000)
    }

    /** 是否已逾期需立即维保/更换 */
    fun isMaintenanceDue(): Boolean {
        val nextDate = getNextMaintenanceDate()
        if (nextDate <= 0L) {
            return maintenanceIntervalDays > 0 && lastMaintainedAt <= 0L
        }
        return System.currentTimeMillis() > nextDate
    }

    /** 是否在 15 天内临期需要维保/更换 */
    fun isMaintenanceDueSoon(): Boolean {
        val nextDate = getNextMaintenanceDate()
        if (nextDate <= 0L) return false
        val diffMs = nextDate - System.currentTimeMillis()
        return diffMs in 0..(15L * 24 * 60 * 60 * 1000)
    }

    /** 类别展示名称 */
    fun getCategoryDisplayName(): String {
        return when (category) {
            "power_tool" -> "⚡ 电动工具"
            "hand_tool" -> "🔨 手工工具"
            "hardware" -> "🔩 五金耗材"
            "maintenance" -> "🛠️ 设备维保"
            else -> "🔧 综合工具"
        }
    }
}

// =========================================================================
// 🪴 第一性原理收纳：家庭绿植花卉、多肉与水肥养护日历模型 (Plant Care Vault)
// =========================================================================

data class PlantCareRecord(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",                         // 绿植昵称/位置标识 (如 "南阳台龟背竹", "客厅电视柜琴叶榕", "书桌姬秋丽多肉")
    val species: String = "",                      // 植物品种 (如 "龟背竹", "琴叶榕", "姬秋丽", "蝴蝶兰")
    val location: String = "",                     // 摆放位置 (如 "南阳台花架上层", "客厅东侧散光位")
    val lightDemand: String = "semi_shade",        // 光照需求: "full_sun" (☀️ 强光全日照), "semi_shade" (⛅ 散光半阴), "shade" (☁️ 喜阴耐阴)
    val waterIntervalDays: Int = 7,                // 浇水周期天数 (如 7 天)
    val lastWateredAt: Long = 0L,                  // 最近一次浇水时间戳
    val fertilizeIntervalDays: Int = 30,           // 施肥周期天数 (如 30 天, 0 表示不设施肥)
    val lastFertilizedAt: Long = 0L,               // 最近一次施肥时间戳
    val careTips: String = "",                     // 养护要领 (如 "表土干透浇透，夏季早晚喷叶面水")
    val photoPath: String = ""                     // 绿植成长照片沙盒路径
) {
    /** 计算下次浇水截止时间戳 */
    fun getNextWaterDate(): Long {
        if (waterIntervalDays <= 0) return 0L
        val base = if (lastWateredAt > 0L) lastWateredAt else 0L
        if (base == 0L) return 0L
        return base + (waterIntervalDays.toLong() * 24 * 60 * 60 * 1000)
    }

    /** 是否逾期需要浇水 */
    fun isWaterDue(): Boolean {
        val nextDate = getNextWaterDate()
        if (nextDate <= 0L) return waterIntervalDays > 0 && lastWateredAt <= 0L
        return System.currentTimeMillis() > nextDate
    }

    /** 是否 1 天内临近需浇水 */
    fun isWaterDueSoon(): Boolean {
        val nextDate = getNextWaterDate()
        if (nextDate <= 0L) return false
        val diffMs = nextDate - System.currentTimeMillis()
        return diffMs in 0..(24L * 60 * 60 * 1000)
    }

    /** 计算下次施肥截止时间戳 */
    fun getNextFertilizeDate(): Long {
        if (fertilizeIntervalDays <= 0) return 0L
        val base = if (lastFertilizedAt > 0L) lastFertilizedAt else 0L
        if (base == 0L) return 0L
        return base + (fertilizeIntervalDays.toLong() * 24 * 60 * 60 * 1000)
    }

    /** 是否逾期需要施肥 */
    fun isFertilizeDue(): Boolean {
        if (fertilizeIntervalDays <= 0) return false
        val nextDate = getNextFertilizeDate()
        if (nextDate <= 0L) return lastFertilizedAt <= 0L
        return System.currentTimeMillis() > nextDate
    }

    /** 光照习性展示名称 */
    fun getLightDemandDisplayName(): String {
        return when (lightDemand) {
            "full_sun" -> "☀️ 全日照"
            "shade" -> "☁️ 耐阴喜阴"
            else -> "⛅ 散光半阴"
        }
    }
}

// =========================================================================
// 🐾 第一性原理收纳：家庭萌宠档案、疫苗驱虫与主粮耗材模型 (Pet Care Vault)
// =========================================================================

data class PetCareRecord(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",                         // 宠物昵称 (如 "咪咪", "布丁", "旺财")
    val species: String = "猫咪",                   // 物种/品种 (如 "英短银渐层", "金毛犬", "玄凤鹦鹉")
    val birthDate: Long = 0L,                      // 出生或领养日期时间戳
    val weightKg: Double = 0.0,                    // 当前体重 (kg)
    val microchipId: String = "",                  // 电子芯片号 / 防疫证编号 (脱敏复制)
    val dewormIntervalDays: Int = 30,              // 体内外驱虫周期天数 (如 30 天，0 表示不定期驱虫)
    val lastDewormedAt: Long = 0L,                 // 最近一次驱虫时间戳
    val vaccineIntervalDays: Int = 365,            // 核心疫苗接种周期天数 (如 365 天/年，0 表示无需定期接种)
    val lastVaccinatedAt: Long = 0L,               // 最近一次疫苗接种时间戳
    val foodBrand: String = "",                    // 主粮品牌与收纳位置 (如 "渴望鸡肉粮 · 阳台储粮桶 #01")
    val notes: String = "",                        // 绝育状态、过敏禁忌与健康病历备忘
    val photoPath: String = ""                     // 萌宠专属生活照沙盒路径
) {
    /** 计算下次驱虫截止时间戳 */
    fun getNextDewormDate(): Long {
        if (dewormIntervalDays <= 0) return 0L
        val base = if (lastDewormedAt > 0L) lastDewormedAt else 0L
        if (base == 0L) return 0L
        return base + (dewormIntervalDays.toLong() * 24 * 60 * 60 * 1000)
    }

    /** 是否逾期需要驱虫 */
    fun isDewormDue(): Boolean {
        val nextDate = getNextDewormDate()
        if (nextDate <= 0L) return dewormIntervalDays > 0 && lastDewormedAt <= 0L
        return System.currentTimeMillis() > nextDate
    }

    /** 是否 7 天内临近需要驱虫 */
    fun isDewormDueSoon(): Boolean {
        val nextDate = getNextDewormDate()
        if (nextDate <= 0L) return false
        val diffMs = nextDate - System.currentTimeMillis()
        return diffMs in 0..(7L * 24 * 60 * 60 * 1000)
    }

    /** 计算下次疫苗接种截止时间戳 */
    fun getNextVaccineDate(): Long {
        if (vaccineIntervalDays <= 0) return 0L
        val base = if (lastVaccinatedAt > 0L) lastVaccinatedAt else 0L
        if (base == 0L) return 0L
        return base + (vaccineIntervalDays.toLong() * 24 * 60 * 60 * 1000)
    }

    /** 是否逾期需要接种疫苗 */
    fun isVaccineDue(): Boolean {
        val nextDate = getNextVaccineDate()
        if (nextDate <= 0L) return vaccineIntervalDays > 0 && lastVaccinatedAt <= 0L
        return System.currentTimeMillis() > nextDate
    }

    /** 是否 30 天内临近需要接种疫苗 */
    fun isVaccineDueSoon(): Boolean {
        val nextDate = getNextVaccineDate()
        if (nextDate <= 0L) return false
        val diffMs = nextDate - System.currentTimeMillis()
        return diffMs in 0..(30L * 24 * 60 * 60 * 1000)
    }
}

// =========================================================================
// 📚 第一性原理收纳：家庭书房藏书、实体借阅与阅读笔记模型 (Bookshelf Vault)
// =========================================================================

data class BookRecord(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",                         // 书名 (如 "三体", "置身事内", "被讨厌的勇气")
    val author: String = "",                        // 作者/译者 (如 "刘慈欣", "兰小欢")
    val category: String = "社科人文",               // 分类 (文学小说 / 社科人文 / 经管商业 / 科技数码 / 亲子绘本 / 其它)
    val bookshelfLocation: String = "",             // 书架物理定位 (如 "书房A柜第2层", "客厅电视柜书架")
    val totalPages: Int = 300,                      // 全书总页数
    val currentPages: Int = 0,                      // 当前已读页数
    val readingStatus: String = "unread",           // 阅读状态: "reading"(在读), "unread"(在架未读), "finished"(已读完), "lent"(外借中)
    val rating: Float = 5.0f,                       // 个人推荐评分 (1.0 ~ 5.0 星)
    val borrowerName: String = "",                  // 借阅人姓名 (如 "同事张伟")
    val lentDate: Long = 0L,                        // 借出时间戳
    val summaryNotes: String = "",                  // 核心书摘、精读心得与金句备忘
    val coverPath: String = ""                      // 封面或实物照片沙盒路径
) {
    /** 计算当前阅读进度百分比 (0~100) */
    fun getProgressPercent(): Int {
        if (totalPages <= 0) return 0
        return ((currentPages.toDouble() / totalPages) * 100.0).toInt().coerceIn(0, 100)
    }

    /** 是否处于外借状态 */
    fun isLent(): Boolean = readingStatus == "lent" || borrowerName.isNotBlank()

    /** 是否已完成阅读 */
    fun isFinished(): Boolean = readingStatus == "finished" || (totalPages > 0 && currentPages >= totalPages)

    /** 是否正在阅读中 */
    fun isReading(): Boolean = readingStatus == "reading" && !isFinished() && !isLent()

    /** 阅读状态展示名称 */
    fun getStatusDisplayName(): String {
        return when {
            isLent() -> "📤 外借中 (${borrowerName})"
            isFinished() -> "✅ 已读完"
            isReading() -> "📖 在读中 (${getProgressPercent()}%)"
            else -> "⏳ 在架待读"
        }
    }
}

// =========================================================================
// 🍷 第一性原理收纳：家庭茶窖、酒品珍藏与适饮熟成时效模型 (Cellar & Tea Vault)
// =========================================================================

data class BeverageTeaRecord(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",                         // 珍藏品名 (如 "飞天茅台 53度", "易武古树普洱生茶", "奔富 Bin 407", "耶加雪菲日晒咖啡豆")
    val category: String = "茶品干货",              // 分类: "茶品干货", "葡萄酒庄", "烈酒名酿", "精品咖啡", "其它佳酿"
    val vintageYear: Int = 2020,                   // 生产/采摘/陈化起始年份 (如 2018)
    val originRegion: String = "",                 // 产区产地 (如 "贵州茅台镇", "法国波尔多梅多克", "云南西双版纳")
    val storageLocation: String = "",              // 存放位置与环境 (如 "餐边柜恒温酒柜 #02", "书房避光存茶缸")
    val qty: Int = 1,                              // 在库数量
    val unit: String = "瓶",                       // 单位 (瓶 / 饼 / 罐 / 袋)
    val openedAt: Long = 0L,                       // 开瓶/拆封时间戳 (0 表示未开封整件陈化)
    val bestDrinkingYear: Int = 2030,              // 最佳适饮/熟成峰值年份 (0 表示不限)
    val openShelfLifeDays: Int = 0,                // 开封后保质/赏味天数 (如红酒3天，咖啡豆30天，0 表示无需限制)
    val tastingNotes: String = "",                 // 风味特征、香气口感与醒酒/冲泡要领 (如 "陈香浓郁，两颊生津；建议95℃盖碗冲泡")
    val rating: Float = 5.0f,                      // 珍藏推荐评分 (1.0 ~ 5.0 星)
    val photoPath: String = ""                     // 实物/标签照片沙盒路径
) {
    /** 计算已陈化/存藏年数 */
    fun getAgingYears(): Int {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        if (vintageYear <= 0) return 0
        return (currentYear - vintageYear).coerceAtLeast(0)
    }

    /** 是否已开瓶/拆封 */
    fun isOpened(): Boolean = openedAt > 0L

    /** 开封后是否已超过保质/最佳赏味期 */
    fun isOpenExpired(): Boolean {
        if (!isOpened() || openShelfLifeDays <= 0) return false
        val expireMs = openedAt + (openShelfLifeDays.toLong() * 24 * 3600 * 1000)
        return System.currentTimeMillis() > expireMs
    }

    /** 当前是否正处于适饮/熟成峰值黄金期 */
    fun isPeakDrinkingNow(): Boolean {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        return bestDrinkingYear in 1..currentYear
    }

    /** 状态展示名称 */
    fun getStatusDisplayName(): String {
        return when {
            isOpenExpired() -> "🔴 开封已超期"
            isOpened() -> "🍷 已开瓶品鉴中"
            isPeakDrinkingNow() -> "✨ 适饮熟成黄金期"
            else -> "📦 存藏陈化中 (${getAgingYears()}年)"
        }
    }
}

// =========================================================================
// 💡 全态数字大脑：闪念想法与灵感收纳模型 (Ideas & Inspiration Stream)
// =========================================================================

data class IdeaRecord(
    val id: String = UUID.randomUUID().toString(),
    val content: String = "",                       // 想法正文 (Markdown/纯文本)
    val tags: List<String> = emptyList(),           // 标签集合 (如 "生活灵感", "读书心得", "待办")
    val voiceMemoPath: String = "",                 // 语音速记原音频沙盒路径 (如有)
    val moodEmoji: String = "💡",                   // 心情/类别 Emoji
    val isPinned: Boolean = false,                  // 是否置顶
    val colorHex: String = "#10B981",               // 卡片主题色
    val linkedAssetIds: List<String> = emptyList(), // 关联的实体物品/藏书/空间 ID 集合
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
) {
    fun getPreview(maxChars: Int = 60): String {
        val trimmed = content.trim().replace("\n", " ")
        return if (trimmed.length <= maxChars) trimmed else "${trimmed.take(maxChars)}..."
    }
}

// =========================================================================
// 📰 全态数字大脑：智能截图与网络文章知识剪藏模型 (Knowledge & Web Clips)
// =========================================================================

data class ClippingRecord(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",                         // 文章/剪藏标题
    val originalUrl: String = "",                   // 原始链接 (如有)
    val sourcePlatform: String = "screenshot",      // 来源: "screenshot"(截图), "wechat", "zhihu", "web", "note"
    val fullMarkdown: String = "",                  // 离线纯净正文 Markdown
    val ocrRawText: String = "",                    // 截图 OCR 提取的全文索引文本
    val localImagePaths: List<String> = emptyList(),// 离线保存的截图或文章配图私有路径
    val summary: String = "",                       // 核心摘要 / 重点金句
    val tags: List<String> = emptyList(),           // 归档分类标签
    val readingProgress: Float = 0.0f,              // 阅读进度 (0.0 ~ 1.0)
    val isArchived: Boolean = false,                // 是否已归档
    val linkedAssetIds: List<String> = emptyList(), // 跨维关联的实物/空间 ID
    val capturedAt: Long = System.currentTimeMillis()
) {
    fun getSearchableContent(): String {
        return "$title $summary $fullMarkdown $ocrRawText ${tags.joinToString(" ")}"
    }
}







