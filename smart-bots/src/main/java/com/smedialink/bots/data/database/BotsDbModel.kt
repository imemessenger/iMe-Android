package com.smedialink.bots.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.smedialink.bots.data.model.BotStatus
import com.smedialink.bots.data.model.BotType
import com.smedialink.bots.domain.model.BotLanguage
import java.util.*

@Entity
data class BotsDbModel(
        @PrimaryKey
        val id: String,
        val sku: String? = null,
        val lang: BotLanguage = BotLanguage.RU,
        val avatarOriginal: String = "",
        val avatarRounded: String = "",
        val titleLocales: MutableMap<String, String> = mutableMapOf(),
        val descriptionLocales: MutableMap<String, String> = mutableMapOf(),
        val title: String = "",
        val description: String = "",
        val installs: Long = 0,
        val priority: Long = 0,
        val reviews: Long = 0,
        val rating: Float = 0f,
        val ownRating: Int = 0,
        val installLogged: Int = 0,
        val useAssets: Int = 0,
        val botUpdated: Int = 0,
        val tags: List<String> = emptyList(),
        val file: String = "",
        val hash: String = "",
        val phrases: Long = 0,
        val themes: Long = 0,
        val created: Date = Date(),
        val updated: Date = Date(),
        val price: String? = null,
        val type: BotType,
        val status: BotStatus
)
