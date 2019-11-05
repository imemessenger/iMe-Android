package com.smedialink.smartpanel.model.content

import com.smedialink.smartpanel.model.SmartPanelTabContent
import org.telegram.tgnet.TLRPC

// Ответ в виде гифки
data class TabBotMediaAnswerItem(
    override val contentType: SmartPanelTabContent.Type,
    val media: TLRPC.BotInlineResult
) : SmartPanelTabContent {

    companion object {
        fun map(from: List<TLRPC.BotInlineResult>): List<SmartPanelTabContent> =
            from.map { TabBotMediaAnswerItem(SmartPanelTabContent.Type.BOT_MEDIA_ANSWER, it) }
    }
}
