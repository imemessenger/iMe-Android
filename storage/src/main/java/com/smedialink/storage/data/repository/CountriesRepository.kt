package com.smedialink.storage.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.smedialink.storage.data.database.StorageDatabase
import com.smedialink.storage.data.database.model.CountryDbModel
import com.smedialink.storage.data.database.model.parser.SnapshotParser
import com.smedialink.storage.data.mapper.CountryMapper
import com.smedialink.storage.domain.model.Country
import com.smedialink.storage.domain.model.EmptySnapshotException
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers

class CountriesRepository private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: CountriesRepository? = null

        fun getInstance(context: Context): CountriesRepository {
            if (INSTANCE == null) {
                synchronized(CountriesRepository::class) {
                    INSTANCE = CountriesRepository(context)
                }
            }
            return INSTANCE!!
        }


        private const val COUNTRIES_COLLECTION_NAME = "countries"
        private const val SHARED_PREFERENCES_COUNTRIES = "SHARED_PREFERENCES_COUNTRIES"
        private const val SHARED_PREFERENCES_KEY_CURRENT_COUNTRY = "SHARED_PREFERENCES_KEY_CURRENT_COUNTRY"
        private const val SHARED_PREFERENCES_KEY_CURRENT_BOT_LANGUAGE = "SHARED_PREFERENCES_KEY_CURRENT_BOT_LANGUAGE"

        val RUSSIAN_REGIONS: Set<String> = setOf(
                // Russian
                "ru_MD", "ru_UA", "ru_RU", "ru_KZ", "ru_KG", "ru_BY", "ru",
                // Armenian
                "hy_AM", "hy",
                // Uzbek
                "uz_Cyrl_UZ", "uz_Cyrl",
                // Tajik
                "tg_Cyrl_TJ", "tg_Cyrl",
                // Azerbaijani
                "az_Cyrl_AZ", "az_Cyrl"

        )

        val RUSSIAN_PHONE_CODES: Set<String> = setOf(
                "7", "373", "374", "375", "380", "992", "994", "996", "998"
        )

        fun isRussia(phone: String, locale: String): Boolean {
            val phoneCode = if (phone.length == 11) phone.first().toString() else ""
            return RUSSIAN_PHONE_CODES.contains(phoneCode) || RUSSIAN_REGIONS.contains(locale)
        }
    }


    private val remoteDatabase = FirebaseFirestore.getInstance()
    private val database: StorageDatabase = StorageDatabase.getInstance(context)
    private val countryDao = database.countryDao()
    private val snapshotParser: SnapshotParser = SnapshotParser()
    private val countryMapper = CountryMapper()
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_COUNTRIES, Context.MODE_PRIVATE)

    fun observeCountries(phone: String, locale: String): Observable<List<Country>> =
            countryDao.streamCountries()
                    .map { channels -> channels.map { countryMapper.mapFromDb(it, getCurrentCountry(phone, locale)) } }
                    .map { channels -> channels.sortedBy { it.name } }
                    .toObservable()

    fun observeRemoteCountries(phone: String, locale: String): Observable<List<Country>> =
            Observable.create<List<CountryDbModel>>
            { emitter ->
                remoteDatabase.collection(COUNTRIES_COLLECTION_NAME)
                        .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, exception ->
                            when {
                                exception != null -> emitter.onError(exception)
                                snapshot == null -> emitter.onError(EmptySnapshotException("Collection $COUNTRIES_COLLECTION_NAME is empty"))
                                else -> emitter.onNext(snapshotParser.parseCountriesToDb(snapshot))
                            }
                        }
            }.observeOn(Schedulers.io())
                    .flatMap {
                        Completable.fromAction {
                            countryDao.deleteAll()
                            countryDao.insertOrReplace(it)
                        }.toSingleDefault(it)
                                .toObservable()
                    }
                    .map { channels -> channels.map { countryMapper.mapFromDb(it, getCurrentCountry(phone, locale)) } }

    fun getCurrentCountry(phone: String, locale: String): String =
            sharedPreferences.getString(SHARED_PREFERENCES_KEY_CURRENT_COUNTRY, null)
                    ?: getDefaultCountry(phone,locale).also {
                        sharedPreferences.edit().putString(SHARED_PREFERENCES_KEY_CURRENT_COUNTRY, it).apply()
                    }

    fun setCurrentCountry(country: String) {
        sharedPreferences.edit().putString(SHARED_PREFERENCES_KEY_CURRENT_COUNTRY, country).apply()
    }

    private fun getDefaultCountry(phone: String, locale: String): String =
            if(isRussia(phone,locale)) "ru"
                else  "us"


    private fun getDefaultLanguage(phone: String, locale: String): String =
            if(isRussia(phone,locale)) "ru"
            else  "eng"



    fun getCurrentBotLanguage(phone: String, locale: String): String =
            sharedPreferences.getString(SHARED_PREFERENCES_KEY_CURRENT_BOT_LANGUAGE, null)
                    ?: getDefaultLanguage(phone,locale).also {
                        sharedPreferences.edit().putString(SHARED_PREFERENCES_KEY_CURRENT_BOT_LANGUAGE, it).apply()
                    }

    fun setCurrentBotLanguage(language: String) {
        sharedPreferences.edit().putString(SHARED_PREFERENCES_KEY_CURRENT_BOT_LANGUAGE, language).apply()
    }


}