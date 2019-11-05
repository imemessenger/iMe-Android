package com.smedialink.storage.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {
    val FROM_2_TO_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE DialogFoldersDbModel_copy (id INTEGER NOT NULL, userId INTEGER NOT NULL, name TEXT NOT NULL, backgroundId INTEGER NOT NULL, pinned INTEGER NOT NULL, children TEXT NOT NULL, PRIMARY KEY(id))")
            database.execSQL("INSERT INTO DialogFoldersDbModel_copy (userId, name, backgroundId, pinned, children) SELECT userId, name, backgroundId, pinned, children FROM DialogFoldersDbModel")
            database.execSQL("DROP TABLE DialogFoldersDbModel")
            database.execSQL("ALTER TABLE DialogFoldersDbModel_copy RENAME TO DialogFoldersDbModel")
        }
    }
}
