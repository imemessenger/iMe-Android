package com.smedialink.storage.data.database.dao

import androidx.room.*
import com.smedialink.storage.data.database.model.CountryDbModel
import io.reactivex.Flowable

@Dao
abstract class CountrylDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertOrReplace(entity: CountryDbModel): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertOrReplace(entities: List<CountryDbModel>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insertButIgnore(entities: List<CountryDbModel>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract fun insert(entity: CountryDbModel): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract fun insert(entities: List<CountryDbModel>): List<Long>


    @Query("SELECT * FROM CountryDbModel")
    abstract fun streamCountries(): Flowable<List<CountryDbModel>>

    @Update
    abstract fun update(entity: CountryDbModel)

    @Update
    abstract fun update(entities: List<CountryDbModel>)

    @Query("DELETE FROM CountryDbModel")
    abstract fun deleteAll()

    @Transaction
    open fun upsert(countries: List<CountryDbModel>) {
        val insertResult = insert(countries)

        val updateList = countries.filterIndexed { i, _ ->
            insertResult[i] == -1L
        }

        if (updateList.isNotEmpty()) {
            updateList.forEach {
                update(it)
            }

        }
    }

}
