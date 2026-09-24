package com.rec.vncsbs.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.rec.vncsbs.viewmodel.VncViewModel

@Composable
fun VncScreen(
    viewModel: VncViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        SbsRemoteView(
            frame = uiState.frame,
            modifier = Modifier.fillMaxSize()
        )

        Button(
            onClick = {
                viewModel.testVncConnection()
            }
        ) {
            Text("VNC CONNECT")
        }
    }
}
