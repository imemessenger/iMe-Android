package com.smedialink.bots.domain

import com.smedialink.bots.data.model.Response
import com.smedialink.bots.domain.model.BotLanguage

interface AigramBot {

    // Идентификатор бота
    val botId: String
    val language: BotLanguage

    /**
     * Получение ответов от ботов
     * @param words Входящее предложение в виде токенизированного списка слов
     */
    suspend fun getResponse(words: List<String>): Response?
}
