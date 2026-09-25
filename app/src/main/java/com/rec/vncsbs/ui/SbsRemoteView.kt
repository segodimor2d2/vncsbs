package com.rec.vncsbs.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

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
    println(
        "SbsRemoteView: FRAME ${frame.width}x${frame.height} " +
        "${frame.pixels.size} bytes"
    )

    val bitmap = run {
        if (
            frame.width > 0 &&
            frame.height > 0 &&
            frame.pixels.size >= frame.width * frame.height * 4
        ) {
            val colors = IntArray(
                frame.width * frame.height
            )

            for (y in 0 until frame.height) {
                for (x in 0 until frame.width) {

                    val pixelIndex =
                        (y * frame.width + x) * 4

                    val b =
                        frame.pixels[pixelIndex].toInt() and 0xFF

                    val g =
                        frame.pixels[pixelIndex + 1].toInt() and 0xFF

                    val r =
                        frame.pixels[pixelIndex + 2].toInt() and 0xFF

                    val a = 255

                    colors[y * frame.width + x] =
                        (a shl 24) or
                        (r shl 16) or
                        (g shl 8) or
                        b
                }
            }

            Bitmap.createBitmap(
                colors,
                frame.width,
                frame.height,
                Bitmap.Config.ARGB_8888
            )
        } else {
            null
        }
    }

    if (bitmap != null) {
        Row(
            modifier = modifier.fillMaxSize()
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .padding(4.dp),
                contentScale = ContentScale.FillBounds
            )

            Image(
                bitmap = bitmap.asImageBitmap(),
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
