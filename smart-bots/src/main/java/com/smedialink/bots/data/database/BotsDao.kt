package com.smedialink.bots.data.database

import androidx.room.*
import com.smedialink.bots.data.model.BotStatus
import com.smedialink.bots.data.network.BotVoteInfo
import com.smedialink.bots.data.network.Response
import com.smedialink.bots.data.network.SmartBotsApi
import io.reactivex.Flowable
import io.reactivex.Single

@Dao
abstract class BotsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertOrReplace(entity: BotsDbModel): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertOrReplace(entities: List<BotsDbModel>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insertButIgnore(entities: List<BotsDbModel>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract fun insert(entity: BotsDbModel): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract fun insert(entities: List<BotsDbModel>): List<Long>


    @Query("DELETE from BotsDbModel WHERE id NOT IN (:ignoredIds)")
    abstract fun deleteByIgnored(ignoredIds: Array<String>)

    @Query("DELETE from BotsDbModel")
    abstract fun deleteAll()

    @Query("UPDATE  BotsDbModel SET hash =:hash , botUpdated =:botUpdated, useAssets=:useAssets WHERE id=:botId")
    abstract fun updateBot(botId: String, hash: String, botUpdated: Int,useAssets: Int)

    @Update
    abstract fun update(entity: BotsDbModel)

    @Update
    abstract fun update(entities: List<BotsDbModel>)

    @Transaction
    open fun upsert(entity: BotsDbModel) {
        val id = insert(entity)
        if (id == -1L) {
            update(entity)
        }
    }

    @Transaction
    open fun upsert(entities: List<BotsDbModel>) {
        val insertResult = insert(entities)

        val updateList = entities.filterIndexed { i, _ ->
            insertResult[i] == -1L
        }

        if (!updateList.isEmpty()) {
            update(updateList)
        }
    }

    @Delete
    abstract fun delete(entity: BotsDbModel)

    @Delete
    abstract fun delete(entities: List<BotsDbModel>)

    @Query("SELECT * FROM BotsDbModel")
    abstract fun streamAll(): Flowable<List<BotsDbModel>>

    @Query("SELECT * FROM BotsDbModel WHERE id = :botId")
    abstract fun streamBot(botId: String): Flowable<BotsDbModel>

    @Query("SELECT * FROM BotsDbModel WHERE id = :botId")
    abstract fun getById(botId: String): BotsDbModel?

    @Query("UPDATE BotsDbModel set status=:newBotStatus WHERE status =:botStatus")
    abstract fun resetDownloads(newBotStatus: BotStatus, botStatus: BotStatus = BotStatus.DOWNLOADING)

    @Query("SELECT * FROM BotsDbModel WHERE sku = :sku")
    abstract fun getBySku(sku: String): BotsDbModel?

    @Query("SELECT * FROM BotsDbModel WHERE sku = :sku")
    abstract fun getBotBySku(sku: String): Single<BotsDbModel>

    @Query("SELECT * FROM BotsDbModel WHERE id = :botId")
    abstract fun getFlowableById(botId: String): Flowable<BotsDbModel>

    @Query("SELECT * FROM BotsDbModel WHERE type = :status")
    abstract fun getByStatus(status: BotStatus): List<BotsDbModel>

    @Query("SELECT * FROM BotsDbModel ")
    abstract fun getAll(): List<BotsDbModel>

    @Query("UPDATE BotsDbModel SET ownRating = :value WHERE id = :botId")
    abstract fun saveOwnRating(botId: String, value: Int): Long

    @Query("SELECT ownRating FROM BotsDbModel WHERE type = :botId")
    abstract fun getOwnRating(botId: String): Int

    @Query("UPDATE BotsDbModel SET installLogged = 1 WHERE id = :botId")
    abstract fun saveInstallEvent(botId: String): Long

    @Query("UPDATE BotsDbModel SET useAssets = 0 WHERE id = :botId")
    abstract fun disableAssets(botId: String): Long

    @Query("SELECT installLogged FROM BotsDbModel WHERE id = :botId")
    abstract fun isInstallEventLogged(botId: String): Int

    @Transaction
    open fun saveRatings(response: Response<List<BotVoteInfo>>) {
        if (response.status.toLowerCase() == SmartBotsApi.STATUS_OK) {
            response.payload.forEach { bot ->
                saveOwnRating(bot.name, bot.rating.toInt())
            }
        }
    }

}
