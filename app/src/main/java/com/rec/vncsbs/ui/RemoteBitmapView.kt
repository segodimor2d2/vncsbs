package com.rec.vncsbs.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import com.rec.vncsbs.vnc.RemoteFramebuffer

/** Shared by both SBS eyes. Accessed exclusively on the UI thread. */
internal class RemoteBitmapRenderer(private val framebuffer: RemoteFramebuffer) {
    val bitmap = Bitmap.createBitmap(framebuffer.width, framebuffer.height, Bitmap.Config.ARGB_8888)
        .apply { setHasAlpha(false) }
    private var revision = -1L

    fun refresh() {
        revision = framebuffer.readChanges(revision) { pixels, firstRow, rowCount ->
            bitmap.setPixels(pixels, firstRow * framebuffer.width, framebuffer.width,
                0, firstRow, framebuffer.width, rowCount)
        }
    }
}

internal class RemoteBitmapView(context: Context) : View(context) {
    private var renderer: RemoteBitmapRenderer? = null
    private var revision = -1L
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val destination = RectF()

    init {
        // Rasterize into Android's managed layer synchronously. The GPU never holds
        // our mutable source bitmap while the next update writes into it.
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun update(value: RemoteBitmapRenderer, frameRevision: Long) {
        if (renderer !== value || revision != frameRevision) {
            renderer = value
            revision = frameRevision
            value.refresh()
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val current = renderer ?: return
        val bitmap = current.bitmap
        val scale = minOf(width.toFloat() / bitmap.width, height.toFloat() / bitmap.height)
        val left = (width - bitmap.width * scale) / 2f
        val top = (height - bitmap.height * scale) / 2f
        destination.set(left, top, left + bitmap.width * scale, top + bitmap.height * scale)
        canvas.drawBitmap(bitmap, null, destination, paint)
    }
}
