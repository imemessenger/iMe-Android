package com.smedialink.storage.data.database.dao

import androidx.room.*
import com.smedialink.storage.data.database.model.ChannelDbModel
import io.reactivex.Flowable
import io.reactivex.Single

@Dao
abstract class ChannelDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertOrReplace(entity: ChannelDbModel): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertOrReplace(entities: List<ChannelDbModel>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insertButIgnore(entities: List<ChannelDbModel>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract fun insert(entity: ChannelDbModel): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract fun insert(entities: List<ChannelDbModel>): List<Long>

    @Query("SELECT * FROM ChannelDbModel WHERE id = :channelId")
    abstract fun streamChannel(channelId: String): Flowable<ChannelDbModel>

    @Query("SELECT * FROM ChannelDbModel WHERE id = :channelId")
    abstract fun getChannel(channelId: String): ChannelDbModel?

    @Query("SELECT * FROM ChannelDbModel")
    abstract fun getChannels(): Single<List<ChannelDbModel>>

    @Query("SELECT * FROM ChannelDbModel")
    abstract fun streamChannels(): Flowable<List<ChannelDbModel>>

    @Query("UPDATE  ChannelDbModel set joined=1 WHERE id=:channelId")
    abstract fun joinChannel(channelId: Long)

    @Query("UPDATE  ChannelDbModel set joined=0 WHERE id=:channelId")
    abstract fun leaveChannel(channelId: Long)

    @Query("UPDATE ChannelDbModel SET ownRating = :value WHERE id = :channelId")
    abstract fun saveOwnRating(channelId: String, value: Int): Long

    @Query("SELECT ownRating FROM ChannelDbModel WHERE id = :channelId")
    abstract fun getOwnRating(channelId: String): Int

    @Query("DELETE FROM ChannelDbModel WHERE id not in(:ignoredIds)")
    abstract fun deleteAllByIgnoredIds(ignoredIds: Array<String>): Int

    @Update
    abstract fun update(entity: ChannelDbModel)

    @Update
    abstract fun update(entities: List<ChannelDbModel>)

    @Transaction
    open fun upsert(channel: ChannelDbModel) {
        val id = insert(channel)
        if (id == -1L) {
            update(channel)
        }
    }

    @Transaction
    open fun upsert(channels: List<ChannelDbModel>) {
        deleteAllByIgnoredIds(channels.map { it.id }.toTypedArray())
        val insertResult = insert(channels)

        val updateList = channels.filterIndexed { i, _ ->
            insertResult[i] == -1L
        }

        if (updateList.isNotEmpty()) {
            updateList.forEach {
                val cachedChannel = getChannel(it.id)
                it.ownRating = cachedChannel?.ownRating ?: 0
                update(it)
            }

        }
    }

}
