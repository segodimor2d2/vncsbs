package com.rec.vncsbs.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput

internal fun Modifier.simulatedTouchpad(
    enabled: Boolean,
    density: Float,
    onMove: (Float, Float) -> Unit,
    onClick: (Int) -> Unit,
    onDragButton: (Boolean) -> Unit,
    onScroll: (Float, Float) -> Unit,
    onMenu: () -> Unit,
    onRelease: () -> Unit
): Modifier = pointerInput(enabled, density) {
    if (!enabled) return@pointerInput
    try {
        awaitEachGesture {
            val first = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (first.type != PointerType.Touch) return@awaitEachGesture
            first.consume()
            val origins = mutableMapOf(first.id to first.position)
            var moved = false
            var dragging = false
            var maximumFingers = 1
            var menuOpened = false
            val longPressDeadline = first.uptimeMillis + viewConfiguration.longPressTimeoutMillis
            var time = first.uptimeMillis
            try {
                while (true) {
                    var event = if (!moved && !dragging && maximumFingers == 1) {
                        withTimeoutOrNull((longPressDeadline - time).coerceAtLeast(1)) {
                            awaitPointerEvent(PointerEventPass.Initial)
                        }
                    } else awaitPointerEvent(PointerEventPass.Initial)
                    if (event == null) {
                        dragging = true
                        onDragButton(true)
                        event = awaitPointerEvent(PointerEventPass.Initial)
                    }
                    val touches = event.changes.filter { it.type == PointerType.Touch }
                    val pressed = touches.filter { it.pressed }
                    maximumFingers = maxOf(maximumFingers, pressed.size)
                    time = touches.maxOfOrNull { it.uptimeMillis } ?: time
                    for (change in touches) {
                        val origin = origins.getOrPut(change.id) { change.position }
                        if ((change.position - origin).getDistance() > viewConfiguration.touchSlop) moved = true
                    }
                    if (maximumFingers >= 3) {
                        if (!menuOpened) {
                            onRelease()
                            dragging = false
                            menuOpened = true
                            onMenu()
                        }
                    } else if (maximumFingers == 2) {
                        if (dragging) { onDragButton(false); dragging = false }
                        if (pressed.size == 2 && pressed.all { it.previousPressed } && moved) {
                            val delta = pressed.fold(Offset.Zero) { sum, change ->
                                sum + change.position - change.previousPosition
                            } / 2f / density
                            // Natural scrolling: swipe upwards to move down the page.
                            onScroll(-delta.x / 32f, delta.y / 32f)
                        }
                    } else if (pressed.size == 1 && pressed[0].previousPressed && (moved || dragging)) {
                        val change = pressed[0]
                        val delta = (change.position - change.previousPosition) / density
                        onMove(delta.x, delta.y)
                    }
                    touches.forEach { it.consume() }
                    if (pressed.isEmpty()) {
                        val duration = time - first.uptimeMillis
                        if (!moved && !dragging && !menuOpened && duration < viewConfiguration.longPressTimeoutMillis) {
                            onClick(if (maximumFingers == 2) 2 else 1)
                        }
                        break
                    }
                }
            } finally {
                onRelease()
            }
        }
    } finally {
        onRelease()
    }
}
