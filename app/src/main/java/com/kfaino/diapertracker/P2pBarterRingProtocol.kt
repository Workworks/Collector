package com.kfaino.diapertracker

import java.util.UUID

/**
 * 🤝 社区/熟人去中心化闲置物资免信任互换协议 (P2P Barter Ring Protocol)
 */
object P2pBarterRingProtocol {

    data class BarterListing(
        val listingId: String,
        val offerItemName: String,
        val desiredCategory: String,
        val ownerDeviceAlias: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    fun createListing(itemName: String, targetCategory: String, alias: String): BarterListing {
        return BarterListing(
            listingId = "barter_" + UUID.randomUUID().toString().take(8),
            offerItemName = itemName,
            desiredCategory = targetCategory,
            ownerDeviceAlias = alias
        )
    }
}