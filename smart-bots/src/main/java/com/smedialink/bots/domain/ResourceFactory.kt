package com.smedialink.bots.domain

import com.smedialink.bots.data.model.Response

interface ResourceFactory {

    /**
     * Ответы для бота праздников
     * @param botId идентификатор бота
     *
     * @return Список ответов
     */
    fun getHolidaysResponses(botId: String,fromAssets:Boolean): List<Response>

    /**
     * Получение мешка слов основного классификатора
     * @param botId идентификатор бота
     * @param fromAssets если true, то загружаются ресурсы из ассетов, иначе скачанные
     *
     * @return Map вида позиция - слово
     */
    fun getBotWordsBag(botId: String, fromAssets: Boolean): Map<Int, String>

    /**
     * Ответы для бота в основном классификаторе
     * @param botId идентификатор бота
     * @param fromAssets если true, то загружаются ресурсы из ассетов, иначе скачанные
     *
     * @return Список ответов
     */
    fun getBotResponsesList(botId: String, fromAssets: Boolean): List<Response>

    /**
     * Получение пути к ml модели основного классификатора
     *
     * @param botId идентификатор бота
     * @param fromAssets если true, то загружаются ресурсы из ассетов, иначе скачанные
     *
     * @return Путь к модели классификатора
     */
    fun getBotMlModelPath(botId: String, fromAssets: Boolean): String

    /**
     * Получение пути к ml модели валидатора намерения
     *
     * @param botId идентификатор бота
     * @param fromAssets если true, то загружаются ресурсы из ассетов, иначе скачанные
     *
     * @return Путь к модели валидатора намерения
     */
    fun getIntentValidatorMlPath(botId: String, classifiedIndex: Int, fromAssets: Boolean): String

    /**
     * Получение мешка слов валидатора намерения
     * @param botId идентификатор бота
     * @param fromAssets если true, то загружаются ресурсы из ассетов, иначе скачанные
     *
     * @return Map вида позиция - слово
     */
    fun getIntentValidatorWordsBag(botId: String, classifiedIndex: Int, fromAssets: Boolean): Map<Int, String>

    /**
     * Ответы валидатора намерений
     * @param botId идентификатор бота
     * @param fromAssets если true, то загружаются ресурсы из ассетов, иначе скачанные
     *
     * @return Список позиций и ответов
     */
    fun getIntentValidatorResponses(botId: String, classifiedIndex: Int, fromAssets: Boolean): Map<Int, String>
}
