package com.kfaino.diapertracker

import android.app.Activity
import android.widget.Toast

/**
 * 🧹 引导教程沙盒演示数据生命周期管理 (Demo Data Sandbox)
 * 记录教程演练期间注入的演示示例数据，并在教程完成或中途退出时 100% 安全回滚清理。
 */
object TourSandbox {

    private val demoEntryIds = mutableSetOf<String>()
    private val demoVoucherIds = mutableSetOf<String>()
    private val demoIdentityDocIds = mutableSetOf<String>()
    private val demoMedicineIds = mutableSetOf<String>()
    private val demoFoodIds = mutableSetOf<String>()
    private val demoHonorIds = mutableSetOf<String>()

    fun registerDemoEntry(id: String) = demoEntryIds.add(id)
    fun registerDemoVoucher(id: String) = demoVoucherIds.add(id)
    fun registerDemoIdentityDoc(id: String) = demoIdentityDocIds.add(id)
    fun registerDemoMedicine(id: String) = demoMedicineIds.add(id)
    fun registerDemoFood(id: String) = demoFoodIds.add(id)
    fun registerDemoHonor(id: String) = demoHonorIds.add(id)

    /** 执行沙盒演示数据彻底回滚清理，确保不影响真实资产记录 */
    fun cleanup(activity: Activity) {
        val store = DataStore(activity)
        var cleanedCount = 0

        if (demoEntryIds.isNotEmpty()) {
            val all = store.loadAll().filterNot { demoEntryIds.contains(it.id) }
            store.saveAll(all)
            cleanedCount += demoEntryIds.size
            demoEntryIds.clear()
        }

        if (demoVoucherIds.isNotEmpty()) {
            val all = store.getVouchers().filterNot { demoVoucherIds.contains(it.id) }
            store.saveVouchers(all)
            cleanedCount += demoVoucherIds.size
            demoVoucherIds.clear()
        }

        if (demoIdentityDocIds.isNotEmpty()) {
            val all = store.getIdentityDocs().filterNot { demoIdentityDocIds.contains(it.id) }
            store.saveIdentityDocs(all)
            cleanedCount += demoIdentityDocIds.size
            demoIdentityDocIds.clear()
        }

        if (demoMedicineIds.isNotEmpty()) {
            val all = store.getMedicines().filterNot { demoMedicineIds.contains(it.id) }
            store.saveMedicines(all)
            cleanedCount += demoMedicineIds.size
            demoMedicineIds.clear()
        }

        if (demoFoodIds.isNotEmpty()) {
            val all = store.getFoods().filterNot { demoFoodIds.contains(it.id) }
            store.saveFoods(all)
            cleanedCount += demoFoodIds.size
            demoFoodIds.clear()
        }

        if (demoHonorIds.isNotEmpty()) {
            val all = store.getHonorCredentials().filterNot { demoHonorIds.contains(it.id) }
            store.saveHonorCredentials(all)
            cleanedCount += demoHonorIds.size
            demoHonorIds.clear()
        }

        if (cleanedCount > 0) {
            Toast.makeText(activity, "🧹 教程演示数据已自动安全回滚清理，未影响真实资产", Toast.LENGTH_SHORT).show()
        }
    }
}
