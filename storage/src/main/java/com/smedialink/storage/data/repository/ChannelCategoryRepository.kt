package com.smedialink.storage.data.repository

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.smedialink.storage.data.database.StorageDatabase
import com.smedialink.storage.data.database.dao.ChannelCategoryDao
import com.smedialink.storage.data.database.model.ChannelCategoryDbModel
import com.smedialink.storage.data.database.model.parser.SnapshotParser
import com.smedialink.storage.data.mapper.ChannelCategoryMapper
import com.smedialink.storage.domain.model.ChannelCategory
import com.smedialink.storage.domain.model.EmptySnapshotException
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers

class ChannelCategoryRepository private constructor(context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: ChannelCategoryRepository? = null

        fun getInstance(context: Context): ChannelCategoryRepository {
            if (INSTANCE == null) {
                synchronized(StorageRepository::class) {
                    INSTANCE = ChannelCategoryRepository(context)
                }
            }
            return INSTANCE!!
        }

        private const val CHANNEL_CATEGORY_COLLECTION_NAME = "telegram_channel_categories"
    }

    private val remoteDatabase = FirebaseFirestore.getInstance()
    private val channelCategoryMapper: ChannelCategoryMapper = ChannelCategoryMapper()
    private val database: StorageDatabase = StorageDatabase.getInstance(context)
    private val channelCategoryDao: ChannelCategoryDao = database.channelCategoryDao()
    private val snapshotParser: SnapshotParser = SnapshotParser()


    fun observeChannelCategories(): Observable<List<ChannelCategory>> =
            channelCategoryDao.streamChannelCategories()
                    .map { channelCategories -> channelCategories.map { channelCategoryMapper.mapFromDb(it) } }
                    .toObservable()

    fun observeChannelCategory(categoryId: String): Observable<ChannelCategory> =
            channelCategoryDao.streamChannelCategory(categoryId)
                    .map { channelCategory -> channelCategoryMapper.mapFromDb(channelCategory)  }
                    .toObservable()

    fun getChannelCategories(tags: Set<String>): Single<List<ChannelCategory>> =
            channelCategoryDao.getChannelsCategories()
                    .map { categories ->
                        categories.filter { category -> containsTags(category.tags, tags) }
                    }
                    .map { channelCategories -> channelCategories.map { channelCategoryMapper.mapFromDb(it) } }

    fun observeRemoteChannelsCategories(): Observable<List<ChannelCategory>> =
            Observable.create<List<ChannelCategoryDbModel>> { emitter ->
                remoteDatabase.collection(CHANNEL_CATEGORY_COLLECTION_NAME)
                        .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, exception ->
                            when {
                                exception != null -> emitter.onError(exception)
                                snapshot == null -> emitter.onError(EmptySnapshotException("Collection $CHANNEL_CATEGORY_COLLECTION_NAME is empty"))
                                else -> emitter.onNext(snapshotParser.parseChannelCategories(snapshot))
                            }
                        }
            }
                    .observeOn(Schedulers.io())
                    .flatMap {
                        Completable.fromAction {
                            channelCategoryDao.upsert(it)
                        }.toSingleDefault(it)
                                .toObservable()
                    }
                    .map { dbModels -> dbModels.map { channelCategoryMapper.mapFromDb(it) } }

    private fun containsTags(tags: Set<String>, containsTags: Set<String>): Boolean =
            tags.find { containsTags.contains(it) } != null

}