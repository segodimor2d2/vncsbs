package com.rec.vncsbs.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rec.vncsbs.viewmodel.VncViewModel

@Composable
fun VncScreen(
    viewModel: VncViewModel
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        SbsRemoteView(
            modifier = Modifier.fillMaxSize()
        )
    }
}
