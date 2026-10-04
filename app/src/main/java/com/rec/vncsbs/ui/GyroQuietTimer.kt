package com.rec.vncsbs.ui

internal class GyroQuietTimer {
    private var quietSince: Long? = null
    private var centered = false
    private var lastThreshold = Float.NaN
    private var lastDuration = 0
    var elapsedMs: Int = 0
        private set

    fun reset() {
        quietSince = null
        centered = false
        elapsedMs = 0
    }

    fun update(timestampNs: Long, speed: Float, threshold: Float, durationMs: Int): Boolean {
        if (threshold != lastThreshold || durationMs != lastDuration) {
            reset()
            lastThreshold = threshold
            lastDuration = durationMs
        }
        if (!speed.isFinite() || speed > threshold) {
            reset()
            return false
        }
        val start = quietSince ?: timestampNs.also { quietSince = it }
        elapsedMs = ((timestampNs - start) / 1_000_000L).coerceIn(0L, durationMs.toLong()).toInt()
        if (!centered && elapsedMs >= durationMs) {
            centered = true
            return true
        }
        return false
    }
}
