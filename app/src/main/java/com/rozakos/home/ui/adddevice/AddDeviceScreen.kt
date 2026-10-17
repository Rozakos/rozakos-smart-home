package com.rozakos.home.ui.adddevice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.rozakos.home.data.device.DiscoveredDevice
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rozakos.home.domain.model.DeviceCategory
import com.rozakos.home.domain.model.DeviceTypeRegistry
import com.rozakos.home.ui.components.DeviceArtwork
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import android.content.Intent
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeviceScreen(
    viewModel: AddDeviceViewModel,
    onBack: () -> Unit,
    onDeviceAdded: () -> Unit,
    onStartBleOnboarding: () -> Unit,
    onOpenSetupPortal: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedType = DeviceTypeRegistry.byId(uiState.selectedType)
    var pendingAdd by remember { mutableStateOf<DiscoveredDevice?>(null) }
    var mode by rememberSaveable { mutableStateOf(0) }
    val context = LocalContext.current

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onDeviceAdded()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add device") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                TabRow(selectedTabIndex = mode) {
                    listOf("Nearby", "Manual", "New device").forEachIndexed { index, title ->
                        Tab(selected = mode == index, onClick = { mode = index }, text = { Text(title) })
                    }
                }
                if (uiState.saving) LinearProgressIndicator(Modifier.fillMaxWidth())
                uiState.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp)) }
            }
            if (mode == 0) item {
                Text("Find devices on your network", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Connect your phone and device to the same Wi-Fi. Devices already in your home are hidden.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp))
                NearbyDevicesSection(
                    scanning = uiState.scanning,
                    scannedOnce = uiState.scannedOnce,
                    devices = uiState.nearby,
                    onRescan = viewModel::scan,
                    onStop = viewModel::stopScan,
                    error = uiState.scanError,
                    alreadyAddedCount = uiState.alreadyAddedCount,
                    onAdd = { pendingAdd = it },
                )
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = { mode = 2 }, modifier = Modifier.fillMaxWidth()) { Text("Set up a new device") }
                TextButton(onClick = { mode = 1 }, modifier = Modifier.fillMaxWidth()) { Text("Add by IP address") }
            }

            if (mode != 0) item {
                SectionTitle("Category")
                val categories = DeviceCategory.entries.filter { DeviceTypeRegistry.byCategory(it).isNotEmpty() }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories.size) { index ->
                        val category = categories[index]
                        FilterChip(
                            selected = uiState.selectedCategory == category,
                            onClick = { viewModel.selectCategory(category) },
                            label = { Text(category.displayName) },
                        )
                    }
                }
            }

            if (mode != 0) item {
                SectionTitle("Device type")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DeviceTypeRegistry.byCategory(uiState.selectedCategory).forEach { type ->
                        val selected = type.id == uiState.selectedType
                        Card(
                            modifier = Modifier.semantics { this.selected = selected },
                            onClick = { viewModel.selectType(type.id) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface,
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                DeviceArtwork(type.id, Modifier.size(54.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(type.displayName, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        type.description,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                                if (selected) Icon(Icons.Outlined.CheckCircle, contentDescription = "Selected device type",
                                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }

            if (mode == 2 && selectedType.bleProvisioning) item {
                SectionTitle("Quick setup")
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            "Set up over Bluetooth",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            "Send your WiFi details to a nearby device over Bluetooth — no network switching or IP address needed.",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Button(
                            onClick = onStartBleOnboarding,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Outlined.Bluetooth, contentDescription = null)
                            Text("Start Bluetooth setup", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }

            if (mode == 1) item {
                SectionTitle("Manual connection")
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = viewModel::updateName,
                        label = { Text("Device name") },
                        singleLine = true,
                        isError = uiState.nameError != null,
                        supportingText = { uiState.nameError?.let { Text(it) } },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = uiState.ipAddress,
                        onValueChange = viewModel::updateIpAddress,
                        label = { Text("IP address") },
                        placeholder = { Text("192.168.1.50") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, autoCorrectEnabled = false),
                        isError = uiState.ipError != null,
                        supportingText = { Text(uiState.ipError ?: "The address shown on your device or in your router.") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = uiState.room,
                        onValueChange = viewModel::updateRoom,
                        label = { Text("Room or group") },
                        placeholder = { Text("Office") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = viewModel::save,
                        enabled = !uiState.saving,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (uiState.saving) "Adding…" else "Add to home")
                    }
                }
            }

            if (mode == 2) item {
                SectionTitle(if (selectedType.setupUrl != null) "Wi-Fi setup" else "Connect the device")
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        selectedType.setupInstructions.forEachIndexed { index, text ->
                            Text("${index + 1}. $text")
                        }
                        if (selectedType.setupNetworkPrefix != null) Text("Setup network: ${selectedType.setupNetworkPrefix}XXXX",
                            style = MaterialTheme.typography.labelLarge)
                        if (selectedType.setupUrl != null) OutlinedButton(onClick = {
                            context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                        }, modifier = Modifier.fillMaxWidth()) { Text("Open Wi-Fi settings") }
                        if (selectedType.setupUrl != null) OutlinedButton(onClick = onOpenSetupPortal, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Outlined.OpenInBrowser, contentDescription = null)
                            Text("Open setup portal", modifier = Modifier.padding(start = 8.dp))
                        }
                        TextButton(onClick = { mode = 0; viewModel.scan() }, modifier = Modifier.fillMaxWidth()) { Text("Find it on my network") }
                    }
                }
            }

        }
    }

    pendingAdd?.let { device ->
        AddDiscoveredDialog(
            device = device,
            onDismiss = { pendingAdd = null },
            onConfirm = { name, room ->
                pendingAdd = null
                viewModel.addDiscovered(device, name, room)
            },
        )
    }
}

@Composable
private fun NearbyDevicesSection(
    scanning: Boolean,
    scannedOnce: Boolean,
    devices: List<DiscoveredDevice>,
    onRescan: () -> Unit,
    onStop: () -> Unit,
    error: String?,
    alreadyAddedCount: Int,
    onAdd: (DiscoveredDevice) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Nearby devices",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        if (scanning) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            TextButton(onClick = onStop) { Text("Stop") }
        } else {
            TextButton(onClick = onRescan) {
                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Scan again", modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    if (devices.isEmpty()) {
        Text(
            text = when {
                error != null -> error
                scanning -> "Looking for Rozakos devices on your WiFi…"
                alreadyAddedCount > 0 -> "All $alreadyAddedCount discovered devices are already in your home."
                scannedOnce -> "No new devices found. Make sure the device is powered on and on the same WiFi as this phone, or set it up below."
                else -> ""
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        devices.forEach { device ->
            val type = DeviceTypeRegistry.byId(device.type)
            Card(
                onClick = { onAdd(device) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DeviceArtwork(type.id, Modifier.size(48.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(device.name, fontWeight = FontWeight.SemiBold)
                        Text(
                            listOfNotNull(type.displayName, device.ipAddress, device.firmware?.let { "fw $it" })
                                .joinToString(" · "),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    FilledTonalButton(onClick = { onAdd(device) }) { Text("Add") }
                }
            }
        }
    }
}

@Composable
private fun AddDiscoveredDialog(
    device: DiscoveredDevice,
    onDismiss: () -> Unit,
    onConfirm: (name: String, room: String) -> Unit,
) {
    var name by remember(device) { mutableStateOf(device.name) }
    var room by remember(device) { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add ${DeviceTypeRegistry.byId(device.type).displayName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Found at ${device.ipAddress}.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("Room (optional)") },
                    placeholder = { Text("Office") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, room) }, enabled = name.isNotBlank()) { Text("Add device") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}
