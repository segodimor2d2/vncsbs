package com.rec.vncsbs.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import com.rec.vncsbs.vnc.RemoteFramebuffer
import java.io.ByteArrayInputStream

data class RemoteFrame(
    val width: Int = 0,
    val height: Int = 0,
    val pixels: ByteArray = ByteArray(0),
    val framebuffer: RemoteFramebuffer? = null,
    val revision: Long = 0
)

@Composable
fun SbsRemoteView(
    frame: RemoteFrame,
    modifier: Modifier = Modifier,
    screenPadding: Dp = 4.dp,
    outerSidePadding: Dp = 0.dp,
    leftViewRightPadding: Dp = 0.dp,
    rightViewLeftPadding: Dp = 0.dp,
    zoom: Float = 1f,
    panX: Dp = 0.dp,
    panY: Dp = 0.dp,
    leadKB: Boolean = false
) {
    val screenCount = if (
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    ) 2 else 1

    val framebuffer = remember(frame.framebuffer, frame.width, frame.height, if (frame.framebuffer == null) frame.pixels else null) {
        frame.framebuffer ?: if (frame.width > 0 && frame.height > 0 &&
            frame.pixels.size.toLong() >= frame.width.toLong() * frame.height * 4) {
            RemoteFramebuffer(frame.width, frame.height).also {
                it.readRectangle(ByteArrayInputStream(frame.pixels), 0, 0, frame.width, frame.height)
                it.commit()
            }
        } else null
    }
    val renderer = remember(framebuffer) { framebuffer?.let { RemoteBitmapRenderer(it) } }

    if (renderer != null) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(screenPadding)
                .padding(horizontal = outerSidePadding)
        ) {
            repeat(screenCount) { index ->
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .padding(
                            start = if (screenCount == 2 && index == 1) {
                                rightViewLeftPadding
                            } else 0.dp,
                            end = if (screenCount == 2 && index == 0) {
                                leftViewRightPadding
                            } else 0.dp
                        )
                        .clipToBounds()
                ) {
                    AndroidView(
                        factory = { RemoteBitmapView(it) },
                        update = { it.update(renderer, frame.revision) },
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = zoom
                                scaleY = zoom
                                translationX = panX.toPx()
                                translationY = panY.toPx()
                            }
                    )
                    if (leadKB) {
                        val aspect = frame.width.toFloat() / frame.height
                        val fittedWidth = minOf(maxWidth, maxHeight * aspect)
                        val fittedHeight = fittedWidth / aspect
                        val imageLeft = (maxWidth - fittedWidth * zoom) / 2 + panX
                        val imageBottom = (maxHeight + fittedHeight * zoom) / 2 + panY
                        Text(
                            text = "@",
                            color = Color.White,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .offset(
                                    x = imageLeft.coerceIn(0.dp, maxWidth),
                                    y = (imageBottom - maxHeight).coerceIn(-maxHeight, 0.dp)
                                )
                                .background(Color.Black.copy(alpha = 0.8f))
                                .padding(6.dp)
                        )
                    }
                }
            }
        }
    }
}
