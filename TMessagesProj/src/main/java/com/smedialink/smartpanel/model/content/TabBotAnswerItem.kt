package com.smedialink.smartpanel.model.content

import com.smedialink.smartpanel.model.SmartPanelTabContent

// Класс, определяющий ответ бота
data class TabBotAnswerItem(
    override val contentType: SmartPanelTabContent.Type,
    val botId: String,
    val phrase: String,
    val tag: String,
    val link: String = ""
) : SmartPanelTabContent

