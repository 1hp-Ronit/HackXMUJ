package com.pulsenet.app.data.remote

import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * The PulseNet rescue-dashboard backend (see backend/server.js), not MongoDB
 * Atlas directly — keeps database credentials out of the shipped APK and
 * avoids depending on Atlas's Data API, which MongoDB has been retiring.
 */
interface BackendApiService {
    @POST("api/messages/bulk")
    suspend fun insertMessages(@Body request: BulkInsertRequest): Response<BulkInsertResponse>
}

@JsonClass(generateAdapter = true)
data class BulkInsertRequest(val documents: List<MessageDocument>)

@JsonClass(generateAdapter = true)
data class BulkInsertResponse(val inserted: Int?)
