package com.pulsenet.app.sensor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ShakePatternDetectorTest {

    private lateinit var detector: ShakePatternDetector

    @Before
    fun setUp() {
        detector = ShakePatternDetector()
    }

    @Test
    fun fiveSpikesWithinWindowTriggers() {
        var triggered = false
        val timestamps = listOf(0L, 500L, 1000L, 1500L, 2000L)
        for (t in timestamps) {
            triggered = detector.onSample(20.0, t) || triggered
        }
        assertTrue(triggered)
    }

    @Test
    fun samplesBelowThresholdNeverTrigger() {
        var triggered = false
        for (t in listOf(0L, 500L, 1000L, 1500L, 2000L)) {
            triggered = detector.onSample(5.0, t) || triggered
        }
        assertFalse(triggered)
    }

    @Test
    fun spikesSpreadBeyondWindowDoNotTrigger() {
        // Each spike more than 3000ms after the first, so the sliding window
        // never accumulates 5 within range.
        var triggered = false
        for (t in listOf(0L, 3500L, 7000L, 10500L, 14000L)) {
            triggered = detector.onSample(20.0, t) || triggered
        }
        assertFalse(triggered)
    }

    @Test
    fun fourSpikesAloneDoNotTrigger() {
        var triggered = false
        for (t in listOf(0L, 500L, 1000L, 1500L)) {
            triggered = detector.onSample(20.0, t) || triggered
        }
        assertFalse(triggered)
    }

    @Test
    fun cooldownPreventsImmediateRetrigger() {
        val firstBatch = listOf(0L, 500L, 1000L, 1500L, 2000L)
        firstBatch.forEach { detector.onSample(20.0, it) }

        // Well within the 30s cooldown.
        val retriggered = detector.onSample(20.0, 5000L)
        assertFalse(retriggered)
    }

    @Test
    fun canTriggerAgainAfterCooldownExpires() {
        val firstBatch = listOf(0L, 500L, 1000L, 1500L, 2000L)
        firstBatch.forEach { detector.onSample(20.0, it) }

        var triggeredAgain = false
        val secondBatch = listOf(32_000L, 32_500L, 33_000L, 33_500L, 34_000L)
        for (t in secondBatch) {
            triggeredAgain = detector.onSample(20.0, t) || triggeredAgain
        }
        assertTrue(triggeredAgain)
    }
}
