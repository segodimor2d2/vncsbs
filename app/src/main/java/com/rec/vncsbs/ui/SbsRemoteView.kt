package com.rec.vncsbs.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
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
    modifier: Modifier = Modifier
) {
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
            modifier = modifier.fillMaxSize()
        ) {
            Image(
                bitmap = currentBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .padding(4.dp),
                contentScale = ContentScale.FillBounds
            )

            Image(
                bitmap = currentBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .padding(4.dp),
                contentScale = ContentScale.FillBounds
            )
        }
    }
}
