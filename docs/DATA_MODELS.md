# 🗄️ 数据模型与存储协议规范 (DATA_MODELS.md)

本文档详细定义了 **Collecter** 的核心数据模型实体、字段说明及其在本地 JSON 存储中的序列化 Key。

---

## 1. 核心实体模型：`Entry` (资产/流水记录)

`Entry` 是系统最核心的资产记录模型，囊括了**物品管理、折旧损耗、保质期到期、在役/退役待办、按期订阅以及空间平面图定位**等全套维度。

### 1.1 Kotlin 属性与 JSON 映射表

| 字段名称 (Kotlin) | 类型 | JSON 映射 Key | 默认值 | 详细业务说明 |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `String` | `"id"` | `UUID` | 全局唯一记录 ID |
| `category` | `String` | `"cat"` | `"数码"` | 分类名称 (如 数码, 日用, 零食, 耗材等) |
| `brand` | `String` | `"brand"` | `"物品"` | 物品名称 / 品牌规格 (如 "联想拯救者 Y9000X") |
| `qty` | `Int` | `"qty"` | `1` | 数量 |
| `price` | `Double` | `"price"` | `0.0` | 购入单价 (元/单位) |
| `currentValuation` | `Double` | `"cur_val"` | `0.0` | 当前二手估值/折旧估值 (为 0 时默认原价) |
| `purchaseDate` | `Long` | `"p_date"` | 毫秒时间戳 | 购入日期 (用于计算拥有天数与日均成本) |
| `ts` | `Long` | `"ts"` | 毫秒时间戳 | 记录创建或流转时间戳 |
| `isIn` | `Boolean` | `"in"` | `true` | `true`=购入入库, `false`=消耗出库 |
| `notes` | `String` | `"notes"` | `""` | 备注说明 |
| `unit` | `String` | `"unit"` | `"件"` | 计量单位 (件, 台, 片, 包, 个, 箱, 瓶等) |
| `location` | `String` | `"loc"` | `""` | 当前放置位置文本 (如 "主卧衣柜二层") |
| `houseId` | `String` | `"h_id"` | `"default_house"` | 所属空间 ID |
| `houseName` | `String` | `"h_name"` | `"我的家"` | 所属空间名称 (如 "🏠 自己的家") |
| `roomName` | `String` | `"r_name"` | `""` | 所属房间名称 (如 "主卧") |
| `pinX` | `Float` | `"px"` | `-1.0` | 平面图相对坐标 X (0.0 ~ 1.0) |
| `pinY` | `Float` | `"py"` | `-1.0` | 平面图相对坐标 Y (0.0 ~ 1.0) |
| `locationHistory` | `List<LocationMovement>` | `"loc_hist"` | `[]` | 历史位置流转变迁轨迹 |
| `isImportant` | `Boolean` | `"imp"` | `false` | 是否为 VIP 贵重/防丢关注物品 |
| `reminderEnabled` | `Boolean` | `"rem_en"` | `false` | 是否开启系统定期核对提醒 |
| `reminderIntervalDays`| `Int` | `"rem_int"` | `1` | 提醒核对间隔天数 (1, 3, 7, 15, 30) |
| `reminderTime` | `String` | `"rem_tm"` | `"09:00"` | 每日提醒时间 |
| `lastCheckedAt` | `Long` | `"chk_ts"` | `0L` | 上次打卡核对确认在位的时间戳 |
| `isRetired` | `Boolean` | `"is_ret"` | `false` | `false`=在役, `true`=已退役 (如闲鱼代售) |
| `retiredAt` | `Long` | `"ret_at"` | `0L` | 退役归置时间戳 |
| `retiredAction` | `String` | `"ret_act"` | `""` | 归置渠道 (如 "📦 挂闲鱼代售", "🎁 赠送亲友") |
| `retiredSoldPrice` | `Double` | `"ret_sp"` | `0.0` | 二手出掉回血变现金额 (元) |
| `retiredNote` | `String` | `"ret_note"` | `""` | 退役备注 |
| `isSubscription` | `Boolean` | `"is_sub"` | `false` | 是否为周期订阅型资产 |
| `subCycle` | `String` | `"sub_cyc"` | `"按月"` | 订阅周期 ("按月", "按年", "按季", "按周") |
| `subNextBillingDate` | `Long` | `"sub_nxt"` | `0L` | 下次续费扣款日期毫秒时间戳 |
| `subAutoRenew` | `Boolean` | `"sub_rnw"` | `true` | 是否自动续费 |
| `assetType` | `String` | `"a_type"` | `"consumable"` | 物品管理分类 (`depreciating`/`expiring`/`durable`/`consumable`) |
| `manufactureDate` | `Long` | `"m_date"` | `0L` | 生产日期时间戳 |
| `expiryDate` | `Long` | `"e_date"` | `0L` | 保质期到期日期时间戳 |
| `photoPath` | `String` | `"img_p"` | `""` | 实物照片沙盒文件名 (如 `photo_uuid.jpg`) |
| `receiptPath` | `String` | `"rec_p"` | `""` | 购买发票/保修卡沙盒文件名 (如 `receipt_uuid.jpg`) |

---

## 2. 空间与房间模型：`HouseSpace` & `HouseRoom`

### 2.1 `HouseSpace` (家庭/空间)
- `id`: 空间唯一标识
- `name`: 空间名称（如 `🏠 自己的家`、`🏡 父母家`、`🏢 办公室`、`🚗 汽车后备箱`）
- `type`: 空间类型
- `rooms`: `List<HouseRoom>` 房间列表
- `isDefault`: 是否为默认空间

### 2.2 `HouseRoom` (房间/区域)
- `id`: 房间唯一标识
- `name`: 房间名称（如 `主卧`、`阳台`、`书房`、`储物间`）
- `icon`: 房间 Emoji 图标（如 `🛏️`、`🚪`、`📚`、`📦`）
- `colorHex`: 房间主题色彩（如 `#10B981`、`#3B82F6`）
- `xPct`, `yPct`: 房间在平面图上的左上角相对坐标 (0.0 ~ 1.0)
- `widthPct`, `heightPct`: 房间在平面图上的相对宽高 (0.0 ~ 1.0)

---

## 3. 位置变迁轨迹：`LocationMovement`

记录物品每一次被挪动的历史流转节点：
- `location`: 挪动后的位置名称（如 `"主卧衣柜二层"`）
- `houseName`: 空间名称
- `roomName`: 房间名称
- `pinX`, `pinY`: 平面图打点坐标
- `movedAt`: 挪动时间戳
- `note`: 挪动备注（如 `"从客厅茶几移入"`）

---

## 4. 数据备份 JSON 结构示例

导出 JSON 数据包时具有完整的向下兼容性，格式如下：

```json
{
  "version": 18,
  "app": "Collecter",
  "exported_at": 1723900000000,
  "entries": [
    {
      "id": "c1f7b8d0-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
      "cat": "数码",
      "brand": "联想拯救者 Y9000X",
      "qty": 1,
      "price": 8999.00,
      "cur_val": 5500.00,
      "p_date": 1690000000000,
      "ts": 1690000000000,
      "in": true,
      "notes": "京东自营首发购入",
      "unit": "台",
      "loc": "书房电竞桌",
      "h_id": "house_default",
      "h_name": "🏠 自己的家",
      "r_name": "书房",
      "px": 0.35,
      "py": 0.42,
      "imp": true,
      "a_type": "depreciating",
      "img_p": "photo_a1b2c3d4.jpg",
      "rec_p": "receipt_e5f6g7h8.jpg"
    }
  ]
}
```
