package com.smedialink.storage.data.database.dao

import androidx.room.*
import com.smedialink.storage.data.database.model.DialogFoldersDbModel
import io.reactivex.Flowable
import io.reactivex.Single

@Dao
abstract class DialogFoldersDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(folder: DialogFoldersDbModel): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(folders: List<DialogFoldersDbModel>): List<Long>

    @Query("DELETE FROM DialogFoldersDbModel")
    abstract fun clearAll()

    @Query("SELECT * FROM DialogFoldersDbModel WHERE userId LIKE :userId")
    abstract fun getAll(userId: Long): Single<List<DialogFoldersDbModel>>

    @Query("SELECT * FROM DialogFoldersDbModel WHERE id LIKE :folderId AND userId LIKE :userId")
    abstract fun getFolder(folderId: Long, userId: Long): DialogFoldersDbModel

    @Query("SELECT * FROM DialogFoldersDbModel WHERE userId LIKE :userId")
    abstract fun getFolders(userId: Long): List<DialogFoldersDbModel>

    @Query("DELETE FROM DialogFoldersDbModel WHERE id LIKE :folderId AND userId LIKE :userId")
    abstract fun deleteFolder(folderId: Long, userId: Long): Int

    @Query("UPDATE DialogFoldersDbModel SET pinned = :shouldPin WHERE id LIKE :folderId AND userId LIKE :userId")
    abstract fun pinFolder(folderId: Long, userId: Long, shouldPin: Boolean)

    @Query("UPDATE DialogFoldersDbModel SET name = :newName WHERE id LIKE :folderId AND userId LIKE :userId")
    abstract fun renameFolder(folderId: Long, userId: Long, newName: String)

    @Query("UPDATE DialogFoldersDbModel SET avatar = :newAvatar WHERE id LIKE :folderId AND userId LIKE :userId")
    abstract fun setAvatar(folderId: Long, userId: Long, newAvatar: String)

    @Query("SELECT * FROM DialogFoldersDbModel WHERE userId LIKE :userId")
    abstract fun streamAll(userId: Long): Flowable<List<DialogFoldersDbModel>>

    @Transaction
    open fun saveDialogToFolder(folderId: Long, userId: Long, dialogId: Long) {
        val current = getFolder(folderId, userId)
        val newChildren = current.children.toMutableSet()
        newChildren.add(dialogId)
        val newFolder = current.copy(children = newChildren)
        insert(newFolder)
    }

    @Transaction
    open fun saveDialogsToFolder(folderId: Long, userId: Long, dialogIds: Array<Long>) {
        val current = getFolder(folderId, userId)
        val newChildren = current.children.toMutableSet()
        newChildren.addAll(dialogIds)
        val newFolder = current.copy(children = newChildren)
        insert(newFolder)
    }


    @Transaction
    open fun removeDialogsFromFolder(folderId: Long, userId: Long, dialogIds: Array<Long>) {
        val current = getFolder(folderId, userId)
        val newChildren = current.children.toMutableSet()
        newChildren.removeAll(dialogIds)
        val newFolder = current.copy(children = newChildren)
        insert(newFolder)
    }

    @Transaction
    open fun deleteDialog(userId: Long, dialogId: Long) {
        val allFolders = getFolders(userId)
        for (folder in allFolders) {
            val folderDialogs = folder.children.toMutableSet()
            if (folderDialogs.contains(dialogId)) {
                folderDialogs.remove(dialogId)

                val newFolder = folder.copy(children = folderDialogs)
                if (folderDialogs.isNotEmpty()) {
                    insert(newFolder)
                } else {
                    deleteFolder(newFolder.id, newFolder.userId)
                }
            }
        }
    }
}
