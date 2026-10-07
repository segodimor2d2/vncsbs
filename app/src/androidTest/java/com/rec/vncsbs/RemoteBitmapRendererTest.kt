package com.rec.vncsbs

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rec.vncsbs.ui.RemoteBitmapRenderer
import com.rec.vncsbs.vnc.RemoteFramebuffer
import java.io.ByteArrayInputStream
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RemoteBitmapRendererTest {
    @Test fun reusesBitmapAndKeepsChangesAcrossSkippedFrames() {
        val framebuffer = RemoteFramebuffer(2, 2)
        val renderer = RemoteBitmapRenderer(framebuffer)
        renderer.refresh()
        val bitmap = renderer.bitmap
        assertEquals(0xff000000.toInt(), bitmap.getPixel(0, 0))
        framebuffer.readRectangle(ByteArrayInputStream(byteArrayOf(0, 0, -1, 0)), 0, 0, 1, 1)
        framebuffer.commit()
        framebuffer.readRectangle(ByteArrayInputStream(byteArrayOf(0, -1, 0, 0)), 1, 1, 1, 1)
        framebuffer.commit()
        renderer.refresh()
        assertSame(bitmap, renderer.bitmap)
        assertEquals(0xffff0000.toInt(), bitmap.getPixel(0, 0))
        assertEquals(0xff00ff00.toInt(), bitmap.getPixel(1, 1))
        assertEquals(0xff000000.toInt(), bitmap.getPixel(1, 0))
        val version = bitmap.generationId
        renderer.refresh()
        assertEquals(version, bitmap.generationId)
        val secondEye = RemoteBitmapRenderer(framebuffer)
        secondEye.refresh()
        assertTrue(bitmap.sameAs(secondEye.bitmap))
    }
}
