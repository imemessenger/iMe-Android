package com.smedialink.storage.data.repository

import android.content.Context
import com.smedialink.storage.data.database.StorageDatabase
import com.smedialink.storage.data.database.dao.DialogFoldersDao
import com.smedialink.storage.data.mapper.DialogFolderMapper
import com.smedialink.storage.domain.model.StoredDialogsFolder
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import kotlin.random.Random

class StorageRepository private constructor(context: Context) {

    companion object {
        const val SHARED_PREF_IME_SETTINGS = "SHARED_PREF_IME_SETTINGS"
        @Volatile
        private var INSTANCE: StorageRepository? = null

        fun getInstance(context: Context): StorageRepository {
            if (INSTANCE == null) {
                synchronized(StorageRepository::class) {
                    INSTANCE = StorageRepository(context)
                }
            }
            return INSTANCE!!
        }

        // Случайный id, используемый для выбора фона аватарки созданной папки, в дальнейшем хранится в бд
        fun getRandomBackgroundId(): Int {
            return Random.nextInt(1000)
        }
    }

    private val database: StorageDatabase = StorageDatabase.getInstance(context)
    private val foldersDao: DialogFoldersDao = database.dialogFoldersDao()
    private val mapper: DialogFolderMapper = DialogFolderMapper()

    // Получение папок пользователя
    fun getUserFolders(userId: Long): Single<List<StoredDialogsFolder>> =
            foldersDao.getAll(userId)
                    .map { list -> list.map { mapper.mapFromDb(it) } }

    fun saveFolders(folders: List<StoredDialogsFolder>, userId: Long): Completable = Completable.fromAction {
        foldersDao.clearAll()
        foldersDao.insert(mapper.mapToDb(userId, folders))
    }

    // Для подписки на текущий список папок
    fun streamAvailableFolders(userId: Long): Observable<List<StoredDialogsFolder>> =
            foldersDao.streamAll(userId)
                    .map { list -> list.map { mapper.mapFromDb(it) } }
                    .toObservable()

    // Сохранение диалога в папку
    fun saveSingleDialogToFolder(folderId: Long, userId: Long, dialogId: Long): Completable =
            Completable.fromAction {
                foldersDao.saveDialogToFolder(folderId, userId, dialogId)
            }

    fun saveDialogsToFolder(folderId: Long, userId: Long, dialogIds: ArrayList<Long>): Completable =
            Completable.fromAction {
                foldersDao.saveDialogsToFolder(folderId, userId, dialogIds.toTypedArray())
            }


    // Удаление диалога из папки
    fun removeDialogsFromFolder(folderId: Long, userId: Long, dialogIds: java.util.ArrayList<Long>): Completable =
            Completable.fromAction {
                foldersDao.removeDialogsFromFolder(folderId, userId, dialogIds.toTypedArray())
            }

    // Сохранение списка диалогов в папку
    fun addDialogsToFolder(folderId: Long, userId: Long, dialogs: List<Long>): Completable =
            Completable.fromAction {
                val folder = foldersDao.getFolder(folderId, userId)
                val resultingDialogs = folder.children + dialogs
                val newFolder = StoredDialogsFolder(folderId, folder.backgroundId, folder.name, folder.avatar, folder.pinned, resultingDialogs)
                val mapped = mapper.mapToDb(userId, newFolder)
                foldersDao.insert(mapped)
            }

    // Создание новой папки и сохранение диалогов туда
    fun saveDialogsToNewFolder(userId: Long, folderName: String, avatar: String, dialogs: List<Long>): Completable =
            Completable.fromAction {
                val newFolder = StoredDialogsFolder(0, getRandomBackgroundId(), folderName, avatar, false, dialogs.toSet())
                val mapped = mapper.mapToDb(userId, newFolder)
                foldersDao.insert(mapped)
            }

    // Пин папки
    fun pinFolder(folderId: Long, userId: Long, shouldPin: Boolean): Completable =
            Completable.fromAction {
                foldersDao.pinFolder(folderId, userId, shouldPin)
            }

    // Переименование папки
    fun renameFolder(folderId: Long, userId: Long, newName: String, avatar: String): Completable =
            Completable.fromAction {
                foldersDao.renameFolder(folderId, userId, newName)
                if (avatar.isNotEmpty()) {
                    foldersDao.setAvatar(folderId, userId, avatar)
                }
            }

    // Удаление папки
    fun deleteFolder(folderId: Long, userId: Long): Completable =
            Completable.fromAction {
                foldersDao.deleteFolder(folderId, userId)
            }

    // Удаление диалога из всех сохраненных папок
    fun deleteDialog(userId: Long, dialogId: Long): Completable =
            Completable.fromAction {
                foldersDao.deleteDialog(userId, dialogId)
            }

    // Удаление диалога из всех сохраненных папок
    fun deleteDialogs(userId: Long, dialogs: ArrayList<Long>): Completable =
        Completable.fromAction {
            dialogs.forEach { dialog ->
                foldersDao.deleteDialog(userId, dialog)
            }
        }

    fun clearFolders():Completable  = Completable.fromAction {
        foldersDao.clearAll()
    }

}
