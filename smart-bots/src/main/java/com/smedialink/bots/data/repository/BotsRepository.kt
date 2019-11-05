package com.smedialink.bots.data.repository

import android.content.Context
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.QuerySnapshot
import com.smedialink.bots.BotConstants
import com.smedialink.bots.data.database.*
import com.smedialink.bots.data.factory.JsonResourceFactory
import com.smedialink.bots.data.model.BotStatus
import com.smedialink.bots.data.model.BotType
import com.smedialink.bots.data.model.bot.HolidaysBot
import com.smedialink.bots.data.model.bot.NeuroBot
import com.smedialink.bots.data.network.SmartBotsApi
import com.smedialink.bots.domain.AigramBot
import com.smedialink.bots.domain.ResourceFactory
import com.smedialink.bots.domain.exception.EmptySnapshotException
import com.smedialink.bots.domain.model.BotLanguage
import com.smedialink.bots.domain.model.ShopProduct
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.util.*

class BotsRepository(context: Context, botsPath: File) {

    companion object {
        fun initialBotsList(): List<BotsDbModel> =
                BotConstants.predefinedBots.map { name ->
                    BotsDbModel(
                            id = name,
                            installLogged = 1,
                            useAssets = 1,
                            hash = BotConstants.hashes[name] ?: "",
                            status = BotStatus.ENABLED,
                            lang = if (name.contains("_eng")) BotLanguage.EN else BotLanguage.RU,
                            type = BotType.resolveByName(name)
                    )
                }

        // Коллекции
        private const val BOTS_COLLECTION_NAME = "bots"
        private const val CATEGORIES_COLLECTION_NAME = "bot_categories"
        private const val TAGS_COLLECTION_NAME = "tags"
        // Боты
        private const val FIELD_TITLE = "title"
        private const val FIELD_DESCRIPTION = "description"
        private const val FIELD_SKU = "sku"
        private const val FIELD_INSTALLS = "installs"
        private const val FIELD_PRIORITY = "priority"
        private const val FIELD_RATING = "rating"
        private const val FIELD_LOCALES = "locales"
        private const val FIELD_REVIEWS = "reviews"
        private const val FIELD_TAGS = "tags"
        private const val FIELD_AVATARS = "avatars"
        private const val KEY_AVATAR_ORIGINAL = "original"
        private const val KEY_AVATAR_ROUNDED = "rounded"
        private const val FIELD_MODEL = "model"
        private const val KEY_MODEL_FILE = "file"
        private const val KEY_MODEL_HASH = "hash"
        private const val KEY_MODEL_LANG = "lang"
        private const val KEY_MODEL_PHRASES = "phrases"
        private const val KEY_MODEL_THEMES = "themes"
        private const val KEY_MODEL_UPDATED = "updated"
        private const val FIELD_CREATED = "created"
        // Теги
        private const val TAGS_FIELD_TITLE = "title"
        private const val TAGS_FIELD_HIDDEN = "hidden"
        // Категории
        private const val CATEGORIES_FIELD_TITLE = "title"
        private const val CATEGORIES_FIELD_PRIORITY = "priority"
        private const val CATEGORIES_FIELD_TAGS = "tags"


    }

    private val remoteDatabase = FirebaseFirestore.getInstance()
    private val botsApi: SmartBotsApi = SmartBotsApi.getInstance()
    private val factory: ResourceFactory = JsonResourceFactory(context.assets, botsPath)
    private val botsDatabase: BotsDatabase = BotsDatabase.getInstance(context.applicationContext)
    private val botsDao: BotsDao = botsDatabase.botsDao()
    private val tagsDao: BotsTagDao = botsDatabase.tagsDao()
    private val categoriesDao: BotsCategoryDao = botsDatabase.categoryDao()

    fun getSkus(): Single<List<String>> = Single.create { emitter ->
        try {
            val list = botsDao.getAll().mapNotNull { it.sku }
            emitter.onSuccess(list)
        } catch (e: Exception) {
            emitter.onError(e)
        }
    }

    fun sendAppInstallEvent(userId: Long): Completable =
            botsApi.appInstall(
                    appId = SmartBotsApi.CLIENT_ID,
                    type = SmartBotsApi.TYPE_INSTALL,
                    userId = userId
            ).flatMapCompletable { response ->
                when {
                    response.status == SmartBotsApi.STATUS_OK ->
                        Completable.complete()
                    response.status == SmartBotsApi.STATUS_ERROR ->
                        Completable.error(Exception(response.message))
                    else ->
                        Completable.error(Exception("Unknown error"))
                }
            }

