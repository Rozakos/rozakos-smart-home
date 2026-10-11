package com.rozakos.home.ui.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.rozakos.home.data.protocol.DeviceState
import com.rozakos.home.domain.model.ConnectionStatus
import com.rozakos.home.domain.model.DeviceTypeRegistry
import com.rozakos.home.ui.components.LiveStateList
import com.rozakos.home.ui.util.formatLastSeen
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(
    viewModel: DeviceDetailViewModel,
    deviceId: String,
    onBack: () -> Unit,
    onOpenWebUi: (String, String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var editingRoom by remember { mutableStateOf(false) }

    LaunchedEffect(deviceId) {
        viewModel.load(deviceId)
        while (true) {      // keep live values fresh while the screen is open
            delay(20_000)
            viewModel.refresh()
        }
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
        val webUiUrl = "http://${device.ipAddress}${uiState.info?.webUiPath ?: type.webUiPath}"

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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Room", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(onClick = { editingRoom = true }) {
                                Text(device.room ?: "Assign", fontWeight = FontWeight.Medium)
                            }
                        }
                        (uiState.info?.firmware ?: device.firmware)?.let { InfoRow("Firmware", it) }
                        device.uid?.let { InfoRow("Device ID", it) }
                        InfoRow("Last seen", formatLastSeen(device.lastSeenEpochMillis))
                    }
                }
            }

            uiState.state?.let { live ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                when (live) {
                                    is DeviceState.Quotes -> "On the display now" +
                                        (live.marketState?.let { " · ${marketLabel(it)}" } ?: "")
                                    is DeviceState.Followers -> "Followers"
                                },
                                fontWeight = FontWeight.SemiBold,
                            )
                            LiveStateList(live)
                        }
                    }
                }
            }

            uiState.watchlist?.let { editor ->
                item {
                    WatchlistCard(
                        editor = editor,
                        onNewSymbol = viewModel::updateNewSymbol,
                        onAdd = viewModel::addSymbol,
                        onRemove = viewModel::removeSymbol,
                        onMove = viewModel::moveSymbol,
                        onRefreshSeconds = viewModel::setRefreshSeconds,
                        onSave = viewModel::saveWatchlist,
                        onDiscard = viewModel::discardWatchlistChanges,
                    )
                }
            }

            if (uiState.status == ConnectionStatus.Offline) {
                item {
                    Text(
                        "The device isn't answering. Check that it's powered on and on the same WiFi as this phone.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            item {
                OutlinedButton(
                    onClick = { onOpenWebUi(device.customName, webUiUrl) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.OpenInBrowser, contentDescription = null)
                    Text(
                        if (uiState.watchlist != null) "All settings (device web page)" else "Open device settings",
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }

    if (editingRoom && device != null) {
        RoomDialog(
            initial = device.room.orEmpty(),
            onDismiss = { editingRoom = false },
            onSave = {
                viewModel.setRoom(it)
                editingRoom = false
            },
        )
    }
}

@Composable
private fun WatchlistCard(
    editor: WatchlistEditor,
    onNewSymbol: (String) -> Unit,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
    onMove: (String, Int) -> Unit,
    onRefreshSeconds: (Int) -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
) {
    val draft = editor.draft
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Watchlist", fontWeight = FontWeight.SemiBold)
            Text(
                "${draft.symbols.size} of ${draft.maxSymbols} symbols, shown in this order.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            draft.symbols.forEachIndexed { index, symbol ->
                if (index > 0) HorizontalDivider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(symbol, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onMove(symbol, -1) }, enabled = index > 0) {
                        Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "Move $symbol up")
                    }
                    IconButton(onClick = { onMove(symbol, 1) }, enabled = index < draft.symbols.lastIndex) {
                        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Move $symbol down")
                    }
                    IconButton(onClick = { onRemove(symbol) }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Remove $symbol")
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = editor.newSymbol,
                    onValueChange = onNewSymbol,
                    label = { Text("Add symbol") },
                    placeholder = { Text("AAPL or BTC-USD") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onAdd() }),
                    modifier = Modifier.weight(1f),
                )
                FilledTonalIconButton(onClick = onAdd, enabled = editor.newSymbol.isNotBlank()) {
                    Icon(Icons.Outlined.Add, contentDescription = "Add symbol")
                }
            }
            Text("Refresh every ${formatSeconds(draft.refreshSeconds)}", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = draft.refreshSeconds.toFloat(),
                onValueChange = { onRefreshSeconds((it / 15).toInt() * 15) },
                valueRange = 15f..600f,
            )
            editor.message?.let {
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            if (editor.dirty || editor.saving) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onSave, enabled = !editor.saving, modifier = Modifier.weight(1f)) {
                        Text(if (editor.saving) "Saving…" else "Save to device")
                    }
                    OutlinedButton(onClick = onDiscard, enabled = !editor.saving) { Text("Discard") }
                }
            }
        }
    }
}

@Composable
private fun RoomDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var room by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Room") },
        text = {
            OutlinedTextField(
                value = room,
                onValueChange = { room = it },
                label = { Text("Room or group") },
                placeholder = { Text("Office") },
                singleLine = true,
            )
        },
        confirmButton = { TextButton(onClick = { onSave(room) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
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

private fun formatSeconds(s: Int): String = if (s % 60 == 0) "${s / 60} min" else if (s > 60) "${s / 60} min ${s % 60} s" else "$s s"

private fun marketLabel(state: String): String = when (state.uppercase()) {
    "PRE" -> "Pre-market"
    "REGULAR" -> "Market open"
    "POST" -> "After hours"
    "CLOSED" -> "Market closed"
    else -> state
}

private val ConnectionStatus.label: String
    get() = when (this) {
        ConnectionStatus.Unknown -> "Checking…"
        ConnectionStatus.Online -> "Online"
        ConnectionStatus.Offline -> "Offline"
    }
