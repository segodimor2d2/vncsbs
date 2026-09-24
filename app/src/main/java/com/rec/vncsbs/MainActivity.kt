package com.rec.vncsbs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.rec.vncsbs.ui.VncScreen
import com.rec.vncsbs.viewmodel.VncViewModel

class MainActivity : ComponentActivity() {

    private lateinit var vncViewModel: VncViewModel

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
