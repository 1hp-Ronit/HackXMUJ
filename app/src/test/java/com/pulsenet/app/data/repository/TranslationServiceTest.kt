package com.pulsenet.app.data.repository

import com.pulsenet.app.data.local.entity.MessageEntity
import com.pulsenet.app.data.remote.SarvamApiService
import com.pulsenet.app.data.remote.TranslateRequest
import com.pulsenet.app.data.remote.TranslateResponse
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

private class FakeSarvamApiService(private val translatedText: String) : SarvamApiService {
    var callCount = 0
        private set

    override suspend fun translate(apiKey: String, request: TranslateRequest): TranslateResponse {
        callCount++
        return TranslateResponse(translated_text = translatedText)
    }
}

class TranslationServiceTest {

    private fun message(content: String) = MessageEntity(
        messageId = "msg-1",
        senderPublicKey = "pubkey",
        senderAlias = "Ronit",
        content = content,
        latitude = 26.9124,
        longitude = 75.7873,
        priority = 2,
        hopCount = 0,
        signature = "sig",
        createdAtEpochMs = 1_726_000_000_000,
        receivedAtEpochMs = 1_726_000_000_000
    )

    @Test
    fun englishContentSkipsTranslationCall() = runTest {
        val fakeApi = FakeSarvamApiService(translatedText = "should not be used")
        val service = TranslationService(fakeApi)

        val (translated, severity) = service.translateAndClassify(message("I need an ambulance"))

        assertEquals(0, fakeApi.callCount)
        assertEquals("I need an ambulance", translated)
        assertEquals("HIGH", severity)
    }

    @Test
    fun nonEnglishContentIsTranslatedThenClassified() = runTest {
        val fakeApi = FakeSarvamApiService(translatedText = "I am trapped under a building")
        val service = TranslationService(fakeApi)

        val (translated, severity) = service.translateAndClassify(message("मैं इमारत के नीचे फंसा हूँ"))

        assertEquals(1, fakeApi.callCount)
        assertEquals("I am trapped under a building", translated)
        assertEquals("CRITICAL", severity)
    }

    @Test
    fun translationFailureFallsBackToOriginalContent() = runTest {
        val failingApi = object : SarvamApiService {
            override suspend fun translate(apiKey: String, request: TranslateRequest): TranslateResponse {
                throw IOException("network down")
            }
        }
        val service = TranslationService(failingApi)

        val (translated, severity) = service.translateAndClassify(message("मदद चाहिए trapped"))

        assertEquals("मदद चाहिए trapped", translated)
        assertEquals("CRITICAL", severity)
    }
}