    fun sendBotInstallEvent(botId: String, userId: Int): Completable =
            botsApi.botInstall(botId, SmartBotsApi.TYPE_INSTALL, userId)
                    .flatMapCompletable { response ->
                        when {
                            response.status == SmartBotsApi.STATUS_OK ->
                                Completable.complete()
                            response.status == SmartBotsApi.STATUS_ERROR ->
                                Completable.error(Exception(response.message))
                            else ->
                                Completable.error(Exception("Unknown error"))
                        }
                    }

    fun sendBotRating(botId: String, userId: Long, rating: Int): Single<Int> =
            botsApi.voteForBot(botId, rating, userId)
                    .flatMap { response ->
                        when {
                            response.status == SmartBotsApi.STATUS_OK ->
                                Single.just(rating)
                            response.status == SmartBotsApi.STATUS_ERROR ->
                                Single.error(Exception(response.message))
                            else ->
                                Single.error(Exception("Unknown error"))
                        }
                    }
                    .doOnSuccess { botsDao.saveOwnRating(botId, it) }
                    .onErrorReturn { botsDao.getOwnRating(botId) }

    fun fetchVotes(userId: Long): Completable =
            botsApi.getBotsVoting(userId)
                    .flatMapCompletable { response ->
                        botsDao.saveRatings(response)
                        Completable.complete()
                    }
                    .subscribeOn(Schedulers.io())

    fun validateReceipts(items: List<ShopProduct.Receipt>): Single<List<Boolean>> =
            botsApi.validatePurchases(items)
                    .map { it.payload }
                    .map { list -> list.map { it.success } }

    fun getActiveBotsList(): Single<List<AigramBot>> =
            Single.create { emitter ->
                try {
                    val bots = botsDao.getAll()
                            .filter { it.status == BotStatus.ENABLED }
                            .map { dbBot ->
                                if (dbBot.type == BotType.NEURO)
                                    NeuroBot(dbBot.id, factory, dbBot.useAssets != 0, dbBot.lang)
                                else
                                    HolidaysBot(factory, dbBot.useAssets != 0)
                            }
                    emitter.onSuccess(bots)
                } catch (e: Exception) {
                    emitter.onError(e)
                }
            }

    fun getBotsListObservable(): Observable<List<BotsDbModel>> =
            botsDao.streamAll().toObservable()

    fun getSingleBotObservable(botId: String): Observable<BotsDbModel> =
            botsDao.streamBot(botId).toObservable()

    fun getAllCategories(): Observable<List<BotsCategoryDbModel>> =
            categoriesDao.getAll().toObservable()

    fun updateBotStatus(botId: String, newStatus: BotStatus): Completable =
            Completable.fromAction {
                botsDao.getById(botId)?.copy(status = newStatus)?.let { botsDao.update(it) }
            }

