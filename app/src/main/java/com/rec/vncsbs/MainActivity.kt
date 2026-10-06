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

class MainActivity : ComponentActivity() {

    private lateinit var vncViewModel: VncViewModel
    var leadKB by mutableStateOf(false)
        private set

    fun centerViewport() {
        onCenterPan?.invoke()
    }

    fun handleLeaderKeyEvent(event: KeyEvent): Boolean {
        if (leadKB) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_ESCAPE -> leadKB = false
                    KeyEvent.KEYCODE_C -> centerViewport()
                }
            }
            return true
        }
        // Accept both a dedicated @ key and the @ character produced by a keyboard layout.
        if (event.isShiftPressed &&
            (event.keyCode == KeyEvent.KEYCODE_AT || event.unicodeChar == '@'.code)
        ) {
            if (event.action == KeyEvent.ACTION_DOWN) leadKB = true
            return true
        }
        return false
    }

    var onToggleMenu: (() -> Unit)? = null
    var onCenterPan: (() -> Unit)? = null
    var onToggleGyroPan: (() -> Unit)? = null
    var onToggleGyroAutoCenter: (() -> Unit)? = null
    var onZoomChange: ((Int) -> Unit)? = null
    var onPanChange: ((Int, Int) -> Unit)? = null
    var onPanSensitivityChange: ((Int) -> Unit)? = null

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (handleLeaderKeyEvent(event)) return true
        if (event.isCtrlPressed && event.isShiftPressed && event.keyCode in setOf(
                KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_U, KeyEvent.KEYCODE_I,
                KeyEvent.KEYCODE_J, KeyEvent.KEYCODE_K, KeyEvent.KEYCODE_H, KeyEvent.KEYCODE_L,
                KeyEvent.KEYCODE_Y, KeyEvent.KEYCODE_O, KeyEvent.KEYCODE_P, KeyEvent.KEYCODE_N, KeyEvent.KEYCODE_M
            )) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_W -> onToggleMenu?.invoke()
                    KeyEvent.KEYCODE_M -> centerViewport()
                    KeyEvent.KEYCODE_P -> onToggleGyroPan?.invoke()
                    KeyEvent.KEYCODE_N -> onToggleGyroAutoCenter?.invoke()
                    KeyEvent.KEYCODE_Y -> onPanSensitivityChange?.invoke(10)
                    KeyEvent.KEYCODE_O -> onPanSensitivityChange?.invoke(-10)
                    KeyEvent.KEYCODE_U -> onZoomChange?.invoke(10)
                    KeyEvent.KEYCODE_I -> onZoomChange?.invoke(-10)
                    KeyEvent.KEYCODE_J -> onPanChange?.invoke(0, 1)
                    KeyEvent.KEYCODE_K -> onPanChange?.invoke(0, -1)
                    KeyEvent.KEYCODE_H -> onPanChange?.invoke(-1, 0)
                    KeyEvent.KEYCODE_L -> onPanChange?.invoke(1, 0)
                }
            }
            return true
        }
        return super.dispatchKeyEvent(event)
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
