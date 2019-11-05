package com.smedialink.bots.domain.model

import com.smedialink.bots.data.model.BotStatus
import com.smedialink.bots.data.model.BotType

data class ShopItem(
        val botId: String,
        val sku: String?,
        val avatar: String,
        val language: BotLanguage,
        val title: String,
        val description: String,
        val installs: Long,
        val priority: Long,
        val reviews: Long,
        val rating: Float,
        val ownRating: Int,
        val phrases: Long,
        val themes: Long,
        val created: String,
        val updated: String,
        val tags: List<SmartTag>,
        val price: String?,
        val downloadLink: String,
        val type: BotType,
        val status: BotStatus
) {
    val searchField: String
        get() = title + description
}