    // Получение обновлений коллекции ботов
    fun getRemoteBotUpdates(): Observable<QuerySnapshot> =
            Observable.create { emitter ->
                remoteDatabase.collection(BOTS_COLLECTION_NAME)
                        .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, exception ->
                            when {
                                exception != null -> emitter.onError(exception)
                                snapshot == null -> emitter.onError(EmptySnapshotException("Collection $BOTS_COLLECTION_NAME is empty"))
                                else -> emitter.onNext(snapshot)
                            }
                        }
            }

    fun updateRemoteBotHash(botId: String): Completable =
            Single.create<String> { emitter ->
                remoteDatabase.collection(BOTS_COLLECTION_NAME)
                        .document(botId)
                        .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, exception ->
                            when {
                                exception != null -> emitter.onError(exception)
                                snapshot == null -> emitter.onError(EmptySnapshotException("Document $botId is empty"))
                                else -> {
                                    val model = snapshot.get(FIELD_MODEL) as? Map<String, Any>
                                    val remoteHash = model?.get(KEY_MODEL_HASH) as String? ?: ""
                                    emitter.onSuccess(remoteHash)
                                }

                            }
                        }
            }
                    .observeOn(Schedulers.io())
                    .flatMapCompletable { remoteHash ->
                        Completable.fromAction {
                            botsDao.updateBot(
                                    botId = botId,
                                    hash = remoteHash,
                                    botUpdated = 1,
                                    useAssets = 0
                            )
                        }
                    }

    fun storeBotDocuments(snapshot: QuerySnapshot) {
        val bots = mutableListOf<BotsDbModel>()

        snapshot.documents.forEach { document ->
            val existingBot = botsDao.getById(document.id)
            val avatars = document.get(FIELD_AVATARS) as? Map<String, String>
            val model = document.get(FIELD_MODEL) as? Map<String, Any>
            val botSku = document.getString(FIELD_SKU)
            val baseHash = existingBot?.hash ?: ""
            val remoteHash = model?.get(KEY_MODEL_HASH) as String? ?: ""
            val modelUpdated = (model?.get(KEY_MODEL_UPDATED) as? Timestamp)?.toDate() ?: Date()
            val modelLang = model?.get(KEY_MODEL_LANG) as? String ?: "ru"
            val avatarOriginal = avatars?.get(KEY_AVATAR_ORIGINAL) ?: ""
            val titleLocales: MutableMap<String, String> = mutableMapOf()
            val descriptionLocales: MutableMap<String, String> = mutableMapOf()
            if (document.get(FIELD_LOCALES) != null) {
                (document.get(FIELD_LOCALES) as HashMap<String, HashMap<String, String>>).forEach { entry ->
                    titleLocales[entry.key] = entry.value[FIELD_TITLE] ?: ""
                    descriptionLocales[entry.key] = entry.value[FIELD_DESCRIPTION] ?: ""
                }
            }


            val avatarRounded = avatars?.get(KEY_AVATAR_ROUNDED) ?: ""
            if (avatars != null && model != null && avatarOriginal.isNotBlank() && avatarRounded.isNotBlank()) {
                // TODO AIGRAM проверить как срабатывает обновление
                // для проверки обновления сравниваем хеши и если новый не равен базовому,
                // то сразу сохраняем новый в бд и меняем статус на доступно обновление
                val updateAvailable = baseHash.isNotEmpty() &&
                        remoteHash.isNotEmpty() &&
                        baseHash != remoteHash &&
                        (existingBot?.status == BotStatus.ENABLED || existingBot?.status == BotStatus.UPDATE_AVAILABLE)
                val botStatus = when {
                    updateAvailable -> BotStatus.UPDATE_AVAILABLE
                    existingBot != null -> {
                        if (existingBot.status != BotStatus.ENABLED && existingBot.status != BotStatus.DISABLED && botIsPreinstalled(existingBot.id)) {
                            BotStatus.ENABLED
                        } else {
                            existingBot.status
                        }
                    }
                    botSku.isNullOrEmpty().not() && botIsPreinstalled(document.id) -> BotStatus.ENABLED
                    botSku.isNullOrEmpty().not() -> BotStatus.PAID
                    else -> {
                        if (botIsPreinstalled(document.id)) {

                            BotStatus.ENABLED
                        } else {
                            BotStatus.AVAILABLE
                        }
                    }
                }
                val newBot = BotsDbModel(
                        id = document.id,
                        sku = botSku,
                        avatarOriginal = avatarOriginal,
                        avatarRounded = avatarRounded,
                        title = document.getString(FIELD_TITLE) ?: "",
                        lang = BotLanguage.fromValue(modelLang),
                        titleLocales = titleLocales,
                        descriptionLocales = descriptionLocales,
                        description = document.getString(FIELD_DESCRIPTION) ?: "",
                        installs = document.getLong(FIELD_INSTALLS) ?: 0,
                        priority = document.getLong(FIELD_PRIORITY) ?: 0,
                        rating = document.getDouble(FIELD_RATING)?.toFloat() ?: 0f,
                        reviews = document.getLong(FIELD_REVIEWS) ?: 0,
                        ownRating = existingBot?.ownRating ?: 0,
                        installLogged = existingBot?.installLogged ?: 0,
                        useAssets = if (botIsPreinstalled(document.id) && existingBot?.botUpdated != 1) 1 else 0,
                        botUpdated = existingBot?.botUpdated?:0,
                        tags = document.get(FIELD_TAGS) as? List<String> ?: emptyList(),
                        file = model[KEY_MODEL_FILE] as String? ?: "",
                        hash = if (baseHash.isEmpty()) remoteHash else baseHash,
                        phrases = model[KEY_MODEL_PHRASES] as Long? ?: 0,
                        themes = model[KEY_MODEL_THEMES] as Long? ?: 0,
                        created = document.getTimestamp(FIELD_CREATED)?.toDate() ?: Date(),
                        updated = modelUpdated,
                        price = if (botSku.isNullOrBlank()) null else existingBot?.price,
                        type = BotType.resolveById(document.id),
                        status = botStatus
                )
                bots.add(newBot)
            }
        }
        botsDao.deleteByIgnored(bots.map { it.id }.toTypedArray())
        botsDao.insertOrReplace(bots)
    }

    // Получение текущих тегов
    fun getTagsInfo(): Single<QuerySnapshot> =
            Single.create { emitter ->
                remoteDatabase.collection(TAGS_COLLECTION_NAME).get()
                        .addOnFailureListener { exception -> emitter.onError(exception) }
                        .addOnSuccessListener { snapshot ->
                            if (snapshot != null) {
                                emitter.onSuccess(snapshot)
                            } else {
                                emitter.onError(EmptySnapshotException("Collection $TAGS_COLLECTION_NAME is empty"))
                            }
                        }
            }

    // Сохраняем теги
    fun storeTagDocuments(snapshot: QuerySnapshot) {
        val tags = mutableListOf<TagDbModel>()
        snapshot.documents.forEach { document ->
            val hidden = document.getBoolean(TAGS_FIELD_HIDDEN) ?: false
            val locales: MutableMap<String, String> = mutableMapOf()
            (document.get("locales") as? HashMap<String, HashMap<String, String>>)?.forEach { entry ->
                locales[entry.key] = entry.value["title"] ?: ""
            }
            tags.add(
                    TagDbModel(
                            id = document.id,
                            title = document.getString(TAGS_FIELD_TITLE) ?: "",
                            hidden = if (hidden) 1 else 0,
                            locales = locales
                    )
            )
        }
        tagsDao.insertOrReplace(tags)
    }

    // Получение текущих категорий
    fun getCategoriesInfo(): Single<QuerySnapshot> =
            Single.create { emitter ->
                remoteDatabase.collection(CATEGORIES_COLLECTION_NAME).get()
                        .addOnFailureListener { exception -> emitter.onError(exception) }
                        .addOnSuccessListener { snapshot ->
                            if (snapshot != null) {
                                emitter.onSuccess(snapshot)
                            } else {
                                emitter.onError(EmptySnapshotException("Collection $CATEGORIES_COLLECTION_NAME is empty"))
                            }
                        }
            }

    // Сохраняем теги
    fun storeCategoryDocuments(snapshot: QuerySnapshot) {
        val categories = mutableListOf<BotsCategoryDbModel>()
        snapshot.documents.forEach { document ->
            val locales: MutableMap<String, String> = mutableMapOf()
            (document.get("locales") as? HashMap<String, HashMap<String, String>>)?.forEach { entry ->
                locales[entry.key] = entry.value["title"] ?: ""
            }
            categories.add(
                    BotsCategoryDbModel(
                            id = document.id,
                            title = document.getString(CATEGORIES_FIELD_TITLE) ?: "",
                            priority = document.getLong(CATEGORIES_FIELD_PRIORITY)?.toInt() ?: 0,
                            tags = document.get(CATEGORIES_FIELD_TAGS) as? List<String>
                                    ?: emptyList(),
                            locales = locales
                    )
            )
        }
        categoriesDao.insertOrReplace(categories)
    }

    // Получение тега по id
    fun getTag(tagId: String): TagDbModel? =
            tagsDao.getById(tagId)

    // Получение тега по id
    fun getTags(): List<TagDbModel> =
            tagsDao.getAll()
                    .filterNotNull()

    // Получение тега по id
    fun getTags(ids: Array<String>): List<TagDbModel> =
            tagsDao.getAll(ids)
                    .filterNotNull()


    fun storePurchasesInfo(products: List<ShopProduct>): Completable =
            Completable.fromAction {
                // Сохраняем инфу обо всех продуктах
                products.forEach { product ->
                    val existingBot = botsDao.getBySku(product.sku)
                    val newBot = existingBot?.copy(
                            price = product.price
                    )
                    newBot?.let { botsDao.insertOrReplace(it) }
                }

                // Помечаем доступным то что кулено
                products.filter { it.receipt != null }.forEachIndexed { index, product ->
                    val existingBot = botsDao.getBySku(product.sku)
                    if (existingBot?.status == BotStatus.PAID) {
                        val newBot = existingBot.copy(
                                status = BotStatus.AVAILABLE
                        )
                        newBot.let { botsDao.insertOrReplace(it) }
                    }
                }
            }

    fun getBotById(botId: String): BotsDbModel? =
            botsDao.getById(botId)

    fun getBotBySku(sku: String): Single<BotsDbModel> =
            botsDao.getBotBySku(sku)

    fun resetDownloads(): Completable =
            Completable.fromAction {
                botsDao.resetDownloads(BotStatus.AVAILABLE)
            }

    private fun botIsPreinstalled(botId: String) =
            initialBotsList().findLast { it.id == botId } != null

}
