package com.smedialink.bots.data.database


import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.smedialink.bots.data.database.converter.Converter
import com.smedialink.bots.data.repository.BotsRepository
import java.util.concurrent.Executors

@Database(
        entities = [
            BotsCategoryDbModel::class,
            BotsDbModel::class,
            TagDbModel::class,
            HolidaysDbModel::class,
            RecentDbModel::class
        ],
        version = 12,
        exportSchema = false
)
@TypeConverters(Converter::class)
abstract class BotsDatabase : RoomDatabase() {

    abstract fun botsDao(): BotsDao
    abstract fun categoryDao(): BotsCategoryDao
    abstract fun tagsDao(): BotsTagDao
    abstract fun holidaysDao(): HolidaysDao
    abstract fun recentDao(): RecentDao

    companion object {
        @Volatile
        private var INSTANCE: BotsDatabase? = null

        fun getInstance(context: Context): BotsDatabase {
            if (INSTANCE == null) {
                synchronized(BotsDatabase::class) {
                    INSTANCE = Room.databaseBuilder(context.applicationContext,
                            BotsDatabase::class.java, DB_NAME)
                            .addCallback(object : Callback() {
                                override fun onOpen(db: SupportSQLiteDatabase) {
                                    super.onCreate(db)
                                    Executors.newSingleThreadScheduledExecutor().execute {
                                        Log.d("BotsDatabase","onopen")
                                        getInstance(context).botsDao().insertButIgnore(BotsRepository.initialBotsList())
                                    }
                                }
                            })
                            .fallbackToDestructiveMigration()
                            .addMigrations(Migrations.FROM_6_TO_7)
                            .build()
                }
            }
            return INSTANCE!!
        }

        private const val DB_NAME = "bots.db"
    }
}
