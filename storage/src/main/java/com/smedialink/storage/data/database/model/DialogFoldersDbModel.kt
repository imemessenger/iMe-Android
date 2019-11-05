package com.smedialink.storage.data.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class DialogFoldersDbModel(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val userId: Long,
    val name: String,
    val backgroundId: Int,
    val avatar: String = "",
    val pinned: Boolean,
    val children: Set<Long>
)
