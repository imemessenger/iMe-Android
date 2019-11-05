package com.smedialink.settings

data class BackupModel(val tabsSorting: List<BackupTab>,
                       val folders: List<FolderBackupModel>,
                       val autoBotsDialogs: Boolean,
                       val autoBotsGroups: Boolean,
                       val oftenUsedBots: Boolean)