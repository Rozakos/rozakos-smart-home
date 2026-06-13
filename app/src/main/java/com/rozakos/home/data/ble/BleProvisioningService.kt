package com.rozakos.home.data.ble

import kotlinx.coroutines.flow.Flow

/** A nearby device discovered while scanning for the provisioning service. */
data class ProvisioningCandidate(
    val name: String,
    val address: String,
    val rssi: Int,
)

/** Progress emitted while a single device is driven through WiFi provisioning. */
sealed interface ProvisioningProgress {
    data object Connecting : ProvisioningProgress
    data object Preparing : ProvisioningProgress
    data object SendingCredentials : ProvisioningProgress
    data object WaitingForWifi : ProvisioningProgress
    data class Success(val ipAddress: String, val info: ProvisionedDeviceInfo?) : ProvisioningProgress
    data class Failure(val message: String) : ProvisioningProgress
}

interface BleProvisioningService {
    /** True when a Bluetooth adapter exists and is switched on. */
    fun isBluetoothReady(): Boolean

    /** Emits the running set of nearby provisionable devices until the flow is cancelled. */
    fun scan(): Flow<List<ProvisioningCandidate>>

    /** Drives one device through WiFi provisioning, emitting progress until a terminal state. */
    fun provision(address: String, ssid: String, password: String): Flow<ProvisioningProgress>
}
