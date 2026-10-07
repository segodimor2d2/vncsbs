package com.rec.vncsbs.ui

import android.graphics.Bitmap
import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext

data class RemoteFrame(
    val width: Int = 0,
    val height: Int = 0,
    val pixels: ByteArray = ByteArray(0)
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

    val frameChannel = remember {
        Channel<RemoteFrame>(Channel.CONFLATED)
    }

    var bitmap by remember {
        mutableStateOf<Bitmap?>(null)
    }

    LaunchedEffect(frame) {
        frameChannel.trySend(frame)
    }

    LaunchedEffect(Unit) {

        for (currentFrame in frameChannel) {

            if (
                currentFrame.width <= 0 ||
                currentFrame.height <= 0 ||
                currentFrame.pixels.size <
                    currentFrame.width *
                    currentFrame.height *
                    4
            ) {
                continue
            }

            val newBitmap = withContext(Dispatchers.Default) {

                val colors = IntArray(
                    currentFrame.width *
                    currentFrame.height
                )

                for (y in 0 until currentFrame.height) {
                    for (x in 0 until currentFrame.width) {

                        val pixelIndex =
                            (y * currentFrame.width + x) * 4

                        val b =
                            currentFrame.pixels[pixelIndex]
                                .toInt() and 0xFF

                        val g =
                            currentFrame.pixels[pixelIndex + 1]
                                .toInt() and 0xFF

                        val r =
                            currentFrame.pixels[pixelIndex + 2]
                                .toInt() and 0xFF

                        colors[y * currentFrame.width + x] =
                            (255 shl 24) or
                            (r shl 16) or
                            (g shl 8) or
                            b
                    }
                }

                Bitmap.createBitmap(
                    currentFrame.width,
                    currentFrame.height,
                    Bitmap.Config.ARGB_8888
                ).apply {
                    setPixels(
                        colors,
                        0,
                        currentFrame.width,
                        0,
                        0,
                        currentFrame.width,
                        currentFrame.height
                    )
                }
            }

            bitmap = newBitmap
        }
    }

    val currentBitmap = bitmap

    if (currentBitmap != null) {
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
                    Image(
                        bitmap = currentBitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = zoom
                                scaleY = zoom
                                translationX = panX.toPx()
                                translationY = panY.toPx()
                            },
                        contentScale = ContentScale.Fit
                    )
                    if (leadKB) {
                        val aspect = currentBitmap.width.toFloat() / currentBitmap.height
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
