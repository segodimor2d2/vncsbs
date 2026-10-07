package com.rec.vncsbs

import android.view.KeyEvent
import android.view.View
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.rec.vncsbs.ui.RemoteInputConnection
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AndroidKeyboardTest {
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    private fun keyboardVisible() = ViewCompat.getRootWindowInsets(composeRule.activity.window.decorView)
        ?.isVisible(WindowInsetsCompat.Type.ime()) == true

    @Test fun menuButtonShowsAndHidesAndroidKeyboard() {
        composeRule.runOnUiThread { composeRule.activity.onToggleMenu?.invoke() }
        composeRule.onNodeWithTag("android-keyboard-toggle").performScrollTo().performClick()
        composeRule.waitUntil(7000) { keyboardVisible() }
        composeRule.onNodeWithTag("android-keyboard-toggle").performScrollTo().performClick()
        composeRule.waitUntil(7000) { !keyboardVisible() }
    }

    @Test fun compositionCommitsOnceAndDeletesSendKeyPressAndRelease() {
        val commits = mutableListOf<String>()
        val keys = mutableListOf<Pair<Int, Int>>()
        composeRule.runOnUiThread {
            val connection = RemoteInputConnection(View(composeRule.activity),
                { commits += it }, { keys += it.keyCode to it.action; true })
            connection.setComposingText("a", 1)
            connection.setComposingText("á", 1)
            assertEquals(emptyList<String>(), commits)
            connection.commitText("á", 1)
            connection.finishComposingText()
            assertEquals(listOf("á"), commits)
            connection.deleteSurroundingText(1, 0)
            assertEquals(listOf(KeyEvent.KEYCODE_DEL to KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_DEL to KeyEvent.ACTION_UP), keys)
        }
    }
}
