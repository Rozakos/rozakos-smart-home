package com.rozakos.home.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.rozakos.home.data.ble.ProvisioningCandidate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BleOnboardingScreen(
    viewModel: BleOnboardingViewModel,
    onBack: () -> Unit,
    onDeviceAdded: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onDeviceAdded()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        if (result.values.all { it }) viewModel.startScan()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bluetooth setup") },
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
            when (uiState.step) {
                OnboardingStep.Scan -> scanStep(
                    uiState = uiState,
                    onScan = {
                        if (!viewModel.isBluetoothReady()) {
                            return@scanStep
                        }
                        permissionLauncher.launch(blePermissions())
                    },
                    onStop = viewModel::stopScan,
                    onSelect = viewModel::select,
                    bluetoothReady = viewModel.isBluetoothReady(),
                )

                OnboardingStep.Credentials -> credentialsStep(
                    uiState = uiState,
                    onSsid = viewModel::updateSsid,
                    onPassword = viewModel::updatePassword,
                    onProvision = viewModel::provision,
                    onBack = viewModel::backToScan,
                )

                OnboardingStep.Provisioning -> provisioningStep(uiState)

                OnboardingStep.Done -> doneStep(
                    uiState = uiState,
                    onName = viewModel::updateDeviceName,
                    onRoom = viewModel::updateRoom,
                    onSave = viewModel::saveDevice,
                )
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.scanStep(
    uiState: BleOnboardingUiState,
    onScan: () -> Unit,
    onStop: () -> Unit,
    onSelect: (ProvisioningCandidate) -> Unit,
    bluetoothReady: Boolean,
) {
    item {
        Text(
            "Power on the CYD Stock Ticker and keep it nearby. New devices advertise over Bluetooth for a few minutes after boot.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }

    if (!bluetoothReady) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(
                    "Turn on Bluetooth to scan for devices.",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }

    item {
        if (uiState.scanning) {
            OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) {
                CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                Text("Stop scan", modifier = Modifier.padding(start = 12.dp))
            }
        } else {
            Button(onClick = onScan, modifier = Modifier.fillMaxWidth(), enabled = bluetoothReady) {
                Icon(Icons.Outlined.Bluetooth, contentDescription = null)
                Text("Scan for devices", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }

    uiState.error?.let { error ->
        item {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
    }

    items(uiState.candidates, key = { it.address }) { candidate ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect(candidate) },
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Outlined.Bluetooth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(candidate.name, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${candidate.address} · ${candidate.rssi} dBm",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }

    if (uiState.scanning && uiState.candidates.isEmpty()) {
        item {
            Text(
                "Searching…",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.credentialsStep(
    uiState: BleOnboardingUiState,
    onSsid: (String) -> Unit,
    onPassword: (String) -> Unit,
    onProvision: () -> Unit,
    onBack: () -> Unit,
) {
    item {
        Text(
            "Send your home WiFi details to ${uiState.selected?.name ?: "the device"}.",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
    item {
        Text(
            "Use a 2.4 GHz network — most ESP32 boards do not support 5 GHz.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    item {
        OutlinedTextField(
            value = uiState.ssid,
            onValueChange = onSsid,
            label = { Text("WiFi network (SSID)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    item {
        OutlinedTextField(
            value = uiState.password,
            onValueChange = onPassword,
            label = { Text("WiFi password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
    uiState.error?.let { error ->
        item {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
    }
    item {
        Button(onClick = onProvision, modifier = Modifier.fillMaxWidth()) {
            Text("Send and connect")
        }
    }
    item {
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Choose a different device")
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.provisioningStep(
    uiState: BleOnboardingUiState,
) {
    item {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            CircularProgressIndicator()
            Text(uiState.progressLabel, style = MaterialTheme.typography.titleMedium)
            Text(
                "Keep the app open and the device powered on.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.doneStep(
    uiState: BleOnboardingUiState,
    onName: (String) -> Unit,
    onRoom: (String) -> Unit,
    onSave: () -> Unit,
) {
    item {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("Connected to WiFi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
    item {
        Text(
            "The device is reachable at ${uiState.resultIp}. Name it and add it to your home.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    item {
        OutlinedTextField(
            value = uiState.deviceName,
            onValueChange = onName,
            label = { Text("Device name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    item {
        OutlinedTextField(
            value = uiState.room,
            onValueChange = onRoom,
            label = { Text("Room or group") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    uiState.error?.let { error ->
        item {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
    }
    item {
        Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
            Text("Add to home")
        }
    }
}

private fun blePermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
