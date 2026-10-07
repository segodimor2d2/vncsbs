package com.rec.vncsbs

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LeaderKeyboardTest {
    @Test fun ordinaryAtWithSimultaneousFirmwareShiftDoesNotActivate() {
        assertFalse(shouldActivateLeader(false, true, true, 100, 100))
    }

    @Test fun atWithPreviouslyHeldShiftActivates() {
        assertTrue(shouldActivateLeader(false, true, true, 200, 100))
    }

    @Test fun atWithoutShiftDoesNotActivate() {
        assertFalse(shouldActivateLeader(true, true, false, 200, null))
    }

    @Test fun dedicatedAtWithShiftActivates() {
        assertTrue(shouldActivateLeader(true, true, true, 200, null))
    }

    @Test fun otherCharactersWithShiftDoNotActivate() {
        assertFalse(shouldActivateLeader(false, false, true, 200, 100))
    }
}
