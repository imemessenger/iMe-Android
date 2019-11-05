package com.smedialink.smartpanel.model

// Виды контента, которые могут отображаться во вкладке бота
interface SmartPanelTabContent {
    val contentType: Type

    enum class Type(val value: Int) {
        NORMAL_BOT_ANSWER(1),
        ADVERT_BOT_ANSWER(2),   // пока не используется
        NORMAL_BOT_LABEL(3),
        BOT_MEDIA_ANSWER(4);
    }
}
