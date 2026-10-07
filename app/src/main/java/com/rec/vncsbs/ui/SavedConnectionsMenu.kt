package com.rec.vncsbs.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import com.rec.vncsbs.viewmodel.VncConnection

@Composable
internal fun SavedConnectionsMenu(
    connections: List<VncConnection>,
    connecting: Boolean,
    onConnect: (VncConnection) -> Unit,
    onDelete: (VncConnection) -> Unit
) {
    var deletion by remember { mutableStateOf<VncConnection?>(null) }
    Column(Modifier.testTag("saved-connections-list")) {
        if (connections.isEmpty()) Text("Nenhuma conexão salva", color = Color.White)
        connections.forEach { connection ->
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = { onConnect(connection) },
                        enabled = !connecting
                    ) {
                        Text("${connection.host}:${connection.port}",
                            color = Color.White.copy(alpha = if (connecting) 0.4f else 1f))
                    }
                    TextButton(
                        onClick = { deletion = if (deletion == connection) null else connection },
                        modifier = Modifier.testTag("delete-${connection.host}:${connection.port}")
                    ) { Text("x", color = Color.White) }
                }
                if (deletion == connection) {
                    Column {
                        Text("Apagar esta conexão?", color = Color.White)
                        Row {
                            TextButton(onClick = { onDelete(connection); deletion = null }) {
                                Text("confirmar e deletar", color = Color(0xFFFF8080))
                            }
                            TextButton(onClick = { deletion = null }) {
                                Text("cancelar", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
