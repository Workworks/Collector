package com.kfaino.diapertracker

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kfaino.diapertracker.databinding.DialogTutorialBinding

/**
 * 📖 功能全景与使用教程中心 (Tutorial & User Guide Center)
 * - 覆盖全系统 22+ 核心功能体系
 * - 每个功能卡片均提供跟手步骤教程与「🚀 交互跟手演练」入口
 * - 支持全景漫游或单项针对性演练，中途可随时退出
 */
object TutorialDialog {

    data class TutorialItem(
        val category: String, // "spatial", "asset", "photo", "sync"
        val icon: String,
        val title: String,
        val summary: String,
        val steps: List<String>,
        val tip: String = "",
        val tourKey: String = "" // 对应 InteractiveGuideTour 的单项演练 key
    )

    private val TUTORIAL_LIST = listOf(
        // 1. AI 智能管家
        TutorialItem(
            category = "asset",
            icon = "🤖",
            title = "AI 资产智能管家与自然语言寻物",
            summary = "端侧离线语义问答，支持一句话寻物找位置、资产财务穿透与临期库存预警。",
            steps = listOf(
                "在首页顶栏点击【··· 更多工具】展开快捷栏，点击【🤖 智能管家】进入对话界面；",
                "自然语言寻物：输入「我的相机和充电器放在哪了？」，AI 自动定位所属空间房间并一键导航到平面图；",
                "财务价值洞察：输入「数码类设备总共花了多少钱？」，毫秒级汇总数量、原值与折旧现值；",
                "风险与耗材预警：提问「最近有什么要过期了？」或「哪些耗材需要补货？」，智能呈现清单。"
            ),
            tip = "100% 本地端侧离线执行，无需联网即可进行毫秒级语义索引匹配！",
            tourKey = "ai_concierge"
        ),

        // 2. 场景装备套装
        TutorialItem(
            category = "spatial",
            icon = "🎒",
            title = "场景装备套装与出行打包核对",
            summary = "自由创建摄影/出差/露营装备包，出行前一键开启装箱进度环与扫码打卡核对。",
            steps = listOf(
                "在首页快捷工具栏点击【🎒 装备套装】，查看预设或点击【➕ 新建】自定义装备包；",
                "点击套装卡片勾选关联库内具体物品（如机身、镜头、电池、存储卡）；",
                "出发前点击【🚀 打包核对】，进入沉浸式装箱清单模式；",
                "逐项勾选或扫码装箱，实时查看装箱进度环（如 8/10 80%），全套齐备后一键打卡。"
            ),
            tip = "出差旅游或户外露营前打包核对，彻底告别落带充电线或小配件的烦恼！",
            tourKey = "kit_manager"
        ),

        // 3. 电商订单抓取
        TutorialItem(
            category = "asset",
            icon = "🛒",
            title = "电商订单与淘口令无感智能抓取",
            summary = "支持淘宝/京东/拼多多订单截图 OCR 与淘口令剪贴板自动秒录入库。",
            steps = listOf(
                "订单截图 OCR：在记账弹窗点击【⚡ 拍照发票/小票/包装识物】，上传淘宝或京东订单截图，AI 自动过滤无用词并提取商品名、实付价、规格与下单日期；",
                "剪贴板无感桥接：在手机复制淘宝淘口令或京东商品链接，切回 Collecter 自动弹出高定提示卡片，一键填写入库；",
                "局域网 Web 大屏：电脑浏览器访问控制台（8848端口），直接粘贴电商文本秒级同步至手机。"
            ),
            tip = "自动识别并提取商品颜色/规格参数写入备注，记账快如闪电！",
            tourKey = "ecommerce"
        ),

        // 4. 空间平面图与寻物
        TutorialItem(
            category = "spatial",
            icon = "🗺️",
            title = "空间平面图与寻物双向穿梭",
            summary = "可视化家庭空间多房间 Canvas 平面图，实现由物找空间与由空间看物品双向穿梭。",
            steps = listOf(
                "在「我的 ➔ 空间平面图与寻物地图」中创建空间（如「自己的家」、「父母家」）并编辑房间网格布局；",
                "记账或编辑物品时，点击「📍 选择空间与房间」，在平面图上轻触即可完成图钉精准打点；",
                "由物找空间：在首页卡片点击「📍 主卧 · 衣柜」等位置标签，自动弹出平面图并在对应坐标闪烁【金色呼吸光晕】；",
                "由空间看物品：在全景平面图模式点击任意房间，底部自动滑出该房间内存放的【全部在库物品清单】。"
            ),
            tip = "长按房间卡片可以快速自定义房间色彩与图标！",
            tourKey = "floorplan"
        ),

        // 5. 收纳箱专属二维码与智能扫码
        TutorialItem(
            category = "spatial",
            icon = "🏷️",
            title = "收纳箱专属二维码与智能扫码",
            summary = "为实体收纳箱生成二维码标签，扫一扫秒查箱内清单；扫描商品条码快速记账。",
            steps = listOf(
                "在平面图点击任意房间，在滑出的物品抽屉中点击【🏷️ 生成二维码标签】；",
                "标签包含空间名称、在库件数与专属二维码，可点击【保存到相册】并打印贴在收纳箱体；",
                "首页顶栏点击【📷 扫一扫】，对准收纳箱二维码即可秒级展开箱内物品；",
                "扫描任意商品条形码（如 EAN-13 商品码），系统将自动匹配库内物品或快速填入条码新建记账。"
            ),
            tip = "支持离线无网识别，收纳箱二维码完全在本地离线生成与解析！",
            tourKey = "qr_scan"
        ),

        // 6. 4 维分类与折旧
        TutorialItem(
            category = "asset",
            icon = "📉",
            title = "4 维物品分类与日均消费折旧计算",
            summary = "精准追踪数码折旧、保质期到期、耐用品与消耗品，知晓每一笔钱花得值不值。",
            steps = listOf(
                "📉 折旧资产（电脑/手机/相机）：系统按拥有天数动态计算【日均消费成本（元/天）】；",
                "⏳ 保质期物品（药品/食品/美妆）：选择保质期快速标签（30天/半年/1年/3年），卡片自动展示临期倒计时与红黄绿状态；",
                "🛋️ 长期耐用品（家具/乐器/首饰）：仅记录拥有天数与原价，不平摊折旧费；",
                "📦 日常消耗品（纸尿裤/咖啡豆/耗材）：关注库存数量与单件购买成本。"
            ),
            tip = "日均消费 = (购入原价 - 二手估值) / 拥有天数，用得越久日均成本越低！",
            tourKey = "ecommerce"
        ),

        // 7. 实物照片与购买凭证沙盒
        TutorialItem(
            category = "photo",
            icon = "📸",
            title = "实物照片与购买发票/保修卡凭证",
            summary = "100% 离线私有沙盒存储，实物照片作为列表封面，电子发票保修卡防丢备查。",
            steps = listOf(
                "在记账或编辑界面，点击【📷 实物照片】插槽从相册挑选实物大图；",
                "点击【🧾 发票/保修卡】插槽上传购买截图、电子发票或保修凭证；",
                "列表卡片左侧自动展示圆角实物封面，右下角点亮【🧾 凭证】交互徽章；",
                "点击任意缩略图或发票徽章，直接拉起沉浸式全屏查看器，支持多指手势缩放与系统分享。"
            ),
            tip = "照片自动纠正拍摄 EXIF 旋转角并进行高效压缩，存放在应用私有沙盒绝不上报！",
            tourKey = "ecommerce"
        ),

        // 8. 物品在役/退役与闲鱼转卖
        TutorialItem(
            category = "asset",
            icon = "📦",
            title = "物品在役/退役与闲鱼代售回血",
            summary = "全生命周期管理闲置物品，记录二手转卖回血金额与归置轨迹。",
            steps = listOf(
                "当物品闲置或损坏时，在卡片点击【更多 ➔ 物品退役与待办归置】；",
                "选择归置方案：挂闲鱼代售、挂转转二手、赠送亲友、封箱入库、环保回收或报废；",
                "出掉后记录回血金额，回血收益将自动并入报表统计与生活流明细；",
                "首页筛选器随时切换【在役在库】与【退役归置】资产。"
            ),
            tip = "断舍离出掉的闲置物品回血金额会抵扣原始支出，让财务更清晰！",
            tourKey = "chart_radar"
        ),

        // 9. 周期订阅资产
        TutorialItem(
            category = "asset",
            icon = "🔄",
            title = "周期订阅资产与扣费前预警",
            summary = "统一纳管 iCloud、ChatGPT Plus、宽带与流媒体年卡，折算月均与年化支出。",
            steps = listOf(
                "首页切换到【🔄 周期订阅】资产池，点击「+」添加订阅；",
                "设置扣费周期（按月/按季/按年/按周）与下次扣费日期；",
                "系统自动折算【月均支出】与【年化总支出】看板；",
                "扣费前 1~3 天，系统通知栏将自动推送扣费预警提醒。"
            ),
            tip = "可以在「我的 ➔ 更多设置」中一键测试通知栏提醒！",
            tourKey = "full"
        ),

        // 10. 动态环形图与健康雷达
        TutorialItem(
            category = "asset",
            icon = "📊",
            title = "动态交互环形图与断舍离健康雷达",
            summary = "多维资产分析中心，动态环形图直观透视，健康雷达智能治理闲置。",
            steps = listOf(
                "切换到底栏【报表】页面，查看顶部的【🎨 资产版图分布】交互式环形图；",
                "轻触环形图任意分类扇区，扇区自动放大高亮，中心显示该分类金额与占比；",
                "查看【🧹 闲置资产与断舍离雷达】：系统自动计算 100 分制资产流转健康分；",
                "雷达自动识别超过 180 天未打卡或临期闲置物品，提供【✅ 确认在位】或【📦 挂闲鱼/归置】一键治理。"
            ),
            tip = "定期在闲置雷达中点击确认在位，可以让资产健康分保持在 95+ 优秀水平！",
            tourKey = "chart_radar"
        ),

        // 11. 局域网 Web 大屏控制台
        TutorialItem(
            category = "sync",
            icon = "🌐",
            title = "局域网免装 Web 网页大屏资产控制台",
            summary = "同一 Wi-Fi 下，电脑无需安装软件，浏览器输入手机 IP 即可进入全功能大屏大盘。",
            steps = listOf(
                "点击首页顶栏【⚡ 局域网互传】启动本地微服务，记录屏幕上的 Web 控制台地址（如 `http://192.168.1.100:8848`）；",
                "在电脑或 iPad 浏览器直接打开该地址；",
                "在宽屏大盘上进行搜索、快速添加资产、多选「批量移动位置」与「批量删除」，所有操作毫秒级同步回手机。"
            ),
            tip = "电脑端支持直接导出包含全部维度的 CSV 资产总表与粘贴电商文本一键导入！",
            tourKey = "lan_sync"
        ),

        // 12. 空间实物沉浸式大盘点
        TutorialItem(
            category = "spatial",
            icon = "📋",
            title = "空间实物沉浸式巡检大盘点模式",
            summary = "定期对房间或收纳箱进行盲盘巡检，自动输出盘盈盘亏差异报告并打卡。",
            steps = listOf(
                "点击首页顶栏【📋 实物大盘点】或前往【我的 ➔ 📋 空间实物巡检与大盘点】；",
                "选择盘点区域（如「主卧衣柜」或「🌟 全空间所有资产」）；",
                "实物巡检核对：逐项点击【✅ 确认在位】、【🔢 数量有误修改】或【❌ 缺失未找到】；",
                "点击【完成盘点】，系统自动生成差异报告（完全吻合数、调整明细、缺失清单）并刷新在位时间。"
            ),
            tip = "完成盘点后报告可一键复制发送至家庭群或备份！",
            tourKey = "audit"
        ),

        // 13. 多账本独立空间
        TutorialItem(
            category = "asset",
            icon = "📚",
            title = "多账本独立空间与资产隔离体系",
            summary = "支持个人资产、家庭仓储、办公物资与自定义独立账本自由切换。",
            steps = listOf(
                "点击首页左上角账本标题（如「🏠 个人资产 ▾」）或前往【我的 ➔ 📚 多账本体系管理】；",
                "一键点选切换当前工作账本，或点击「➕ 新建自定义账本」；",
                "各账本出入库流水、分类与折旧独立核算，互不干扰。"
            ),
            tip = "可在个人物品与公司办公固定资产之间秒速切换！",
            tourKey = "ledgers"
        ),

        // 14. 桌面小组件
        TutorialItem(
            category = "sync",
            icon = "📱",
            title = "桌面小组件 (App Widgets)",
            summary = "无需打开 App，在手机主屏幕一眼查看临期预警与资产看板。",
            steps = listOf(
                "在手机桌面长按空白处，选择【微件 / 小组件 / Widgets】；",
                "找到【Collecter】应用，提供两款精美小组件：",
                "1. ⏳【临期与订阅预警小组件】：实时显示即将过期的食品药品与近期扣费订阅倒计时；",
                "2. 💰【资产看板与快捷记账小组件】：显示在役资产总值与日均消费，点击「+」一秒直达记账。"
            ),
            tip = "每次在 App 内增删改物品或发生变动时，桌面小组件将自动同步刷新！",
            tourKey = "privacy_sync"
        ),

        // 15. 生物识别隐私锁
        TutorialItem(
            category = "sync",
            icon = "🔐",
            title = "生物识别指纹与面容隐私锁",
            summary = "保护个人财产估值与贵重首饰、房产证、重要凭据的位置隐私。",
            steps = listOf(
                "前往【我的 ➔ 更多设置 ➔ 🔐 生物识别隐私锁】；",
                "点击开启开关，系统将调起一次指纹/面容或锁屏密码校验确认；",
                "开启后，每次冷启动或从后台切换回前台时，需通过生物识别验证方可进入应用。"
            ),
            tip = "基于 AndroidX 原生 BiometricPrompt，生物信息完全受系统硬件芯片安全隔离！",
            tourKey = "privacy_sync"
        ),

        // 16. WebDAV 云同步与 CSV 导出
        TutorialItem(
            category = "sync",
            icon = "☁️",
            title = "WebDAV 私有云同步与 Excel CSV 导出",
            summary = "支持坚果云、Nextcloud 等私有网盘一键云备份，支持 Excel 兼容 CSV 分享。",
            steps = listOf(
                "前往【我的 ➔ 更多设置 ➔ ☁️ WebDAV 私有云同步】；",
                "填入 WebDAV 服务器 URL（如坚果云 `https://dav.jianguoyun.com/dav/`）、用户名和应用独立密码；",
                "点击【测试连接】校验无误后，点击【上传备份到云端】即可将完整数据安全上传；",
                "换机或多设备时，在目标设备点击【从云端恢复数据】即可一秒还原；",
                "在【数据报表】或【数据备份】中可一键生成带 UTF-8 BOM 的 Excel 兼容 CSV 资产总表与流水表，分享至微信或电脑。"
            ),
            tip = "坚果云用户请在坚果云网页版「账户信息 ➔ 安全设置 ➔ 第三方应用管理」中生成应用独立密码！",
            tourKey = "privacy_sync"
        ),

        // 17. 闲鱼/转转文案 AI 助手
        TutorialItem(
            category = "asset",
            icon = "🤖",
            title = "闲鱼/转转退役转卖 AI 智能文案与定价助手",
            summary = "一键生成高转化商品标题与详细文案，自动估算二手秒出参考价与合理挂牌价。",
            steps = listOf(
                "在物品退役弹窗中勾选【🔴 已退役】并选择「挂闲鱼代售」；",
                "点击【🤖 AI 生成闲鱼转卖文案与秒出估价】；",
                "系统根据原价、拥有天数、成色与品牌自动计算秒出价并生成包含背景、成色、配件与包邮的高转化文案；",
                "点击【📋 复制全部文案并采纳秒出价】，直接打开闲鱼粘贴发布！"
            ),
            tip = "文案已自动包含热门搜索 Tag 标签，大幅提升闲鱼曝光率！",
            tourKey = "chart_radar"
        ),

        // 18. 蓝牙标签打印机
        TutorialItem(
            category = "spatial",
            icon = "🖨️",
            title = "蓝牙便携热敏标签打印机直连",
            summary = "支持 ESC/POS 标准协议，蓝牙直连便携标签机打印防水物理收纳标签。",
            steps = listOf(
                "在空间平面图或收纳箱二维码弹窗中点击【🖨️ 蓝牙标签机打印】；",
                "从列表中选择已配对的蓝牙标签机；",
                "一键直出包含空间名、区域箱号、物品清单摘要与二维码的物理标签纸并贴于箱体。"
            ),
            tip = "兼容市面上主流 58mm/80mm 蓝牙热敏便携标签打印机！",
            tourKey = "floorplan"
        ),

        // 19. NFC 智能标签
        TutorialItem(
            category = "spatial",
            icon = "🏷️",
            title = "NFC 智能标签「碰一碰」写卡与零点击寻物",
            summary = "将收纳箱写入 NTAG NFC 贴纸，手机背部碰一碰秒级识别定位并展开箱内清单。",
            steps = listOf(
                "写卡：在收纳箱二维码弹窗中点击【🏷️ 写入 NFC 贴纸】，手机背部靠近贴纸即可完成烧录；",
                "寻物：开启手机 NFC，手机碰触收纳箱贴纸，系统立即识别并在平面图上高亮定位并展开物品清单。"
            ),
            tip = "支持标准 NTAG213/215/216 智能贴纸，几毛钱一张即可实现全屋智能寻物！",
            tourKey = "floorplan"
        ),

        // 20. 极速热更新
        TutorialItem(
            category = "sync",
            icon = "🎮",
            title = "类似游戏极速热更新 (Dynamic Hot Patch)",
            summary = "告别漫长的 50MB+ 全量包下载，仅下载几百 KB 增量补丁，免重装即刻无感生效！",
            steps = listOf(
                "前往【我的 ➔ 🎮 极速热更新补丁】一键检查可用增量补丁；",
                "补丁体积通常仅几百 KB（约为完整 APK 的 1.5%），秒级极速下载；",
                "下载完成后由底层沙盒引擎动态加载，无需弹出系统安装器权限，瞬间无感生效体验最新特性；",
                "系统内置崩溃熔断自愈机制，若连续启动异常会自动秒级回滚到纯净 Base 版本。"
            ),
            tip = "冷启动时系统会自动在后台静默预检，有最新补丁会即时贴心提醒！",
            tourKey = "hot_patch"
        )
    )

