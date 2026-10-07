package com.rec.vncsbs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.rec.vncsbs.ui.simulatedTouchpad
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class SimulatedTouchpadTest {
    @get:Rule val composeRule = createComposeRule()
    private val clicks = mutableListOf<Int>()
    private val movements = mutableListOf<Offset>()
    private val scrolls = mutableListOf<Offset>()
    private var menuCount = 0
    private var dragging = false

    private fun showTouchpad() {
        composeRule.setContent {
            Box(Modifier.fillMaxSize().testTag("touchpad").simulatedTouchpad(
                enabled = true, density = 1f,
                onMove = { x, y -> movements += Offset(x, y) },
                onClick = { clicks += it },
                onDragButton = { dragging = it },
                onScroll = { x, y -> scrolls += Offset(x, y) },
                onMenu = { menuCount++ },
                onRelease = { dragging = false }
            ))
        }
    }

    @Test fun oneFingerTapClicksAndSwipeMovesWithoutClicking() {
        showTouchpad()
        composeRule.onNodeWithTag("touchpad").performTouchInput { click(center) }
        composeRule.runOnIdle { assertEquals(listOf(1), clicks); clicks.clear() }
        composeRule.onNodeWithTag("touchpad").performTouchInput {
            down(center)
            moveBy(Offset(90f, 0f))
            up()
        }
        composeRule.runOnIdle {
            assertTrue(movements.sumOf { it.x.toDouble() } > 0)
            assertTrue(clicks.isEmpty())
        }
    }

    @Test fun twoFingerTapRightClicksAndSwipeScrollsWithoutOpeningMenu() {
        showTouchpad()
        composeRule.onNodeWithTag("touchpad").performTouchInput {
            down(0, center - Offset(40f, 0f))
            down(1, center + Offset(40f, 0f))
            up(0)
            up(1)
        }
        composeRule.runOnIdle { assertEquals(listOf(2), clicks); clicks.clear() }
        composeRule.onNodeWithTag("touchpad").performTouchInput {
            down(0, center - Offset(40f, 0f))
            down(1, center + Offset(40f, 0f))
            moveBy(0, Offset(0f, -100f))
            moveBy(1, Offset(0f, -100f))
            up(0)
            up(1)
        }
        composeRule.runOnIdle {
            assertTrue(scrolls.any { it.y < 0 })
            assertTrue(clicks.isEmpty())
            assertTrue(movements.isEmpty())
            assertEquals(0, menuCount)
        }
    }

    @Test fun threeFingersOpenMenuWithoutRemoteClick() {
        showTouchpad()
        composeRule.onNodeWithTag("touchpad").performTouchInput {
            down(0, center - Offset(60f, 0f))
            down(1, center)
            down(2, center + Offset(60f, 0f))
            up(0); up(1); up(2)
        }
        composeRule.runOnIdle { assertEquals(1, menuCount); assertTrue(clicks.isEmpty()) }
    }

    @Test fun holdingOneFingerStartsDragAndLiftingReleasesIt() {
        showTouchpad()
        composeRule.onNodeWithTag("touchpad").performTouchInput { down(center) }
        composeRule.waitUntil(2500) { dragging }
        composeRule.onNodeWithTag("touchpad").performTouchInput {
            moveBy(Offset(80f, 0f))
            up()
        }
        composeRule.runOnIdle {
            assertFalse(dragging)
            assertTrue(movements.isNotEmpty())
            assertTrue(clicks.isEmpty())
        }
    }
}
