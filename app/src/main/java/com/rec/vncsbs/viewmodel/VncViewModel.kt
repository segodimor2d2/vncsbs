package com.rec.vncsbs.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
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
    val connectionError: String? = null,
    val frame: RemoteFrame = RemoteFrame()
)

data class VncConnection(
    val host: String = "192.168.1.100",
    val port: Int = 5900,
    val password: String = ""
)

class VncViewModel(application: Application) : AndroidViewModel(application) {
    private val connectionPreferences = application.getSharedPreferences("vnc_connection", 0)
    private var pendingConnection: VncConnection? = null

    fun lastConnection() = VncConnection(
        host = connectionPreferences.getString("host", "192.168.1.100") ?: "192.168.1.100",
        port = connectionPreferences.getInt("port", 5900),
        password = connectionPreferences.getString("password", "") ?: ""
    )

    private val frameChannel =
        Channel<RemoteFrame>(Channel.CONFLATED)

    private val _uiState = MutableStateFlow(
        VncUiState(
            frame = createTestFrame()
        )
    )

    private val vncClient = VncClient(
        onConnectionChanged = { connected ->
            viewModelScope.launch {
                if (connected) {
                    pendingConnection?.let { connection ->
                        connectionPreferences.edit()
                            .putString("host", connection.host)
                            .putInt("port", connection.port)
                            .putString("password", connection.password)
                            .apply()
                    }
                }
                pendingConnection = null
                _uiState.update {
                    it.copy(
                        connected = connected,
                        connecting = false,
                        connectionError = if (!connected && it.connecting)
                            "Não foi possível conectar. Confira o servidor, a porta e a senha."
                        else null
                    )
                }
            }
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

    fun connect(host: String, port: Int, password: String = "") {
        if (_uiState.value.connecting || _uiState.value.connected) return
        if (host.isBlank() || port !in 1..65535) return
        pendingConnection = VncConnection(host.trim(), port, password)
        _uiState.update { it.copy(connecting = true, connectionError = null) }
        vncClient.connect(host = host.trim(), port = port, password = password)
    }

    fun disconnect() {
        vncClient.disconnect()
    }

    fun sendKeyEvent(keysym: Int, down: Boolean) {
        if (_uiState.value.connected) vncClient.sendKeyEvent(keysym, down)
    }

    override fun onCleared() {
        vncClient.close()
        super.onCleared()
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
