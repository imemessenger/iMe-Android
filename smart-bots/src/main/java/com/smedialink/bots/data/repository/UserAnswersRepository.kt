package com.smedialink.bots.data.repository

import android.content.Context
import com.smedialink.bots.BotConstants
import com.smedialink.bots.data.database.BotsDatabase
import com.smedialink.bots.data.database.HolidaysDao
import com.smedialink.bots.data.database.RecentDao
import java.text.SimpleDateFormat
import java.util.*

class UserAnswersRepository(context: Context) {

    private val yearTag: String by lazy {
        SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
    }

    private val botsDatabase: BotsDatabase = BotsDatabase.getInstance(context.applicationContext)
    private val holidaysDao: HolidaysDao = botsDatabase.holidaysDao()
    private val recentDao: RecentDao = botsDatabase.recentDao()

    fun getTagsForUser(userId: Int): Set<String> {
        return holidaysDao.getTagsForUser(userId)
    }

    fun saveHolidayGreeting(userId: Int, tag: String) {
        val newTag = "$tag.$yearTag"
        holidaysDao.saveForUser(userId, newTag)
    }

    fun increaseResponseCounter(botId: String, tag: String, position: Int) {
        if (botId != BotConstants.FREQUENT_ANSWERS_ID) {
            recentDao.increaseCounter(botId, tag, position)
        }
    }

    fun getPositionWithMaxCounter(botId: String, tag: String): Int {
        return recentDao.getSortedPositions(botId, tag)?.firstOrNull() ?: -1
    }

    fun getCounterForPosition(botId: String, tag: String, position: Int): Int {
        return recentDao.getCounter(botId, tag, position) ?: 0
    }
}
