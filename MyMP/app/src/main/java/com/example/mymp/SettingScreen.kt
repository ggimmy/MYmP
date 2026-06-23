package com.example.mymp

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    viewModel: mympViewModel,
    onBack: () -> Unit
) {
    val uiState = viewModel.uiState
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeContentPadding()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {

        Row {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Torna indietro")
            }
            Text("Impostazioni server", modifier = Modifier.padding(top = 12.dp))
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        uiState.servers.forEach { server ->
            ServerEditRow(server = server, onSave = { updated -> viewModel.updateServer(updated) })
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        }
    }
}

@Composable
private fun ServerEditRow(
    server: ServerEntity,
    onSave: (ServerEntity) -> Unit
) {
    var name by remember(server.id) { mutableStateOf(server.serverName) }
    var ip by remember(server.id) { mutableStateOf(server.ipAddress) }

    Text("Server ${server.id}")

    OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Nome") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )

    OutlinedTextField(
        value = ip,
        onValueChange = { ip = it },
        label = { Text("Indirizzo IP") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )

    Button(
        onClick = { onSave(server.copy(serverName = name, ipAddress = ip)) },
        modifier = Modifier.padding(top = 4.dp)
    ) {
        Text("Salva Server ${server.id}")
    }
}