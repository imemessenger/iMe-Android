package com.smedialink.storage.data.repository

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.smedialink.storage.data.database.StorageDatabase
import com.smedialink.storage.data.database.dao.ChannelDao
import com.smedialink.storage.data.database.model.parser.SnapshotParser
import com.smedialink.storage.data.mapper.ChannelMapper
import com.smedialink.storage.data.network.AiGramApi
import com.smedialink.storage.domain.model.Channel
import com.smedialink.storage.domain.model.EmptySnapshotException
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single

class ChannelRepository private constructor(context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: ChannelRepository? = null

        fun getInstance(context: Context): ChannelRepository {
            if (INSTANCE == null) {
                synchronized(StorageRepository::class) {
                    INSTANCE = ChannelRepository(context)
                }
            }
            return INSTANCE!!
        }

        private const val CHANNELS_COLLECTION_NAME = "telegram_channels"

    }

    private val remoteDatabase = FirebaseFirestore.getInstance()
    private val channelMapper: ChannelMapper = ChannelMapper()
    private val database: StorageDatabase = StorageDatabase.getInstance(context)
    private val channelDao: ChannelDao = database.channelDao()
    private val snapshotParser: SnapshotParser = SnapshotParser()
    private val auGramApi: AiGramApi = AiGramApi.getInstance()


    fun observeChannels(country: String): Observable<List<Channel>> =
            channelDao.streamChannels()
                    .map { channels -> channels.map { channelMapper.mapFromDb(it) } }
                    .toObservable()
                    .map { it.filter { channel->channel.countries.contains(country) } }

    fun observeChannel(channelId: String): Observable<Channel> =
            channelDao.streamChannel(channelId)
                    .map { channel -> channelMapper.mapFromDb(channel) }
                    .toObservable()


    fun observeRemoteChannels(): Observable<List<Channel>> =
            Observable.create<List<Channel>>
            { emitter ->
                remoteDatabase.collection(CHANNELS_COLLECTION_NAME)
                        .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, exception ->
                            when {
                                exception != null -> emitter.onError(exception)
                                snapshot == null -> emitter.onError(EmptySnapshotException("Collection $CHANNELS_COLLECTION_NAME is empty"))
                                else -> emitter.onNext(snapshotParser.parseChannels(snapshot))
                            }
                        }
            }

    fun sendChannelRating(channelId: String, userId: Long, rating: Int): Single<Int> =
            auGramApi.voteForChannel(channelId, rating, userId)
                    .flatMap { response ->
                        when {
                            response.status == AiGramApi.STATUS_OK ->
                                Single.just(rating)
                            response.status == AiGramApi.STATUS_ERROR ->
                                Single.error(Exception(response.message))
                            else ->
                                Single.error(Exception("Unknown error"))
                        }
                    }
                    .doOnSuccess { channelDao.saveOwnRating(channelId, it) }
                    .onErrorReturn { channelDao.getOwnRating(channelId) }


    fun getChannels(): Single<List<Channel>> =
            channelDao.getChannels()
                    .map { channelMapper.mapFromDb(it) }

    fun saveChannels(channels: List<Channel>): Completable =
            Completable.fromAction {
                channelDao.upsert(channelMapper.mapToDb(channels))
            }


}