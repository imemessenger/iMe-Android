package com.smedialink.storage.data.network

import com.smedialink.storage.BuildConfig
import io.reactivex.Single
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface AiGramApi {
    companion object {
        const val STATUS_OK = "ok"
        const val STATUS_ERROR = "error"
        const val TYPE_INSTALL = 1
        const val CLIENT_ID = "telegram_client"

        // TODO AIGRAM Base url
        private const val BASE_URL =
                "https://us-central1-ime-messenger.cloudfunctions.net/"

        private val interceptor =
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG)
                        HttpLoggingInterceptor.Level.BASIC
                    else
                        HttpLoggingInterceptor.Level.NONE
                }

        private val client =
                OkHttpClient.Builder().addInterceptor(interceptor).build()

        fun getInstance(): AiGramApi {
            if (INSTANCE == null) {
                INSTANCE = Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .client(client)
                        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                        .create(AiGramApi::class.java)
            }

            return INSTANCE!!
        }

        private var INSTANCE: AiGramApi? = null
    }


    @GET("/commitChannelReview")
    fun voteForChannel(@Query("channel_id") channelId: String,
                   @Query("rating") rating: Int,
                   @Query("user_id") userId: Long): Single<Response<String>>

}
