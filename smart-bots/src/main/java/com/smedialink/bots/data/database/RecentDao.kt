package com.smedialink.bots.data.database

import androidx.room.*

@Dao
abstract class RecentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertOrReplace(entity: RecentDbModel): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertOrReplace(entities: List<RecentDbModel>): List<Long>

    @Query("SELECT counter FROM RecentDbModel WHERE botId LIKE :botId AND tag LIKE :tag AND position LIKE :position")
    abstract fun getCounter(botId: String, tag: String, position: Int): Int?

    @Query("SELECT position FROM RecentDbModel WHERE botId LIKE :botId AND tag LIKE :tag ORDER BY counter DESC")
    abstract fun getSortedPositions(botId: String, tag: String): List<Int>?

    @Transaction
    open fun increaseCounter(botId: String, tag: String, position: Int) {

        val current = getCounter(botId, tag, position) ?: 0

        val newRecent = RecentDbModel(
            botId = botId,
            tag = tag,
            position = position,
            counter = current + 1
        )

        insertOrReplace(newRecent)
    }
}
