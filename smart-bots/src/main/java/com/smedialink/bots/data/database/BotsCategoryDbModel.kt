package com.smedialink.bots.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class BotsCategoryDbModel(
        @PrimaryKey
        val id: String,
        val title: String,
        val priority: Int,
        val tags: List<String>,
        val locales: Map<String, String>
)
