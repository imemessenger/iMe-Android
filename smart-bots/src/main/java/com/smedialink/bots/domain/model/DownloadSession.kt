package com.smedialink.bots.domain.model

import java.io.File

data class DownloadSession(val file: File,
                           val botId: String)