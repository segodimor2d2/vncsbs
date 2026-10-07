package com.rec.vncsbs

import com.rec.vncsbs.vnc.VncPointerState
import com.rec.vncsbs.vnc.encodePointerEvent
import org.junit.Assert.*
import org.junit.Test

class VncPointerTest {
    @Test fun encodesNetworkCoordinatesAndButtons() {
        assertArrayEquals(byteArrayOf(5, 5, 0x12, 0x34, 0x56, 0x78),
            encodePointerEvent(0x1234, 0x5678, 5))
    }

    @Test fun relativeMovementAccumulatesFractionsAndClampsToDesktop() {
        val pointer = VncPointerState()
        pointer.resize(100, 80)
        assertEquals(50, pointer.move(0.5f, 0f, 0)!!.x)
        assertEquals(51, pointer.move(0.5f, 0f, 0)!!.x)
        val packet = pointer.move(1000f, -1000f, 0)!!
        assertEquals(99, packet.x)
        assertEquals(0, packet.y)
        assertNull(pointer.move(Float.NaN, 0f, 0))
    }

    @Test fun centeringUpdatesTheNextRelativeMovementWithoutChangingButtons() {
        val pointer = VncPointerState()
        assertNull(pointer.center())
        pointer.resize(101, 81)
        pointer.move(40f, 30f, 1)
        val centered = pointer.center()!!
        assertEquals(50, centered.x)
        assertEquals(40, centered.y)
        assertEquals(1, centered.buttons)
        val next = pointer.move(3f, -2f, 1)!!
        assertEquals(53, next.x)
        assertEquals(38, next.y)
    }

    @Test fun rightAndMiddleButtonsMapToVncAndReleaseOnCaptureLoss() {
        val pointer = VncPointerState()
        pointer.resize(100, 80)
        assertEquals(4, pointer.move(0f, 0f, 2)!!.buttons)
        assertEquals(2, pointer.move(0f, 0f, 4)!!.buttons)
        assertEquals(7, pointer.move(0f, 0f, 7)!!.buttons)
        assertEquals(0, pointer.release()!!.buttons)
        assertNull(pointer.release())
    }

    @Test fun wheelPulsesPreserveHeldButtonAndFractionalScrolling() {
        val pointer = VncPointerState()
        pointer.resize(100, 80)
        pointer.move(0f, 0f, 1)
        assertTrue(pointer.scroll(0f, 0.5f).isEmpty())
        assertEquals(listOf(9, 1), pointer.scroll(0f, 0.5f).map { it.buttons })
        assertEquals(listOf(17, 1, 17, 1), pointer.scroll(0f, -2f).map { it.buttons })
        assertEquals(listOf(65, 1), pointer.scroll(1f, 0f).map { it.buttons })
        assertEquals(listOf(33, 1), pointer.scroll(-1f, 0f).map { it.buttons })
    }
}
