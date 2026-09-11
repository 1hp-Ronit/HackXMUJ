package com.pulsenet.app.data.remote

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface SarvamApiService {
    @POST("translate")
    suspend fun translate(
        @Header("api-subscription-key") apiKey: String,
        @Body request: TranslateRequest
    ): TranslateResponse
}

@JsonClass(generateAdapter = true)
data class TranslateRequest(
    val input: String,
    val source_language_code: String = "auto",
    val target_language_code: String = "en",
    val mode: String = "code-mixed"
)

@JsonClass(generateAdapter = true)
data class TranslateResponse(val translated_text: String)
