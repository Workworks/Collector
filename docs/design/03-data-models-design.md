# 数据模型与仓储设计 (Data Models & Vault Contracts)

---

## 1. 核心实体定义 (`DataModels.kt`)

### 1.1 核心资产实体 (`Entry`)
```kotlin
data class Entry(
    val id: String = UUID.randomUUID().toString(),
    val brand: String = "",                         // 物品品牌 / 名称
    val category: String = "日用品",                // 物品分类
    val price: Double = 0.0,                       // 购入单价
    val qty: Int = 1,                              // 数量
    val unit: String = "件",                       // 单位
    val location: String = "",                     // 存放位置
    val houseName: String = "我的家",              // 所属空间房产
    val roomName: String = "",                     // 所属房间
    val pinX: Float = -1f,                         // Canvas 图钉 X
    val pinY: Float = -1f,                         // Canvas 图钉 Y
    val locationHistory: List<LocationMovement> = emptyList(), // 变迁轨迹
    val isIn: Boolean = true,                      // 是否在库 / 入库
    val ts: Long = System.currentTimeMillis(),      // 记录时间戳
    val notes: String = "",                        // 备注说明
    val photoPath: String = "",                    // 私有沙盒实物照片路径
    val receiptPath: String = "",                  // 购买发票/保修卡路径
    val barcode: String = "",                      // 条形码 / 资产码
    // 折旧与退役属性
    val isDepreciating: Boolean = true,            // 是否计提折旧
    val mfgDate: Long = 0L,                        // 生产日期
    val expDate: Long = 0L,                        // 到期日期
    val isDurable: Boolean = false,                // 是否为耐用品
    val originalPrice: Double = price,             // 购入原值
    val currentValuation: Double = price,          // 当前残值估值
    val isRetired: Boolean = false,                // 是否已退役 / 待办归置
    val retiredAction: String = "",                // 退役处置方式 (闲鱼/赠送/丢弃)
    // 周期订阅资产属性
    val isSubscription: Boolean = false,           // 是否为周期订阅服务
    val subPrice: Double = price,                  // 订阅扣费金额
    val subCycle: SubCycle = SubCycle.MONTHLY,     // 扣费周期 (月/季/年)
    val subNextBillingDate: Long = 0L,             // 下次扣费时间
    val subAutoRenew: Boolean = true               // 是否自动续费
)
```

---

## 2. 12 大收纳馆模型清单

| 收纳馆编号 | 数据实体 Data Class | 关键业务字段 | 存储持久化 Key |
| :---: | :--- | :--- | :--- |
| **01** | `VoucherRecord` | `title`, `type`, `valueAmount`, `remainingTimes`, `expiryDate` | `vouchers_v1` |
| **02** | `IdentityDocRecord` | `nameOnDoc`, `member`, `docType`, `expiryDate`, `certNumber` | `identity_docs_v1` |
| **03** | `MedicineRecord` | `name`, `category`, `dosage`, `expiryDate`, `isOpened`, `openedValidityDays` | `medicines_v1` |
| **04** | `FoodRecord` | `name`, `zone`, `qty`, `expDate`, `openedAt`, `storageMethod` | `foods_v1` |
| **05** | `HonorCredentialRecord` | `title`, `member`, `issuer`, `certNumber`, `certDate`, `hasAnnualAudit` | `honors_v1` |
| **06** | `WardrobeRecord` | `name`, `season`, `category`, `color`, `material`, `wearCount`, `isSealed` | `wardrobe_v1` |
| **07** | `EmergencyItem` | `name`, `kitType`, `category`, `expiryDate`, `testedAt`, `location` | `emergency_v1` |
| **08** | `ToolMaintenanceRecord` | `name`, `spec`, `category`, `maintenanceIntervalDays`, `lastMaintainedAt` | `tools_v1` |
| **09** | `PlantCareRecord` | `name`, `species`, `lightDemand`, `waterIntervalDays`, `lastWateredAt` | `plants_v1` |
| **10** | `PetCareRecord` | `name`, `species`, `breed`, `weightKg`, `chipNumber`, `dewormIntervalDays` | `pets_v1` |
| **11** | `BookRecord` | `title`, `author`, `totalPages`, `currentPages`, `bookshelfLocation`, `lentDate` | `books_v1` |
| **12** | `BeverageTeaRecord` | `name`, `category`, `vintageYear`, `optimalAgingYear`, `isOpened`, `openedDays` | `beverages_v1` |

---

## 3. 数据兼容性与迁移契约

1. **安全默认值原则**：所有 JSON 字段反序列化时必须使用 `optString("key", "")`、`optLong("key", 0L)`、`optBoolean("key", false)`，绝不假设字段必然存在。
2. **Key 命名不变性**：已有 JSON 键名一律保持向后兼容，严禁重命名或删除已上线持久化字段。
3. **往返验证要求**：任何对序列化编解码器的修改，必须通过 `BackupCodecTest.kt` 往返无损导入导出测试。
