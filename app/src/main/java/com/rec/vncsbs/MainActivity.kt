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

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.isCtrlPressed && event.isShiftPressed && event.keyCode in setOf(
                KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_J, KeyEvent.KEYCODE_K
            )) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_W -> onToggleMenu?.invoke()
                    KeyEvent.KEYCODE_J -> onZoomChange?.invoke(10)
                    KeyEvent.KEYCODE_K -> onZoomChange?.invoke(-10)
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
