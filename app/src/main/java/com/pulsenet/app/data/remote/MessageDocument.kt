package com.pulsenet.app.data.remote

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MessageDocument(
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
