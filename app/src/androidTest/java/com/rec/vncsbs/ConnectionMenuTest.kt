package com.rec.vncsbs

import android.view.KeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class ConnectionMenuTest {
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    private fun openConnectionMenu() {
        composeRule.runOnUiThread { composeRule.activity.onToggleMenu?.invoke() }
        composeRule.onNodeWithText("vnc connect").performClick()
        composeRule.onNodeWithTag("connection-server").assertIsDisplayed()
        composeRule.onNode(isDialog()).assertDoesNotExist()
    }

    @Test fun validatesInlineConnectionFields() {
        openConnectionMenu()
        composeRule.onNodeWithTag("connection-server").performTextReplacement("127.0.0.1")
        composeRule.onNodeWithTag("connection-port").performTextReplacement("0")
        composeRule.onNodeWithTag("connection-connect").assertIsNotEnabled()
        composeRule.onNodeWithTag("connection-port").performTextReplacement("5900")
        composeRule.onNodeWithTag("connection-connect").assertIsEnabled()
        composeRule.onNodeWithTag("connection-server").performTextClearance()
        composeRule.onNodeWithTag("connection-connect").assertIsNotEnabled()
        composeRule.onNodeWithText("close").performScrollTo().performClick()
        composeRule.onNodeWithTag("connection-server").assertDoesNotExist()
        composeRule.runOnIdle { assertFalse(composeRule.activity.localKeyboardInputFocused) }
    }

    @Test fun keyboardEditsFieldEvenWithLeaderEnabled() {
        openConnectionMenu()
        composeRule.runOnUiThread { composeRule.activity.toggleLeaderKeyboard() }
        composeRule.onNodeWithTag("connection-server").performTextClearance()
        composeRule.onNodeWithTag("connection-server").performClick()
        composeRule.runOnIdle {
            assertTrue(composeRule.activity.localKeyboardInputFocused)
            assertFalse(composeRule.activity.handleLeaderKeyEvent(
                KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_J)))
        }
        composeRule.onNodeWithTag("connection-server").performKeyInput { pressKey(Key.J) }
        composeRule.onNodeWithTag("connection-server").assertTextContains("j")
    }
}
