package com.smedialink.storage.domain.useCase

import android.content.Context
import android.util.Log
import com.smedialink.storage.data.repository.ChannelCategoryRepository
import com.smedialink.storage.data.repository.ChannelRepository
import com.smedialink.storage.data.repository.CountriesRepository
import com.smedialink.storage.domain.model.Channel
import com.smedialink.storage.domain.model.ChannelCategory
import com.smedialink.storage.domain.model.ChannelCategoryCollection
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers

class AiChannelsManager private constructor(context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: AiChannelsManager? = null

        fun getInstance(context: Context): AiChannelsManager {
            if (INSTANCE == null) {
                synchronized(AiChannelsManager::class) {
                    INSTANCE = AiChannelsManager(context)
                }
            }

            return INSTANCE!!
        }
    }


    private var channelsRepository = ChannelRepository.getInstance(context)
    private var countriesRepository = CountriesRepository.getInstance(context)
    private val channelCategoryRepository = ChannelCategoryRepository.getInstance(context)
    private val disposables = CompositeDisposable()

    fun observeChannelsCategories(phone: String, locale: String): Observable<List<ChannelCategoryCollection>> =
            Observable
                    .combineLatest(channelsRepository.observeChannels(countriesRepository.getCurrentCountry(phone, locale)), channelCategoryRepository.observeChannelCategories(),
                            BiFunction { channels: List<Channel>, categories: List<ChannelCategory> ->
                                createChannelCategoryCollection(channels, categories)
                            })
                    .distinctUntilChanged()
                    .map { categories -> categories.sortedBy { it.category.title } }

    fun observeChannelsCategory(categoryId: String, phone: String, locale: String): Observable<ChannelCategoryCollection> =
            Observable
                    .combineLatest(channelsRepository.observeChannels(countriesRepository.getCurrentCountry(phone, locale)), channelCategoryRepository.observeChannelCategory(categoryId),
                            BiFunction { channels: List<Channel>, category: ChannelCategory ->
                                createChannelCategoryCollection(channels, category)
                            })
                    .distinctUntilChanged()

    fun observeCategories(): Observable<List<ChannelCategory>> =
            channelCategoryRepository.observeChannelCategories()
                    .map { categories -> categories.sortedBy { it.title } }

    fun observeAllChannels(phone: String, locale: String): Observable<List<Channel>> =
            channelsRepository.observeChannels(countriesRepository.getCurrentCountry(phone, locale))

    fun observeChannel(channelId: String): Observable<Channel> =
            channelsRepository.observeChannel(channelId)


    fun observePupularChannels(phone: String, locale: String): Observable<List<Channel>> =
            channelsRepository.observeChannels(countriesRepository.getCurrentCountry(phone, locale))
                    .map { channels -> channels.sortedByDescending { it.subscribers } }


    fun observeRemoteChannels(): Observable<List<Channel>> =
            channelsRepository.observeRemoteChannels()

    fun saveChannels(channels: List<Channel>, subscribedChannels: Set<String>): Completable =
            setJoinedChannels(subscribedChannels,channels)
                    .flatMapCompletable {    channelsRepository.saveChannels(it) }


    fun subscribeRemoteChannelCategories(): Disposable =
            channelCategoryRepository.observeRemoteChannelsCategories()
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe({}, {
                        it.printStackTrace()
                    })

    fun getChannelCategories(tags: Set<String>): Single<List<ChannelCategory>> =
            channelCategoryRepository.getChannelCategories(tags)


    fun updateJoinedChanels(joinedChannelNames: Set<String>): Completable =
            channelsRepository.getChannels()
                    .flatMap { setJoinedChannels(joinedChannelNames, it) }
                    .flatMapCompletable { channelsRepository.saveChannels(it) }

    fun cancel() {
        disposables.clear()
    }

    fun sendChannelRating(channelId: String, userId: Long, rating: Int) {
        channelsRepository.sendChannelRating(channelId, userId, rating)
                .subscribeOn(Schedulers.io())
                .subscribe({
                    Log.d("Remote event", "Channel $channelId rating $rating event, user id $userId")
                }, { it.printStackTrace() })
                .also { disposables.add(it) }
    }

    private fun createChannelCategoryCollection(channels: List<Channel>, categories: List<ChannelCategory>): List<ChannelCategoryCollection> {
        val result = mutableListOf<ChannelCategoryCollection>()

        categories.forEach { category ->
            if (category.tags.isNotEmpty()) {
                val items = findItemsByTags(category.tags, channels).sortedByDescending { it.subscribers }
                result.add(ChannelCategoryCollection(category, items))
            }
        }

        return result
    }

    private fun createChannelCategoryCollection(channels: List<Channel>, category: ChannelCategory): ChannelCategoryCollection =
            ChannelCategoryCollection(category, findItemsByTags(category.tags, channels).sortedByDescending { it.subscribers })


    private fun findItemsByTags(categoryTags: Set<String>, items: List<Channel>): List<Channel> {
        val result = mutableListOf<Channel>()

        // Сравниваем по id тегов, не по названиям
        items.forEach { item ->
            if (categoryTags.find { tag -> item.tags.contains(tag) } != null) {
                result.add(item)
            }

        }

        return result
    }

    private fun setJoinedChannels(joinedChannelNames: Set<String>, channels: List<Channel>): Single<List<Channel>> =
            Single.fromCallable {
                channels.map {
                    it.apply {
                        this.joined = joinedChannelNames.find { joinedName -> joinedName.equals(this.id, ignoreCase = true) } != null
                    }
                }
            }
}