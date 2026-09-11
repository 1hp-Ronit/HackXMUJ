package com.pulsenet.app.data.remote

import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AtlasApiService {
    @POST("endpoint/data/v1/action/insertMany")
    suspend fun insertMany(
        @Header("api-key") apiKey: String,
        @Body request: InsertManyRequest
    ): Response<InsertManyResponse>
}

@JsonClass(generateAdapter = true)
data class InsertManyRequest(
    val collection: String,
    val database: String,
    val dataSource: String,
    val documents: List<AtlasMessageDocument>
)

@JsonClass(generateAdapter = true)
data class InsertManyResponse(val insertedIds: List<String>?)

@JsonClass(generateAdapter = true)
data class AtlasMessageDocument(
    val messageId: String,
    val senderAlias: String,
    val senderPublicKey: String,
    val content: String,
    val location: GeoJsonPoint,
    val priority: Int,
    val hopCount: Int,
    val signature: String,
    val createdAt: String,
    val translatedContent: String?,
    val severityTag: String?
)

/** MongoDB 2dsphere GeoJSON: coordinates MUST be [longitude, latitude], in that order. */
@JsonClass(generateAdapter = true)
data class GeoJsonPoint(
    val type: String = "Point",
    val coordinates: List<Double>
)
