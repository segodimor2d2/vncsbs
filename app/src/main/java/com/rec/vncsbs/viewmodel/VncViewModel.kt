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
import org.json.JSONArray
import org.json.JSONObject

data class VncUiState(
    val connected: Boolean = false,
    val connecting: Boolean = false,
    val connectionError: String? = null,
    val savedConnections: List<VncConnection> = emptyList(),
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
    private var queuedConnection: VncConnection? = null

    private fun loadSavedConnections(): List<VncConnection> {
        val saved = connectionPreferences.getString("saved_connections", null)
        if (saved == null) return if (connectionPreferences.contains("host")) listOf(lastConnection()) else emptyList()
        return runCatching {
            val entries = JSONArray(saved)
            List(entries.length()) { index ->
                val entry = entries.getJSONObject(index)
                VncConnection(entry.getString("host"), entry.getInt("port"), entry.getString("password"))
            }
        }.getOrDefault(emptyList())
    }

    private fun persistSavedConnections(connections: List<VncConnection>) {
        val entries = JSONArray()
        connections.forEach { connection ->
            entries.put(JSONObject().put("host", connection.host)
                .put("port", connection.port).put("password", connection.password))
        }
        connectionPreferences.edit().putString("saved_connections", entries.toString()).apply()
        _uiState.update { it.copy(savedConnections = connections) }
    }

    fun deleteSavedConnection(connection: VncConnection) {
        persistSavedConnections(_uiState.value.savedConnections.filterNot {
            it.host == connection.host && it.port == connection.port
        })
    }

    fun connectSaved(connection: VncConnection) {
        if (_uiState.value.connecting) return
        if (_uiState.value.connected) {
            queuedConnection = connection
            _uiState.update { it.copy(connecting = true, connectionError = null) }
            vncClient.disconnect()
        } else connect(connection.host, connection.port, connection.password)
    }

    fun lastConnection() = VncConnection(
        host = connectionPreferences.getString("host", "192.168.1.100") ?: "192.168.1.100",
        port = connectionPreferences.getInt("port", 5900),
        password = connectionPreferences.getString("password", "") ?: ""
    )

    private val frameChannel =
        Channel<RemoteFrame>(Channel.CONFLATED)

    private val _uiState = MutableStateFlow(
        VncUiState(
            savedConnections = loadSavedConnections(),
            frame = createTestFrame()
        )
    )

    private val vncClient = VncClient(
        onConnectionChanged = { connected ->
            viewModelScope.launch {
                if (connected) {
                    pendingConnection?.let { connection ->
                        persistSavedConnections(listOf(connection) + _uiState.value.savedConnections.filterNot {
                            it.host == connection.host && it.port == connection.port
                        })
                        connectionPreferences.edit()
                            .putString("host", connection.host)
                            .putInt("port", connection.port)
                            .putString("password", connection.password)
                            .apply()
                    }
                }
                pendingConnection = null
                val nextConnection = if (!connected) queuedConnection else null
                if (!connected) queuedConnection = null
                _uiState.update {
                    it.copy(
                        connected = connected,
                        connecting = false,
                        connectionError = if (!connected && it.connecting)
                            if (nextConnection == null) "Não foi possível conectar. Confira o servidor, a porta e a senha." else null
                        else null
                    )
                }
                nextConnection?.let { connect(it.host, it.port, it.password) }
            }
        }
    ) { frame ->
        frameChannel.trySend(frame)
    }

    val uiState: StateFlow<VncUiState> =
        _uiState.asStateFlow()

    init {
        viewModelScope.launch {

            for (frame in frameChannel) {

                _uiState.update { it.copy(frame = frame) }
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

    fun sendText(text: String) {
        var index = 0
        while (index < text.length) {
            val codePoint = Character.codePointAt(text, index)
            val keysym = when (codePoint) {
                10, 13 -> 0xff0d
                9 -> 0xff09
                else -> if (codePoint <= 0xff) codePoint else 0x01000000 or codePoint
            }
            sendKeyEvent(keysym, true)
            sendKeyEvent(keysym, false)
            index += Character.charCount(codePoint)
        }
    }

    fun sendPointerEvent(x: Int, y: Int, buttons: Int) {
        val state = _uiState.value
        if (!state.connected || state.frame.width <= 0 || state.frame.height <= 0) return
        vncClient.sendPointerEvent(
            x.coerceIn(0, minOf(state.frame.width - 1, 65535)),
            y.coerceIn(0, minOf(state.frame.height - 1, 65535)),
            buttons
        )
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
