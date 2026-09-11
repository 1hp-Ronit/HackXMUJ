package com.pulsenet.app.sensor

/**
 * Detects the "tap the back of the phone rapidly" distress pattern: 5 sharp
 * accelerometer spikes above [thresholdMs2] within [windowMs] of each other.
 * Deliberately violent enough that walking or normal pocket movement won't
 * false-trigger it. Pure Kotlin with no Android sensor dependency, so it's
 * directly unit-testable; [DistressSensorManager] feeds it raw samples.
 */
class ShakePatternDetector(
    private val thresholdMs2: Double = 15.0,
    private val requiredSpikes: Int = 5,
    private val windowMs: Long = 3000L,
    private val cooldownMs: Long = 30_000L
) {
    private val spikeTimestamps = ArrayDeque<Long>()
    private var cooldownUntilMs: Long = 0L

    /** Feed one accelerometer magnitude sample; returns true exactly once when SOS should fire. */
    fun onSample(magnitude: Double, timestampMs: Long): Boolean {
        if (magnitude < thresholdMs2) return false
        if (timestampMs < cooldownUntilMs) return false

        spikeTimestamps.addLast(timestampMs)
        while (spikeTimestamps.isNotEmpty() && timestampMs - spikeTimestamps.first() > windowMs) {
            spikeTimestamps.removeFirst()
        }

        if (spikeTimestamps.size >= requiredSpikes) {
            spikeTimestamps.clear()
            cooldownUntilMs = timestampMs + cooldownMs
            return true
        }
        return false
    }
}
