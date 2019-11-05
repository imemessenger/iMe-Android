package com.smedialink.storage.data.mapper

import com.smedialink.storage.data.database.model.DialogFoldersDbModel
import com.smedialink.storage.domain.model.StoredDialogsFolder

class DialogFolderMapper {

    fun mapToDb(userId: Long, folder: StoredDialogsFolder): DialogFoldersDbModel =
            DialogFoldersDbModel(folder.id, userId, folder.name, folder.backgroundId, folder.avatar.orEmpty(), folder.pinned, folder.ids)

    fun mapToDb(userId: Long, folders: List<StoredDialogsFolder>): List<DialogFoldersDbModel> =
            folders.map { mapToDb(userId,it) }

    fun mapFromDb(dbFolder: DialogFoldersDbModel): StoredDialogsFolder =
        StoredDialogsFolder(dbFolder.id, dbFolder.backgroundId, dbFolder.name, dbFolder.avatar, dbFolder.pinned, dbFolder.children)
}
