package com.smedialink.storage.data.network

import com.google.gson.annotations.SerializedName

data class Response<T>(
    @SerializedName("status")
    val status: String,
    @SerializedName("message")
    val message: String,
    @SerializedName("payload")
    val payload: T
)
