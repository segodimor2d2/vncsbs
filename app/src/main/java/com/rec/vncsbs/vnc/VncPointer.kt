package com.rec.vncsbs.vnc

internal data class PointerPacket(val x: Int, val y: Int, val buttons: Int)

internal fun encodePointerEvent(x: Int, y: Int, buttons: Int): ByteArray {
    require(x in 0..65535 && y in 0..65535 && buttons in 0..255)
    return byteArrayOf(5, buttons.toByte(), (x ushr 8).toByte(), x.toByte(),
        (y ushr 8).toByte(), y.toByte())
}

internal class VncPointerState {
    private var width = 0
    private var height = 0
    private var x = 0f
    private var y = 0f
    private var buttons = 0
    private var scrollX = 0f
    private var scrollY = 0f

    fun resize(newWidth: Int, newHeight: Int) {
        if (newWidth == width && newHeight == height) return
        width = newWidth.coerceIn(0, 65535)
        height = newHeight.coerceIn(0, 65535)
        x = (width / 2).toFloat()
        y = (height / 2).toFloat()
        buttons = 0
        scrollX = 0f
        scrollY = 0f
    }

    fun move(dx: Float, dy: Float, androidButtons: Int): PointerPacket? {
        if (width == 0 || height == 0 || !dx.isFinite() || !dy.isFinite()) return null
        x = (x + dx).coerceIn(0f, (width - 1).toFloat())
        y = (y + dy).coerceIn(0f, (height - 1).toFloat())
        buttons = (androidButtons and 1) or
            (if (androidButtons and 2 != 0) 4 else 0) or
            (if (androidButtons and 4 != 0) 2 else 0)
        return packet()
    }

    fun scroll(horizontal: Float, vertical: Float): List<PointerPacket> {
        if (width == 0 || height == 0 || !horizontal.isFinite() || !vertical.isFinite()) return emptyList()
        scrollX += horizontal.coerceIn(-100f, 100f)
        scrollY += vertical.coerceIn(-100f, 100f)
        val result = mutableListOf<PointerPacket>()
        fun wheel(bit: Int) {
            result += packet().copy(buttons = buttons or bit)
            result += packet()
        }
        while (scrollY >= 1f) { wheel(8); scrollY -= 1f }
        while (scrollY <= -1f) { wheel(16); scrollY += 1f }
        while (scrollX >= 1f) { wheel(64); scrollX -= 1f }
        while (scrollX <= -1f) { wheel(32); scrollX += 1f }
        return result
    }

    fun release(): PointerPacket? {
        val hadButtons = buttons != 0
        buttons = 0
        scrollX = 0f
        scrollY = 0f
        return if (hadButtons && width > 0 && height > 0) packet() else null
    }

    private fun packet() = PointerPacket(x.toInt(), y.toInt(), buttons)
}
