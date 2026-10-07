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
            val client = VncClient(onConnectionChanged = { if (it) connected.countDown() }) {}
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
                    read(8) // SetEncodings
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
                }
            } finally {
                client.close()
            }
        }
    }
}
