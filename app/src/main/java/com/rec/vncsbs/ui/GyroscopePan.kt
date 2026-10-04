package com.rec.vncsbs.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.rec.vncsbs.MainActivity
import kotlin.math.abs

@Composable
internal fun GyroscopePanEffect(
    enabled: Boolean,
    sensitivity: Int,
    quietThreshold: Float,
    quietTimeMs: Int,
    autoCenterEnabled: Boolean,
    onCenter: () -> Unit,
    onQuietTime: (Int) -> Unit,
    onPan: (Int, Int) -> Unit,
    onStop: () -> Unit
): Boolean {
    val context = LocalContext.current
    val activity = context as? MainActivity
    val manager = remember(context) {
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    }
    val sensor = remember(manager) { manager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE) }
    val currentSensitivity = rememberUpdatedState(sensitivity)
    val currentThreshold = rememberUpdatedState(quietThreshold)
    val currentQuietTime = rememberUpdatedState(quietTimeMs)
    val currentCenter = rememberUpdatedState(onCenter)
    val currentQuietCallback = rememberUpdatedState(onQuietTime)
    val currentPan = rememberUpdatedState(onPan)
    val currentStop = rememberUpdatedState(onStop)

    DisposableEffect(activity, manager, sensor, enabled, autoCenterEnabled) {
        val quietTimer = GyroQuietTimer()
        var timestamp = 0L
        var remainderX = 0f
        var remainderY = 0f
        val listener = object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

            override fun onSensorChanged(event: SensorEvent) {
                val previous = timestamp
                timestamp = event.timestamp
                if (previous == 0L || event.timestamp - previous > 100_000_000L) {
                    quietTimer.reset()
                    remainderX = 0f
                    remainderY = 0f
                }
                val threshold = currentThreshold.value
                val speed = kotlin.math.sqrt(event.values.take(3).sumOf { (it * it).toDouble() }).toFloat()
                if (autoCenterEnabled && quietTimer.update(event.timestamp, speed, threshold, currentQuietTime.value)) {
                    remainderX = 0f
                    remainderY = 0f
                    currentCenter.value()
                }
                currentQuietCallback.value(quietTimer.elapsedMs)
                if (speed <= threshold || previous == 0L) return
                val dt = (event.timestamp - previous) * 1e-9f
                if (dt <= 0f || dt > 0.1f) return
                val x = event.values[0]
                val y = event.values[1]
                @Suppress("DEPRECATION")
                val rotation = activity?.windowManager?.defaultDisplay?.rotation
                val (screenX, screenY) = when (rotation) {
                    Surface.ROTATION_90 -> -y to x
                    Surface.ROTATION_180 -> -x to -y
                    Surface.ROTATION_270 -> y to -x
                    else -> x to y
                }
                // One sensitivity step per five degrees; ignore small sensor noise.
                val gain = currentSensitivity.value / (Math.PI.toFloat() / 36f)
                if (abs(screenY) > threshold) remainderX += screenY * dt * gain
                if (abs(screenX) > threshold) remainderY += screenX * dt * gain
                val dx = remainderX.toInt()
                val dy = remainderY.toInt()
                remainderX -= dx
                remainderY -= dy
                if (dx != 0 || dy != 0) currentPan.value(dx, dy)
            }
        }
        fun stop() {
            manager?.unregisterListener(listener)
            timestamp = 0L
            quietTimer.reset()
            currentQuietCallback.value(0)
            remainderX = 0f
            remainderY = 0f
            currentStop.value()
        }
        fun start() {
            if (enabled && sensor != null) {
                timestamp = 0L
                manager?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> start()
                Lifecycle.Event.ON_PAUSE -> stop()
                else -> Unit
            }
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose {
            activity?.lifecycle?.removeObserver(observer)
            stop()
        }
    }
    return sensor != null && activity != null
}
