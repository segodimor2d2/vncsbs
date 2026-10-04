package com.rec.vncsbs

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.rec.vncsbs.ui.VncScreen
import com.rec.vncsbs.viewmodel.VncViewModel

class MainActivity : ComponentActivity() {

    private lateinit var vncViewModel: VncViewModel
    var onToggleMenu: (() -> Unit)? = null
    var onZoomChange: ((Int) -> Unit)? = null
    var onPanChange: ((Int, Int) -> Unit)? = null
    var onPanSensitivityChange: ((Int) -> Unit)? = null

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.isCtrlPressed && event.isShiftPressed && event.keyCode in setOf(
                KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_U, KeyEvent.KEYCODE_I,
                KeyEvent.KEYCODE_J, KeyEvent.KEYCODE_K, KeyEvent.KEYCODE_H, KeyEvent.KEYCODE_L,
                KeyEvent.KEYCODE_Y, KeyEvent.KEYCODE_O
            )) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_W -> onToggleMenu?.invoke()
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
