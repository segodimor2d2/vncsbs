package com.rec.vncsbs

import android.view.InputDevice
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.rec.vncsbs.ui.MouseCaptureView
import com.rec.vncsbs.vnc.PointerPacket
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class MouseCaptureTest {
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    @Test fun capturesMouseMovesAndReleasesDraggedButton() {
        lateinit var view: MouseCaptureView
        val packets = mutableListOf<PointerPacket>()
        composeRule.runOnUiThread {
            view = MouseCaptureView(composeRule.activity)
            view.sendPointer = { packets += it }
            composeRule.activity.addContentView(view, ViewGroup.LayoutParams(1, 1))
        }
        composeRule.waitUntil(5000) { view.hasWindowFocus() }
        composeRule.runOnUiThread { view.update(true, 100, 80) }
        composeRule.waitUntil(5000) { view.hasPointerCapture() }
        composeRule.runOnUiThread {
            event(MotionEvent.ACTION_BUTTON_PRESS, 0f, 0f, MotionEvent.BUTTON_PRIMARY).useEvent {
                assertTrue(view.onCapturedPointerEvent(it))
            }
            event(MotionEvent.ACTION_MOVE, 6f, -4f, MotionEvent.BUTTON_PRIMARY).useEvent {
                assertTrue(view.onCapturedPointerEvent(it))
            }
            view.update(false, 100, 80)
        }
        composeRule.waitUntil(5000) { !view.hasPointerCapture() }
        assertEquals(listOf(PointerPacket(50, 40, 1), PointerPacket(56, 36, 1),
            PointerPacket(56, 36, 0)), packets)
    }

    private fun event(action: Int, x: Float, y: Float, buttons: Int): MotionEvent {
        val properties = MotionEvent.PointerProperties().apply {
            id = 0
            toolType = MotionEvent.TOOL_TYPE_MOUSE
        }
        val coordinates = MotionEvent.PointerCoords().apply { this.x = x; this.y = y }
        return MotionEvent.obtain(100, 100, action, 1, arrayOf(properties), arrayOf(coordinates),
            0, buttons, 1f, 1f, 0, 0, InputDevice.SOURCE_MOUSE_RELATIVE, 0)
    }

    private fun MotionEvent.useEvent(block: (MotionEvent) -> Unit) {
        try { block(this) } finally { recycle() }
    }
}
