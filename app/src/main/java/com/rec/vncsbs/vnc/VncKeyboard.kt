package com.rec.vncsbs.vnc

import android.view.KeyEvent

internal fun encodeKeyEvent(keysym: Int, down: Boolean) = byteArrayOf(
    4, if (down) 1 else 0, 0, 0,
    (keysym ushr 24).toByte(), (keysym ushr 16).toByte(),
    (keysym ushr 8).toByte(), keysym.toByte()
)

internal fun vncKeysym(event: KeyEvent): Int? {
    val special = when (event.keyCode) {
        KeyEvent.KEYCODE_DEL -> 0xff08
        KeyEvent.KEYCODE_TAB -> 0xff09
        KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> 0xff0d
        KeyEvent.KEYCODE_ESCAPE -> 0xff1b
        KeyEvent.KEYCODE_FORWARD_DEL -> 0xffff
        KeyEvent.KEYCODE_INSERT -> 0xff63
        KeyEvent.KEYCODE_MOVE_HOME -> 0xff50
        KeyEvent.KEYCODE_MOVE_END -> 0xff57
        KeyEvent.KEYCODE_DPAD_LEFT -> 0xff51
        KeyEvent.KEYCODE_DPAD_UP -> 0xff52
        KeyEvent.KEYCODE_DPAD_RIGHT -> 0xff53
        KeyEvent.KEYCODE_DPAD_DOWN -> 0xff54
        KeyEvent.KEYCODE_PAGE_UP -> 0xff55
        KeyEvent.KEYCODE_PAGE_DOWN -> 0xff56
        KeyEvent.KEYCODE_SHIFT_LEFT -> 0xffe1
        KeyEvent.KEYCODE_SHIFT_RIGHT -> 0xffe2
        KeyEvent.KEYCODE_CTRL_LEFT -> 0xffe3
        KeyEvent.KEYCODE_CTRL_RIGHT -> 0xffe4
        KeyEvent.KEYCODE_ALT_LEFT -> 0xffe9
        KeyEvent.KEYCODE_ALT_RIGHT -> 0xffea
        KeyEvent.KEYCODE_META_LEFT -> 0xffeb
        KeyEvent.KEYCODE_META_RIGHT -> 0xffec
        KeyEvent.KEYCODE_CAPS_LOCK -> 0xffe5
        KeyEvent.KEYCODE_NUM_LOCK -> 0xff7f
        KeyEvent.KEYCODE_SCROLL_LOCK -> 0xff14
        KeyEvent.KEYCODE_SYSRQ -> 0xff61
        KeyEvent.KEYCODE_BREAK -> 0xff13
        in KeyEvent.KEYCODE_F1..KeyEvent.KEYCODE_F12 -> 0xffbe + event.keyCode - KeyEvent.KEYCODE_F1
        else -> null
    }
    if (special != null) return special
    // Shortcut modifiers are sent as separate key events. Android character maps
    // can return zero or an unrelated character for Alt/Ctrl/Meta combinations.
    // Keep Shift and lock states so the remote keysym still has the correct case.
    val shortcutModifiers = KeyEvent.META_CTRL_MASK or KeyEvent.META_ALT_MASK or KeyEvent.META_META_MASK
    val char = event.getUnicodeChar(event.metaState and shortcutModifiers.inv())
    if (char == 0 || char and android.view.KeyCharacterMap.COMBINING_ACCENT != 0) return null
    return if (char <= 0xff) char else 0x01000000 or char
}
