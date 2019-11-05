package com.smedialink.bots.data.database

import androidx.room.Entity

@Entity(primaryKeys = ["botId", "tag", "position"])
data class RecentDbModel(
    val botId: String,
    val tag: String,
    val position: Int,
    val counter: Int
)
