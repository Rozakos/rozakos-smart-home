package com.rozakos.home.ui.device

import androidx.compose.foundation.layout.Arrangement
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rozakos.home.data.protocol.DeviceState
import com.rozakos.home.data.market.MarketSymbol
import com.rozakos.home.domain.model.ConnectionStatus
import com.rozakos.home.domain.model.DeviceTypeRegistry
import com.rozakos.home.ui.components.LiveStateList
import com.rozakos.home.ui.components.DeviceArtwork
import com.rozakos.home.ui.components.DeviceStatusBadge
import com.rozakos.home.ui.components.deviceCaption
import com.rozakos.home.ui.util.formatLastSeen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner

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
    var exitDialog by rememberSaveable { mutableStateOf(false) }
    var connectionDetails by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val editor = uiState.watchlist
    val requestBack: () -> Unit = {
        when {
            editor?.saving == true -> scope.launch { snackbar.showSnackbar("Please wait for saving to finish.") }.let { }
            editor?.dirty == true -> exitDialog = true
            else -> onBack()
        }
    }
    BackHandler(enabled = editor?.dirty == true || editor?.saving == true, onBack = requestBack)
    LaunchedEffect(editor?.message) {
        if (editor?.message == "Saved to the device.") snackbar.showSnackbar("Watchlist saved to device")
    }

    LaunchedEffect(deviceId, lifecycleOwner) {
        viewModel.load(deviceId)
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        while (true) {      // keep live values fresh while the screen is open
            viewModel.refresh()
            delay(20_000)
        }
        }
    }

    val device = uiState.device
    val type = device?.let { DeviceTypeRegistry.byId(it.type) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(device?.customName ?: "Device") },
                navigationIcon = {
                    IconButton(onClick = requestBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh, enabled = device != null && !uiState.checking) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (editor != null && (editor.dirty || editor.saving)) {
                Surface(tonalElevation = 3.dp) {
                    Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (editor.saving) "Saving changes…" else "Unsaved changes",
                            style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        if (uiState.status == ConnectionStatus.Offline) Text("Reconnect the device to save.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = viewModel::saveWatchlist,
                                enabled = !editor.saving && uiState.status == ConnectionStatus.Online,
                                modifier = Modifier.weight(1f)) {
                                Text(if (editor.saving) "Saving…" else "Save to device")
                            }
                            OutlinedButton(onClick = viewModel::discardWatchlistChanges, enabled = !editor.saving) { Text("Discard") }
                        }
                    }
                }
            }
        },
    ) { padding ->
        if (device == null || type == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                if (uiState.loadingDevice) CircularProgressIndicator()
                else Text("Device not found.", modifier = Modifier.padding(16.dp))
            }
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
            uiState.dataError?.takeIf { uiState.status == ConnectionStatus.Online }?.let { message -> item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(message, style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = viewModel::refresh, enabled = !uiState.checking) { Text("Try again") }
                    }
                }
            } }
            if (uiState.status == ConnectionStatus.Offline) item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Device is offline", fontWeight = FontWeight.SemiBold)
                        Text("Check its power and make sure your phone is on the same Wi-Fi network.",
                            style = MaterialTheme.typography.bodyMedium)
                        OutlinedButton(onClick = viewModel::refresh, enabled = !uiState.checking) { Text("Try again") }
                    }
                }
            }
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
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            DeviceArtwork(device.type, Modifier.size(88.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                                Text(device.customName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                                Text(
                                    deviceCaption(device.type),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                DeviceStatusBadge(uiState.status)
                            }
                        }
                        if (uiState.checking) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Room", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(.4f))
                            TextButton(onClick = { editingRoom = true }, modifier = Modifier.weight(.6f)) {
                                Text(device.room ?: "Assign", fontWeight = FontWeight.Medium, maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            }
                        }
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
                                    is DeviceState.Quotes -> "Latest quotes" +
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
                        symbolNames = uiState.symbolNames,
                        search = uiState.symbolSearch,
                        onSearch = viewModel::searchSymbols,
                        onSelectSymbol = viewModel::addSearchResult,
                        onNewSymbol = viewModel::updateNewSymbol,
                        onAdd = viewModel::addSymbol,
                        onRemove = viewModel::removeSymbol,
                        onMove = viewModel::moveSymbol,
                        onRefreshSeconds = viewModel::setRefreshSeconds,
                    )
                }
            }

            item {
                TextButton(onClick = { connectionDetails = !connectionDetails }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (connectionDetails) "Hide connection details" else "Connection details")
                }
                if (connectionDetails) Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        InfoRow("IP address", device.ipAddress)
                        (uiState.info?.firmware ?: device.firmware)?.let { InfoRow("Firmware", it) }
                        device.uid?.let { InfoRow("Device ID", it) }
                        InfoRow("Last seen", formatLastSeen(device.lastSeenEpochMillis))
                    }
                }
            }
            item {
                if (editor?.dirty == true || editor?.saving == true) Text(
                    "Save or discard your changes before opening device web settings.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(
                    onClick = { onOpenWebUi(device.customName, webUiUrl) },
                    enabled = editor?.dirty != true && editor?.saving != true,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.OpenInBrowser, contentDescription = null)
                    Text(
                        "Device web settings",
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }

    if (exitDialog) AlertDialog(
        onDismissRequest = { exitDialog = false },
        title = { Text("Discard unsaved changes?") },
        text = { Text("Your device is still using the saved watchlist. These edits have not been applied.") },
        confirmButton = { TextButton(onClick = { viewModel.discardWatchlistChanges(); exitDialog = false; onBack() }) {
            Text("Discard and leave", color = MaterialTheme.colorScheme.error)
        } },
        dismissButton = { TextButton(onClick = { exitDialog = false }) { Text("Keep editing") } },
    )

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
    symbolNames: Map<String, String>,
    search: SymbolSearchState,
    onSearch: (String) -> Unit,
    onSelectSymbol: (MarketSymbol) -> Unit,
    onNewSymbol: (String) -> Unit,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
    onMove: (String, Int) -> Unit,
    onRefreshSeconds: (Int) -> Unit,
) {
    val draft = editor.draft
    var searching by rememberSaveable { mutableStateOf(false) }
    var manualEntry by rememberSaveable { mutableStateOf(false) }
    var changingInterval by rememberSaveable { mutableStateOf(false) }
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
            OutlinedButton(
                onClick = { onSearch(""); searching = true },
                enabled = !editor.saving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                Text("Find a company or symbol", modifier = Modifier.padding(start = 8.dp))
            }
            if (manualEntry) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = editor.newSymbol,
                    onValueChange = onNewSymbol,
                    label = { Text("Ticker symbol") },
                    placeholder = { Text("NVDA or BTC-USD") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onAdd() }),
                    modifier = Modifier.weight(1f),
                )
                FilledTonalIconButton(onClick = onAdd, enabled = editor.newSymbol.isNotBlank() && !editor.saving) {
                    Icon(Icons.Outlined.Add, contentDescription = "Add symbol")
                }
            }
            draft.symbols.forEachIndexed { index, symbol ->
                if (index > 0) HorizontalDivider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(symbol, fontWeight = FontWeight.Medium)
                        symbolNames[symbol]?.let { Text(it, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) }
                    }
                    IconButton(onClick = { onMove(symbol, -1) }, enabled = index > 0 && !editor.saving) {
                        Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "Move $symbol up")
                    }
                    IconButton(onClick = { onMove(symbol, 1) }, enabled = index < draft.symbols.lastIndex && !editor.saving) {
                        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Move $symbol down")
                    }
                    IconButton(onClick = { onRemove(symbol) }, enabled = draft.symbols.size > 1 && !editor.saving) {
                        Icon(Icons.Outlined.Close, contentDescription = "Remove $symbol")
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Refresh interval", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text("Every ${formatSeconds(draft.refreshSeconds)}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { changingInterval = true }, enabled = !editor.saving) { Text("Change") }
            }
            editor.message?.let {
                Text(it, color = if (it == "Saved to the device.") MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    if (changingInterval) RefreshIntervalDialog(
        initial = draft.refreshSeconds,
        onDismiss = { changingInterval = false },
        onConfirm = { onRefreshSeconds(it); changingInterval = false },
    )
    if (searching) SymbolSearchSheet(
        search = search, selected = draft.symbols, maxSymbols = draft.maxSymbols, saving = editor.saving,
        onQuery = onSearch,
        onSelect = { onSelectSymbol(it); searching = false; onSearch("") },
        onManual = { searching = false; manualEntry = true; onSearch("") },
        onDismiss = { searching = false; onSearch("") },
    )
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
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(.4f))
        Text(value, fontWeight = FontWeight.Medium, modifier = Modifier.weight(.6f),
            textAlign = TextAlign.End, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun RefreshIntervalDialog(initial: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var text by rememberSaveable(initial) { mutableStateOf(initial.toString()) }
    val seconds = text.toIntOrNull()
    val valid = seconds != null && seconds in 15..3600
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Refresh interval") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("How often should the device fetch new quotes?")
                OutlinedTextField(value = text, onValueChange = { text = it.take(5) },
                    label = { Text("Seconds") }, singleLine = true,
                    supportingText = { Text("15 to 3,600 seconds") }, isError = text.isNotEmpty() && !valid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (valid) onConfirm(seconds!!) }),
                    modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(15, 30, 60).forEach { value -> TextButton(onClick = { text = value.toString() }) {
                        Text(formatSeconds(value))
                    } }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(seconds!!) }, enabled = valid) { Text("Use interval") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun formatSeconds(s: Int): String = if (s % 60 == 0) "${s / 60} min" else if (s > 60) "${s / 60} min ${s % 60} s" else "$s s"

private fun marketLabel(state: String): String = when (state.uppercase()) {
    "PRE" -> "Pre-market"
    "REGULAR" -> "Market open"
    "POST" -> "After hours"
    "CLOSED" -> "Market closed"
    else -> state
}
