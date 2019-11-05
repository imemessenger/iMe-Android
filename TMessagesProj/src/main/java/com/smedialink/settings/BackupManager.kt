package com.smedialink.settings

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.google.android.exoplayer2.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.smedialink.bots.usecase.AiBotsManager
import com.smedialink.shop.BotSettingsActivity
import com.smedialink.storage.data.repository.StorageRepository
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import org.telegram.messenger.*
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLRPC
import org.telegram.ui.DialogsTabManager
import java.io.*


class BackupManager(private val messagesController: MessagesController,
                    private val storageRepository: StorageRepository) {


    private val reqIds: MutableSet<Int> = mutableSetOf()
    private val dialogsTabManager: DialogsTabManager by lazy { DialogsTabManager.getInstance() }
    private val foldersFolderBackupMapper: FolderBackupMapper by lazy { FolderBackupMapper() }
    private val backupPreferences: SharedPreferences = MessagesController.getBackupSettings()
    private val globalPreferences: SharedPreferences = MessagesController.getGlobalMainSettings()
    var firstBackupCheck: Boolean
        get() = backupPreferences.getBoolean(SHARED_PREF_KEY_FIRST_BACKUP_CHEKC, true)
        set(value) {
            backupPreferences.edit().putBoolean(SHARED_PREF_KEY_FIRST_BACKUP_CHEKC, value).apply()
        }
    var backupAlertShown: Boolean
        get() = backupPreferences.getBoolean(SHARED_PREF_KEY_BACKUP_ALERT_SHOWN, false)
        set(value) {
            backupPreferences.edit().putBoolean(SHARED_PREF_KEY_BACKUP_ALERT_SHOWN, value).apply()
        }
    var sharedPrefAutoBackup: Boolean
        get() = globalPreferences.getBoolean(SHARED_PREF_KEY_AUTO_BACKUP, false)
        set(value) {
            globalPreferences.edit().putBoolean(SHARED_PREF_KEY_AUTO_BACKUP, value).apply()
        }


    fun loadBackup(fileLoader: FileLoader): Maybe<BackupModel> =
            loadMessage()
                    .flatMap { message ->
                        Maybe.create<BackupModel> { emitter ->
                            val file = FileLoader.getPathToMessage(message)
                            if (file.exists()) {
                                retrieveBackupSettings(file)?.let {
                                    Log.d("BackupTag", "file exist")
                                    emitter.onSuccess(it)
                                } ?: emitter.onComplete()
                            } else {
                                Log.d("BackupTag", "file not exist... load started")
                                fileLoader.loadFile(message.media.document, message, 0, 0)
                                emitter.onError(BackupFileNotExistException())
                            }
                        }
                    }


    fun saveSettings(currentAccount: Int, userId: Long, accountInstance: AccountInstance, context: Context): Completable =
            loadMessage()
                    .doOnComplete { throw BackupFileNotExistException() }
                    .flatMapCompletable { backupMessage ->
                        getBackupFilePath(userId, context)
                                .flatMapCompletable { path ->
                                    Completable.fromAction {
                                        val editMessage = MessageObject(currentAccount, backupMessage, false)
                                        SendMessagesHelper.prepareSendingDocuments(accountInstance, arrayListOf(path), arrayListOf(path), null, null, null, userId, null, null, editMessage, true, 0)
                                    }
                                }
                    }.onErrorResumeNext {
                        if (it is BackupFileNotExistException) {
                            getBackupFilePath(userId, context)
                                    .flatMapCompletable { path ->
                                        Completable.fromAction {
                                            SendMessagesHelper.prepareSendingDocuments(accountInstance, arrayListOf(path), arrayListOf(path), null, LocaleController.getString("iMe_setting", R.string.iMe_setting), null, userId, null, null, null, true, 0)
                                        }
                                    }
                        } else Completable.error(it)
                    }


    fun applySettings(backupModel: BackupModel, userId: Long): Completable =
            storageRepository.saveFolders(foldersFolderBackupMapper.mapToDialogsFolder(backupModel.folders), userId)
                    .observeOn(AndroidSchedulers.mainThread())
                    .andThen(Completable.fromAction {
                        val editor = globalPreferences.edit()
                        editor.putBoolean(BotSettingsActivity.SHARED_PREF_KEY_OFTEN_USED_BOTS_ENABLED, backupModel.oftenUsedBots)
                        editor.putBoolean(BotSettingsActivity.SHARED_PREF_KEY_AUTO_BOTS_DIALOGS_ENABLED, backupModel.autoBotsDialogs)
                        editor.putBoolean(BotSettingsActivity.SHARED_PREF_KEY_AUTO_BOTS_GROUPS_ENABLED, backupModel.autoBotsGroups)
                        editor.apply()
                    })


    fun cancelRequests() {
        reqIds.forEach {
            messagesController.connectionsManager.cancelRequest(it, true)
        }
    }

    private fun loadMessage(): Maybe<TLRPC.Message> =
            Maybe.create { emitter ->
                val query = BACKUP_FILE_NAME
                val req = TLRPC.TL_messages_search()
                req.peer = messagesController.getInputPeer(messagesController.userConfig.clientUserId)
                if (req.peer == null) {
                    emitter.onComplete()
                }
                req.limit = 1
                req.q = query
                req.offset_id = 0

                req.filter = TLRPC.TL_inputMessagesFilterEmpty()

                reqIds.add(messagesController.connectionsManager.sendRequest(req, { response, error ->
                    error?.let {
                        emitter.onError(Throwable(it.text))
                    }
                    if (response != null) {
                        val res = response as TLRPC.messages_Messages
                        if (res.messages.isEmpty()) {
                            emitter.onComplete()
                        } else {
                            emitter.onSuccess(res.messages[0])
                        }
                    } else {
                        emitter.onComplete()
                    }
                }, ConnectionsManager.RequestFlagFailOnServerErrors))


            }


    private fun retrieveBackupSettings(file: File): BackupModel? {
        return try {
            Gson().fromJson(readFile(file), BackupModel::class.java)
        } catch (e: JsonSyntaxException) {
            e.printStackTrace()
            null
        }
    }

    private fun readFile(file: File): String = try {
        val text = StringBuilder()
        val br = BufferedReader(FileReader(file))
        var line = br.readLine()
        while (line != null) {
            text.append(line)
            text.append('\n')
            line = br.readLine()
        }
        br.close()
        val base64Text = text.toString()
        val data = Base64.decode(base64Text.toByteArray(Charsets.UTF_8), Base64.DEFAULT)
        String(data, Charsets.UTF_8)
    } catch (e: IOException) {
        ""
    }


    private fun writeFile(text: String, context: Context): File {
        val data = text.toByteArray(Charsets.UTF_8)
        val base64Text = Base64.encodeToString(data, Base64.DEFAULT)

        val backupDir = File(context.cacheDir, File.separator + BACKUP_DIR_NAME)
        if (backupDir.exists()) {
            backupDir.listFiles().forEach { it.delete() }
        } else {
            backupDir.mkdir()
        }
        // Get the directory for the user's public pictures directory.
        val file = File(backupDir, System.currentTimeMillis().toString() + "_" + BACKUP_FILE_NAME)
        try {
            val fOut = FileOutputStream(file)
            val myOutWriter = OutputStreamWriter(fOut)
            myOutWriter.append(base64Text)

            myOutWriter.close()

            fOut.flush()
            fOut.close()
        } catch (e: IOException) {
            Log.e("Exception", "File write failed: $e")
        }
        return file


    }

    private fun getBackupFilePath(userId: Long, context: Context): Single<String> =
            storageRepository.getUserFolders(userId).flatMap { folders ->
                Single.fromCallable {
                    val autoBotsDialogs = globalPreferences.getBoolean(BotSettingsActivity.SHARED_PREF_KEY_AUTO_BOTS_DIALOGS_ENABLED, BotSettingsActivity.AUTO_BOTS_DIALOGS_DEFAULT)
                    val autoBotsGroups = globalPreferences.getBoolean(BotSettingsActivity.SHARED_PREF_KEY_AUTO_BOTS_GROUPS_ENABLED, BotSettingsActivity.AUTO_BOTS_GROUPS_DEFAULT)
                    val oftenUsed = globalPreferences.getBoolean(BotSettingsActivity.SHARED_PREF_KEY_OFTEN_USED_BOTS_ENABLED, BotSettingsActivity.AUTO_BOTS_OFTEN_USED_DEFAULT)

                    val tabsSorting = dialogsTabManager.allTabs.map { BackupTab(it.type.toString(), it.isEnabled, it.position) }
                    val backup = BackupModel(
                            tabsSorting = tabsSorting,
                            folders = foldersFolderBackupMapper.mapToBackup(folders),
                            oftenUsedBots = oftenUsed,
                            autoBotsDialogs = autoBotsDialogs,
                            autoBotsGroups = autoBotsGroups)
                    val jsonString = Gson().toJson(backup)
                    writeFile(jsonString, context).absolutePath
                }
            }


    class BackupFileNotExistException : Exception()


    companion object {

        @Volatile
        private var INSTANCE: BackupManager? = null

        const val BACKUP_FILE_NAME = "backup.ime"
        const val BACKUP_FILE_EXTENSION = ".ime"
        const val BACKUP_DIR_NAME = "ime_backup"
        const val SHARED_PREF_KEY_FIRST_BACKUP_CHEKC = "SHARED_PREF_KEY_FIRST_BACKUP_CHECK"
        const val SHARED_PREF_KEY_BACKUP_ALERT_SHOWN = "SHARED_PREF_KEY_BACKUP_ALERT_SHOWN"
        const val SHARED_PREF_KEY_AUTO_BACKUP = "SHARED_PREF_KEY_AUTO_BACKUP"
        fun getInstance(messagesController: MessagesController,
                        storageRepository: StorageRepository): BackupManager {
            if (INSTANCE == null) {
                synchronized(AiBotsManager::class) {
                    INSTANCE = BackupManager(messagesController, storageRepository)
                }
            }
            return INSTANCE!!
        }

    }
}
