package com.kfaino.diapertracker

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

typealias ResaleAnalytics = AnalyticsQueries.ResaleAnalytics

/** 基于 SharedPreferences 的高可用持久化层，管理物品折旧、在役/退役待办归置、周期订阅资产与空间位置 */
class DataStore(private val ctx: Context) {
    private val prefs = ctx.getSharedPreferences("collector_data", Context.MODE_PRIVATE)
    private val keyEntries: String
        get() {
            val curId = try { LedgerManager.getCurrentLedger(ctx).id } catch (e: Exception) { "default" }
            return if (curId == "default") "entries_v4" else "entries_ledger_$curId"
        }
    private val keyHouses = "houses_v1"
    private val keyCategories = "custom_categories_v2"
    private val keyTheme = "app_theme_mode"
    private val settingsStore = SettingsStore(prefs)
    private val spaceRepo = SpaceRepository(prefs)
    private val categoryRepo = CategoryRepository(prefs)
    private val entryRepo = EntryRepository(prefs)

    private fun notifyWidgets() {
        try {
            ExpiringAndSubWidgetProvider.updateAllWidgets(ctx)
            QuickAddWidgetProvider.updateAllWidgets(ctx)
        } catch (e: Exception) {
            android.util.Log.w("DataStore", "更新桌面小组件失败", e)
        }
    }

