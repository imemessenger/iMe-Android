package com.smedialink.bots.domain

import com.smedialink.bots.usecase.AiBotsManager

interface Replier {

    /**
     * Основной метод получения ответов от ботов
     *
     * @param sentence Исходное предложение в виде списка слов
     * @param userId Идентификатор пользователя Telegram
     * @param callback Коллбек для получения результатов
     */
    fun getResponsesFromBots(sentence: String, userId: Int, callback: AiBotsManager.SmartReplierCallback)

    /**
     * Метод для отмены всех запущенных внутри ботов корутин
     */
    fun cancel()
}
