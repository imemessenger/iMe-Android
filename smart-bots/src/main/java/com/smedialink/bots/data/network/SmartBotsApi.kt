package com.smedialink.bots.data.network

import com.smedialink.bots.BuildConfig
import com.smedialink.bots.domain.model.ShopProduct
import io.reactivex.Single
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface SmartBotsApi {
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

        fun getInstance(): SmartBotsApi {
            if (INSTANCE == null) {
                INSTANCE = Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .client(client)
                        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                        .create(SmartBotsApi::class.java)
            }

            return INSTANCE!!
        }

        private var INSTANCE: SmartBotsApi? = null
    }

    @GET("/getBotsInfo")
    fun getBotsInfo(): Single<Response<List<BotRemoteInfo>>>

    @GET("/getBotVoting")
    fun getBotsVoting(@Query("user_id") userId: Long): Single<Response<List<BotVoteInfo>>>

    @GET("/installDeleteApp")
    fun appInstall(@Query("app_id") appId: String,
                   @Query("type") type: Int,
                   @Query("user_id") userId: Long): Single<Response<String>>

    @GET("/installDeleteBot")
    fun botInstall(@Query("bot_id") botId: String,
                   @Query("type") type: Int,
                   @Query("user_id") userId: Int): Single<Response<String>>

    @GET("/voteBot")
    fun voteForBot(@Query("bot_id") botId: String,
                   @Query("rating") rating: Int,
                   @Query("user_id") userId: Long): Single<Response<String>>

    @POST("/validateReceipts")
    fun validatePurchases(@Body body: List<ShopProduct.Receipt>): Single<Response<List<ValidationInfo>>>
}
