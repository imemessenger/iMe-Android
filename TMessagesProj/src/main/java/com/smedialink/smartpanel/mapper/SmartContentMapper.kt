package com.smedialink.smartpanel.mapper

import com.smedialink.bots.BotConstants
import com.smedialink.bots.domain.model.SmartBotResponse
import com.smedialink.smartpanel.model.SmartBotTab
import com.smedialink.smartpanel.model.SmartPanelTabContent
import com.smedialink.smartpanel.model.content.TabBotAnswerItem
import com.smedialink.smartpanel.model.content.TabBotNameItem

class SmartContentMapper {

    fun mapToTabs(responseList: List<SmartBotResponse>?, recent: Boolean): List<SmartBotTab> {

        val tabs = mutableListOf<SmartBotTab>()

        responseList?.forEach { response ->
            val tabAnswers = mutableListOf<SmartPanelTabContent>()

            // Имя бота вверху таба
            tabAnswers.add(TabBotNameItem(SmartPanelTabContent.Type.NORMAL_BOT_LABEL, response.name))
            // Ответы
            tabAnswers.addAll(responseToContentItems(response))

            if (recent || (!recent && response.id != BotConstants.FREQUENT_ANSWERS_ID)) {
                tabs.add(
                        SmartBotTab(
                                iconRes = response.localAvatar,
                                iconUrl = response.remoteAvatar,
                                botId = response.id,
                                botName = response.name,
                                gif = response.gif,
                                answers = tabAnswers
                        )
                )
            }


        }

        return tabs
    }

    private fun responseToContentItems(response: SmartBotResponse): List<SmartPanelTabContent> =
            response.answers.map { text ->
                TabBotAnswerItem(
                        contentType = SmartPanelTabContent.Type.NORMAL_BOT_ANSWER,
                        botId = response.id,
                        phrase = text,
                        tag = response.tag,
                        link = response.link
                )
            }
}
