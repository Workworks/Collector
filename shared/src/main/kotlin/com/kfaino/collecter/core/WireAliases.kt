package com.kfaino.collecter.core

import org.json.JSONObject
import org.json.JSONArray

/** Explicit wire aliases; canonical keys match historical Android backups. Unknown keys survive. */
object WireAliases {
    private val aliases = mapOf(
        "vouchers" to mapOf("val" to listOf("valueAmount"), "min" to listOf("minSpend"), "rem_t" to listOf("remainingTimes"), "tot_t" to listOf("totalTimes"), "s_date" to listOf("startDate"), "e_date" to listOf("expiryDate"), "plat" to listOf("platform"), "photo" to listOf("photoPath"), "used" to listOf("isUsed"), "used_at" to listOf("usedAt")),
        "identity_docs" to mapOf("name" to listOf("nameOnDoc"), "mem" to listOf("member"), "dtype" to listOf("docType"), "dnum" to listOf("certNumber"), "iss_d" to listOf("issueDate"), "exp_d" to listOf("expiryDate"), "f_photo" to listOf("frontPhotoPath"), "b_photo" to listOf("backPhotoPath")),
        "medicines" to mapOf("cat" to listOf("category"), "loc" to listOf("location"), "dos" to listOf("dosage"), "aud" to listOf("targetAudience"), "e_date" to listOf("expiryDate"), "opened" to listOf("isOpened"), "o_date" to listOf("openedAt"), "o_days" to listOf("openedValidityDays"), "photo" to listOf("photoPath"), "contra" to listOf("contraindications")),
        "foods" to mapOf("loc" to listOf("location"), "e_date" to listOf("expDate"), "opened" to listOf("isOpened"), "o_date" to listOf("openedAt"), "photo" to listOf("photoPath")),
        "honors" to mapOf("mem" to listOf("member"), "cat" to listOf("category"), "cnum" to listOf("certNumber"), "photo" to listOf("photoPath")),
        "wardrobe" to mapOf("cat" to listOf("category"), "mat" to listOf("material"), "loc" to listOf("storageLocation"), "photo" to listOf("photoPath"), "wear_cnt" to listOf("wearCount"), "last_worn" to listOf("lastWornAt"), "sealed" to listOf("isSealed"), "sealed_at" to listOf("sealedAt")),
        "emergency" to mapOf("kit" to listOf("kitType"), "cat" to listOf("category"), "loc" to listOf("location"), "exp_d" to listOf("expiryDate"), "photo" to listOf("photoPath")),
        "tools" to mapOf("cat" to listOf("category"), "loc" to listOf("location"), "interval_d" to listOf("maintenanceIntervalDays"), "last_maint_d" to listOf("lastMaintainedAt"), "photo" to listOf("photoPath")),
        "plants" to mapOf("light" to listOf("lightDemand"), "loc" to listOf("location"), "water_d" to listOf("waterIntervalDays"), "last_water_d" to listOf("lastWateredAt"), "fert_d" to listOf("fertilizeIntervalDays"), "last_fert_d" to listOf("lastFertilizedAt"), "photo" to listOf("photoPath"), "tips" to listOf("careTips")),
        "pets" to mapOf("weight" to listOf("weightKg"), "food" to listOf("foodBrand"), "deworm_d" to listOf("dewormIntervalDays"), "last_deworm_d" to listOf("lastDewormedAt"), "vax_d" to listOf("vaccineIntervalDays"), "last_vax_d" to listOf("lastVaccinatedAt"), "photo" to listOf("photoPath")),
        "books" to mapOf("total_p" to listOf("totalPages"), "cur_p" to listOf("currentPages"), "location" to listOf("bookshelfLocation"), "borrower" to listOf("borrowerName"), "lent_d" to listOf("lentDate")),
        "beverages" to mapOf("vintage" to listOf("vintageYear"), "location" to listOf("storageLocation"), "origin" to listOf("originRegion"), "opened_at" to listOf("openedAt"), "photo" to listOf("photoPath")),
        "entries" to mapOf("cat" to listOf("category"), "loc" to listOf("location"), "h_name" to listOf("houseName"), "r_name" to listOf("roomName"), "px" to listOf("pinX"), "py" to listOf("pinY"), "in" to listOf("isIn"), "img_p" to listOf("photoPath"), "rec_p" to listOf("receiptPath"), "m_date" to listOf("mfg_date"), "e_date" to listOf("exp_date"), "p_date" to listOf("purchase_date"), "cur_val" to listOf("current_valuation"), "is_ret" to listOf("is_retired"), "ret_at" to listOf("retired_date"), "ret_act" to listOf("retired_action"), "ret_sp" to listOf("retired_sold_price"), "ret_note" to listOf("retired_note"), "sub_cyc" to listOf("sub_cycle"), "sub_nxt" to listOf("sub_next_billing_date"), "sub_rnw" to listOf("sub_auto_renew"), "imp" to listOf("is_important"), "rem_en" to listOf("reminder_enabled"), "rem_int" to listOf("reminder_interval_days"), "chk_ts" to listOf("last_checked_date")),
    )

    fun convert(document: JSONObject, expand: Boolean = false): JSONObject {
        val root = JSONObject(document.toString())
        val cycles = mapOf("MONTHLY" to "按月", "QUARTERLY" to "按季", "YEARLY" to "按年", "WEEKLY" to "按周", "HALF_YEARLY" to "按半年")
        for ((collection, fields) in aliases) {
            val arr = root.optJSONArray(collection) ?: continue
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                if (collection == "entries" && !expand && !item.has("sub_cyc") && item.has("sub_cycle")) {
                    val value = item.getString("sub_cycle")
                    item.put("sub_cyc", cycles[value] ?: value)
                }
                for ((canonical, alternatives) in fields) {
                    if (!item.has(canonical)) alternatives.firstOrNull { item.has(it) }?.let { item.put(canonical, item.get(it)) }
                    for (alias in alternatives) {
                        if (expand && item.has(canonical)) item.put(alias, item.get(canonical))
                        else if (!expand) item.remove(alias)
                    }
                }
            }
        }
        root.optJSONArray("entries")?.let { entries ->
            for (i in 0 until entries.length()) {
                val item = entries.getJSONObject(i)
                if (item.has("sub_cyc")) {
                    val value = item.getString("sub_cyc")
                    if (expand) item.put("sub_cycle", cycles.entries.firstOrNull { it.value == value }?.key ?: value)
                }
            }
        }
        root.optJSONObject("ledger_entries")?.let { ledgers ->
            for (key in ledgers.keys()) {
                val wrapper = JSONObject().put("entries", ledgers.getJSONArray(key))
                ledgers.put(key, convert(wrapper, expand).getJSONArray("entries"))
            }
        }
        return root
    }
}
