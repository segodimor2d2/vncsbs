package com.rec.vncsbs.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SbsRemoteView(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxSize()
    ) {
        RemoteView(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .padding(4.dp)
        )

        RemoteView(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .padding(4.dp)
        )
    }
}

@Composable
private fun RemoteView(
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
    ) {
        drawRect(
            color = Color.DarkGray
        )

        drawCircle(
            color = Color.White,
            radius = size.minDimension * 0.18f,
            center = Offset(
                x = size.width / 2f,
                y = size.height / 2f
            )
        )

        drawLine(
            color = Color.White,
            start = Offset(
                x = 0f,
                y = size.height / 2f
            ),
            end = Offset(
                x = size.width,
                y = size.height / 2f
            ),
            strokeWidth = 4f
        )

        drawLine(
            color = Color.White,
            start = Offset(
                x = size.width / 2f,
                y = 0f
            ),
            end = Offset(
                x = size.width / 2f,
                y = size.height
            ),
            strokeWidth = 4f
        )
    }
}
