package com.pulsenet.app.data.repository

import com.pulsenet.app.data.remote.AtlasApiService
import com.pulsenet.app.data.remote.InsertManyRequest
import com.pulsenet.app.data.remote.InsertManyResponse
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

/** [failOnCallIndex] makes the Nth call (0-indexed) return an HTTP error, to test retry paths. */
class FakeAtlasApiService(private val failOnCallIndex: Int? = null) : AtlasApiService {
    val receivedRequests = mutableListOf<InsertManyRequest>()
    private var callCount = 0

    override suspend fun insertMany(apiKey: String, request: InsertManyRequest): Response<InsertManyResponse> {
        receivedRequests.add(request)
        val currentCall = callCount++
        return if (failOnCallIndex != null && currentCall == failOnCallIndex) {
            Response.error(500, "".toResponseBody("application/json".toMediaType()))
        } else {
            Response.success(InsertManyResponse(insertedIds = request.documents.map { it.messageId }))
        }
    }
}
