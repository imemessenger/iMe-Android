package com.smedialink.bots.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.reactivex.Flowable

@Dao
abstract class BotsCategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertOrReplace(entity: BotsCategoryDbModel): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertOrReplace(entities: List<BotsCategoryDbModel>): List<Long>

    @Query("SELECT * FROM BotsCategoryDbModel WHERE id = :categoryId")
    abstract fun getById(categoryId: String): BotsCategoryDbModel?

    @Query("SELECT * FROM BotsCategoryDbModel")
    abstract fun getAll(): Flowable<List<BotsCategoryDbModel>>
}
