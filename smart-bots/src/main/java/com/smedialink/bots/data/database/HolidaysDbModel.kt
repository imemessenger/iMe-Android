package com.smedialink.bots.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class HolidaysDbModel(
    @PrimaryKey
    val userId: Int,
    val tags: String
)
