package com.rec.vncsbs.ui

import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo

internal class RemoteInputConnection(
    view: View,
    private val commit: (String) -> Unit,
    private val keyEvent: (KeyEvent) -> Boolean
) : BaseInputConnection(view, true) {
    override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
        text?.toString()?.takeIf { it.isNotEmpty() }?.let(commit)
        editable?.clear()
        return true
    }

    override fun finishComposingText(): Boolean {
        val pending = editable?.toString().orEmpty()
        if (pending.isNotEmpty()) commit(pending)
        editable?.clear()
        return super.finishComposingText()
    }

    override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
        if (!editable.isNullOrEmpty()) return super.deleteSurroundingText(beforeLength, afterLength)
        repeat(beforeLength.coerceIn(0, 100)) { press(KeyEvent.KEYCODE_DEL) }
        repeat(afterLength.coerceIn(0, 100)) { press(KeyEvent.KEYCODE_FORWARD_DEL) }
        return true
    }

    override fun deleteSurroundingTextInCodePoints(beforeLength: Int, afterLength: Int): Boolean {
        if (!editable.isNullOrEmpty()) return super.deleteSurroundingTextInCodePoints(beforeLength, afterLength)
        return deleteSurroundingText(beforeLength, afterLength)
    }

    override fun sendKeyEvent(event: KeyEvent): Boolean = keyEvent(event)

    override fun performEditorAction(actionCode: Int): Boolean {
        if (actionCode == EditorInfo.IME_ACTION_NONE || actionCode == EditorInfo.IME_ACTION_UNSPECIFIED) {
            press(KeyEvent.KEYCODE_ENTER)
            return true
        }
        return false
    }

    private fun press(code: Int) {
        keyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        keyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
    }
}
