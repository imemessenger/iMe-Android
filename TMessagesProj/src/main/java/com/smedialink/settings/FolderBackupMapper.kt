package com.smedialink.settings

import com.smedialink.storage.data.repository.StorageRepository
import com.smedialink.storage.domain.model.StoredDialogsFolder

class FolderBackupMapper {

    private fun mapToBackup(storedFolder: StoredDialogsFolder): FolderBackupModel =
            FolderBackupModel(
                    name = storedFolder.name,
                    avatar = storedFolder.avatar.orEmpty(),
                    pinned = storedFolder.pinned,
                    members = storedFolder.ids
            )

    fun mapToBackup(storedFolders: List<StoredDialogsFolder>): List<FolderBackupModel> =
            storedFolders.map { mapToBackup(it) }


    private fun mapToDialogsFolder(backupFolderModel: FolderBackupModel): StoredDialogsFolder = StoredDialogsFolder(
            backgroundId = StorageRepository.getRandomBackgroundId(),
            name = backupFolderModel.name,
            avatar = backupFolderModel.avatar,
            ids = backupFolderModel.members,
            pinned = backupFolderModel.pinned
    )

    fun mapToDialogsFolder(backupFolderModels: List<FolderBackupModel>): List<StoredDialogsFolder> =
            backupFolderModels.map { mapToDialogsFolder(it) }
}