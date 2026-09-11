package com.pulsenet.app.domain.model

enum class Priority(val value: Int) {
    SOS(0),
    MEDICAL(1),
    RESOURCE(2),
    GENERAL(3);

    companion object {
        fun fromValue(value: Int): Priority = entries.firstOrNull { it.value == value } ?: GENERAL
    }
}
