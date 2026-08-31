# 系统总体架构设计 (System Architecture)

---

## 1. 系统总体架构图

```mermaid
graph TD
    subgraph UI_Layer ["1. UI 视图与交互层 (UI & Interaction Layer)"]
        UI_Frag["核心页面 Fragments<br/>(Home / Timeline / Report / Profile)"]
        UI_Vault["12 大专业收纳馆与场景舱<br/>(Voucher / Family / Medicine / Food / Honor / Wardrobe / Emergency / Tool / Plant / Pet / Book / Beverage)"]
        UI_Assist["智能工具与引导体系<br/>(AiConcierge / ModernDialog / GuideTour / DatePicker / GlobalSearch)"]
        UI_Widget["桌面小组件 (3 套)<br/>(ExpiringAndSub / QuickAdd / VaultAlertWidget)"]
    end

    subgraph Controller_Layer ["2. 核心控制与适配层 (Controllers & Adapters)"]
        Ctrl_Main["主控制器 MainActivity / DesktopMainWindow"]
        Ctrl_Adapters["列表适配器族<br/>(Asset / Category / History / Room / Sub / MonthStat / VaultAdapters)"]
        Ctrl_Engines["智能引擎助手<br/>(SmartIntake / ClipboardBridge / BluetoothPrinter / Nfc / Audit / StorageCleanup)"]
    end

    subgraph Data_Layer ["3. 核心数据管理层 (Data Management Layer)"]
        Data_Store["数据门面 DataStore"]
        Data_Ledger["多账本隔离 LedgerManager"]
        Data_Repos["收纳馆持久化仓储集 VaultRepositories"]
        Data_Models["数据模型契约 DataModels (Entry / HouseSpace / LocationMovement / 12 Vault Records)"]
    end

    subgraph Security_Layer ["4. 隐私安全与合规护栏 (Security & Compliance)"]
        Sec_Bio["生物识别隐私锁 BiometricLockHelper"]
        Sec_Watermark["证照防盗流水印 IdentityWatermarkHelper"]
        Sec_Update["官方源优先 UpdateSource (api.github.com 严格置顶)"]
        Sec_Patch["补丁防穿越 PatchArchive & 验签 SHA256withRSA"]
    end

    subgraph Sync_Layer ["5. 同步与多通道分发 (Sync & Distribution)"]
        Sync_WebDav["WebDAV 私有云同步 WebDavSyncHelper (坚果云 / Nextcloud)"]
        Sync_LAN["局域网免装 Web 互传 LanSyncHelper & LanShareServer (8848 端口)"]
        Sync_Update["增量热更新 HotPatchEngine & HotUpdateManager"]
        Sync_Export["Excel 兼容导出 ExportManager (带 UTF-8 BOM CSV)"]
    end

    subgraph Storage_Layer ["6. 本地离线私有沙盒存储 (Local Sandbox Storage)"]
        Store_Prefs[("SharedPreferences & 手写 JSON<br/>(entries_v4 / houses_v1 / vaults)")]
        Store_Vault[("应用私有沙盒图片库<br/>(/files/item_vault/)")]
        Store_Media[("系统媒体库隔离目录<br/>(Pictures/Collecter)")]
    end

    %% 连线关系
    UI_Frag --> Ctrl_Main
    UI_Vault --> Ctrl_Main
    UI_Assist --> Ctrl_Main
    UI_Widget --> Data_Store

    Ctrl_Main --> Ctrl_Adapters
    Ctrl_Main --> Ctrl_Engines
    Ctrl_Main --> Data_Store
    Ctrl_Main --> Sec_Bio

    Ctrl_Engines --> Data_Store
    Ctrl_Engines --> Sec_Watermark

    Data_Store --> Data_Ledger
    Data_Store --> Data_Repos
    Data_Store --> Data_Models
    Data_Store --> Store_Prefs
    Data_Store --> Store_Vault

    Ctrl_Main --> Sync_WebDav
    Ctrl_Main --> Sync_LAN
    Ctrl_Main --> Sync_Update
    Ctrl_Main --> Sync_Export

    Sync_Update --> Sec_Update
    Sync_Update --> Sec_Patch
    Sync_Export --> Store_Media
```

---

## 2. 模块层级划分说明

1. **UI 视图与交互层**：负责全景收纳卡片渲染、Canvas 图钉交互、自定义环形统计图、沉浸式弹窗与桌面小组件。
2. **控制器与适配层**：管理生命周期、分发业务事件、协调多模态输入（OCR / 剪贴板 / 蓝牙 / NFC）与数据流转。
3. **核心数据管理层**：通过门面模式（`DataStore`）统一纳管账本、主资产与 12 馆持久化仓储，负责 JSON 序列化与数据完整性校验。
4. **安全与合规护栏**：保障本地生物隐私、证件流水印、官方更新源优先级与动态补丁防投毒验签。
5. **同步与互传层**：提供 WebDAV 私有网盘备份、局域网免装大屏互传、UTF-8 BOM CSV 导出与热更新下载调度。
6. **离线沙盒存储层**：基于 SharedPreferences 和应用专属内部存储目录进行数据隔离，严格禁止明文网络上报。
