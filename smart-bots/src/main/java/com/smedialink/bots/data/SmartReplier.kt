package com.smedialink.bots.data

import com.smedialink.bots.BotConstants
import com.smedialink.bots.data.mapper.ResponseMapper
import com.smedialink.bots.data.model.Response
import com.smedialink.bots.data.repository.UserAnswersRepository
import com.smedialink.bots.domain.Replier
import com.smedialink.bots.domain.model.BotLanguage
import com.smedialink.bots.domain.model.SmartBotResponse
import com.smedialink.bots.usecase.AiBotsManager
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.CoroutineContext

class SmartReplier(
        private val manager: AiBotsManager,
        private val mapper: ResponseMapper,
        private val repository: UserAnswersRepository
) : Replier, CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private val job: Job = Job()

    private val currentYearTag: String =
            SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())

    override fun getResponsesFromBots(sentence: String, userId: Int, callback: AiBotsManager.SmartReplierCallback) {
        launch {
            try {

                val result = mutableListOf<SmartBotResponse>()
                val detected = mutableListOf<Response>()

                withContext(Dispatchers.IO) {
                    manager.activeBots.forEach { bot ->
                        bot.getResponse(splitLemmas(bot.language, sentence))?.let { response ->
                            detected.add(response)
                        }
                    }

                    result.addAll(buildBotsList(detected, userId))
                }

                callback.onSuccess(result)

            } catch (e: Exception) {
                callback.onError(e)
            }
        }
    }

    /**
     * Построение списка ответов, основные правила:
     * - если сработал праздничный, то он на первой позиции
     * - если сработал ассистент, то он на первой позиции, либо после праздничного
     * - для праздничного выполняется проверка был ли пользователь уже поздравлен,
     *   когда кого-то поздравляем, то за его userId закрепляется тег праздника
     *
     *   @param responses Текущий список ответов
     *   @param userId id пользователя telegram
     *
     *   @return Форматированный список ответов
     */
    private fun buildBotsList(responses: List<Response>, userId: Int): List<SmartBotResponse> {
        val assistant = responses.firstOrNull { it.botId == BotConstants.ASSISTANT_ID }
        val holidays = responses.firstOrNull { it.botId == BotConstants.HOLIDAYS_ID }
        val others = responses.filterNot { it.botId == BotConstants.ASSISTANT_ID || it.botId == BotConstants.HOLIDAYS_ID }

        val recentAnswersList = mutableListOf<Pair<Int, String>>()

        (listOf(assistant) + others).forEach { response ->
            if (response != null) {
                val maxPosition = repository.getPositionWithMaxCounter(response.botId, response.tag)
                if (maxPosition != -1) {
                    val maxCounter = repository.getCounterForPosition(response.botId, response.tag, maxPosition)
                    recentAnswersList.add(Pair(maxCounter, response.answers[maxPosition]))
                }
            }
        }

        var recent: Response? = null

        if (recentAnswersList.isNotEmpty()) {

            val sortedList = recentAnswersList.sortedByDescending { it.first }
            val sortedAnswers = sortedList.map { it.second }

            recent = Response(
                    botId = BotConstants.FREQUENT_ANSWERS_ID,
                    tag = "",
                    answers = sortedAnswers
            )
        }

        val fullTag = "${holidays?.tag}.$currentYearTag"
        val congratulated = repository.getTagsForUser(userId)

        return if (userId != 0 && congratulated.contains(fullTag)) {
            (listOf(recent, assistant) + others).filterNotNull()
        } else {
            (listOf(recent, holidays, assistant) + others).filterNotNull()
        }
                .map { mapper.map(it) }
    }

    override fun cancel() {
        job.cancelChildren()
    }

    // "I'm stupid" ->  ['st' , tu'', 'up', 'pi', 'id' ,  'i m', 'm stupid', 'i' , 'm', 'stupid']
    private fun splitLemmas(botLanguage: BotLanguage, string: String): List<String> = when (botLanguage) {
        BotLanguage.RU -> {
            string.split(Regex("[@#$%^&*+-/|\\_><.,?!:;)(= ]"))
                    .asSequence()
                    .filter { it.isNotEmpty() }
                    .map { it.toLowerCase() }
                    .toList()
        }
        BotLanguage.EN -> {
            val result: MutableList<String> = mutableListOf()
            val words = string.split(Regex("[-_.,?!:;')( ]"))
                    .asSequence()
                    .filter { it.isNotEmpty() }
                    .map { it.toLowerCase() }
                    .map { it.trim() }
            words.forEach { word ->
                if (word.length != 2) {
                    result.add(word)
                }
                if (word.length > 1) {
                    val charPairs = word.toList().zipWithNext()
                    charPairs.forEach { charPair ->
                        result.add("${charPair.first}${charPair.second}")
                    }
                }
            }
            words.zipWithNext().forEach { wordsPair ->
                result.add("${wordsPair.first} ${wordsPair.second}")
            }
            result
        }

    }

}
