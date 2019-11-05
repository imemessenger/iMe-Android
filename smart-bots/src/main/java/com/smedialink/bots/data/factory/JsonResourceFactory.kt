package com.smedialink.bots.data.factory

import android.content.res.AssetManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.smedialink.bots.data.model.Response
import com.smedialink.bots.data.response.ClassifierResponse
import com.smedialink.bots.data.response.HolidaysResponse
import com.smedialink.bots.domain.ResourceFactory
import com.smedialink.bots.extensions.asString
import java.io.File

class JsonResourceFactory(
        private val assetManager: AssetManager,
        private val installPath: File
) : ResourceFactory {

    private val gson: Gson = Gson()

    override fun getHolidaysResponses(botId: String, fromAssets: Boolean): List<Response> {
        val path =
                if (fromAssets)  "bots/$botId/response_Holidays.json"
                else "$installPath/$botId/main_cl/${botId}_data.json"

        val jsonString = if (fromAssets) assetManager.open(path).asString() else File(path).readText()
        val responsesType = object : TypeToken<List<HolidaysResponse>>() {}.type
        val responses: List<HolidaysResponse> = gson.fromJson(jsonString, responsesType)

        return responses.map { item ->
            Response(
                    botId = botId,
                    tag = item.tag,
                    answers = item.response
            )
        }
    }

    override fun getBotWordsBag(botId: String, fromAssets: Boolean): Map<Int, String> {
        val path =
                if (fromAssets) "bots/$botId/main_cl/${botId}_data.json"
                else "$installPath/$botId/main_cl/${botId}_data.json"
        val jsonString = if (fromAssets) assetManager.open(path).asString() else File(path).readText()
        val responsesType = object : TypeToken<ClassifierResponse>() {}.type
        val classifierResponse: ClassifierResponse = gson.fromJson(jsonString, responsesType)
        return classifierResponse.words.mapIndexed { index, str -> index to str }.toMap()
    }

    override fun getBotResponsesList(botId: String, fromAssets: Boolean): List<Response> {
        val path =
                if (fromAssets) "bots/$botId/main_cl/${botId}_data.json"
                else "$installPath/$botId/main_cl/${botId}_data.json"

        val jsonString = if (fromAssets) assetManager.open(path).asString() else File(path).readText()
        val responsesType = object : TypeToken<ClassifierResponse>() {}.type
        val classifierResponse: ClassifierResponse = gson.fromJson(jsonString, responsesType)

        return classifierResponse.classes.map { item ->
            Response(
                    botId = botId,
                    tag = item.tag,
                    gif = item.gif ?: "",
                    answers = item.response
            )
        }
    }

    override fun getBotMlModelPath(botId: String, fromAssets: Boolean): String =
            if (fromAssets) "bots/$botId/main_cl/${botId}_model.tflite"
            else "$installPath/$botId/main_cl/${botId}_model.tflite"

    override fun getIntentValidatorMlPath(botId: String, classifiedIndex: Int, fromAssets: Boolean): String =
            if (fromAssets) "bots/$botId/intents_cl/intent_$classifiedIndex/intent_${classifiedIndex}_model.tflite"
            else "$installPath/$botId/intents_cl/intent_$classifiedIndex/intent_${classifiedIndex}_model.tflite"

    override fun getIntentValidatorWordsBag(botId: String, classifiedIndex: Int, fromAssets: Boolean): Map<Int, String> {
        val path =
                if (fromAssets) "bots/$botId/intents_cl/intent_$classifiedIndex/intent_${classifiedIndex}_data.json"
                else "$installPath/$botId/intents_cl/intent_$classifiedIndex/intent_${classifiedIndex}_data.json"
        val jsonString = if (fromAssets) assetManager.open(path).asString() else File(path).readText()
        val responsesType = object : TypeToken<ClassifierResponse>() {}.type
        val classifierResponse: ClassifierResponse = gson.fromJson(jsonString, responsesType)
        return classifierResponse.words.mapIndexed { index, str -> index to str }.toMap()
    }

    override fun getIntentValidatorResponses(botId: String, classifiedIndex: Int, fromAssets: Boolean): Map<Int, String> {
        val path =
                if (fromAssets) "bots/$botId/intents_cl/intent_$classifiedIndex/intent_${classifiedIndex}_data.json"
                else "$installPath/$botId/intents_cl/intent_$classifiedIndex/intent_${classifiedIndex}_data.json"
        val jsonString = if (fromAssets) assetManager.open(path).asString() else File(path).readText()
        val responsesType = object : TypeToken<ClassifierResponse>() {}.type
        val classifierResponse: ClassifierResponse = gson.fromJson(jsonString, responsesType)
        return classifierResponse.classes.mapIndexed { index, clazz -> index to clazz.tag }.toMap()
    }
}
