package com.smedialink.storage.data.database


import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.smedialink.storage.data.database.converter.StorageConverter
import com.smedialink.storage.data.database.dao.ChannelCategoryDao
import com.smedialink.storage.data.database.dao.ChannelDao
import com.smedialink.storage.data.database.dao.CountrylDao
import com.smedialink.storage.data.database.dao.DialogFoldersDao
import com.smedialink.storage.data.database.model.ChannelCategoryDbModel
import com.smedialink.storage.data.database.model.ChannelDbModel
import com.smedialink.storage.data.database.model.CountryDbModel
import com.smedialink.storage.data.database.model.DialogFoldersDbModel

@Database(
        entities = [
            DialogFoldersDbModel::class,
            ChannelDbModel::class,
            ChannelCategoryDbModel::class,
            CountryDbModel::class
        ],
        version = 6
)
@TypeConverters(StorageConverter::class)
abstract class StorageDatabase : RoomDatabase() {

    abstract fun dialogFoldersDao(): DialogFoldersDao

    abstract fun channelDao(): ChannelDao

    abstract fun countryDao(): CountrylDao

    abstract fun channelCategoryDao(): ChannelCategoryDao


    companion object {
        @Volatile
        private var INSTANCE: StorageDatabase? = null

        open fun getInstance(context: Context): StorageDatabase {
            if (INSTANCE == null) {
                synchronized(StorageDatabase::class) {
                    INSTANCE =
                            Room.databaseBuilder(context.applicationContext, StorageDatabase::class.java, StorageDatabase.DB_NAME)
                                    .addMigrations(Migrations.FROM_2_TO_3)
                                    .fallbackToDestructiveMigration()
                                    .build()
                }
            }
            return INSTANCE!!
        }

        private const val DB_NAME = "ime-storage.db"
    }
}
