package com.smedialink.bots.data.database

import androidx.room.*


@Dao
abstract class HolidaysDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertOrReplace(entity: HolidaysDbModel): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertOrReplace(entities: List<HolidaysDbModel>): List<Long>

    @Query("SELECT tags FROM HolidaysDbModel WHERE userId LIKE :userId")
    abstract fun getByUserId(userId: Int): String?

    @Transaction
    open fun saveForUser(userId: Int, fullTag: String) {
        val tags = getTagsForUser(userId).toMutableSet()
        tags.add(fullTag)
        val newData = HolidaysDbModel(userId, tags.joinToString(DELIMITER))
        insertOrReplace(newData)
    }

    @Transaction
    open fun getTagsForUser(userId: Int): Set<String> =
        getByUserId(userId)
            ?.split(DELIMITER)
            ?.filterNot { it == DELIMITER }
            ?.toMutableSet()
            ?: emptySet()

    companion object {
        private const val DELIMITER = " "
    }
}
