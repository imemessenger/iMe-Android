package com.smedialink.bots.usecase

import android.app.DownloadManager
import android.content.Context
import android.content.Context.DOWNLOAD_SERVICE
import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.QuerySnapshot
import com.smedialink.bots.BotConstants
import com.smedialink.bots.data.SmartReplier
import com.smedialink.bots.data.database.TagDbModel
import com.smedialink.bots.data.mapper.BotCategoryMapper
import com.smedialink.bots.data.mapper.ResponseMapper
import com.smedialink.bots.data.mapper.ShopItemMapper
import com.smedialink.bots.data.model.BotStatus
import com.smedialink.bots.data.repository.BotsRepository
import com.smedialink.bots.data.repository.UserAnswersRepository
import com.smedialink.bots.domain.AigramBot
import com.smedialink.bots.domain.Replier
import com.smedialink.bots.domain.model.*
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.jetbrains.annotations.NotNull
import java.io.File
import net.lingala.zip4j.core.ZipFile as ZipArchive

// Управление ботами iMe и всем, что с ними связано
// выступает единым источником правды
class AiBotsManager private constructor(context: Context, private val downloadsPath: File, private val destinationPath: File) {

    companion object {
        fun getInstance(context: Context, downloadsPath: File, destinationPath: File): AiBotsManager {
            if (INSTANCE == null) {
                synchronized(AiBotsManager::class) {
                    INSTANCE = AiBotsManager(context, downloadsPath, destinationPath)
                }
            }

            return INSTANCE!!
        }

        @Volatile
        private var INSTANCE: AiBotsManager? = null

        val botFeatureRegions: Set<String> = setOf(
                // Russian
                "ru_MD", "ru_UA", "ru_RU", "ru_KZ", "ru_KG", "ru_BY", "ru",
                // Armenian
                "hy_AM", "hy",
                // Uzbek
                "uz_Cyrl_UZ", "uz_Cyrl",
                // Tajik
                "tg_Cyrl_TJ", "tg_Cyrl",
                // Azerbaijani
                "az_Cyrl_AZ", "az_Cyrl"

        )

        val botFeatureCountryPhoneCodes: Set<String> = setOf(
                "7", "373", "374", "375", "380", "992", "994", "996", "998"
        )
    }

    // Коллбек события об установке приложения
    interface AppInstalledCallback {
        fun onSuccess()
    }

    // Коллбеки получения ответов от ботов
    interface SmartReplierCallback {
        fun onSuccess(@NotNull responses: List<SmartBotResponse>)
        fun onError(@NotNull throwable: Throwable)
    }

    // Коллбек о загрузке инфы из Firebase
    interface FirebaseSnapshotCallback {
        fun onSuccess()
    }

    // Коллбек при смене списка активных ботов
    interface BotsListChangedCallback {
        fun onSuccess()
    }

    // Список текущих активных ботов
    val activeBots = mutableListOf<AigramBot>()
    // Текущие загрузки (downloadId + путь)
    val downloads = mutableMapOf<Long, DownloadSession>()

    var currentTags: List<TagDbModel> = listOf()
    private val answersRepository = UserAnswersRepository(context)
    val botsRepository: BotsRepository = BotsRepository(context, destinationPath)
    private val responseMapper: ResponseMapper = ResponseMapper(botsRepository, context)
    private val shopItemMapper: ShopItemMapper = ShopItemMapper()
    private val categoriesMapper: BotCategoryMapper = BotCategoryMapper()
    private val replier: Replier = SmartReplier(this, responseMapper, answersRepository)
    private val downloadManager = context.getSystemService(DOWNLOAD_SERVICE) as DownloadManager

    private val disposables = CompositeDisposable()

    var botDisableCallback: BotsListChangedCallback? = null

    // Флаг для обновление инфы о покупках один раз за сессию при получении первого обновления из Firebase
    private var purchaseInfoPreloaded = false

    // Получение всех sku
    fun getAvailableSkus(): Single<List<String>> =
            botsRepository.getSkus()

    // Получение ответов от ботов
    fun getAvailableResponses(sentence: String, userId: Int, smartReplierCallback: SmartReplierCallback) {
        replier.getResponsesFromBots(sentence, userId, smartReplierCallback)
    }

