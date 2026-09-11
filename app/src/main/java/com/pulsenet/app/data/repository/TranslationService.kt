package com.pulsenet.app.data.repository

import com.pulsenet.app.BuildConfig
import com.pulsenet.app.data.local.entity.MessageEntity
import com.pulsenet.app.data.remote.SarvamApiService
import com.pulsenet.app.data.remote.SeverityClassifier
import com.pulsenet.app.data.remote.TranslateRequest
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Translates non-English distress messages via Sarvam AI and tags severity, as
 * the plug-in for [SyncRepository.flushUnsyncedMessages]'s `translate` seam.
 */
@Singleton
class TranslationService @Inject constructor(
    private val sarvamApiService: SarvamApiService
) {
    private companion object {
        const val RATE_LIMIT_DELAY_MS = 100L
    }

    suspend fun translateAndClassify(message: MessageEntity): Pair<String, String> {
        val translated = if (isLikelyNonEnglish(message.content)) {
            val result = runCatching {
                sarvamApiService.translate(
                    BuildConfig.SARVAM_API_KEY,
                    TranslateRequest(input = message.content)
                ).translated_text
            }.getOrDefault(message.content)
            delay(RATE_LIMIT_DELAY_MS)
            result
        } else {
            message.content
        }
        return translated to SeverityClassifier.classify(translated)
    }

    /**
     * True if the text contains any letter from a non-Latin script — covers both
     * "non-ASCII" and "mixed script" content. Pure-Latin Hinglish (Hindi words
     * spelled phonetically) isn't detectable this way without real language
     * detection, which is out of scope for a keyword-based hackathon heuristic.
     */
    private fun isLikelyNonEnglish(text: String): Boolean = text.any { char ->
        char.isLetter() && Character.UnicodeScript.of(char.code).let {
            it != Character.UnicodeScript.LATIN && it != Character.UnicodeScript.COMMON
        }
    }
}
