package com.smedialink.bots.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.smedialink.bots.BotConstants
import com.smedialink.bots.data.model.BotStatus
import com.smedialink.bots.data.model.BotType

object Migrations {
    val FROM_6_TO_7 = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(DROP_OLD_BOTS_TABLE_QUERY)
            database.execSQL(CREATE_BOTS_TABLE_QUERY)
            database.execSQL(DROP_OLD_RECENT_TABLE_QUERY)
            database.execSQL(CREATE_RECENT_TABLE_QUERY)
            database.execSQL(CREATE_CATEGORIES_TABLE_QUERY)
            database.execSQL(CREATE_TAGS_TABLE_QUERY)

            BotConstants.predefinedBots.forEach { name ->
                database.execSQL(getBotInsertionQuery(name))
            }
        }
    }

    private const val BOTS_TABLE_NAME = "BotsDbModel"

    private const val CREATE_BOTS_TABLE_QUERY = """
        CREATE TABLE $BOTS_TABLE_NAME (
        id TEXT NOT NULL,
        sku TEXT NOT NULL,
        avatarOriginal TEXT NOT NULL,
        avatarRounded TEXT NOT NULL,
        title TEXT NOT NULL,
        description TEXT NOT NULL,
        installs INTEGER NOT NULL,
        priority INTEGER NOT NULL,
        reviews INTEGER NOT NULL,
        rating REAL NOT NULL,
        ownRating INTEGER NOT NULL,
        installLogged INTEGER NOT NULL,
        useAssets INTEGER NOT NULL,
        tags TEXT NOT NULL,
        file TEXT NOT NULL,
        hash TEXT NOT NULL,
        phrases INTEGER NOT NULL,
        themes INTEGER NOT NULL,
        created INTEGER NOT NULL,
        updated INTEGER NOT NULL,
        price TEXT NOT NULL,
        type TEXT NOT NULL,
        status TEXT NOT NULL,
        PRIMARY KEY(id))
        """


    private const val RECENT_TABLE_NAME = "RecentDbModel"

    private const val CREATE_RECENT_TABLE_QUERY = """
        CREATE TABLE $RECENT_TABLE_NAME (
        botId TEXT NOT NULL,
        tag TEXT NOT NULL,
        position INTEGER NOT NULL,
        counter INTEGER NOT NULL,
        PRIMARY KEY(botId, tag, position))
        """

    private const val DROP_OLD_BOTS_TABLE_QUERY = "DROP TABLE ShopDbModel"
    private const val DROP_OLD_RECENT_TABLE_QUERY = "DROP TABLE RecentDbModel"

    private const val CREATE_CATEGORIES_TABLE_QUERY = """
        CREATE TABLE BotsCategoryDbModel (
        id TEXT NOT NULL,
        title TEXT NOT NULL,
        priority INTEGER NOT NULL,
        tags TEXT NOT NULL,
        PRIMARY KEY(id))
        """

    private const val CREATE_TAGS_TABLE_QUERY = """
        CREATE TABLE BotsTagDbModel (
        id TEXT NOT NULL,
        title TEXT NOT NULL,
        hidden INTEGER NOT NULL,
        PRIMARY KEY(id))
        """

    private fun getBotInsertionQuery(name: String): String =
        """
            INSERT INTO $BOTS_TABLE_NAME (
                id, sku, avatarOriginal, avatarRounded, title, description, installs, priority, reviews, rating, ownRating,
                installLogged, useAssets, tags, file, hash, phrases, themes, created, updated, price, type, status
            )
            VALUES (
                '$name', '', '', '', '', '', '0', '0', '0', '0.0', '0', '1', '1', '', '', '', '0', '0', '0', '0', '',
                '${BotType.resolveByName(name).name}', '${BotStatus.ENABLED.name}'
            );
        """.trimIndent()
}
