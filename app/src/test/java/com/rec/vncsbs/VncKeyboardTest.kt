package com.rec.vncsbs

import com.rec.vncsbs.vnc.encodeKeyEvent
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class VncKeyboardTest {
    @Test fun encodesPressAndRelease() {
        assertArrayEquals(byteArrayOf(4, 1, 0, 0, 0, 0, 0, 97), encodeKeyEvent(97, true))
        assertArrayEquals(byteArrayOf(4, 0, 0, 0, 0, 0, 0xff.toByte(), 0xe3.toByte()),
            encodeKeyEvent(0xffe3, false))
    }

    @Test fun encodesUnicodeKeysymInNetworkByteOrder() {
        assertArrayEquals(byteArrayOf(4, 1, 0, 0, 1, 1, 0xf6.toByte(), 0),
            encodeKeyEvent(0x0101f600, true))
    }
}
