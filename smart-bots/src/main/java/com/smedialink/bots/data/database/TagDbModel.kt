package com.smedialink.bots.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class TagDbModel(
    @PrimaryKey
    val id: String,
    val title: String,
    val hidden: Int = 0,
    val locales: Map<String, String>

)
