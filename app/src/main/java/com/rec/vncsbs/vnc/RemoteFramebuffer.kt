package com.rec.vncsbs.vnc

import java.io.InputStream

/** One network writer; readers only access committed pixels under [readChanges]. */
class RemoteFramebuffer(val width: Int, val height: Int) {
    init {
        require(width > 0 && height > 0 && width.toLong() * height <= Int.MAX_VALUE / 4)
    }

    private val working = IntArray(width * height) { 0xff000000.toInt() }
    private val published = working.copyOf()
    private val rowBytes = ByteArray(width * 4)
    private val rowVersions = LongArray(height)
    private var revision = 0L
    private var left = width
    private var top = height
    private var right = 0
    private var bottom = 0

    fun readRectangle(input: InputStream, x: Int, y: Int, w: Int, h: Int) {
        require(x >= 0 && y >= 0 && w >= 0 && h >= 0 &&
            x.toLong() + w <= width && y.toLong() + h <= height) { "Retângulo VNC fora da tela" }
        for (row in y until y + h) {
            var offset = 0
            val length = w * 4
            while (offset < length) {
                val count = input.read(rowBytes, offset, length - offset)
                check(count > 0) { "Conexão encerrada durante leitura de pixels" }
                offset += count
            }
            val destination = row * width + x
            for (column in 0 until w) {
                val index = column * 4
                working[destination + column] = 0xff000000.toInt() or
                    ((rowBytes[index + 2].toInt() and 255) shl 16) or
                    ((rowBytes[index + 1].toInt() and 255) shl 8) or
                    (rowBytes[index].toInt() and 255)
            }
        }
        if (w > 0 && h > 0) {
            left = minOf(left, x)
            top = minOf(top, y)
            right = maxOf(right, x + w)
            bottom = maxOf(bottom, y + h)
        }
    }

    fun writeRectangle(x: Int, y: Int, w: Int, h: Int, pixels: IntArray) {
        require(x >= 0 && y >= 0 && w > 0 && h > 0 &&
            x.toLong() + w <= width && y.toLong() + h <= height)
        require(pixels.size == w * h)
        for (row in 0 until h) pixels.copyInto(working, (y + row) * width + x, row * w, (row + 1) * w)
        left = minOf(left, x)
        top = minOf(top, y)
        right = maxOf(right, x + w)
        bottom = maxOf(bottom, y + h)
    }

    /** Call only after all rectangles of a server update have arrived. */
    @Synchronized
    fun commit(): Long {
        if (left < right && top < bottom) {
            revision++
            for (row in top until bottom) {
                val start = row * width + left
                working.copyInto(published, start, start, row * width + right)
                rowVersions[row] = revision
            }
            left = width
            top = height
            right = 0
            bottom = 0
        }
        return revision
    }

    /** The array is borrowed only for the callback; never retain or mutate it. */
    @Synchronized
    fun readChanges(afterRevision: Long, copyRows: (IntArray, Int, Int) -> Unit): Long {
        var row = 0
        while (row < height) {
            if (afterRevision >= 0 && rowVersions[row] <= afterRevision) {
                row++
                continue
            }
            val start = row++
            while (row < height && (afterRevision < 0 || rowVersions[row] > afterRevision)) row++
            copyRows(published, start, row - start)
        }
        return revision
    }
}
