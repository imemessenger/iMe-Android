package com.smedialink.bots.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
abstract class BotsTagDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertOrReplace(entity: TagDbModel): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertOrReplace(entities: List<TagDbModel>): List<Long>

    @Query("SELECT * FROM TagDbModel WHERE id = :tagId")
    abstract fun getById(tagId: String): TagDbModel?

    @Query("SELECT * FROM TagDbModel")
    abstract fun getAll(): List<TagDbModel?>

    @Query("SELECT * FROM TagDbModel WHERE id IN (:ids)")
    abstract fun getAll(ids:Array<String>): List<TagDbModel?>
}
