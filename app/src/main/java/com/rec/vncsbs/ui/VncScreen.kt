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
    BackHandler(enabled = menuExpanded) { menuExpanded = false }
    DisposableEffect(context) {
        val activity = context as? MainActivity
        activity?.onToggleMenu = { toggleMenu.value() }
        activity?.onZoomChange = { changeZoom.value(it) }
        onDispose {
            activity?.onToggleMenu = null
            activity?.onZoomChange = null
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
                zoom = settings.zoomPercent / 100f
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

                        TextButton(
                            onClick = { updateSettings(settingsStore.defaults()) },
                            modifier = Modifier.padding(horizontal = 0.dp)
                        ) { Text("Reset", color = Color.White) }

                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Text("allPad", color = Color.White)
                            Text("${settings.generalPadding} dp", color = Color.White)
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
                        }

                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("LRpad", color = Color.White)
                            Text("${settings.outerSidePadding} dp", color = Color.White)
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
                        }

                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("L-padR", color = Color.White)
                            Text("${settings.leftViewRightPadding} dp", color = Color.White)
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
                        }

                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("R-padL", color = Color.White)
                            Text("${settings.rightViewLeftPadding} dp", color = Color.White)
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
                        }

                    }
                    Column(
                        modifier = Modifier
                            .widthIn(max = 440.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {

                            Text("Zoom: ${settings.zoomPercent}%", color = Color.White)

                            TextButton(
                                onClick = { changeZoom.value(10) },
                                enabled = settings.zoomPercent < 400
                            ) { Text("CSj+", color = Color.White) }

                            TextButton(
                                onClick = { changeZoom.value(-10) },
                                enabled = settings.zoomPercent > 25
                            ) { Text("CSk-", color = Color.White) }

                        }
                    }
                }
            }
        }

        if (menuExpanded) {

            TextButton(

                onClick = { viewModel.testVncConnection() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .safeDrawingPadding()
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.5f))

            ) { Text("VNC CONNECT", color = Color.White) }
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
