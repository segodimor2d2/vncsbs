package com.rec.vncsbs.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rec.vncsbs.ui.RemoteFrame
import com.rec.vncsbs.vnc.VncClient
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VncUiState(
    val connected: Boolean = false,
    val connecting: Boolean = false,
    val frame: RemoteFrame = RemoteFrame()
)

class VncViewModel : ViewModel() {

    private val frameChannel =
        Channel<RemoteFrame>(Channel.CONFLATED)

    private val _uiState = MutableStateFlow(
        VncUiState(
            frame = createTestFrame()
        )
    )

    private val vncClient = VncClient(
        onConnectionChanged = { connected ->
            _uiState.update { it.copy(connected = connected, connecting = false) }
        }
    ) { frame ->

        println(
            "VncViewModel: RECEBEU FRAME " +
                "${frame.width}x${frame.height} " +
                "${frame.pixels.size} bytes"
        )

        frameChannel.trySend(frame)
    }

    val uiState: StateFlow<VncUiState> =
        _uiState.asStateFlow()

    init {
        viewModelScope.launch {

            for (frame in frameChannel) {

                _uiState.update { it.copy(frame = frame) }

                println(
                    "VncViewModel: STATE ATUALIZADO"
                )
            }
        }
    }

    fun testVncConnection() {
        if (_uiState.value.connecting || _uiState.value.connected) return
        _uiState.update { it.copy(connecting = true) }
        vncClient.connect(
            host = "192.168.31.127",
            port = 5900,
            password = "987654"
        )
    }

    fun connect(
        host: String,
        port: Int
    ) {
        if (_uiState.value.connecting || _uiState.value.connected) return
        _uiState.update { it.copy(connecting = true) }
        vncClient.connect(
            host = host,
            port = port,
            password = ""
        )
    }

    fun disconnect() {
        vncClient.disconnect()
    }

    private fun createTestFrame(): RemoteFrame {

        val width = 320
        val height = 240

        val pixels = ByteArray(
            width * height * 4
        )

        for (y in 0 until height) {
            for (x in 0 until width) {

                val index = (y * width + x) * 4

                val top = y < height / 2
                val left = x < width / 2

                val value = if (top == left) {
                    255
                } else {
                    80
                }

                pixels[index] = value.toByte()
                pixels[index + 1] = value.toByte()
                pixels[index + 2] = value.toByte()
                pixels[index + 3] = 255.toByte()
            }
        }

        return RemoteFrame(
            width = width,
            height = height,
            pixels = pixels
        )
    }
}
