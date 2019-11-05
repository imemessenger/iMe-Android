package com.smedialink.settings

data class FolderBackupModel(val name: String,
                             val avatar: String,
                             val pinned: Boolean,
                             val members: Set<Long>
)