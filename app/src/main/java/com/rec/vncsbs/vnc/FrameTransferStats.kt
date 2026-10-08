package com.rec.vncsbs.vnc

import java.io.FilterInputStream
import java.io.InputStream

/** RFB bytes consumed per update, including headers (excluding TCP/IP overhead). */
data class FrameTransferStats(
    val wireBytes: Long,
    val rawPixelBytes: Long,
    val tightRectangles: Int,
    val rawRectangles: Int
)

internal class CountingInputStream(input: InputStream) : FilterInputStream(input) {
    var bytesRead = 0L
        private set
    override fun read(): Int = `in`.read().also { if (it >= 0) bytesRead++ }
    override fun read(bytes: ByteArray, offset: Int, length: Int): Int =
        `in`.read(bytes, offset, length).also { if (it > 0) bytesRead += it }
}
