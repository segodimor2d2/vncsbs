package com.rec.vncsbs

import com.rec.vncsbs.vnc.RemoteFramebuffer
import java.io.ByteArrayInputStream
import org.junit.Assert.*
import org.junit.Test

class RemoteFramebufferTest {
    private fun pixels(vararg colors: Int): ByteArray = colors.flatMap {
        listOf(it.toByte(), (it shr 8).toByte(), (it shr 16).toByte(), 0.toByte())
    }.toByteArray()

    private fun snapshot(buffer: RemoteFramebuffer): IntArray {
        val result = IntArray(buffer.width * buffer.height)
        buffer.readChanges(-1) { source, row, count ->
            source.copyInto(result, row * buffer.width, row * buffer.width,
                (row + count) * buffer.width)
        }
        return result
    }

    @Test fun preservesRgbAndForcesOpaqueAlpha() {
        val buffer = RemoteFramebuffer(3, 1)
        buffer.readRectangle(ByteArrayInputStream(pixels(0xff0000, 0x00ff00, 0x0000ff)), 0, 0, 3, 1)
        buffer.commit()
        assertArrayEquals(intArrayOf(0xffff0000.toInt(), 0xff00ff00.toInt(), 0xff0000ff.toInt()), snapshot(buffer))
    }

    @Test fun publishesOnlyCompleteUpdatesAndPreservesOtherPixels() {
        val buffer = RemoteFramebuffer(3, 2)
        val black = snapshot(buffer)
        buffer.readRectangle(ByteArrayInputStream(pixels(0xff0000)), 1, 0, 1, 1)
        assertArrayEquals(black, snapshot(buffer))
        buffer.readRectangle(ByteArrayInputStream(pixels(0x0000ff)), 2, 1, 1, 1)
        buffer.commit()
        black[1] = 0xffff0000.toInt()
        black[5] = 0xff0000ff.toInt()
        assertArrayEquals(black, snapshot(buffer))
    }

    @Test fun skippedFramesKeepAllChangesAndReadersTrackTheirOwnRevision() {
        val buffer = RemoteFramebuffer(2, 3)
        buffer.readRectangle(ByteArrayInputStream(pixels(0xff0000)), 0, 0, 1, 1)
        val first = buffer.commit()
        buffer.readRectangle(ByteArrayInputStream(pixels(0x00ff00)), 1, 2, 1, 1)
        val latest = buffer.commit()
        val rows = mutableListOf<Int>()
        assertEquals(latest, buffer.readChanges(0) { _, row, count ->
            rows.addAll(row until row + count)
        })
        assertEquals(listOf(0, 2), rows)
        rows.clear()
        buffer.readChanges(first) { _, row, count -> rows.addAll(row until row + count) }
        assertEquals(listOf(2), rows)
        buffer.readChanges(latest) { _, _, _ -> fail("No pixels changed") }
        assertEquals(0xffff0000.toInt(), snapshot(buffer)[0])
        assertEquals(0xff00ff00.toInt(), snapshot(buffer)[5])
    }

    @Test fun handlesFragmentedNetworkReads() {
        val buffer = RemoteFramebuffer(2, 1)
        val input = object : ByteArrayInputStream(pixels(0x123456, 0xabcdef)) {
            override fun read(bytes: ByteArray, offset: Int, length: Int): Int =
                super.read(bytes, offset, minOf(length, 1))
        }
        buffer.readRectangle(input, 0, 0, 2, 1)
        buffer.commit()
        assertArrayEquals(intArrayOf(0xff123456.toInt(), 0xffabcdef.toInt()), snapshot(buffer))
    }

    @Test fun rejectsOutOfBoundsRectanglesBeforeReading() {
        val buffer = RemoteFramebuffer(2, 2)
        val input = ByteArrayInputStream(pixels(0xffffff))
        assertThrows(IllegalArgumentException::class.java) {
            buffer.readRectangle(input, 2, 0, 1, 1)
        }
        assertEquals(4, input.available())
    }

    @Test fun truncatedUpdateNeverPublishesPartialPixels() {
        val buffer = RemoteFramebuffer(1, 2)
        assertThrows(IllegalStateException::class.java) {
            buffer.readRectangle(ByteArrayInputStream(pixels(0xffffff)), 0, 0, 1, 2)
        }
        assertArrayEquals(intArrayOf(0xff000000.toInt(), 0xff000000.toInt()), snapshot(buffer))
    }
    @Test fun concurrentReadersNeverSeeHalfAnUpdate() {
        val buffer = RemoteFramebuffer(2, 1)
        val writer = Thread {
            repeat(500) { index ->
                val color = if (index % 2 == 0) 0xff0000 else 0x00ff00
                buffer.readRectangle(ByteArrayInputStream(pixels(color, color)), 0, 0, 2, 1)
                buffer.commit()
            }
        }
        writer.start()
        try {
            repeat(500) {
                val image = snapshot(buffer)
                assertEquals(image[0], image[1])
            }
        } finally {
            writer.join()
        }
    }

}
