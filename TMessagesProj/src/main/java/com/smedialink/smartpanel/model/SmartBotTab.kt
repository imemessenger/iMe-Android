package com.smedialink.smartpanel.model

// Класс, определяющего контент отдельной вкладки панели
class SmartBotTab(
    val iconRes: Int,
    val iconUrl: String,
    val botId: String,
    val botName: String,
    val gif: String,
    val answers: List<SmartPanelTabContent>
)
