# 🏗️ 系统架构与模块设计文档 (ARCHITECTURE.md)

本文档阐述 **Collecter** 的技术架构、目录组织、核心模块及其交互流程。

---

## 1. 系统总体架构

```mermaid
graph TD
    A[UI 视图层<br/>Fragments, Dialogs & Widgets] --> B[核心控制器 & 适配器<br/>MainActivity / Adapters]
    B --> C[数据存储与管理层<br/>DataStore & CategoryManager]
    B --> D[沙盒图片与附件管理<br/>ImageVaultHelper]
    B --> E[交互式平面图 & 标签引擎<br/>FloorPlanView & BoxQrCodeDialog]
    B --> F[智能扫码识别引擎<br/>ScannerActivity (ZXing Core)]
    B --> G[系统通知与定时提醒<br/>NotificationHelper & ReminderReceiver]
    B --> H[生物识别与隐私安全<br/>BiometricLockHelper]
    B --> I[WebDAV 私有云同步<br/>WebDavSyncHelper]
    B --> J[热更新与版本分发<br/>UpdateManager]
    B --> K[多格式导出管理<br/>ExportManager]
    C --> L[(本地 SharedPreferences & JSON)]
    D --> M[(本地沙盒私有存储 /files/item_vault/)]
    I --> N[(WebDAV 私有网盘 / 坚果云 / Nextcloud)]
```

---

## 2. 核心模块与职责划分

| 模块文件 | 职责说明 | 关键技术点 |
| :--- | :--- | :--- |
| `MainActivity.kt` | 主界面容器，负责底部导航切换、中央浮动记账按钮（FAB）、记账弹窗、扫码结果分发与生物识别生命周期拦截 | ActivityResultContracts, ViewBinding, BiometricPrompt |
| `HomeFragment.kt` | 首页仪表盘：总资产/日均消费统计看板、物品与订阅切换、在役/退役筛选、VIP 关注卡片、扫一扫与备份提醒 | RecyclerView, MaterialCardView, Filter |
| `TimelineFragment.kt` | 「生活流」明细流水：按时间逆序展示出入库历史记录，支持快捷增删改 | Adapter, DateFormat |
| `ReportFragment.kt` | 「报表」统计中心：资产分类交互式环形图 (`DonutChartView`)、闲置资产与断舍离健康雷达、品牌价值 Top 5 | Custom Canvas 渲染, 健康度评分算法 |
| `ProfileFragment.kt` | 「我的」设置面板：分类管理、空间平面图、深浅主题、触感震动、生物识别锁、WebDAV 云同步与 GitHub 热更新 | MaterialSwitch, WebDAV, Dialog |
| `DataStore.kt` | 数据访问对象（DAO），封装所有物品、空间房屋、分类、配置项的持久化与备份 JSON 生成，广播桌面小组件刷新 | SharedPreferences, JSON, AppWidgetBroadcast |
| `DataModels.kt` | 核心数据实体定义：`Entry`、`HouseSpace`、`HouseRoom`、`LocationMovement`、`CustomCategory` | Kotlin Data Class |
| `BoxQrCodeDialog.kt` | 收纳箱/房间专属二维码标签生成器：离线生成二维码图片、绘制精致标签卡片并保存相册或分享打印 | ZXing QRCodeWriter, Canvas, MediaStore |
| `ScannerActivity.kt` | 智能扫码相机 Activity：基于 ZXing 离线识别收纳箱专属协议（`collecter://room?...`）与通用商品条码 | ZXing DecoratedBarcodeView |
| `DonutChartView.kt` | 资产分类占比交互式环形图：原生 Canvas 绘制空心环形图，平滑展开动效，触碰扇区高亮与中心总额联动 | Custom View, Canvas, ValueAnimator |
| `ExpiringAndSubWidgetProvider.kt` | 桌面小组件 1：临期保质期与周期订阅扣费倒计时小组件 | AppWidgetProvider, RemoteViews |
| `QuickAddWidgetProvider.kt` | 桌面小组件 2：在役资产净值/日均消费看板与一键记一笔小组件 | AppWidgetProvider, PendingIntent |
| `BiometricLockHelper.kt` | 生物识别隐私锁：调用系统原生指纹/面容/锁屏凭据认证，保护资产隐私 | BiometricPrompt, BiometricManager |
| `WebDavSyncHelper.kt` | WebDAV 私有云同步引擎：支持坚果云、Nextcloud、群晖 Synology，原生 HttpURLConnection 实现 | HTTP Basic Auth, PUT, GET, PROPFIND |
| `ImageVaultHelper.kt` | 本地私有沙盒图片存储引擎：图片压缩、EXIF 角度纠偏、采样下采样与 LRU 内存双级缓存 | ExifInterface, LruCache, Bitmap |
| `FloorPlanView.kt` | 自定义 Canvas 平面图视图：支持空间网格绘制、房间触控拾取、图钉打点、脉冲高亮与在库物品数量徽章 | Custom View, Canvas, Touch Event |
| `FloorPlanDialog.kt` | 平面图交互弹窗：支持选点模式、全景房间浏览、房间资产抽屉列表与从物品一键穿梭定位 | ViewBinding, Material Dialog |
| `NotificationHelper.kt` | 系统定时提醒管理：订阅到期预警（提前 1~3 天）、VIP 物品核对打卡、保质期到期通知 | NotificationManager, AlarmManager |
| `ExportManager.kt` | 数据导出引擎：生成带 UTF-8 BOM 的 Excel 兼容 CSV 资产总表与流水表，支持系统级分享 | FileProvider, Intent.ACTION_SEND |
| `UpdateManager.kt` | GitHub Releases 在线热更新引擎：多镜像下载、后台静默预缓存与 0 秒秒级安装 | HttpURLConnection, PackageInstaller |
| `ViewExt.kt` | UI 交互动效与触感震动扩展：按压回弹微缩放动效 (`applyPressScaleAnimation`) 与统一马达震动 (`performAppHapticFeedback`) | ObjectAnimator, HapticFeedbackConstants |
