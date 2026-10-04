package com.rec.vncsbs.ui

import androidx.compose.foundation.background
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.rec.vncsbs.MainActivity
import com.rec.vncsbs.viewmodel.VncViewModel

@Composable
fun VncScreen(
    viewModel: VncViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val settingsStore = remember(context) { MenuSettingsStore(context) }
    var settings by remember(settingsStore) { mutableStateOf(settingsStore.load()) }
    var menuExpanded by rememberSaveable { mutableStateOf(false) }
    val toggleMenu = rememberUpdatedState { menuExpanded = !menuExpanded }
    val changeZoom = rememberUpdatedState { delta: Int ->
        settings = settings.copy(zoomPercent = (settings.zoomPercent + delta).coerceIn(25, 400))
        settingsStore.save(settings)
    }
    val changePan = rememberUpdatedState { dx: Int, dy: Int ->
        settings = settings.copy(
            panX = settings.panX - dx * settings.panSensitivity,
            panY = settings.panY - dy * settings.panSensitivity
        )
        settingsStore.save(settings)
    }
    val changePanSensitivity = rememberUpdatedState { delta: Int ->
        settings = settings.copy(
            panSensitivity = (settings.panSensitivity.toLong() + delta)
                .coerceIn(10L, Int.MAX_VALUE.toLong()).toInt()
        )
        settingsStore.save(settings)
    }
    val centerPan = rememberUpdatedState {
        settings = settings.copy(panX = 0, panY = 0)
        settingsStore.save(settings)
    }
    var gyroQuietElapsedMs by remember { mutableStateOf(0) }
    val latestSettings = rememberUpdatedState(settings)
    val gyroAvailable = GyroscopePanEffect(
        enabled = settings.gyroPanEnabled,
        sensitivity = settings.panSensitivity,
        quietThreshold = settings.gyroQuietThreshold / 100f,
        quietTimeMs = settings.gyroQuietTimeMs,
        autoCenterEnabled = settings.gyroAutoCenterEnabled,
        onCenter = { centerPan.value() },
        onQuietTime = { gyroQuietElapsedMs = it },
        onPan = { dx, dy ->
            settings = settings.copy(panX = settings.panX + dx, panY = settings.panY + dy)
        },
        onStop = { settingsStore.save(latestSettings.value) }
    )
    val toggleGyroPan = rememberUpdatedState {
        if (gyroAvailable) {
            settings = settings.copy(gyroPanEnabled = !settings.gyroPanEnabled)
            settingsStore.save(settings)
        }
    }
    val toggleGyroAutoCenter = rememberUpdatedState {
        settings = settings.copy(gyroAutoCenterEnabled = !settings.gyroAutoCenterEnabled)
        settingsStore.save(settings)
    }
    BackHandler(enabled = menuExpanded) { menuExpanded = false }
    DisposableEffect(context) {
        val activity = context as? MainActivity
        activity?.onToggleMenu = { toggleMenu.value() }
        activity?.onCenterPan = { centerPan.value() }
        activity?.onToggleGyroPan = { toggleGyroPan.value() }
        activity?.onToggleGyroAutoCenter = { toggleGyroAutoCenter.value() }
        activity?.onZoomChange = { changeZoom.value(it) }
        activity?.onPanChange = { dx, dy -> changePan.value(dx, dy) }
        activity?.onPanSensitivityChange = { changePanSensitivity.value(it) }
        onDispose {
            activity?.onToggleMenu = null
            activity?.onCenterPan = null
            activity?.onToggleGyroPan = null
            activity?.onToggleGyroAutoCenter = null
            activity?.onZoomChange = null
            activity?.onPanChange = null
            activity?.onPanSensitivityChange = null
        }
    }

    fun updateSettings(value: MenuSettings) {
        settings = value
        settingsStore.save(value)
    }

    val paddingButtonColors = ButtonDefaults.textButtonColors(
        contentColor = Color.White,
        disabledContentColor = Color.White.copy(alpha = 0.35f)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .toggleMenuWithTwoFingers { toggleMenu.value() }
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            SbsRemoteView(
                frame = uiState.frame,
                modifier = Modifier.fillMaxSize(),
                screenPadding = settings.generalPadding.dp,
                outerSidePadding = settings.outerSidePadding.dp,
                leftViewRightPadding = settings.leftViewRightPadding.dp,
                rightViewLeftPadding = settings.rightViewLeftPadding.dp,
                zoom = settings.zoomPercent / 100f,
                panX = settings.panX.dp,
                panY = settings.panY.dp
            )
        }

        if (menuExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                        .padding(top = 64.dp, bottom = 16.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 440.dp)
                            .verticalScroll(rememberScrollState())
                    ) {

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = { updateSettings(settingsStore.defaults()) },
                                modifier = Modifier.padding(horizontal = 0.dp)
                            ) { Text("reset ", color = Color.White) }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {

                            TextButton(
                                onClick = {
                                    updateSettings(settings.copy(
                                        generalPadding = (settings.generalPadding - 2).coerceAtLeast(0)
                                    ))
                                },
                                enabled = settings.generalPadding > 0,
                                colors = paddingButtonColors
                            ) { Text("−") }
                            TextButton(
                                onClick = {
                                    updateSettings(settings.copy(
                                        generalPadding = (settings.generalPadding + 2).coerceAtMost(100)
                                    ))
                                },
                                enabled = settings.generalPadding < 100,
                                colors = paddingButtonColors
                            ) { Text("+") }
                            Text("allPad ", color = Color.White)
                            Text("${settings.generalPadding} dp", color = Color.White)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = {
                                    updateSettings(settings.copy(
                                        outerSidePadding = (settings.outerSidePadding - 2).coerceAtLeast(0)
                                    ))
                                },
                                enabled = settings.outerSidePadding > 0,
                                colors = paddingButtonColors
                            ) { Text("−") }
                            TextButton(
                                onClick = {
                                    updateSettings(settings.copy(
                                        outerSidePadding = (settings.outerSidePadding + 2).coerceAtMost(100)
                                    ))
                                },
                                enabled = settings.outerSidePadding < 100,
                                colors = paddingButtonColors
                            ) { Text("+") }
                            Text("< pad > ", color = Color.White)
                            Text("${settings.outerSidePadding} dp", color = Color.White)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = {
                                    updateSettings(settings.copy(
                                        leftViewRightPadding = (settings.leftViewRightPadding - 2).coerceAtLeast(0)
                                    ))
                                },
                                enabled = settings.leftViewRightPadding > 0,
                                colors = paddingButtonColors
                            ) { Text("−") }
                            TextButton(
                                onClick = {
                                    updateSettings(settings.copy(
                                        leftViewRightPadding = (settings.leftViewRightPadding + 2).coerceAtMost(100)
                                    ))
                                },
                                enabled = settings.leftViewRightPadding < 100,
                                colors = paddingButtonColors
                            ) { Text("+") }
                            Text("L-padR ", color = Color.White)
                            Text("${settings.leftViewRightPadding} dp", color = Color.White)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = {
                                    updateSettings(settings.copy(
                                        rightViewLeftPadding = (settings.rightViewLeftPadding - 2).coerceAtLeast(0)
                                    ))
                                },
                                enabled = settings.rightViewLeftPadding > 0,
                                colors = paddingButtonColors
                            ) { Text("−") }
                            TextButton(
                                onClick = {
                                    updateSettings(settings.copy(
                                        rightViewLeftPadding = (settings.rightViewLeftPadding + 2).coerceAtMost(100)
                                    ))
                                },
                                enabled = settings.rightViewLeftPadding < 100,
                                colors = paddingButtonColors
                            ) { Text("+") }
                            Text("R-padL ", color = Color.White)
                            Text("${settings.rightViewLeftPadding} dp", color = Color.White)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = {
                                    updateSettings(settings.copy(panSensitivity = settings.panSensitivity - 10))
                                },
                                enabled = settings.panSensitivity > 10,
                                colors = paddingButtonColors
                            ) { Text("−") }
                            TextButton(
                                onClick = {
                                    updateSettings(settings.copy(panSensitivity = settings.panSensitivity + 10))
                                },
                                enabled = settings.panSensitivity <= Int.MAX_VALUE - 10,
                                colors = paddingButtonColors
                            ) { Text("+") }
                            Text("sensPan : ${settings.panSensitivity} dp", color = Color.White)
                        }

                    }
                    Column(
                        modifier = Modifier
                            .widthIn(max = 440.dp)
                            .verticalScroll(rememberScrollState())
                    ) {


                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("zoom : ${settings.zoomPercent}%", color = Color.White)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {

                            TextButton(
                                onClick = { changeZoom.value(10) },
                                enabled = settings.zoomPercent < 400
                            ) { Text("CSu+", color = Color.White) }

                            TextButton(
                                onClick = { changeZoom.value(-10) },
                                enabled = settings.zoomPercent > 25
                            ) { Text("CSi-", color = Color.White) }

                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "panX : ${settings.panX}, panY : ${settings.panY}",
                                color = Color.White
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { changePan.value(0, 1) }) {
                                Text("CSj ↑", color = Color.White)
                            }

                            TextButton(onClick = { changePan.value(0, -1) }) {
                                Text("CSk ↓", color = Color.White)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { changePan.value(1, 0) }) {
                                Text("CSl ←", color = Color.White)
                            }

                            TextButton(onClick = { changePan.value(-1, 0) }) {
                                Text("CSh →", color = Color.White)
                            }
                        }

                    }

                    Column(
                        modifier = Modifier
                            .widthIn(max = 440.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = settings.gyroPanEnabled && gyroAvailable,
                                onCheckedChange = { updateSettings(settings.copy(gyroPanEnabled = it)) },
                                enabled = gyroAvailable
                            )
                            Text(" giro (CSp) ", color = Color.White)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = settings.gyroAutoCenterEnabled,
                                onCheckedChange = {
                                    updateSettings(settings.copy(gyroAutoCenterEnabled = it))
                                }
                            )
                            Text(" auto (CSn) : $gyroQuietElapsedMs / ${settings.gyroQuietTimeMs} ms", color = Color.White)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = { updateSettings(settings.copy(gyroQuietThreshold = settings.gyroQuietThreshold - 1)) },
                                enabled = settings.gyroQuietThreshold > 1,
                                colors = paddingButtonColors
                            ) { Text("−") }
                            TextButton(
                                onClick = { updateSettings(settings.copy(gyroQuietThreshold = settings.gyroQuietThreshold + 1)) },
                                enabled = settings.gyroQuietThreshold < 100,
                                colors = paddingButtonColors
                            ) { Text("+") }
                            Text("limite quieto : ${settings.gyroQuietThreshold / 100f} rad/s", color = Color.White)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = { updateSettings(settings.copy(gyroQuietTimeMs = settings.gyroQuietTimeMs - 500)) },
                                enabled = settings.gyroQuietTimeMs > 500,
                                colors = paddingButtonColors
                            ) { Text("−") }

                            TextButton(
                                onClick = { updateSettings(settings.copy(gyroQuietTimeMs = settings.gyroQuietTimeMs + 500)) },
                                enabled = settings.gyroQuietTimeMs <= Int.MAX_VALUE - 500,
                                colors = paddingButtonColors
                            ) { Text("+") }
                            Text("tempo quieto : ${settings.gyroQuietTimeMs} ms", color = Color.White)
                        }
                    }
                }
            }
        }

        if (menuExpanded) {

            TextButton(

                onClick = { viewModel.testVncConnection() },
                enabled = !uiState.connecting && !uiState.connected,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .safeDrawingPadding()
                    .padding(8.dp)
                    .background(
                        when {
                            uiState.connecting -> Color(0xFFB86E00)
                            uiState.connected -> Color(0xFF2E7D32)
                            else -> Color.Black.copy(alpha = 0.5f)
                        }
                    )

            ) {
                Text(
                    when {
                        uiState.connecting -> "CONECTANDO…"
                        uiState.connected -> "VNC CONECTADO"
                        else -> "VNC CONNECT"
                    },
                    color = Color.White
                )
            }
        }
    }
}

private fun Modifier.toggleMenuWithTwoFingers(onToggle: () -> Unit): Modifier =
    pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var toggled = false
            var event = currentEvent
            do {
                if (!toggled && event.changes.count { it.pressed } == 2) {
                    toggled = true
                    onToggle()
                }
                if (toggled) event.changes.forEach { it.consume() }
                if (!event.changes.any { it.pressed }) break
                event = awaitPointerEvent(PointerEventPass.Initial)
            } while (event.changes.any { it.pressed })
        }
    }