    companion object {
        // 通用默认分类
        val DEFAULT_CATEGORIES = listOf("数码", "日用品", "零食", "耗材", "贵重证件", "网络订阅")

        // 常用快捷数量单位
        val COMMON_UNITS = listOf("件", "台", "个", "套", "张", "片", "包", "箱", "瓶", "盒", "本")

        // 常见待办归置渠道
        val RETIRED_ACTIONS = listOf(
            "📦 挂闲鱼代售",
            "📱 挂转转二手",
            "🎁 赠送亲友",
            "♻️ 环保回收 / 以旧换新",
            "🗄️ 封箱入库收藏",
            "🗑️ 报废丢弃"
        )

        const val THEME_SYSTEM = 0
        const val THEME_LIGHT = 1
        const val THEME_DARK = 2

        fun applyThemeMode(mode: Int) {
            when (mode) {
                THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }

    // ==================== 物品出入库、折旧与订阅记录 ====================

    fun loadAll(): List<Entry> = entryRepo.loadAll(keyEntries)

    fun saveAll(entries: List<Entry>) = entryRepo.saveAll(entries, keyEntries) { notifyWidgets() }

    fun updateEntry(index: Int, newEntry: Entry): Boolean = entryRepo.updateEntry(index, newEntry, keyEntries) { notifyWidgets() }

    fun deleteEntryAt(index: Int): Boolean = entryRepo.deleteEntryAt(index, keyEntries) { notifyWidgets() }

    fun setRetired(entryId: String, isRetired: Boolean, action: String = "挂闲鱼代售", soldPrice: Double = 0.0, note: String = "") =
        entryRepo.setRetired(entryId, isRetired, action, soldPrice, note, keyEntries) { notifyWidgets() }

    fun clearAllData() = entryRepo.clearAllData(keyEntries)

    fun getLastUsedUnit(): String = entryRepo.getLastUsedUnit()

    fun setLastUsedUnit(unit: String) = entryRepo.setLastUsedUnit(unit)

    // ==================== 重要物品与订阅提醒 ====================

    fun getImportantEntries(): List<Entry> = loadAll().filter { it.isImportant || it.reminderEnabled }

    fun getSubscriptionEntries(): List<Entry> = loadAll().filter { it.isSubscription }

    fun getNonSubscriptionEntries(): List<Entry> = loadAll().filter { !it.isSubscription }

    fun confirmItemChecked(entryId: String) = entryRepo.confirmItemChecked(entryId, keyEntries) { notifyWidgets() }

    // ==================== 多空间/家庭空间管理 ====================

    fun getHouses(): List<HouseSpace> = spaceRepo.getHouses()

    fun saveHouses(houses: List<HouseSpace>) = spaceRepo.saveHouses(houses)

    fun addHouse(name: String, type: String = "住宅"): HouseSpace = spaceRepo.addHouse(name, type)

    fun deleteHouse(houseId: String): Boolean = spaceRepo.deleteHouse(houseId)

    fun updateHouse(updatedHouse: HouseSpace): Boolean = spaceRepo.updateHouse(updatedHouse)

    fun addRoomToHouse(houseId: String, room: HouseRoom): Boolean = spaceRepo.addRoomToHouse(houseId, room)

    fun updateRoomInHouse(houseId: String, room: HouseRoom): Boolean = spaceRepo.updateRoomInHouse(houseId, room)

    fun deleteRoomFromHouse(houseId: String, roomId: String): Boolean = spaceRepo.deleteRoomFromHouse(houseId, roomId)

    fun resetRoomsInHouse(houseId: String): List<HouseRoom> = spaceRepo.resetRoomsInHouse(houseId)

    // ==================== 通用分类分组管理 ====================

    fun getCategories(): List<String> = categoryRepo.getCategories { loadAll() }

    fun saveCategories(categories: List<String>) = categoryRepo.saveCategories(categories)

    fun addCategory(category: String): Boolean = categoryRepo.addCategory(category) { loadAll() }

    fun deleteCategory(category: String): Boolean = categoryRepo.deleteCategory(category) { loadAll() }

    fun resetCategories(): List<String> = categoryRepo.resetCategories { loadAll() }

    fun isPresetCategory(category: String): Boolean = categoryRepo.isPresetCategory(category)

    // ==================== 主题设置（深色/浅色/系统） ====================

    fun getThemeMode(): Int = settingsStore.getThemeMode()

    fun setThemeMode(mode: Int) = settingsStore.setThemeMode(mode)

    // ==================== GitHub 更新仓库设置 ====================

    fun getGithubRepo(): String = settingsStore.getGithubRepo()

    fun setGithubRepo(repo: String) = settingsStore.setGithubRepo(repo)

    // ==================== 通知提醒设置 ====================

    fun isNotificationEnabled(): Boolean = settingsStore.isNotificationEnabled()

    fun setNotificationEnabled(enabled: Boolean) = settingsStore.setNotificationEnabled(enabled)

    fun getNotificationHour(): Int = settingsStore.getNotificationHour()

    fun getNotificationMinute(): Int = settingsStore.getNotificationMinute()

    fun setNotificationTime(hour: Int, minute: Int) = settingsStore.setNotificationTime(hour, minute)

    // ==================== 备份与恢复 ====================

    fun exportBackupJson(): String = BackupCodec.exportBackupJson(getCategories(), loadAll())

    fun importBackupJson(jsonStr: String): Boolean = BackupCodec.importBackupJson(
        jsonStr,
        getCategories = { getCategories() },
        saveCategories = { saveCategories(it) },
        saveEntries = { saveAll(it) }
    )

    // ==================== 触感震动反馈配置 ====================

    fun isHapticFeedbackEnabled(): Boolean = settingsStore.isHapticFeedbackEnabled()

    fun setHapticFeedbackEnabled(enabled: Boolean) = settingsStore.setHapticFeedbackEnabled(enabled)

    // ==================== 备份提醒持久化控制 ====================

    fun getNextBackupPromptTime(): Long = settingsStore.getNextBackupPromptTime()

    fun snoozeBackupPrompt(days: Int = 3) = settingsStore.snoozeBackupPrompt(days)

    fun recordBackupDone() = settingsStore.recordBackupDone()

    fun shouldShowBackupBanner(): Boolean {
        val list = loadAll()
        if (list.isEmpty()) return false
        val now = System.currentTimeMillis()
        val nextPrompt = getNextBackupPromptTime()
        return now >= nextPrompt
    }

    // ==================== 生物识别指纹应用锁 ====================

    fun isBiometricLockEnabled(): Boolean = settingsStore.isBiometricLockEnabled()

    fun setBiometricLockEnabled(enabled: Boolean) = settingsStore.setBiometricLockEnabled(enabled)

    // ==================== WebDAV 私有云配置 ====================

    fun getWebDavUrl(): String = settingsStore.getWebDavUrl()

    fun setWebDavUrl(url: String) = settingsStore.setWebDavUrl(url)

    fun getWebDavUsername(): String = settingsStore.getWebDavUsername()

    fun setWebDavUsername(user: String) = settingsStore.setWebDavUsername(user)

    fun getWebDavPassword(): String = settingsStore.getWebDavPassword()

    fun setWebDavPassword(pass: String) = settingsStore.setWebDavPassword(pass)

    // ==================== 简易库存模式 (Simplified Mode) ====================

    fun isSimpleMode(): Boolean = settingsStore.isSimpleMode()

    fun setSimpleMode(enabled: Boolean) = settingsStore.setSimpleMode(enabled)

    // ==================== 闲置变现与回血 ROI 统计 (Cashback Analytics) ====================

    fun getResaleAnalytics(): ResaleAnalytics = AnalyticsQueries.getResaleAnalytics(loadAll())

    // ==================== 耗材安全库存预警与智能采购清单 ====================

    fun getLowStockItems(): List<Entry> = AnalyticsQueries.getLowStockItems(loadAll())

    fun generateReplenishmentListText(): String = AnalyticsQueries.generateReplenishmentListText(loadAll())

    // ==================== 🎞️ 时光胶囊与数字资产扩展管理 ====================

    /** 获取所有数字与电子资产（电子相册、软件授权、数字文档等） */
    fun getDigitalAssets(): List<Entry> = loadAll().filter { it.isDigital }

    /** 获取所有带有生活时光回忆瞬间的资产 */
    fun getMemoryAssets(): List<Entry> = loadAll().filter { it.memoryMoments.isNotEmpty() }

    /** 为指定资产追加一条时光回忆瞬间 */
    fun addMemoryMoment(entryId: String, moment: ItemMemoryMoment): Boolean =
        entryRepo.addMemoryMoment(entryId, moment, keyEntries) { notifyWidgets() }

    /** 更新指定资产的某条时光回忆瞬间 */
    fun updateMemoryMoment(entryId: String, moment: ItemMemoryMoment): Boolean =
        entryRepo.updateMemoryMoment(entryId, moment, keyEntries) { notifyWidgets() }

    /** 删除指定资产的某条时光回忆瞬间 */
    fun deleteMemoryMoment(entryId: String, momentId: String): Boolean =
        entryRepo.deleteMemoryMoment(entryId, momentId, keyEntries) { notifyWidgets() }

    // ==================== 📤 实物外借与共享借还流转管理 ====================

    /** 获取所有当前处于借出状态的资产 */
    fun getLentOutAssets(): List<Entry> = loadAll().filter { it.isLentOut }

    /** 获取所有借出已逾期的资产 */
    fun getOverdueLendingAssets(): List<Entry> = loadAll().filter { it.isLendingOverdue() }

    /** 获取所有历史借用人姓名列表 (用于自动补全) */
    fun getAllBorrowerNames(): List<String> = entryRepo.getAllBorrowerNames(keyEntries)

    /** 登记借出资产 */
    fun lendAsset(
        entryId: String,
        borrowerName: String,
        borrowerContact: String = "",
        expectedReturnDate: Long = 0L,
        deposit: Double = 0.0,
        notes: String = "",
        photoPath: String = ""
    ): Boolean = entryRepo.lendAsset(entryId, borrowerName, borrowerContact, expectedReturnDate, deposit, notes, photoPath, keyEntries) { notifyWidgets() }

    /** 归还打卡登记 */
    fun returnAsset(
        entryId: String,
        actualReturnDate: Long = System.currentTimeMillis(),
        returnConditionRating: Int = 5,
        notes: String = ""
    ): Boolean = entryRepo.returnAsset(entryId, actualReturnDate, returnConditionRating, notes, keyEntries) { notifyWidgets() }

    // =========================================================================
    // 📦 第一性原理收纳馆门面
    //
    // 以下方法的实现已拆分至 VaultRepositories.kt，此处仅作委托。
    // 公开签名与拆分前完全一致，调用方无需改动。
    // =========================================================================

    // ---- 🎟️ 时效权益与卡券票据收纳馆 (Voucher & Privilege Vault) ----
    private val voucherRepo = VoucherVaultRepository(prefs)

    fun getVouchers(): List<VoucherRecord> = voucherRepo.getVouchers()
    fun saveVouchers(list: List<VoucherRecord>) = voucherRepo.saveVouchers(list)
    fun addOrUpdateVoucher(voucher: VoucherRecord) = voucherRepo.addOrUpdateVoucher(voucher)
    fun deleteVoucher(voucherId: String) = voucherRepo.deleteVoucher(voucherId)
    fun useTimesCardOneTime(voucherId: String): Boolean = voucherRepo.useTimesCardOneTime(voucherId)
    fun markVoucherUsed(voucherId: String, used: Boolean) = voucherRepo.markVoucherUsed(voucherId, used)

    // ---- 🪪 家庭多成员证照与敏感凭证 (Family Identity & Safe) ----
    private val identityRepo = IdentityVaultRepository(prefs)

    fun getIdentityDocs(): List<IdentityDocument> = identityRepo.getIdentityDocs()
    fun saveIdentityDocs(list: List<IdentityDocument>) = identityRepo.saveIdentityDocs(list)
    fun addOrUpdateIdentityDoc(doc: IdentityDocument) = identityRepo.addOrUpdateIdentityDoc(doc)
    fun deleteIdentityDoc(docId: String) = identityRepo.deleteIdentityDoc(docId)

    // ---- 💊 家庭智能健康药箱 (Medicine & Scenario Vault) ----
    private val medicineRepo = MedicineVaultRepository(prefs)

    fun getMedicines(): List<MedicineRecord> = medicineRepo.getMedicines()
    fun saveMedicines(list: List<MedicineRecord>) = medicineRepo.saveMedicines(list)
    fun addOrUpdateMedicine(medicine: MedicineRecord) = medicineRepo.addOrUpdateMedicine(medicine)
    fun deleteMedicine(medicineId: String) = medicineRepo.deleteMedicine(medicineId)
    fun markMedicineOpened(medicineId: String) = medicineRepo.markMedicineOpened(medicineId)

    // ---- 🥦 冰箱冷冻与食材生鲜鲜度库 (Food & Fresh Vault) ----
    private val foodRepo = FoodVaultRepository(prefs)

    fun getFoods(): List<FoodRecord> = foodRepo.getFoods()
    fun saveFoods(list: List<FoodRecord>) = foodRepo.saveFoods(list)
    fun addOrUpdateFood(food: FoodRecord) = foodRepo.addOrUpdateFood(food)
    fun deleteFood(foodId: String) = foodRepo.deleteFood(foodId)
    fun markFoodOpened(foodId: String) = foodRepo.markFoodOpened(foodId)
    fun consumeFood(foodId: String, delta: Double = 1.0) = foodRepo.consumeFood(foodId, delta)

    // ---- 🏆 全家成长履历与职业荣誉考级勋章馆 (Honor & Credentials) ----
    private val honorRepo = HonorVaultRepository(prefs)

    fun getHonorCredentials(): List<HonorCredential> = honorRepo.getHonorCredentials()
    fun saveHonorCredentials(list: List<HonorCredential>) = honorRepo.saveHonorCredentials(list)
    fun addOrUpdateHonorCredential(honor: HonorCredential) = honorRepo.addOrUpdateHonorCredential(honor)
    fun deleteHonorCredential(honorId: String) = honorRepo.deleteHonorCredential(honorId)

}
