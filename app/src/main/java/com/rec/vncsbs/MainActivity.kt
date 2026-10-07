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
                if (!event.isCtrlPressed && !event.isAltPressed && !event.isMetaPressed) {
                    when (event.unicodeChar) {
                        'R'.code, 'H'.code, 'L'.code, 'J'.code, 'K'.code,
                        '0'.code, '*'.code, '#'.code, '$'.code ->
                            onDisplayAdjustment?.invoke(event.unicodeChar.toChar())
                        'u'.code -> onZoomChange?.invoke(10)
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
    var onDisplayAdjustment: ((Char) -> Unit)? = null

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (handleLeaderKeyEvent(event)) return true
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
