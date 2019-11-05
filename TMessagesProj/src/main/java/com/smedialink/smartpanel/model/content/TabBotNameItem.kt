package com.smedialink.smartpanel.model.content

import com.smedialink.smartpanel.model.SmartPanelTabContent

// Название бота
data class TabBotNameItem(
    override val contentType: SmartPanelTabContent.Type,
    val botName: String
) : SmartPanelTabContent
