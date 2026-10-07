package com.rec.vncsbs.ui

import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
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
    val activity = context as? MainActivity
    val leadKB = activity?.leadKB == true
    val leaderFocusRequester = remember { FocusRequester() }
    val orientation = LocalConfiguration.current.orientation
    val settingsStore = remember(context, orientation) { MenuSettingsStore(context, orientation) }
    var settings by remember(settingsStore) { mutableStateOf(settingsStore.load()) }
    var menuExpanded by rememberSaveable { mutableStateOf(false) }
    var connectionExpanded by rememberSaveable { mutableStateOf(false) }
    var connectionInputFocused by remember { mutableStateOf(false) }
    var keyboardRequested by remember { mutableStateOf(false) }
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val mouseCaptureView = remember(context) { MouseCaptureView(context) }
    LaunchedEffect(keyboardVisible) {
        keyboardRequested = keyboardVisible
    }
    androidx.compose.runtime.SideEffect {
        activity?.remoteKeyboardEnabled = uiState.connected && !connectionInputFocused
        activity?.localKeyboardInputFocused = connectionInputFocused
    }
    val lastConnection = remember(viewModel) { viewModel.lastConnection() }
    var server by rememberSaveable { mutableStateOf(lastConnection.host) }
    var port by rememberSaveable { mutableStateOf(lastConnection.port.toString()) }
    var password by rememberSaveable { mutableStateOf(lastConnection.password) }
    LaunchedEffect(uiState.connected) {
        if (uiState.connected) connectionExpanded = false
    }
    val toggleMenu = rememberUpdatedState { menuExpanded = !menuExpanded }
    val changeZoom = rememberUpdatedState { delta: Int ->
        settings = settings.copy(
            zoomPercent = (settings.zoomPercent.toLong() + delta)
                .coerceIn(25L, Int.MAX_VALUE.toLong()).toInt()
        )
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
        mouseCaptureView.centerPointer()
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
    fun updateSettings(value: MenuSettings) {
        val updated = if (settings.gyroAutoCenterEnabled && !value.gyroAutoCenterEnabled) {
            value.copy(mouseCaptureEnabled = false)
        } else value
        if (settings.mouseCaptureEnabled && !updated.mouseCaptureEnabled) {
            mouseCaptureView.stopCapture()
        }
        settings = updated
        settingsStore.save(updated)
    }

    val toggleGyroAutoCenter = rememberUpdatedState {
        settings = settings.copy(gyroAutoCenterEnabled = !settings.gyroAutoCenterEnabled)
        settingsStore.save(settings)
    }

    val toggleMouseCapture = rememberUpdatedState {
        val enabled = !settings.mouseCaptureEnabled
        updateSettings(settings.copy(mouseCaptureEnabled = enabled))
        if (enabled) menuExpanded = false
    }

    val adjustDisplay = rememberUpdatedState { key: Char ->
        val updated = when (key) {
            'R' -> settingsStore.defaults()
            'H' -> settings.copy(generalPadding = (settings.generalPadding.toLong() + 2).coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
            'L' -> settings.copy(generalPadding = (settings.generalPadding - 2).coerceAtLeast(0))
            'J' -> settings.copy(outerSidePadding = (settings.outerSidePadding.toLong() + 2).coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
            'K' -> settings.copy(outerSidePadding = (settings.outerSidePadding - 2).coerceAtLeast(0))
            '0' -> settings.copy(leftViewRightPadding = (settings.leftViewRightPadding + 2).coerceAtMost(100))
            '*' -> settings.copy(leftViewRightPadding = (settings.leftViewRightPadding - 2).coerceAtLeast(0))
            '#' -> settings.copy(rightViewLeftPadding = (settings.rightViewLeftPadding + 2).coerceAtMost(100))
            '$' -> settings.copy(rightViewLeftPadding = (settings.rightViewLeftPadding - 2).coerceAtLeast(0))
            else -> settings
        }
        updateSettings(updated)
    }

    BackHandler(enabled = menuExpanded && !leadKB) { menuExpanded = false }
    DisposableEffect(context) {
        val activity = context as? MainActivity
        activity?.onToggleMenu = { toggleMenu.value() }
        activity?.onToggleMouseCapture = { toggleMouseCapture.value() }
        activity?.onCenterPan = { centerPan.value() }
        activity?.onToggleGyroPan = { toggleGyroPan.value() }
        activity?.onToggleGyroAutoCenter = { toggleGyroAutoCenter.value() }
        activity?.onZoomChange = { changeZoom.value(it) }
        activity?.onPanChange = { dx, dy -> changePan.value(dx, dy) }
        activity?.onPanSensitivityChange = { changePanSensitivity.value(it) }
        activity?.onDisplayAdjustment = { adjustDisplay.value(it) }
        onDispose {
            mouseCaptureView.stopCapture()
            activity?.remoteKeyboardEnabled = false
            activity?.localKeyboardInputFocused = false
            activity?.onToggleMenu = null
            activity?.onToggleMouseCapture = null
            activity?.onCenterPan = null
            activity?.onToggleGyroPan = null
            activity?.onToggleGyroAutoCenter = null
            activity?.onZoomChange = null
            activity?.onPanChange = null
            activity?.onPanSensitivityChange = null
            activity?.onDisplayAdjustment = null
        }
    }

    val paddingButtonColors = ButtonDefaults.textButtonColors(
        contentColor = Color.White,
        disabledContentColor = Color.White.copy(alpha = 0.35f)
    )
    val touchpadEnabled = settings.mouseCaptureEnabled && uiState.connected &&
        !menuExpanded && !connectionExpanded

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .toggleMenuWithTwoFingers(enabled = !touchpadEnabled) { toggleMenu.value() }
            .simulatedTouchpad(
                enabled = touchpadEnabled,
                density = LocalDensity.current.density,
                onMove = { dx, dy -> mouseCaptureView.moveTouch(dx, dy) },
                onClick = { mouseCaptureView.clickTouch(it) },
                onDragButton = { mouseCaptureView.setTouchButton(1, it) },
                onScroll = { horizontal, vertical -> mouseCaptureView.scrollTouch(horizontal, vertical) },
                onMenu = { toggleMenu.value() },
                onRelease = { mouseCaptureView.releaseTouch() }
            )
    ) {
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { mouseCaptureView },
            modifier = Modifier.size(1.dp),
            update = { view ->
                view.commitText = { text ->
                    if (activity?.handleLeaderText(text) != true) viewModel.sendText(text)
                }
                view.sendKeyboardEvent = { activity?.dispatchKeyEvent(it) == true }
                view.sendPointer = { packet ->
                    viewModel.sendPointerEvent(packet.x, packet.y, packet.buttons)
                }
                view.update(
                    settings.mouseCaptureEnabled && uiState.connected && !connectionExpanded,
                    uiState.frame.width,
                    uiState.frame.height
                )
            }
        )
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
                panY = settings.panY.dp,
                leadKB = leadKB
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
                        .imePadding()
                        .padding(top = 64.dp, bottom = 16.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 440.dp)
                            .verticalScroll(rememberScrollState())
                    ) {

                        TextButton(
                            onClick = { connectionExpanded = !connectionExpanded },
                            enabled = !uiState.connecting,
                            modifier = Modifier.background(
                                when {
                                    uiState.connecting -> Color(0xFFB86E00)
                                    uiState.connected -> Color(0xFF2E7D32)
                                    else -> Color.Black.copy(alpha = 0.5f)
                                }
                            )
                        ) {
                            Text(
                                when {
                                    uiState.connecting -> "connecting…"
                                    uiState.connected -> "vnc connected"
                                    else -> "vnc connect"
                                },
                                color = Color.White
                            )
                        }
                        if (connectionExpanded) {
                            ConnectionMenu(
                                state = uiState,
                                server = server,
                                port = port,
                                password = password,
                                onServerChange = { server = it },
                                onPortChange = { port = it },
                                onPasswordChange = { password = it },
                                onInputFocusChange = { connectionInputFocused = it },
                                onConnect = { viewModel.connect(server, it, password) },
                                onDisconnect = { viewModel.disconnect() },
                                onClose = { connectionExpanded = false }
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = settings.gyroPanEnabled && gyroAvailable,
                                onCheckedChange = { updateSettings(settings.copy(gyroPanEnabled = it)) },
                                enabled = gyroAvailable
                            )
                            Text(" giro @g", color = Color.White)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = settings.mouseCaptureEnabled,
                                onCheckedChange = {
                                    updateSettings(settings.copy(mouseCaptureEnabled = it))
                                    if (it) menuExpanded = false
                                },
                                enabled = uiState.connected && android.os.Build.VERSION.SDK_INT >= 26
                            )
                            Text(" mouse", color = Color.White)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = settings.gyroAutoCenterEnabled,
                                onCheckedChange = {
                                    updateSettings(settings.copy(gyroAutoCenterEnabled = it))
                                }
                            )
                            Text(" autoCent: $gyroQuietElapsedMs / ${settings.gyroQuietTimeMs} ms", color = Color.White)
                        }


                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { centerPan.value() }) {
                                Text("viewCent", color = Color.White)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { activity?.toggleLeaderKeyboard() }) {
                                Text("leadKB", color = Color.White)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = {
                                    activity?.let { host ->
                                        val controller = androidx.core.view.WindowCompat.getInsetsController(
                                            host.window, mouseCaptureView)
                                        if (keyboardVisible || keyboardRequested) {
                                            keyboardRequested = false
                                            controller.hide(androidx.core.view.WindowInsetsCompat.Type.ime())
                                            mouseCaptureView.textInputEnabled = false
                                        } else {
                                            keyboardRequested = true
                                            if (connectionInputFocused) {
                                                androidx.core.view.WindowCompat.getInsetsController(
                                                    host.window, host.currentFocus ?: mouseCaptureView
                                                ).show(androidx.core.view.WindowInsetsCompat.Type.ime())
                                            } else {
                                                mouseCaptureView.textInputEnabled = true
                                                mouseCaptureView.requestFocus()
                                                mouseCaptureView.post {
                                                    context.getSystemService(android.view.inputmethod.InputMethodManager::class.java)
                                                        .restartInput(mouseCaptureView)
                                                    controller.show(androidx.core.view.WindowInsetsCompat.Type.ime())
                                                }
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.testTag("android-keyboard-toggle")
                            ) {
                                Text("teclado", color = Color.White)
                            }
                            Text(if (keyboardVisible) "ligado" else "desligado", color = Color.White)
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
                                        generalPadding = (settings.generalPadding.toLong() + 2)
                                            .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                                    ))
                                },
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
                                        outerSidePadding = (settings.outerSidePadding.toLong() + 2)
                                            .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                                    ))
                                },
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
                            Text("limNoise : ${settings.gyroQuietThreshold / 100f} rad/s", color = Color.White)
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
                            Text("tAutoCent : ${settings.gyroQuietTimeMs} ms", color = Color.White)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {

                            TextButton(
                                onClick = { changeZoom.value(-10) },
                                enabled = settings.zoomPercent > 25
                            ) { Text("@i-", color = Color.White) }

                            TextButton(
                                onClick = { changeZoom.value(10) }
                            ) { Text("@u+", color = Color.White) }

                            Text(" zoom : ${settings.zoomPercent}%", color = Color.White)

                        }


                        Row(verticalAlignment = Alignment.CenterVertically) {

                            TextButton(onClick = { changePan.value(0, 1) }) {
                                Text("@k ↓", color = Color.White)
                            }

                            TextButton(onClick = { changePan.value(0, -1) }) {
                                Text("@j ↑", color = Color.White)
                            }

                            Text( " panY : ${settings.panY}", color = Color.White)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { changePan.value(-1, 0) }) {
                                Text("@l ←", color = Color.White)
                            }

                            TextButton(onClick = { changePan.value(1, 0) }) {
                                Text("@h →", color = Color.White)
                            }
                            Text( " panX : ${settings.panX}", color = Color.White)
                        }


                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = { updateSettings(settingsStore.defaults()) },
                                modifier = Modifier.padding(horizontal = 0.dp)
                            ) { Text("reset ", color = Color.White) }
                        }

                    }

                }
            }

        }
    }

    if (leadKB && !menuExpanded && !connectionExpanded && !keyboardRequested && !keyboardVisible && !touchpadEnabled) {
        Popup(
            alignment = Alignment.BottomStart,
            onDismissRequest = {},
            properties = PopupProperties(
                focusable = true,
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            LaunchedEffect(Unit) {
                leaderFocusRequester.requestFocus()
            }
            Box(
                modifier = Modifier
                    .size(1.dp)
                    .onPreviewKeyEvent { activity?.handleLeaderKeyEvent(it.nativeKeyEvent) == true }
                    .focusRequester(leaderFocusRequester)
                    .focusable()
            )
        }
    }
}

private fun Modifier.toggleMenuWithTwoFingers(enabled: Boolean, onToggle: () -> Unit): Modifier =
    pointerInput(enabled) {
        if (!enabled) return@pointerInput
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
