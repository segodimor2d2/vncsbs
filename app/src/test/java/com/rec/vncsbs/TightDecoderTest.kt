package com.rec.vncsbs

import com.rec.vncsbs.vnc.TightDecoder
import com.rec.vncsbs.vnc.tightEncodings
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.zip.Deflater
import org.junit.Assert.*
import org.junit.Test

class TightDecoderTest {
    private fun decoder() = TightDecoder { _, _, _ -> error("Unexpected JPEG") }
    private fun bytes(vararg values: Int) = values.map { it.toByte() }.toByteArray()

    @Test fun fillCopyPaletteAndGradient() {
        decoder().use { d ->
            assertArrayEquals(intArrayOf(0xff123456.toInt()), d.readRectangle(ByteArrayInputStream(bytes(128, 18, 52, 86)), 1, 1))
            assertArrayEquals(intArrayOf(0xffff0000.toInt(), 0xff00ff00.toInt()), d.readRectangle(ByteArrayInputStream(bytes(0, 255, 0, 0, 0, 255, 0)), 2, 1))
            assertArrayEquals(intArrayOf(0xffff0000.toInt(), 0xff00ff00.toInt(), 0xff00ff00.toInt(), 0xffff0000.toInt()),
                d.readRectangle(ByteArrayInputStream(bytes(64, 1, 1, 255, 0, 0, 0, 255, 0, 64, 128)), 2, 2))
            assertArrayEquals(intArrayOf(0xff0a141e.toInt(), 0xff0b1621.toInt()),
                d.readRectangle(ByteArrayInputStream(bytes(64, 2, 10, 20, 30, 1, 2, 3)), 2, 1))
        }
    }

    @Test fun persistentZlibStreamsAndReset() {
        decoder().use { d ->
            val compressor = Deflater()
            try {
                repeat(3) { index ->
                    if (index == 2) compressor.reset()
                    val raw = ByteArray(12) { (it + index).toByte() }
                    compressor.setInput(raw)
                    val output = ByteArray(128)
                    val size = compressor.deflate(output, 0, output.size, Deflater.SYNC_FLUSH)
                    val packet = ByteArrayOutputStream().apply {
                        write(if (index == 2) 1 else 0)
                        write(size)
                        write(output, 0, size)
                    }.toByteArray()
                    val result = d.readRectangle(ByteArrayInputStream(packet), 4, 1)
                    assertEquals(0xff000000.toInt() or (index shl 16) or ((index + 1) shl 8) or (index + 2), result[0])
                }
            } finally { compressor.end() }
        }
    }

    @Test fun jpegPayloadIsPassedToDecoder() {
        TightDecoder { data, w, h ->
            assertArrayEquals(bytes(1, 2, 3), data)
            assertEquals(1, w); assertEquals(1, h)
            intArrayOf(0xffabcdef.toInt())
        }.use { d -> assertEquals(0xffabcdef.toInt(), d.readRectangle(ByteArrayInputStream(bytes(144, 3, 1, 2, 3)), 1, 1)[0]) }
    }

    @Test fun rejectsTruncatedDataAndInvalidPaletteIndex() {
        decoder().use { d ->
            assertThrows(java.io.EOFException::class.java) { d.readRectangle(ByteArrayInputStream(bytes(0, 1)), 1, 1) }
            assertThrows(IllegalArgumentException::class.java) { d.readRectangle(ByteArrayInputStream(bytes(64, 1, 0, 1, 2, 3, 1)), 1, 1) }
        }
    }

    @Test fun negotiatesTightRawCompressionAndQuality() {
        val buffer = ByteBuffer.wrap(tightEncodings(6))
        assertEquals(2, buffer.get().toInt()); buffer.get()
        assertEquals(4, buffer.short.toInt())
        assertEquals(7, buffer.int); assertEquals(0, buffer.int)
        assertEquals(-250, buffer.int); assertEquals(-26, buffer.int)
        assertEquals(16, tightEncodings(-1).size)
    }
}
