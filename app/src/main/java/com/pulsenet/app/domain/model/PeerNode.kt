package com.pulsenet.app.domain.model

data class PeerNode(
    val endpointId: String,
    val alias: String,
    val connectedAtEpochMs: Long
)
