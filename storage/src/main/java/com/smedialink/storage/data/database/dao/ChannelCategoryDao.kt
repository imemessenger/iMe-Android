package com.smedialink.storage.data.database.dao

import androidx.room.*
import com.smedialink.storage.data.database.model.ChannelCategoryDbModel
import io.reactivex.Flowable
import io.reactivex.Single

@Dao
abstract class ChannelCategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertOrReplace(entity: ChannelCategoryDbModel): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertOrReplace(entities: List<ChannelCategoryDbModel>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insertButIgnore(entities: List<ChannelCategoryDbModel>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract fun insert(entity: ChannelCategoryDbModel): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract fun insert(entities: List<ChannelCategoryDbModel>): List<Long>

    @Query("SELECT * FROM ChannelCategoryDbModel")
    abstract fun getChannelCategory(): Single<ChannelCategoryDbModel>

    @Query("SELECT * FROM ChannelCategoryDbModel")
    abstract fun streamChannelCategories(): Flowable<List<ChannelCategoryDbModel>>

    @Query("SELECT * FROM ChannelCategoryDbModel WHERE id=:categoryId")
    abstract fun streamChannelCategory(categoryId: String): Flowable<ChannelCategoryDbModel>

    @Query("SELECT * FROM ChannelCategoryDbModel")
    abstract fun getChannelsCategories(): Single<List<ChannelCategoryDbModel>>

    @Query("DELETE FROM ChannelCategoryDbModel WHERE id not in(:ignoredIds)")
    abstract fun deleteAllByIgnoredIds(ignoredIds: Array<String>): Int

    @Update
    abstract fun update(entity: ChannelCategoryDbModel)

    @Update
    abstract fun update(entities: List<ChannelCategoryDbModel>)

    @Transaction
    open fun upsert(categories: ChannelCategoryDbModel) {
        val id = insert(categories)
        if (id == -1L) {
            update(categories)
        }
    }

    @Transaction
    open fun upsert(categories: List<ChannelCategoryDbModel>) {
        deleteAllByIgnoredIds(categories.map { it.id }.toTypedArray())
        val insertResult = insert(categories)

        val updateList = categories.filterIndexed { i, _ ->
            insertResult[i] == -1L
        }

        if (updateList.isNotEmpty()) {
            update(updateList)
        }
    }

}