    // Обновление списка текущих активных ботов
    private fun rebuildActiveBotsList() {
        botsRepository.getActiveBotsList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ bots: List<AigramBot> ->
                    activeBots.clear()
                    activeBots.addAll(bots)
                    botDisableCallback?.onSuccess()
                }, { t ->
                    t.printStackTrace()
                })
                .also { disposables.add(it) }
    }

    // Событие об установке приложения
    fun sendAppInstalledEvent(userId: Long, callback: AppInstalledCallback) {
        botsRepository.sendAppInstallEvent(userId)
                .subscribeOn(Schedulers.io())
                .subscribe({
                    callback.onSuccess()
                    Log.d("Remote event", "App installed event, user id $userId")
                }, { it.printStackTrace() })
                .also { disposables.add(it) }
    }

    // Событие об установке бота
    private fun sendBotInstalledEvent(botId: String, userId: Int) {
        botsRepository.sendBotInstallEvent(botId, userId)
                .subscribeOn(Schedulers.io())
                .subscribe({
                    Log.d("Remote event", "Bot $botId installed event, user id $userId")
                }, { it.printStackTrace() })
                .also { disposables.add(it) }
    }

    // Событие о выставлении оценки боту
    fun sendBotRatingEvent(botId: String, userId: Long, rating: Int) {
        botsRepository.sendBotRating(botId, userId, rating)
                .subscribeOn(Schedulers.io())
                .subscribe({
                    Log.d("Remote event", "Bot $botId rating $rating event, user id $userId")
                }, { it.printStackTrace() })
                .also { disposables.add(it) }
    }

    // Все доступные боты для отображения в магазине
    fun getAllBotsObservable(botLanguage: BotLanguage, language: String): Observable<List<ShopItem>> =
            botsRepository.getBotsListObservable()
                    .map { it.filter { bot -> bot.lang == botLanguage } }
                    .map { shopItemMapper.mapList(it, language, botsRepository.getTags()) }
                    .map { list -> list.sortedWith(compareBy(ShopItem::priority, ShopItem::title)) }

    // Все доступные боты для отображения в магазине
    fun getAllBotsObservable(language: String): Observable<List<ShopItem>> =
            botsRepository
                    .getBotsListObservable()
                    .map {
                        if (currentTags.isEmpty()) {
                            currentTags = botsRepository.getTags()
                        }
                        shopItemMapper.mapList(it, language, currentTags)
                    }
                    .map { list -> list.sortedWith(compareBy(ShopItem::priority, ShopItem::title)) }

    // Отдельный бот для отображения инфы
    fun getSingleBotObservable(botId: String, language: String): Observable<ShopItem> =
            botsRepository
                    .getSingleBotObservable(botId)
                    .map {
                        if (currentTags.isEmpty()) {
                            currentTags = botsRepository.getTags()
                        }
                        shopItemMapper.mapItem(it, currentTags, language)
                    }

    // ПОлучение категорий
    fun getAvailableCategories(language: String): Observable<List<SmartBotCategory>> =
            botsRepository
                    .getAllCategories()
                    .map { categoriesMapper.mapList(it, language) }
                    .map { list -> list.sortedByDescending { it.priority } }

    // Подгрузка собственных оценок
    fun fetchVotes(userId: Long) {
        botsRepository.fetchVotes(userId).subscribe({}, { it.printStackTrace() }).also { disposables.add(it) }
    }

    // Подписка на обновления удаленного хранилища ботов
    fun listenForRemoteBotUpdates(callback: FirebaseSnapshotCallback) {
        botsRepository.getRemoteBotUpdates()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe({ snapshot: QuerySnapshot ->
                    if (snapshot.documents.isNotEmpty()) {
                        botsRepository.storeBotDocuments(snapshot)
                        callback.onSuccess()
                        fetchTags()
                        rebuildActiveBotsList()
                    }
                }, { t: Throwable ->
                    t.printStackTrace()
                })
                .also { disposables.add(it) }
    }

    fun handleChosenBotAnswer(botId: String, tag: String, position: Int, userId: Int) {
        Completable.fromAction {
            if (botId == BotConstants.HOLIDAYS_ID) {
                answersRepository.saveHolidayGreeting(userId, tag)
            } else {
                answersRepository.increaseResponseCounter(botId, tag, position)
            }
        }
                .subscribeOn(Schedulers.io())
                .subscribe({}, { e -> e.printStackTrace() })
                .also { disposables.add(it) }
    }

    private fun fetchTags() {
        botsRepository.getTagsInfo()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe({ snapshot: QuerySnapshot ->
                    if (snapshot.documents.isNotEmpty()) {
                        botsRepository.storeTagDocuments(snapshot)
                        fetchCategories()
                    }
                }, { t: Throwable ->
                    t.printStackTrace()
                })
                .also { disposables.add(it) }
    }

    private fun fetchCategories() {
        botsRepository.getCategoriesInfo()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe({ snapshot: QuerySnapshot ->
                    if (snapshot.documents.isNotEmpty()) {
                        botsRepository.storeCategoryDocuments(snapshot)
                        fetchCategories()
                    }
                }, { t: Throwable ->
                    t.printStackTrace()
                })
                .also { disposables.add(it) }
    }

    // Простая валидация чеков
    fun validateReceipts(list: List<ShopProduct.Receipt>): Single<List<Boolean>> =
            botsRepository.validateReceipts(list)

    // Кэширование инфы о текущих покупках
    fun storeActualPurchases(purchases: List<ShopProduct>): Completable =
            botsRepository.storePurchasesInfo(purchases)

    // Помечаем купленного бота доступным
    fun downloadPurchase(sku: String, userId: Int) {
        botsRepository.getBotBySku(sku)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe({ botDbModel ->
                    startBotDownloading(botDbModel.id, botDbModel.title, botDbModel.file, userId)
                }, { it.printStackTrace() })
                .also { disposables.add(it) }
    }

    // Обновить статус бота
    fun updateBotStatus(botId: String, status: BotStatus): Completable =
            botsRepository.updateBotStatus(botId, status)
                    .doOnComplete { rebuildActiveBotsList() }

    // Отключить бота
    fun disableBot(botId: String) {
        updateBotStatus(botId, BotStatus.DISABLED)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Log.d("BotsDownloader", "$botId disabled")
                }, { it.printStackTrace() })
                .also { disposables.add(it) }
    }

    // + задать боту статус DOWNLOADING
    // + запустить DownloadManager, получить ID загрузки
    // + сохранить ID загрузки
    // + по завершению удалить ID из загрузок
    // + распаковать в internal storage
    // + удалить скачанный архив
    // + при выходе из приложения очищать текущие загрузки и сбрасывать статус в бд
    fun startBotDownloading(botId: String, title: String, downloadLink: String, userId: Int) {
        Log.d("BotsDownloader", "Download $botId started")
        Log.d("BotsDownloader", "Download path $downloadsPath")
        updateBotStatus(botId, BotStatus.DOWNLOADING)
                .doOnSubscribe { sendBotInstalledEvent(botId, userId) }
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe({
                    launchDownloadSession(botId, title, downloadLink)
                }, { it.printStackTrace() })
                .also { disposables.add(it) }
    }

    // Запуск загрузки
    private fun launchDownloadSession(botId: String, title: String, downloadLink: String) {
        val destination = File(downloadsPath, botId)
        val request = DownloadManager.Request(Uri.parse(downloadLink))
                .setTitle(title)
                .setDescription("Downloading...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationUri(Uri.fromFile(destination))
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)


        val downloadId = downloadManager.enqueue(request)
        downloads[downloadId] = DownloadSession(destination,botId)
    }

    // Загрузка завершена, обрабатываем
    fun handleDownloadCompletion(downloadId: Long) {
        val sourceZipName = downloads[downloadId]?.file
        Log.d("BotsDownloader", "Download $downloadId completed")
        Log.d("BotsDownloader", "File downloaded to ${sourceZipName?.absolutePath}")
        Log.d("BotsDownloader", "File exists? ${sourceZipName?.exists()}")
        Log.d("BotsDownloader", "File last modified: ${sourceZipName?.lastModified()}")

        Log.d("BotsDownloader", "Unzip to $destinationPath")

        unzip(sourceZipName?.absolutePath, destinationPath.absolutePath)
                .subscribeOn(Schedulers.io())
                .andThen(downloads[downloadId]?.botId?.let {
                            botsRepository.updateRemoteBotHash(it)
                        } ?: Completable.complete())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Log.d("BotsDownloader", "Unzip completed, deletion started")
                    downloads.remove(downloadId)
                    cleanupData(sourceZipName)
                }, { it.printStackTrace() })
                .also { disposables.add(it) }

    }

    // Помечаем завершенным
    private fun cleanupData(archive: File?) {
        if (archive == null) {
            return
        }

        val botId = archive.name
        updateBotStatus(botId, BotStatus.ENABLED)
                .doOnComplete { archive.delete() }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Log.d("BotsDownloader", "$botId installed")
                }, { it.printStackTrace() })
                .also { disposables.add(it) }
    }

    // Распаковка файла
    private fun unzip(zipPath: String?, destination: String): Completable =
            Completable.create { emitter ->
                try {
                    val zip = ZipArchive(zipPath)
                    if (zip.file.exists()) {
                        zip.extractAll(destination)
                    }
                    emitter.onComplete()
                } catch (e: Exception) {
                    emitter.onError(e)
                }
            }

    private fun cleanDownloads(finallyCallback: () -> Unit) {

        downloads.forEach { downloadManager.remove(it.key) }

        botsRepository.resetDownloads()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally { finallyCallback.invoke() }
                .subscribe({
                    Log.d("BotsDownloader", "Downloads cleared")
                }, { it.printStackTrace() })
                .also { disposables.add(it) }
    }


    // Сброс загрузок и очистка данных при выходе из приложения
    fun cancel() {
        cleanDownloads {
            disposables.clear()
        }
    }
}
