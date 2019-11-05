package com.smedialink.bots.data.model.bot

import com.smedialink.bots.BotConstants
import com.smedialink.bots.data.model.Response
import com.smedialink.bots.domain.AigramBot
import com.smedialink.bots.domain.ResourceFactory
import com.smedialink.bots.domain.model.BotLanguage
import java.text.SimpleDateFormat
import java.util.*

class HolidaysBot(factory: ResourceFactory,
                  useAssets: Boolean) : AigramBot {

    override val botId: String = BotConstants.HOLIDAYS_ID
    override val language: BotLanguage = BotLanguage.RU

    private val responseSource: List<Response> =
            factory.getHolidaysResponses(botId, useAssets)

    private val currentDateTag: String by lazy {
        // TODO AIGRAM праздники тест
        // для теста праздников прописать тег даты здесь, например "01.05"
        SimpleDateFormat("dd.MM", Locale.getDefault()).format(Date())
//        "01.09"
    }

    override suspend fun getResponse(words: List<String>): Response? =
            responseSource.firstOrNull { it.tag == currentDateTag }?.let { result ->
                Response(botId = botId, tag = result.tag, answers = result.answers)
            }
}
