package com.rec.vncsbs

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rec.vncsbs.ui.VncScreen
import com.rec.vncsbs.viewmodel.VncViewModel
import com.rec.vncsbs.vnc.vncKeysym

class MainActivity : ComponentActivity() {

    private lateinit var vncViewModel: VncViewModel
    private val remotePressedKeys = mutableMapOf<Pair<Int, Int>, Int>()
    private val shiftPressTimes = mutableMapOf<Pair<Int, Int>, Long>()
    var remoteKeyboardEnabled = false
        set(value) {
            if (field && !value) releaseRemoteKeys()
            field = value
        }

    private fun releaseRemoteKeys() {
        remotePressedKeys.values.forEach { vncViewModel.sendKeyEvent(it, false) }
        remotePressedKeys.clear()
    }
    var leadKB by mutableStateOf(false)
        private set

    fun centerViewport() {
        onCenterPan?.invoke()
    }

    fun handleLeaderKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_SHIFT_LEFT || event.keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT) {
            val key = event.deviceId to event.keyCode
            when (event.action) {
                KeyEvent.ACTION_DOWN -> shiftPressTimes.putIfAbsent(key, event.eventTime)
                KeyEvent.ACTION_UP -> shiftPressTimes.remove(key)
            }
        }
        if (leadKB) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_ESCAPE -> leadKB = false
                    KeyEvent.KEYCODE_C -> centerViewport()
                }
                if (!event.isCtrlPressed && !event.isAltPressed && !event.isMetaPressed) {
                    when (event.unicodeChar) {
                        // F alterna a captura; ao desativar, essa tecla libera o mouse.
                        'F'.code -> onToggleMouseCapture?.invoke()
                        'R'.code, 'H'.code, 'L'.code, 'J'.code, 'K'.code,
                        '0'.code, '*'.code, '#'.code, '$'.code ->
                            onDisplayAdjustment?.invoke(event.unicodeChar.toChar())
                        'u'.code -> onZoomChange?.invoke(10)
                        'h'.code -> onPanChange?.invoke(1, 0)
                        'l'.code -> onPanChange?.invoke(-1, 0)
                        'j'.code -> onPanChange?.invoke(0, -1)
                        'k'.code -> onPanChange?.invoke(0, 1)
                        'i'.code -> onZoomChange?.invoke(-10)
                        'y'.code -> onPanSensitivityChange?.invoke(10)
                        'o'.code -> onPanSensitivityChange?.invoke(-10)
                        'g'.code -> onToggleGyroPan?.invoke()
                        'w'.code -> onToggleMenu?.invoke()
                        't'.code -> onToggleGyroAutoCenter?.invoke()
                    }
                }
            }
            return true
        }
        // A firmware-generated @ may include Shift in the same input report.
        // Require Shift to have arrived earlier for character-based keyboard layouts.
        val shiftPressedAt = shiftPressTimes
            .filterKeys { it.first == event.deviceId }
            .values.minOrNull()
        if (shouldActivateLeader(
                isAtKey = event.keyCode == KeyEvent.KEYCODE_AT,
                isAtCharacter = event.unicodeChar == '@'.code,
                shiftPressed = event.isShiftPressed,
                eventTime = event.downTime,
                shiftPressedAt = shiftPressedAt
            )) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                releaseRemoteKeys()
                leadKB = true
            }
            return true
        }
        return false
    }

    var onToggleMenu: (() -> Unit)? = null
    var onToggleMouseCapture: (() -> Unit)? = null
    var onCenterPan: (() -> Unit)? = null
    var onToggleGyroPan: (() -> Unit)? = null
    var onToggleGyroAutoCenter: (() -> Unit)? = null
    var onZoomChange: ((Int) -> Unit)? = null
    var onPanChange: ((Int, Int) -> Unit)? = null
    var onPanSensitivityChange: ((Int) -> Unit)? = null
    var onDisplayAdjustment: ((Char) -> Unit)? = null

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (handleLeaderKeyEvent(event)) return true
        val key = event.deviceId to event.keyCode
        if (event.action == KeyEvent.ACTION_UP) {
            remotePressedKeys.remove(key)?.let {
                vncViewModel.sendKeyEvent(it, false)
                return true
            }
        }
        if (remoteKeyboardEnabled && event.action == KeyEvent.ACTION_DOWN) {
            val keysym = remotePressedKeys[key] ?: vncKeysym(event)
            if (keysym != null) {
                remotePressedKeys[key] = keysym
                vncViewModel.sendKeyEvent(keysym, true)
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) {
            releaseRemoteKeys()
            shiftPressTimes.clear()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        vncViewModel = ViewModelProvider(this)[VncViewModel::class.java]

        setContent {
            VncScreen(
                viewModel = vncViewModel
            )
        }
    }
}
