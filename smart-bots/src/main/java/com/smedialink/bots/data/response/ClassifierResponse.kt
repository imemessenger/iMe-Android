package com.smedialink.bots.data.response

data class ClassifierResponse(val classes: List<ClassifierItem>, val words: List<String>) {

    data class ClassifierItem(val tag: String, val gif: String?, val response: List<String>)
}
