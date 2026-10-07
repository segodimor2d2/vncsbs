package com.rec.vncsbs

import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rec.vncsbs.vnc.vncKeysym
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VncKeyboardModifierTest {
    private val keyboardId = InputDevice.getDeviceIds().firstOrNull {
        InputDevice.getDevice(it)?.name?.contains("REC_Corne") == true
    } ?: KeyCharacterMap.VIRTUAL_KEYBOARD

    private fun key(code: Int, modifiers: Int, action: Int = KeyEvent.ACTION_DOWN) =
        KeyEvent(100, 100, action, code, 0, modifiers, keyboardId, 0)

    @Test fun altJKRemainLettersForBothPressAndRelease() {
        val alt = KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
        for ((code, char) in listOf(KeyEvent.KEYCODE_J to 'j', KeyEvent.KEYCODE_K to 'k')) {
            assertEquals(char.code, vncKeysym(key(code, alt)))
            assertEquals(char.code, vncKeysym(key(code, alt, KeyEvent.ACTION_UP)))
            assertEquals(char.uppercaseChar().code, vncKeysym(key(code, alt or KeyEvent.META_SHIFT_ON)))
        }
    }

    @Test fun ctrlAndMetaCombinationsRemainLetters() {
        assertEquals('j'.code, vncKeysym(key(KeyEvent.KEYCODE_J,
            KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON)))
        assertEquals('k'.code, vncKeysym(key(KeyEvent.KEYCODE_K,
            KeyEvent.META_META_ON or KeyEvent.META_META_LEFT_ON)))
    }

    @Test fun altIsStillSentAsItsOwnKeysym() {
        assertEquals(0xffe9, vncKeysym(key(KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.META_ALT_ON)))
        assertEquals(0xffe9, vncKeysym(key(KeyEvent.KEYCODE_ALT_LEFT, 0, KeyEvent.ACTION_UP)))
    }
}
