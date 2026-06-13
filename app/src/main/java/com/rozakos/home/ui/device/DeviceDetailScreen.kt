package com.rozakos.home.ui.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rozakos.home.domain.model.ConnectionStatus
import com.rozakos.home.domain.model.DeviceTypeRegistry
import com.rozakos.home.ui.util.formatLastSeen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(
    viewModel: DeviceDetailViewModel,
    deviceId: String,
    onBack: () -> Unit,
    onOpenWebUi: (String, String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(deviceId) {
        viewModel.load(deviceId)
    }

    val device = uiState.device
    val type = device?.let { DeviceTypeRegistry.byId(it.type) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(device?.customName ?: "Device") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh, enabled = device != null) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                    }
                },
            )
        },
    ) { padding ->
        if (device == null || type == null) {
            Text(
                text = "Device not found.",
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp),
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(type.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text(device.customName, fontWeight = FontWeight.SemiBold)
                                Text(
                                    type.displayName,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                        if (uiState.checking) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        InfoRow("Status", uiState.status.label)
                        InfoRow("IP address", device.ipAddress)
                        InfoRow("Category", type.category.displayName)
                        InfoRow("Room", device.room ?: "Not assigned")
                        InfoRow("Last seen", formatLastSeen(device.lastSeenEpochMillis))
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Outlined.Settings, contentDescription = null)
                            Text("Stock Ticker settings", fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            text = "Native controls for symbols, refresh interval, and API token are ready to plug in once firmware exposes settings endpoints. For v1, the existing device WebUI is embedded here.",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Button(
                            onClick = { onOpenWebUi(device.customName, type.webUiUrl(device.ipAddress)) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Outlined.OpenInBrowser, contentDescription = null)
                            Text("Open settings", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = { onOpenWebUi("${device.customName} WebUI", type.webUiUrl(device.ipAddress)) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Open WebUI fallback")
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

private val ConnectionStatus.label: String
    get() = when (this) {
        ConnectionStatus.Unknown -> "Unknown"
        ConnectionStatus.Online -> "Online"
        ConnectionStatus.Offline -> "Offline"
    }
