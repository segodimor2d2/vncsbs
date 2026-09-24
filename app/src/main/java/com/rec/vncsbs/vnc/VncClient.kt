package com.rec.vncsbs.vnc

import com.rec.vncsbs.ui.RemoteFrame
import java.net.Socket
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class VncClient(
    private val onFrame: (RemoteFrame) -> Unit
) {

    private var socket: Socket? = null

    fun connect(
        host: String,
        port: Int,
        password: String = ""
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
                val output = newSocket.getOutputStream()

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

                output.write(versionBytes)
                output.flush()

                println("VncClient: versão RFB enviada")

                val securityTypeCount = input.read()

                if (securityTypeCount < 0) {
                    throw Exception("Conexão encerrada ao ler Security Types")
                }

                println(
                    "VncClient: Security Types disponíveis = $securityTypeCount"
                )

                val securityTypes = ByteArray(
                    securityTypeCount
                )

                var securityOffset = 0

                while (securityOffset < securityTypes.size) {
                    val count = input.read(
                        securityTypes,
                        securityOffset,
                        securityTypes.size - securityOffset
                    )

                    if (count < 0) {
                        throw Exception("Conexão encerrada ao ler Security Types")
                    }

                    securityOffset += count
                }

                println(
                    "VncClient: Security Types = " +
                        securityTypes.joinToString(", ") {
                            (it.toInt() and 0xFF).toString()
                        }
                )

                val hasVncAuthentication = securityTypes.any {
                    (it.toInt() and 0xFF) == 2
                }

                if (!hasVncAuthentication) {
                    throw Exception(
                        "VNC Authentication (2) não disponível"
                    )
                }

                output.write(2)
                output.flush()

                println(
                    "VncClient: Security Type selecionado = 2"
                )

                val challenge = ByteArray(16)

                var challengeOffset = 0

                while (challengeOffset < challenge.size) {
                    val count = input.read(
                        challenge,
                        challengeOffset,
                        challenge.size - challengeOffset
                    )

                    if (count < 0) {
                        throw Exception(
                            "Conexão encerrada ao ler challenge"
                        )
                    }

                    challengeOffset += count
                }

                println(
                    "VncClient: challenge recebido = " +
                        challenge.joinToString(" ") {
                            "%02X".format(it.toInt() and 0xFF)
                        }
                )


                val keyBytes = ByteArray(8)

                for (i in 0 until 8) {
                    val value = if (i < password.length) {
                        password[i].code
                    } else {
                        0
                    }

                    keyBytes[i] = reverseBits(
                        value and 0xFF
                    ).toByte()
                }

                val cipher = Cipher.getInstance("DES/ECB/NoPadding")

                val key = SecretKeySpec(
                    keyBytes,
                    "DES"
                )

                cipher.init(
                    Cipher.ENCRYPT_MODE,
                    key
                )

                val response = cipher.doFinal(challenge)

                output.write(response)
                output.flush()

                println(
                    "VncClient: resposta de autenticação enviada"
                )

                val securityResultBytes = ByteArray(4)

                var resultOffset = 0

                while (resultOffset < securityResultBytes.size) {
                    val count = input.read(
                        securityResultBytes,
                        resultOffset,
                        securityResultBytes.size - resultOffset
                    )

                    if (count < 0) {
                        throw Exception(
                            "Conexão encerrada ao ler SecurityResult"
                        )
                    }

                    resultOffset += count
                }

                val securityResult =
                    ((securityResultBytes[0].toInt() and 0xFF) shl 24) or
                    ((securityResultBytes[1].toInt() and 0xFF) shl 16) or
                    ((securityResultBytes[2].toInt() and 0xFF) shl 8) or
                    (securityResultBytes[3].toInt() and 0xFF)

                println(
                    "VncClient: SecurityResult = $securityResult"
                )

                if (securityResult != 0) {
                    throw Exception(
                        "Autenticação VNC falhou: $securityResult"
                    )
                }

                output.write(1)
                output.flush()

                println(
                    "VncClient: ClientInit enviado"
                )

                val widthBytes = ByteArray(2)
                val heightBytes = ByteArray(2)

                readFully(
                    input,
                    widthBytes
                )

                readFully(
                    input,
                    heightBytes
                )

                val framebufferWidth =
                    ((widthBytes[0].toInt() and 0xFF) shl 8) or
                    (widthBytes[1].toInt() and 0xFF)

                val framebufferHeight =
                    ((heightBytes[0].toInt() and 0xFF) shl 8) or
                    (heightBytes[1].toInt() and 0xFF)

                println(
                    "VncClient: framebuffer = " +
                        "${framebufferWidth}x${framebufferHeight}"
                )

                val pixelFormat = ByteArray(16)

                readFully(
                    input,
                    pixelFormat
                )

                println(
                    "VncClient: PixelFormat = " +
                        pixelFormat.joinToString(" ") {
                            "%02X".format(it.toInt() and 0xFF)
                        }
                )

                val nameLengthBytes = ByteArray(4)

                readFully(
                    input,
                    nameLengthBytes
                )

                val nameLength =
                    ((nameLengthBytes[0].toInt() and 0xFF) shl 24) or
                    ((nameLengthBytes[1].toInt() and 0xFF) shl 16) or
                    ((nameLengthBytes[2].toInt() and 0xFF) shl 8) or
                    (nameLengthBytes[3].toInt() and 0xFF)

                println(
                    "VncClient: desktop name length = $nameLength"
                )

                if (nameLength < 0 || nameLength > 1024) {
                    throw Exception(
                        "Nome do desktop com tamanho inválido: $nameLength"
                    )
                }

                val nameBytes = ByteArray(nameLength)

                readFully(
                    input,
                    nameBytes
                )

                val desktopName = String(
                    nameBytes,
                    Charsets.UTF_8
                )

                println(
                    "VncClient: desktop name = $desktopName"
                )


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

    private fun reverseBits(value: Int): Int {
        var result = 0

        for (i in 0 until 8) {
            result = result or (
                ((value shr i) and 1) shl (7 - i)
            )
        }

        return result
    }

    private fun readFully(
        input: java.io.InputStream,
        buffer: ByteArray
    ) {
        var offset = 0

        while (offset < buffer.size) {
            val count = input.read(
                buffer,
                offset,
                buffer.size - offset
            )

            if (count < 0) {
                throw Exception(
                    "Conexão encerrada durante leitura"
                )
            }

            offset += count
        }
    }

}
