package com.pulsenet.app.data.repository

import com.pulsenet.app.data.remote.BackendApiService
import com.pulsenet.app.data.remote.BulkInsertRequest
import com.pulsenet.app.data.remote.BulkInsertResponse
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

/** [failOnCallIndex] makes the Nth call (0-indexed) return an HTTP error, to test retry paths. */
class FakeBackendApiService(private val failOnCallIndex: Int? = null) : BackendApiService {
    val receivedRequests = mutableListOf<BulkInsertRequest>()
    private var callCount = 0

    override suspend fun insertMessages(request: BulkInsertRequest): Response<BulkInsertResponse> {
        receivedRequests.add(request)
        val currentCall = callCount++
        return if (failOnCallIndex != null && currentCall == failOnCallIndex) {
            Response.error(500, "".toResponseBody("application/json".toMediaType()))
        } else {
            Response.success(BulkInsertResponse(inserted = request.documents.size))
        }
    }
}