    fun show(activity: Activity) {
        val binding = DialogTutorialBinding.inflate(LayoutInflater.from(activity))
        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(binding.root)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation

        var currentCategory = "all"

        fun renderCards() {
            binding.tutorialCardsContainer.removeAllViews()
            val filtered = if (currentCategory == "all") {
                TUTORIAL_LIST
            } else {
                TUTORIAL_LIST.filter { it.category == currentCategory }
            }

            for (item in filtered) {
                val card = MaterialCardView(activity).apply {
                    radius = dpToPx(activity, 16).toFloat()
                    cardElevation = 0f
                    strokeWidth = dpToPx(activity, 1)
                    setStrokeColor(ContextCompat.getColor(context, R.color.card_border))
                    setCardBackgroundColor(ContextCompat.getColor(context, R.color.card))
                    val lp = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = dpToPx(activity, 12) }
                    layoutParams = lp
                }

                val content = LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(
                        dpToPx(activity, 16),
                        dpToPx(activity, 16),
                        dpToPx(activity, 16),
                        dpToPx(activity, 16)
                    )
                }

                // 标题行
                val titleRow = LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }

                val iconTv = TextView(activity).apply {
                    text = item.icon
                    textSize = 20f
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(dpToPx(activity, 36), dpToPx(activity, 36)).apply {
                        marginEnd = dpToPx(activity, 10)
                    }
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(ContextCompat.getColor(context, R.color.input_bg))
                    }
                }

                val titleTv = TextView(activity).apply {
                    text = item.title
                    textSize = 15f
                    paint.isFakeBoldText = true
                    setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                // 针对该功能点的「跟手演练」按钮
                val btnItemTour = TextView(activity).apply {
                    text = "🚀 跟手演练"
                    textSize = 11f
                    setTextColor(ContextCompat.getColor(context, R.color.primary))
                    paint.isFakeBoldText = true
                    background = ContextCompat.getDrawable(activity, R.drawable.bg_chip_inactive)
                    setPadding(dpToPx(activity, 8), dpToPx(activity, 4), dpToPx(activity, 8), dpToPx(activity, 4))
                    applyPressScaleAnimation(0.92f)
                    setOnClickListener {
                        dialog.dismiss()
                        if (activity is MainActivity) {
                            InteractiveGuideTour.startFeatureTour(activity, item.tourKey.ifBlank { "full" })
                        }
                    }
                }

                titleRow.addView(iconTv)
                titleRow.addView(titleTv)
                titleRow.addView(btnItemTour)
                content.addView(titleRow)

                // 概述
                val summaryTv = TextView(activity).apply {
                    text = item.summary
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                    val lp = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = dpToPx(activity, 8)
                        bottomMargin = dpToPx(activity, 10)
                    }
                    layoutParams = lp
                }
                content.addView(summaryTv)

                // 步骤列表
                val stepsContainer = LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = dpToPx(activity, 10).toFloat()
                        setColor(ContextCompat.getColor(context, R.color.input_bg))
                    }
                    setPadding(
                        dpToPx(activity, 12),
                        dpToPx(activity, 10),
                        dpToPx(activity, 12),
                        dpToPx(activity, 10)
                    )
                }

                for ((idx, step) in item.steps.withIndex()) {
                    val stepRow = LinearLayout(activity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.TOP
                        val lp = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            if (idx > 0) topMargin = dpToPx(activity, 6)
                        }
                        layoutParams = lp
                    }

                    val badge = TextView(activity).apply {
                        text = "${idx + 1}"
                        textSize = 10f
                        setTextColor(Color.WHITE)
                        paint.isFakeBoldText = true
                        gravity = Gravity.CENTER
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(ContextCompat.getColor(context, R.color.primary))
                        }
                        layoutParams = LinearLayout.LayoutParams(dpToPx(activity, 16), dpToPx(activity, 16)).apply {
                            topMargin = dpToPx(activity, 2)
                            marginEnd = dpToPx(activity, 8)
                        }
                    }

                    val stepTv = TextView(activity).apply {
                        text = step
                        textSize = 12f
                        setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    }

                    stepRow.addView(badge)
                    stepRow.addView(stepTv)
                    stepsContainer.addView(stepRow)
                }
                content.addView(stepsContainer)

                // 小贴士
                if (item.tip.isNotBlank()) {
                    val tipTv = TextView(activity).apply {
                        text = "💡 贴士: ${item.tip}"
                        textSize = 11f
                        setTextColor(ContextCompat.getColor(context, R.color.primary))
                        val lp = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { topMargin = dpToPx(activity, 8) }
                        layoutParams = lp
                    }
                    content.addView(tipTv)
                }

                card.addView(content)
                binding.tutorialCardsContainer.addView(card)
            }
        }

        fun updateChips() {
            val chips = listOf(
                Pair("all", binding.chipTutAll),
                Pair("spatial", binding.chipTutSpatial),
                Pair("asset", binding.chipTutAsset),
                Pair("photo", binding.chipTutPhoto),
                Pair("sync", binding.chipTutSync)
            )

            for ((cat, chip) in chips) {
                if (cat == currentCategory) {
                    chip.setBackgroundResource(R.drawable.bg_chip_active)
                    chip.setTextColor(Color.WHITE)
                    chip.paint.isFakeBoldText = true
                } else {
                    chip.setBackgroundResource(R.drawable.bg_chip_inactive)
                    chip.setTextColor(ContextCompat.getColor(activity, R.color.text_secondary))
                    chip.paint.isFakeBoldText = false
                }
            }
            renderCards()
        }

        binding.chipTutAll.applyPressScaleAnimation(0.92f)
        binding.chipTutAll.setOnClickListener { currentCategory = "all"; updateChips() }

        binding.chipTutSpatial.applyPressScaleAnimation(0.92f)
        binding.chipTutSpatial.setOnClickListener { currentCategory = "spatial"; updateChips() }

        binding.chipTutAsset.applyPressScaleAnimation(0.92f)
        binding.chipTutAsset.setOnClickListener { currentCategory = "asset"; updateChips() }

        binding.chipTutPhoto.applyPressScaleAnimation(0.92f)
        binding.chipTutPhoto.setOnClickListener { currentCategory = "photo"; updateChips() }

        binding.chipTutSync.applyPressScaleAnimation(0.92f)
        binding.chipTutSync.setOnClickListener { currentCategory = "sync"; updateChips() }

        binding.btnStartInteractiveTour.applyPressScaleAnimation(0.94f)
        binding.btnStartInteractiveTour.setOnClickListener {
            dialog.dismiss()
            if (activity is MainActivity) {
                InteractiveGuideTour.startTour(activity)
            }
        }

        binding.btnCloseTutorial.applyPressScaleAnimation(0.90f)
        binding.btnCloseTutorial.setOnClickListener { dialog.dismiss() }

        binding.btnTutorialGotIt.applyPressScaleAnimation(0.94f)
        binding.btnTutorialGotIt.setOnClickListener { dialog.dismiss() }

        updateChips()
        dialog.show()
    }

    private fun dpToPx(activity: Activity, dp: Int): Int {
        return (dp * activity.resources.displayMetrics.density + 0.5f).toInt()
    }
}
