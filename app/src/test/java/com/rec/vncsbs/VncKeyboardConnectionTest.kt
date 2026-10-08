package com.rec.vncsbs

import com.rec.vncsbs.vnc.VncClient
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class VncKeyboardConnectionTest {
    @Test fun sendsKeysWhileWaitingForFramebuffer() {
        ServerSocket(0).use { server ->
            val connected = CountDownLatch(1)
            val frameReceived = CountDownLatch(1)
            var receivedPixel = 0
            val client = VncClient(onConnectionChanged = { if (it) connected.countDown() }) { frame ->
                frame.framebuffer?.readChanges(-1) { pixels, _, _ -> receivedPixel = pixels[0] }
                frameReceived.countDown()
            }
            try {
                client.connect("127.0.0.1", server.localPort)
                server.soTimeout = 5000
                server.accept().use { socket ->
                    socket.soTimeout = 5000
                    val input = DataInputStream(socket.getInputStream())
                    val output = DataOutputStream(socket.getOutputStream())
                    fun read(size: Int) = ByteArray(size).also { input.readFully(it) }
                    output.write("RFB 003.008\n".toByteArray())
                    output.flush()
                    read(12)
                    output.write(byteArrayOf(1, 2))
                    output.flush()
                    read(1)
                    output.write(ByteArray(16))
                    output.flush()
                    read(16)
                    output.writeInt(0)
                    output.flush()
                    read(1)
                    output.writeShort(1)
                    output.writeShort(1)
                    output.write(ByteArray(16))
                    output.writeInt(0)
                    output.flush()
                    read(20) // SetPixelFormat
                    read(2) // SetEncodings type and padding
                    val encodingCount = input.readUnsignedShort()
                    read(encodingCount * 4)
                    read(10) // FramebufferUpdateRequest
                    assertTrue(connected.await(5, TimeUnit.SECONDS))
                    client.sendKeyEvent(0xffe3, true)
                    client.sendKeyEvent('a'.code, true)
                    client.sendKeyEvent('a'.code, false)
                    client.sendKeyEvent(0xffe3, false)
                    assertArrayEquals(byteArrayOf(4, 1, 0, 0, 0, 0, -1, -29), read(8))
                    assertArrayEquals(byteArrayOf(4, 1, 0, 0, 0, 0, 0, 97), read(8))
                    assertArrayEquals(byteArrayOf(4, 0, 0, 0, 0, 0, 0, 97), read(8))
                    assertArrayEquals(byteArrayOf(4, 0, 0, 0, 0, 0, -1, -29), read(8))
                    client.sendKeyEvent(0xffe9, true)
                    client.sendKeyEvent('j'.code, true)
                    client.sendKeyEvent('j'.code, false)
                    client.sendKeyEvent('k'.code, true)
                    client.sendKeyEvent('k'.code, false)
                    client.sendKeyEvent(0xffe9, false)
                    assertArrayEquals(byteArrayOf(4, 1, 0, 0, 0, 0, -1, -23), read(8))
                    assertArrayEquals(byteArrayOf(4, 1, 0, 0, 0, 0, 0, 106), read(8))
                    assertArrayEquals(byteArrayOf(4, 0, 0, 0, 0, 0, 0, 106), read(8))
                    assertArrayEquals(byteArrayOf(4, 1, 0, 0, 0, 0, 0, 107), read(8))
                    assertArrayEquals(byteArrayOf(4, 0, 0, 0, 0, 0, 0, 107), read(8))
                    assertArrayEquals(byteArrayOf(4, 0, 0, 0, 0, 0, -1, -23), read(8))
                    client.sendPointerEvent(10, 20, 1)
                    client.sendKeyEvent(0xffe9, true)
                    client.sendPointerEvent(30, 40, 1)
                    client.sendPointerEvent(30, 40, 0)
                    client.sendKeyEvent(0xffe9, false)
                    assertArrayEquals(byteArrayOf(5, 1, 0, 10, 0, 20), read(6))
                    assertArrayEquals(byteArrayOf(4, 1, 0, 0, 0, 0, -1, -23), read(8))
                    assertArrayEquals(byteArrayOf(5, 1, 0, 30, 0, 40), read(6))
                    assertArrayEquals(byteArrayOf(5, 0, 0, 30, 0, 40), read(6))
                    assertArrayEquals(byteArrayOf(4, 0, 0, 0, 0, 0, -1, -23), read(8))
                    // Complete the pending update with a Tight fill rectangle.
                    output.writeByte(0)
                    output.writeByte(0)
                    output.writeShort(1)
                    output.writeShort(0); output.writeShort(0)
                    output.writeShort(1); output.writeShort(1)
                    output.writeInt(7)
                    output.write(byteArrayOf(0x80.toByte(), 0x12, 0x34, 0x56))
                    output.flush()
                    assertTrue(frameReceived.await(5, TimeUnit.SECONDS))
                    org.junit.Assert.assertEquals(0xff123456.toInt(), receivedPixel)

                }
            } finally {
                client.close()
            }
        }
    }
}
