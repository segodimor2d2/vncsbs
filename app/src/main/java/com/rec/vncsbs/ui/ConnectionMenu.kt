package com.rec.vncsbs.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rec.vncsbs.viewmodel.VncUiState

@Composable
internal fun ConnectionMenu(
    state: VncUiState,
    server: String,
    port: String,
    password: String,
    onServerChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onInputFocusChange: (Boolean) -> Unit,
    onConnect: (Int) -> Unit,
    onDisconnect: () -> Unit,
    onClose: () -> Unit,
    quality: Int = 6,
    onQualityChange: (Int) -> Unit = {}
) {
    val validPort = port.toIntOrNull()?.takeIf { it in 1..65535 }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(state.connected) {
        if (state.connected) focusManager.clearFocus()
    }
    DisposableEffect(Unit) {
        onDispose { onInputFocusChange(false) }
    }
    val editable = !state.connected && !state.connecting
    val colors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        disabledTextColor = Color.White.copy(alpha = 0.5f),
        focusedContainerColor = Color.Black.copy(alpha = 0.35f),
        unfocusedContainerColor = Color.Black.copy(alpha = 0.35f),
        disabledContainerColor = Color.Black.copy(alpha = 0.35f),
        errorContainerColor = Color.Black.copy(alpha = 0.35f),
        focusedBorderColor = Color.White,
        unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
        focusedLabelColor = Color.White,
        unfocusedLabelColor = Color.White,
        disabledLabelColor = Color.White.copy(alpha = 0.5f),
        disabledBorderColor = Color.White.copy(alpha = 0.25f),
        cursorColor = Color.White,
        errorTextColor = Color.White,
        errorBorderColor = Color(0xFFFF8080),
        errorLabelColor = Color(0xFFFF8080),
        errorSupportingTextColor = Color(0xFFFF8080)
    )
    val fieldModifier = Modifier.widthIn(min = 240.dp, max = 320.dp)
        .onFocusChanged { onInputFocusChange(it.isFocused) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = server, onValueChange = onServerChange,
            label = { Text("server", fontSize = 14.sp) },
            modifier = fieldModifier.testTag("connection-server"),
            singleLine = true, enabled = editable, shape = RectangleShape,
            colors = colors, textStyle = TextStyle(fontSize = 14.sp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
        )
        OutlinedTextField(
            value = port, onValueChange = onPortChange,
            label = { Text("port", fontSize = 14.sp) },
            modifier = fieldModifier.testTag("connection-port"),
            singleLine = true, enabled = editable, shape = RectangleShape,
            colors = colors, textStyle = TextStyle(fontSize = 14.sp),
            isError = validPort == null,
            supportingText = { if (validPort == null) Text("Porta de 1 a 65535") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        OutlinedTextField(
            value = password, onValueChange = onPasswordChange,
            label = { Text("pass", fontSize = 14.sp) },
            modifier = fieldModifier.testTag("connection-password"),
            singleLine = true, enabled = editable, shape = RectangleShape,
            colors = colors, textStyle = TextStyle(fontSize = 14.sp),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        Text(if (quality < 0) "without loss of quality" else "quality $quality/9", color = Color.White)
        androidx.compose.material3.Slider(
            value = quality.toFloat(), onValueChange = { onQualityChange(it.toInt()) },
            valueRange = -1f..9f, steps = 9, enabled = editable,
            modifier = Modifier.widthIn(max = 360.dp).testTag("connection-quality")
        )
        Text("- quality - dataTraffic.", color = Color.White, fontSize = 12.sp)
        state.connectionError?.let { Text(it, color = Color(0xFFFF8080)) }
        Row {
            if (state.connected) {
                TextButton(onClick = onDisconnect) { Text("disconnect", color = Color.White) }
            } else {
                TextButton(
                    onClick = { validPort?.let(onConnect) },
                    enabled = !state.connecting && server.isNotBlank() && validPort != null,
                    modifier = Modifier.testTag("connection-connect")
                ) {
                    Text(if (state.connecting) "connecting…" else "connect",
                        color = if (state.connecting || server.isBlank() || validPort == null)
                            Color.White.copy(alpha = 0.4f) else Color.White)
                }
            }
            TextButton(onClick = { focusManager.clearFocus(); onClose() }, enabled = !state.connecting) {
                Text("close", color = Color.White)
            }
        }
    }
}
