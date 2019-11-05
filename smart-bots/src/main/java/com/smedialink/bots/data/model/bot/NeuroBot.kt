package com.smedialink.bots.data.model.bot

import android.util.Log
import com.google.firebase.ml.common.modeldownload.FirebaseLocalModel
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager
import com.google.firebase.ml.custom.*
import com.smedialink.bots.data.model.Response
import com.smedialink.bots.data.response.ValidationResponse
import com.smedialink.bots.domain.AigramBot
import com.smedialink.bots.domain.ResourceFactory
import com.smedialink.bots.domain.model.BotLanguage
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Основной класс ботов
 */
class NeuroBot(
        override val botId: String,
        private val factory: ResourceFactory,
        private val useAssets: Boolean,
        override val language: BotLanguage
) : AigramBot {

    // Интерпретатор ML модели
    private val classifier: FirebaseModelInterpreter? by lazy {
        val options = FirebaseModelOptions.Builder()
                .setLocalModelName(botId)
                .build()

        FirebaseModelInterpreter.getInstance(options)
    }

    // Список известных классификатору слов (мешок слов)
    private val classifierWords: Map<Int, String> =
            factory.getBotWordsBag(botId, useAssets)

    // Список известных классификатору ответов, загружается из json
    private val classifierResponses: List<Response> =
            factory.getBotResponsesList(botId, useAssets)

    init {
        // Регистрация локальной ML модели и загрузка модели из assets
        val model = if (useAssets) {
            FirebaseLocalModel.Builder(botId)
                    .setAssetFilePath(factory.getBotMlModelPath(botId, useAssets))
                    .build()
        } else {
            FirebaseLocalModel.Builder(botId)
                    .setFilePath(factory.getBotMlModelPath(botId, useAssets))
                    .build()
        }
        FirebaseModelManager.getInstance().registerLocalModel(model)
        Log.d("NeuroBot", "$botId registered")
    }

    /**
     * Получение ответа от бота
     *
     * @param words Токенизированное входящее предложение
     *
     * @return Ответ бота
     */
    override suspend fun getResponse(words: List<String>): Response? {

        // Итоговый ответ
        var response: Response? = null

        // Входящие данные для классификатора
        val classifierInputArray = prepareInput(words, classifierWords)

        val classifierInputs = FirebaseModelInputs.Builder()
                .add(classifierInputArray)
                .build()

        val classifierInputOutputOptions: FirebaseModelInputOutputOptions = FirebaseModelInputOutputOptions.Builder()
                .setInputFormat(0, FirebaseModelDataType.FLOAT32, intArrayOf(1, classifierWords.size))
                .setOutputFormat(0, FirebaseModelDataType.FLOAT32, intArrayOf(1, classifierResponses.size))
                .build()

        // Получаем классифицированный вывод от мл модели
        val classifierOutputs = getOutputsFromModel(classifier, classifierInputs, classifierInputOutputOptions)
        val classifierOutputsArray = classifierOutputs as Array<*>

        // Список для хранения намерений полученных от основного классификатора
        val classifierResult = mutableListOf<ValidationResponse>()
        // Здесь храним индексы намерений
        val classifiedIndexes = mutableMapOf<Float, Int>()

        (classifierOutputsArray[0] as FloatArray).forEachIndexed { index, value ->
            classifierResult.add(ValidationResponse(tag = classifierResponses[index].tag, probability = value))
            classifiedIndexes[value] = index
        }

        // Берем классифицированное намерение с самой большой вероятностью и сравниваем с порогом
        val classifiedResponse = classifierResult
                .filter {
                    it.probability > if (language == BotLanguage.RU) CLASSIFIER_PROBABILITY_THRESHOLD
                    else CLASSIFIER_PROBABILITY_THRESHOLD_ENG
                }
                .sortedByDescending { it.probability }
                .firstOrNull()

        // В интентах нумерация с единицы, поэтому plus 1
        val classifiedTag = classifiedResponse?.tag
        val classifiedIndex = classifiedIndexes[classifiedResponse?.probability]?.plus(1) ?: -1

        Log.d("NeuroBot", "$botId classified response: $classifiedResponse")
        Log.d("NeuroBot", "$botId classified response index: $classifiedIndex")
        Log.d("NeuroBot", "$botId classified tag: $classifiedTag")

        if (classifiedResponse == null || classifiedTag == null) {
            return response
        }

        if (language == BotLanguage.RU) {
            // На данном этапе классификатор определил намерение бота classifiedTag и его индекс classifiedIndex
            // Теперь нам нужно удостовериться что намерение определено верно, для чего используем классификатор намерений
            val intentModelName = "intent_${botId}_$classifiedTag"
            val intentSource =
                    if (useAssets) FirebaseLocalModel.Builder(intentModelName)
                            .setAssetFilePath(factory.getIntentValidatorMlPath(botId, classifiedIndex, useAssets))
                            .build()
                    else FirebaseLocalModel.Builder(intentModelName)
                            .setFilePath(factory.getIntentValidatorMlPath(botId, classifiedIndex, useAssets))
                            .build()


            FirebaseModelManager.getInstance().registerLocalModel(intentSource)

            val intentOptions = FirebaseModelOptions.Builder()
                    .setLocalModelName(intentModelName)
                    .build()

            val intentInterpreter = FirebaseModelInterpreter.getInstance(intentOptions)

            // Мешок слов классификатора намерений
            val intentWords = factory.getIntentValidatorWordsBag(botId, classifiedIndex, useAssets)
            // Ответы классификатора намерений
            val intentResponses = factory.getIntentValidatorResponses(botId, classifiedIndex, useAssets)
            val intentInputArray = prepareInput(words, intentWords)

            val intentInputs = FirebaseModelInputs.Builder()
                    .add(intentInputArray)
                    .build()

            val intentInputOutputOptions: FirebaseModelInputOutputOptions = FirebaseModelInputOutputOptions.Builder()
                    .setInputFormat(0, FirebaseModelDataType.FLOAT32, intArrayOf(1, intentWords.size))
                    .setOutputFormat(0, FirebaseModelDataType.FLOAT32, intArrayOf(1, intentResponses.size))
                    .build()

            val intentOutputs = getOutputsFromModel(intentInterpreter, intentInputs, intentInputOutputOptions)
            val intentOutputsArray = intentOutputs as Array<*>

            // Список для хранения результатов валидации, валидатор вернет вероятности для двух элментов - True и False
            val finalResult = mutableListOf<ValidationResponse>()

            (intentOutputsArray[0] as FloatArray).forEachIndexed { index, value ->
                finalResult.add(ValidationResponse(tag = intentResponses[index], probability = value))
            }

            // Берем элемент с самой высокой вероятностью
            val result = finalResult
                    .filter { it.probability > INTENTS_PROBABILITY_THRESHOLD }
                    .sortedByDescending { it.probability }
                    .firstOrNull()
            // Если тег у элемента True, то classifiedTag определен верно, составляем Response на его основе
            // Фразы берем из ресурсов основного классификатора
            if (result != null && result.tag == "True") {
                response = classifierResponses.firstOrNull { it.tag == classifiedTag }?.let { classified ->
                    Log.d("NeuroBot", "$botId $classifiedTag confirmed")
                    Response(
                            botId = botId,
                            tag = classifiedTag,
                            link = "",
                            gif = classified.gif,
                            answers = classified.answers
                    )
                }
            }
            Log.d("NeuroBot", "$botId intent result $result")

        } else {
            response = classifierResponses.firstOrNull { it.tag == classifiedTag }?.let { classified ->
                Log.d("NeuroBot", "$botId $classifiedTag confirmed")
                Response(
                        botId = botId,
                        tag = classifiedTag,
                        link = "",
                        gif = classified.gif,
                        answers = classified.answers
                )
            }
            Log.d("NeuroBot", "$botId intent result $classifierResponses")
        }

        return response
    }

    // Получение ответа от интерпретатора ML модели
    private suspend fun getOutputsFromModel(interpreter: FirebaseModelInterpreter?, input: FirebaseModelInputs, options: FirebaseModelInputOutputOptions): Any =
            suspendCoroutine { continuation ->
                interpreter?.run(input, options)
                        ?.addOnSuccessListener { result: FirebaseModelOutputs ->
                            continuation.resume(result.getOutput(0))
                        }
                        ?.addOnFailureListener { exception: Exception ->
                            continuation.resumeWithException(exception)
                        }
            }

    /**
     * Подготовка входных данных для ML модели
     *
     * @param words Токенизированное входящее предложение
     * @param bag Мешок слов (слова, которые знает бот)
     *
     * @return Массив для ML модели
     *
     * Размер массива равен размеру мешка слов, изначально массив заполнен нулями.
     * Функция проверяет каждое слово из входящего предложения на наличие в мешке слов,
     * если слово найдено, то в результирующем массиве 0 заменяется на 1
     * Из-за особенности построения моделей на вход требуется массив из одного элемента,
     * который в свою очередь является массивом, поэтому используем Array<FloatArray>
     */
    private fun prepareInput(words: List<String>, bag: Map<Int, String>): Array<FloatArray> {
        val result = FloatArray(bag.size)
        val lemmaSet = words.toSet()

        for (entry in bag) {
            if (lemmaSet.contains(entry.value))
                result[entry.key] = 1.0f
        }

        return arrayOf(result)
    }

    // Порог вероятности, выше которого ответ считается успешным, от 0.0 до 1.0
    private companion object {
        const val CLASSIFIER_PROBABILITY_THRESHOLD = 0.9f   // Порог классификатора
        const val CLASSIFIER_PROBABILITY_THRESHOLD_ENG = 0.85f   // Порог классификатора

        const val INTENTS_PROBABILITY_THRESHOLD = 0.5f      // Порог валидатора намерения
    }
}
