package com.rec.vncsbs

import com.rec.vncsbs.ui.GyroQuietTimer
import org.junit.Assert.*
import org.junit.Test

class GyroQuietTimerTest {
    @Test fun centersOnlyOnceAfterContinuousQuiet() {
        val timer = GyroQuietTimer()
        assertFalse(timer.update(0, 0.01f, 0.02f, 500))
        assertFalse(timer.update(499_000_000, 0.01f, 0.02f, 500))
        assertTrue(timer.update(500_000_000, 0.01f, 0.02f, 500))
        assertFalse(timer.update(900_000_000, 0.01f, 0.02f, 500))
        assertEquals(500, timer.elapsedMs)
    }

    @Test fun movementRestartsCountdown() {
        val timer = GyroQuietTimer()
        timer.update(0, 0f, 0.02f, 500)
        assertFalse(timer.update(400_000_000, 0.03f, 0.02f, 500))
        assertEquals(0, timer.elapsedMs)
        assertFalse(timer.update(500_000_000, 0f, 0.02f, 500))
        assertFalse(timer.update(999_000_000, 0f, 0.02f, 500))
        assertTrue(timer.update(1_000_000_000, 0f, 0.02f, 500))
    }

    @Test fun settingsChangesAndPauseResetCountdown() {
        val timer = GyroQuietTimer()
        timer.update(0, 0f, 0.02f, 500)
        assertFalse(timer.update(500_000_000, 0f, 0.03f, 1000))
        assertFalse(timer.update(1_499_000_000, 0f, 0.03f, 1000))
        assertTrue(timer.update(1_500_000_000, 0f, 0.03f, 1000))
        timer.reset()
        assertFalse(timer.update(2_000_000_000, 0f, 0.03f, 1000))
        assertEquals(0, timer.elapsedMs)
    }
}
