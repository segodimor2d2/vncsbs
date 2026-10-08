package com.rec.vncsbs.vnc

import java.io.Closeable
import java.io.DataInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.zip.Inflater

/** Standard Tight, for the negotiated 32bpp/depth-24 RGB pixel format. */
internal class TightDecoder(private val decodeJpeg: (ByteArray, Int, Int) -> IntArray) : Closeable {
    private val streams = Array(4) { Inflater() }

    fun readRectangle(input: InputStream, width: Int, height: Int): IntArray {
        require(width in 1..2048 && height > 0 && width.toLong() * height <= Int.MAX_VALUE / 3)
        val source = DataInputStream(input)
        val control = source.readUnsignedByte()
        streams.forEachIndexed { index, stream -> if (control and (1 shl index) != 0) stream.reset() }
        val mode = control ushr 4
        val count = width * height
        if (mode == 8) return IntArray(count) { 0 }.also { it.fill(readColor(source)) }
        if (mode == 9) {
            val bytes = ByteArray(readLength(source))
            source.readFully(bytes)
            return decodeJpeg(bytes, width, height).also { require(it.size == count) }
        }
        require(mode <= 7) { "Subencoding Tight inválido: $mode" }
        val filter = if (mode and 4 != 0) source.readUnsignedByte() else 0
        require(filter in 0..2) { "Filtro Tight inválido: $filter" }
        val palette = if (filter == 1) IntArray(source.readUnsignedByte() + 1) { readColor(source) } else null
        val rowSize = if (palette?.size == 2) (width + 7) / 8 else width
        val size = if (palette != null) rowSize * height else count * 3
        val data = ByteArray(size)
        if (size < 12) source.readFully(data) else {
            val compressed = ByteArray(readLength(source))
            source.readFully(compressed)
            val stream = streams[mode and 3]
            require(stream.needsInput()) { "Dados Tight pendentes" }
            stream.setInput(compressed)
            var offset = 0
            while (offset < size) {
                val n = stream.inflate(data, offset, size - offset)
                require(n > 0) { "Dados zlib Tight incompletos" }
                offset += n
            }
            // Consume the SYNC_FLUSH trailer and reject extra decoded bytes.
            val extra = ByteArray(1)
            while (!stream.needsInput()) {
                require(stream.inflate(extra) == 0) { "Dados zlib Tight excedentes" }
                require(!stream.needsDictionary() && !stream.finished()) { "Fluxo zlib Tight inválido" }
                require(stream.needsInput()) { "Fluxo zlib Tight sem progresso" }
            }
        }
        val pixels = IntArray(count)
        for (y in 0 until height) for (x in 0 until width) {
            val i = y * width + x
            if (palette != null) {
                val index = if (palette.size == 2)
                    (data[y * rowSize + x / 8].toInt() ushr (7 - x % 8)) and 1
                else data[i].toInt() and 255
                require(index < palette.size) { "Índice de paleta Tight inválido" }
                pixels[i] = palette[index]
            } else {
                var color = 0xff000000.toInt()
                for (component in 0..2) {
                    val shift = 16 - component * 8
                    var value = data[i * 3 + component].toInt() and 255
                    if (filter == 2) {
                        fun channel(index: Int) = (pixels[index] ushr shift) and 255
                        val left = if (x > 0) channel(i - 1) else 0
                        val above = if (y > 0) channel(i - width) else 0
                        val diagonal = if (x > 0 && y > 0) channel(i - width - 1) else 0
                        value = (value + (left + above - diagonal).coerceIn(0, 255)) and 255
                    }
                    color = color or (value shl shift)
                }
                pixels[i] = color
            }
        }
        return pixels
    }

    private fun readColor(input: DataInputStream): Int = 0xff000000.toInt() or
        (input.readUnsignedByte() shl 16) or (input.readUnsignedByte() shl 8) or input.readUnsignedByte()

    private fun readLength(input: DataInputStream): Int {
        val first = input.readUnsignedByte()
        var length = first and 127
        if (first and 128 != 0) {
            val second = input.readUnsignedByte()
            length = length or ((second and 127) shl 7)
            if (second and 128 != 0) length = length or (input.readUnsignedByte() shl 14)
        }
        require(length > 0) { "Comprimento Tight inválido" }
        return length
    }

    override fun close() { streams.forEach { it.end() } }
}

/** Quality 0..9 enables JPEG; -1 requests lossless Tight. RAW remains a fallback. */
internal fun tightEncodings(quality: Int): ByteArray {
    require(quality in -1..9)
    val encodings = if (quality < 0) intArrayOf(7, 0, -250) else intArrayOf(7, 0, -250, -32 + quality)
    return ByteBuffer.allocate(4 + encodings.size * 4).apply {
        put(2.toByte()); put(0.toByte()); putShort(encodings.size.toShort())
        encodings.forEach { putInt(it) }
    }.array()
}
