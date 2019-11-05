package com.smedialink.bots.domain.model

data class ShopProduct(
    val sku: String,
    val price: String,
    val isPurchased: Boolean,
    val receipt: Receipt?
) {

    data class Receipt(
        val orderId: String,
        val packageName: String,
        val productId: String,
        val purchaseTime: Long,
        val purchaseState: Int,
        val purchaseToken: String
    )
}
