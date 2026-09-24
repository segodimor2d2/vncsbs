package com.rec.vncsbs.vnc

import com.rec.vncsbs.ui.RemoteFrame
import java.net.Socket

class VncClient(
    private val onFrame: (RemoteFrame) -> Unit
) {

    private var socket: Socket? = null

    fun connect(
        host: String,
        port: Int
    ) {
        Thread {
            try {
                println("VncClient: conectando $host:$port")

                val newSocket = Socket(
                    host,
                    port
                )

                socket = newSocket

                println("VncClient: TCP conectado")

                val input = newSocket.getInputStream()

                val versionBytes = ByteArray(12)

                var offset = 0

                while (offset < versionBytes.size) {
                    val count = input.read(
                        versionBytes,
                        offset,
                        versionBytes.size - offset
                    )

                    if (count < 0) {
                        throw Exception("Conexão encerrada pelo servidor")
                    }

                    offset += count
                }

                val version = String(
                    versionBytes,
                    Charsets.US_ASCII
                )

                println("VncClient: servidor RFB = $version")

            } catch (e: Exception) {
                println(
                    "VncClient: erro = ${e.message}"
                )

                socket?.close()
                socket = null
            }
        }.start()
    }

    fun disconnect() {
        try {
            socket?.close()
        } catch (_: Exception) {
        }

        socket = null

        println("VncClient: disconnect")
    }

    fun sendTestFrame() {
        val width = 320
        val height = 240

        val pixels = ByteArray(
            width * height * 4
        )

        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = (y * width + x) * 4
                val top = y < height / 2
                val left = x < width / 2
                val value = if (top == left) 255 else 80

                pixels[index] = value.toByte()
                pixels[index + 1] = value.toByte()
                pixels[index + 2] = value.toByte()
                pixels[index + 3] = 255.toByte()
            }
        }

        onFrame(
            RemoteFrame(
                width = width,
                height = height,
                pixels = pixels
            )
        )
    }
}
