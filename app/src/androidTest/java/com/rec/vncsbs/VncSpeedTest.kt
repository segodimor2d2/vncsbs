package com.rec.vncsbs

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.rec.vncsbs.vnc.VncClient
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertTrue
import org.junit.Test

/** Run explicitly with vncHost and a continuously changing desktop/video. */
class VncSpeedTest {
    @Test fun compareRawAndTight() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val args = InstrumentationRegistry.getArguments()
        // Avoid running this external-server benchmark as part of the normal test suite.
        org.junit.Assume.assumeTrue(args.containsKey("vncHost"))
        val context = instrumentation.targetContext
        val prefs = context.getSharedPreferences("vnc_connection", 0)
        val host = args.getString("vncHost")!!
        val port = args.getString("vncPort")?.toInt() ?: prefs.getInt("port", 5900)
        val password = prefs.getString("password", "")!!
        val seconds = args.getString("sampleSeconds")?.toLong() ?: 10L
        require(seconds in 1..30)
        val csv = StringBuilder("mode,seconds,updates,wire_bytes,raw_pixel_bytes,cpu_ms,tight_rectangles,raw_rectangles\n")
        try {
            for ((label, quality) in listOf("RAW" to null, "Tight-0" to 0, "Tight-6" to 6, "Tight-9" to 9)) {
                val connected = CountDownLatch(1)
                val disconnected = CountDownLatch(1)
                val lock = Any()
                var start = Long.MAX_VALUE
                var end = Long.MAX_VALUE
                var wire = 0L; var raw = 0L; var updates = 0L
                var tightRects = 0L; var rawRects = 0L
                var firstCpu = -1L; var lastCpu = 0L
                val client = VncClient(
                    onConnectionChanged = { if (it) connected.countDown() else disconnected.countDown() },
                    onTransfer = { stats -> synchronized(lock) {
                        if (System.nanoTime() in start until end && stats.rawPixelBytes > 0) {
                            val cpu = android.os.Debug.threadCpuTimeNanos()
                            if (firstCpu < 0) firstCpu = cpu
                            lastCpu = cpu
                            wire += stats.wireBytes; raw += stats.rawPixelBytes; updates++
                            tightRects += stats.tightRectangles; rawRects += stats.rawRectangles
                        }
                    } }
                ) {}
                try {
                    client.connect(host, port, password, quality ?: 6, quality != null)
                    assertTrue("$label: conexão indisponível", connected.await(15, TimeUnit.SECONDS))
                    synchronized(lock) {
                        start = System.nanoTime() + TimeUnit.SECONDS.toNanos(2)
                        end = start + TimeUnit.SECONDS.toNanos(seconds)
                    }
                    assertTrue("$label: desconectou durante a amostra",
                        !disconnected.await(seconds + 2, TimeUnit.SECONDS))
                    synchronized(lock) {
                        assertTrue("$label: sem atualizações; reproduza vídeo no servidor", raw > 0)
                        if (quality != null) assertTrue("Servidor não usou Tight", tightRects > 0)
                        val line = "$label,$seconds,$updates,$wire,$raw,${(lastCpu - firstCpu) / 1e6},$tightRects,$rawRects"
                        csv.append(line).append('\n')
                        Log.i("VncSpeed", line)
                    }
                } finally {
                    client.close()
                    assertTrue("Cliente não encerrou", disconnected.await(5, TimeUnit.SECONDS))
                }
            }
        } finally {
            File(context.filesDir, "vnc-speed.csv").writeText(csv.toString())
        }
    }
}
