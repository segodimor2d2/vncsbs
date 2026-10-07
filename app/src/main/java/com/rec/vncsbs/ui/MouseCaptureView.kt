package com.rec.vncsbs.ui

import android.content.Context
import android.os.Build
import android.view.MotionEvent
import android.view.View
import com.rec.vncsbs.vnc.PointerPacket
import com.rec.vncsbs.vnc.VncPointerState

internal class MouseCaptureView(context: Context) : View(context) {
    private val pointer = VncPointerState()
    var sendPointer: (PointerPacket) -> Unit = {}
    var onUnexpectedCaptureLoss: () -> Unit = {}
    private var releaseRequested = false
    var captureEnabled = false
        private set

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun update(enabled: Boolean, width: Int, height: Int) {
        pointer.resize(width, height)
        captureEnabled = enabled && Build.VERSION.SDK_INT >= 26 && width > 0 && height > 0
        if (captureEnabled) {
            if (hasWindowFocus() && !hasPointerCapture()) {
                releaseRequested = false
                requestFocus()
                requestPointerCapture()
            }
        } else {
            releaseCapture()
        }
    }

    fun releaseCapture() {
        releaseRequested = true
        pointer.release()?.let(sendPointer)
        if (Build.VERSION.SDK_INT >= 26 && hasPointerCapture()) releasePointerCapture()
    }

    fun stopCapture() {
        captureEnabled = false
        releaseCapture()
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (!hasWindowFocus) releaseCapture()
        else if (captureEnabled && Build.VERSION.SDK_INT >= 26) {
            releaseRequested = false
            requestFocus()
            requestPointerCapture()
        }
    }

    override fun onPointerCaptureChange(hasCapture: Boolean) {
        super.onPointerCaptureChange(hasCapture)
        if (!hasCapture) {
            pointer.release()?.let(sendPointer)
            if (!releaseRequested && captureEnabled && hasWindowFocus()) {
                captureEnabled = false
                onUnexpectedCaptureLoss()
            }
        }
    }

    override fun onDetachedFromWindow() {
        releaseCapture()
        super.onDetachedFromWindow()
    }

    override fun onCapturedPointerEvent(event: MotionEvent): Boolean {
        if (!captureEnabled) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_CANCEL -> pointer.release()?.let(sendPointer)
            MotionEvent.ACTION_SCROLL -> pointer.scroll(
                event.getAxisValue(MotionEvent.AXIS_HSCROLL),
                event.getAxisValue(MotionEvent.AXIS_VSCROLL)
            ).forEach(sendPointer)
            MotionEvent.ACTION_MOVE -> {
                for (index in 0 until event.historySize) {
                    pointer.move(event.getHistoricalX(index), event.getHistoricalY(index), event.buttonState)
                        ?.let(sendPointer)
                }
                pointer.move(event.x, event.y, event.buttonState)?.let(sendPointer)
            }
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP,
            MotionEvent.ACTION_BUTTON_PRESS, MotionEvent.ACTION_BUTTON_RELEASE ->
                pointer.move(0f, 0f, event.buttonState)?.let(sendPointer)
        }
        return true
    }
}
