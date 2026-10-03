package com.rec.vncsbs

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import org.junit.Rule
import org.junit.Test

class MenuGestureTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun twoFingerTouchOpensAndClosesMenuWithConnectButton() {
        composeRule.onNodeWithText("VNC CONNECT").assertIsDisplayed()
        composeRule.onNodeWithText("allPad").assertDoesNotExist()

        toggleWithTwoFingers()
        composeRule.onNodeWithText("allPad").assertIsDisplayed()

        toggleWithTwoFingers()
        composeRule.onNodeWithText("allPad").assertDoesNotExist()
        composeRule.onNodeWithText("VNC CONNECT").assertIsDisplayed()
    }

    private fun toggleWithTwoFingers() {
        composeRule.onRoot().performTouchInput {
            down(0, Offset(width * 0.8f, height * 0.7f))
            down(1, Offset(width * 0.9f, height * 0.7f))
            up(0)
            up(1)
        }
        composeRule.waitForIdle()
    }
}
