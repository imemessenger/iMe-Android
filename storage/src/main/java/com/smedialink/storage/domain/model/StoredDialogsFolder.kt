package com.smedialink.storage.domain.model

data class StoredDialogsFolder(
    val id: Long = 0,
    val backgroundId: Int,
    val name: String,
    val avatar: String? = "",
    val pinned: Boolean,
    val ids: Set<Long>
)
