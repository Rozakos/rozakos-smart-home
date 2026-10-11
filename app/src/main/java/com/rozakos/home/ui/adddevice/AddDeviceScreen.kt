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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.rozakos.home.data.device.DiscoveredDevice
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rozakos.home.domain.model.DeviceCategory
import com.rozakos.home.domain.model.DeviceTypeRegistry

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
                NearbyDevicesSection(
                    scanning = uiState.scanning,
                    scannedOnce = uiState.scannedOnce,
                    devices = uiState.nearby,
                    onRescan = viewModel::scan,
                    onAdd = { pendingAdd = it },
                )
            }

            item {
                SectionTitle("Category")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(DeviceCategory.entries.size) { index ->
                        val category = DeviceCategory.entries[index]
                        FilterChip(
                            selected = uiState.selectedCategory == category,
                            onClick = { viewModel.selectCategory(category) },
                            label = { Text(category.displayName) },
                            enabled = DeviceTypeRegistry.byCategory(category).isNotEmpty(),
                        )
                    }
                }
            }

            item {
                SectionTitle("Device type")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DeviceTypeRegistry.byCategory(uiState.selectedCategory).forEach { type ->
                        val selected = type.id == uiState.selectedType
                        Card(
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
                                Icon(
                                    imageVector = type.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Column {
                                    Text(type.displayName, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        type.description,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (selectedType.bleProvisioning) item {
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

            item {
                SectionTitle("Manual connection")
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = viewModel::updateName,
                        label = { Text("Device name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = uiState.ipAddress,
                        onValueChange = viewModel::updateIpAddress,
                        label = { Text("IP address") },
                        placeholder = { Text("192.168.1.50") },
                        singleLine = true,
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
                    uiState.error?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Button(
                        onClick = viewModel::save,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Save device")
                    }
                }
            }

            if (selectedType.setupUrl != null) item {
                SectionTitle("Setup mode")
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
                        AssistChip(
                            onClick = {},
                            label = { Text("Setup AP: ${selectedType.setupNetworkPrefix}XXXX") },
                        )
                        OutlinedButton(onClick = onOpenSetupPortal) {
                            Icon(Icons.Outlined.OpenInBrowser, contentDescription = null)
                            Text("Open setup portal", modifier = Modifier.padding(start = 8.dp))
                        }
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
                scanning -> "Looking for Rozakos devices on your WiFi…"
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
                    Icon(type.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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
