package com.rozakos.home.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import com.rozakos.home.domain.model.ConnectionStatus
import com.rozakos.home.domain.model.Device
import com.rozakos.home.ui.components.DeviceArtwork
import com.rozakos.home.ui.components.DeviceStatusBadge
import com.rozakos.home.ui.components.deviceCaption
import com.rozakos.home.ui.theme.BrandPalette
import com.rozakos.home.ui.components.CardLivePreview
import com.rozakos.home.ui.util.formatLastSeen
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAddDevice: () -> Unit,
    onOpenDevice: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var renameTarget by remember { mutableStateOf<Device?>(null) }
    var removeTarget by remember { mutableStateOf<Device?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(uiState.message) {
        uiState.message?.let { snackbar.showSnackbar(it); viewModel.clearMessage() }
    }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        while (true) {   // live values on the cards; cancelled when the screen leaves
            viewModel.refreshStatuses()
            delay(30_000)
        }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("ROZAKOS", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary)
                        Text("Home", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                actions = {
                    if (uiState.refreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp).padding(end = 4.dp), strokeWidth = 2.dp)
                    }
                    IconButton(onClick = viewModel::refreshStatuses) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh status")
                    }
                },
            )
        },
        floatingActionButton = {
            if (uiState.devices.isNotEmpty()) {
            ExtendedFloatingActionButton(
                modifier = Modifier.semantics { contentDescription = "Add device" },
                onClick = onAddDevice,
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("Add device", fontWeight = FontWeight.SemiBold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
            )
            }
        },
    ) { padding ->
        if (uiState.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (uiState.devices.isEmpty()) {
            EmptyHomeState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                onAddDevice = onAddDevice,
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item(key = "overview") { HomeOverview(uiState) }
                item(key = "devices-title") {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Your devices", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("${uiState.devices.size}", style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                .padding(horizontal = 12.dp, vertical = 6.dp))
                    }
                }
                val showRooms = uiState.sections.any { it.room != null }
                uiState.sections.forEach { section ->
                    if (showRooms) {
                        item(key = "room:${section.room}") {
                            Text(
                                text = section.room ?: "Other devices",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                    items(section.devices, key = { it.device.id }) { card ->
                        DeviceCard(
                            card = card,
                            onOpen = { onOpenDevice(card.device.id) },
                            onRename = { renameTarget = card.device },
                            onRemove = { removeTarget = card.device },
                        )
                    }
                }
            }
        }
    }

    renameTarget?.let { device ->
        RenameDeviceDialog(
            initialName = device.customName,
            onDismiss = { renameTarget = null },
            onSave = { name ->
                viewModel.rename(device.id, name)
                renameTarget = null
            },
        )
    }
    removeTarget?.let { device ->
        AlertDialog(
            onDismissRequest = { removeTarget = null },
            title = { Text("Remove ${device.customName}?") },
            text = { Text("This removes it from your home. The device keeps its Wi-Fi connection and settings.") },
            confirmButton = {
                TextButton(onClick = { viewModel.remove(device.id); removeTarget = null }) {
                    Text("Remove device", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { removeTarget = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun EmptyHomeState(
    modifier: Modifier = Modifier,
    onAddDevice: () -> Unit,
) {
    Box(modifier = modifier.padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DeviceArtwork(com.rozakos.home.domain.model.DeviceTypeId.OledStockTicker, Modifier.size(90.dp))
                DeviceArtwork(com.rozakos.home.domain.model.DeviceTypeId.CydFollowerTracker, Modifier.size(90.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Make yourself at home",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Bring your displays and tickers together. Add a device to get started.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = onAddDevice) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Text("Add device", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun HomeOverview(state: HomeUiState) {
    val status = when {
        state.online == state.devices.size -> if (state.online == 1) "Your device is online" else "All ${state.online} devices are online"
        state.offline > 0 -> "${state.online} online · ${state.offline} offline"
        else -> "Checking your devices…"
    }
    Row(
        Modifier.fillMaxWidth().background(
            Brush.linearGradient(listOf(BrandPalette.Navy, BrandPalette.HeaderEnd)),
            RoundedCornerShape(28.dp),
        ).padding(22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("YOUR SPACE", style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold, color = BrandPalette.Teal)
            Text("Welcome home.", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, color = Color.White)
            Text(status, style = MaterialTheme.typography.bodyMedium, color = BrandPalette.OnHeaderMuted)
        }
        Box(Modifier.size(58.dp).background(Color.White.copy(alpha = .1f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Home, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceCard(
    card: DeviceCardState,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onRemove: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DeviceArtwork(card.device.type, Modifier.size(74.dp))
                Column(Modifier.weight(1f).padding(top = 3.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(card.device.customName, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(deviceCaption(card.device.type), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    DeviceStatusBadge(card.status)
                }
                Box {
                    IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "Actions for ${card.device.customName}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                            onClick = { menuOpen = false; onRename() },
                        )
                        DropdownMenuItem(
                            text = { Text("Remove") },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                            onClick = { menuOpen = false; onRemove() },
                        )
                    }
                }
            }
            card.state?.takeIf { card.status == ConnectionStatus.Online }?.let { CardLivePreview(it) }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(card.device.ipAddress, style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (card.status != ConnectionStatus.Online) {
                        Text(if (card.device.lastSeenEpochMillis == null) "Not connected yet"
                            else "Last seen ${formatLastSeen(card.device.lastSeenEpochMillis)}", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text("Open device", style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun RenameDeviceDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename device") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Device name") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name) }, enabled = name.isNotBlank()) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
