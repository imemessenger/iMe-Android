package com.smedialink.bots.data.mapper

import android.content.Context
import com.smedialink.bots.BotConstants
import com.smedialink.bots.R
import com.smedialink.bots.data.model.Response
import com.smedialink.bots.data.repository.BotsRepository
import com.smedialink.bots.domain.model.SmartBotResponse

class ResponseMapper(private val repository: BotsRepository, context: Context) {

    // TODO AIGRAM праздники
    // Отдельные аватарки для праздников, возможно перенести на бэк?
    private val holidays: Map<String, Int> = mapOf(
        "23.02" to R.drawable.bot_avatar_23_02,
        "08.03" to R.drawable.bot_avatar_08_03,
        "01.04" to R.drawable.bot_avatar_01_04,
        "12.04" to R.drawable.bot_avatar_12_04,
        "28.04" to R.drawable.bot_avatar_28_04,
        "01.05" to R.drawable.bot_avatar_01_05,
        "09.05" to R.drawable.bot_avatar_09_05
    )

    private val frequentAnswersTitle =
        context.getString(R.string.bot_title_recent)

    fun map(from: Response): SmartBotResponse {
        val storedBot = repository.getBotById(from.botId)

        val mappedBotId: String
        val mappedTitle: String
        val mappedLocalAvatar: Int

        if (from.botId == BotConstants.FREQUENT_ANSWERS_ID) {
            mappedBotId = from.botId
            mappedTitle = frequentAnswersTitle
            mappedLocalAvatar = R.drawable.ic_bots_recent
        } else {
            mappedBotId = storedBot?.id ?: ""
            mappedTitle = storedBot?.title ?: ""
            mappedLocalAvatar = holidays[from.tag] ?: 0
        }

        return SmartBotResponse(
            id = mappedBotId,
            name = mappedTitle,
            localAvatar = mappedLocalAvatar,
            remoteAvatar = storedBot?.avatarOriginal ?: "",
            tag = from.tag,
            gif = from.gif,
            link = from.link,
            answers = from.answers
        )
    }
}
