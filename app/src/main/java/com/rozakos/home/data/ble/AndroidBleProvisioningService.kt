package com.rozakos.home.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * BLE provisioning over the Android Bluetooth stack.
 *
 * Permissions (BLUETOOTH_SCAN / BLUETOOTH_CONNECT on API 31+, ACCESS_FINE_LOCATION below) are the
 * caller's responsibility and are requested by the onboarding screen before either method runs,
 * hence the class-wide [SuppressLint] for "MissingPermission".
 */
@SuppressLint("MissingPermission")
class AndroidBleProvisioningService(
    context: Context,
) : BleProvisioningService {

    private val appContext = context.applicationContext
    private val bluetoothManager =
        appContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
    private val adapter: BluetoothAdapter? get() = bluetoothManager?.adapter

    override fun isBluetoothReady(): Boolean = adapter?.isEnabled == true

    override fun scan(): Flow<List<ProvisioningCandidate>> = callbackFlow {
        val scanner = adapter?.bluetoothLeScanner
        if (scanner == null) {
            close(IllegalStateException("Bluetooth is unavailable."))
            return@callbackFlow
        }

        val found = LinkedHashMap<String, ProvisioningCandidate>()
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val name = result.scanRecord?.deviceName ?: device.name ?: return
                found[device.address] = ProvisioningCandidate(name, device.address, result.rssi)
                trySend(found.values.toList())
            }

            override fun onScanFailed(errorCode: Int) {
                close(IllegalStateException("BLE scan failed (code $errorCode)."))
            }
        }

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(BleProvisioningProtocol.SERVICE_UUID))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner.startScan(listOf(filter), settings, callback)
        awaitClose { scanner.stopScan(callback) }
    }

    override fun provision(
        address: String,
        ssid: String,
        password: String,
    ): Flow<ProvisioningProgress> = callbackFlow {
        val scope = this
        val device = adapter?.getRemoteDevice(address)
        if (device == null) {
            scope.trySend(ProvisioningProgress.Failure("Device is no longer available."))
            scope.close()
            return@callbackFlow
        }

        var deviceInfo: ProvisionedDeviceInfo? = null

        fun fail(message: String) {
            scope.trySend(ProvisioningProgress.Failure(message))
            scope.close()
        }

        fun enableStatusNotifications(gatt: BluetoothGatt, service: BluetoothGattService) {
            val statusChar = service.getCharacteristic(BleProvisioningProtocol.STATUS_UUID)
            if (statusChar == null) {
                fail("Device does not expose a provisioning status channel.")
                return
            }
            gatt.setCharacteristicNotification(statusChar, true)
            val cccd = statusChar.getDescriptor(BleProvisioningProtocol.CCCD_UUID)
            if (cccd == null) {
                fail("Device status channel cannot be subscribed to.")
                return
            }
            writeDescriptorCompat(gatt, cccd, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        }

        fun handleStatus(value: ByteArray) {
            val status = runCatching { BleProvisioningProtocol.parseStatus(value) }.getOrNull() ?: return
            when (status.state.lowercase()) {
                "connected" -> {
                    val ip = status.ip
                    if (ip.isNullOrBlank()) {
                        fail("Device joined WiFi but reported no IP address.")
                    } else {
                        scope.trySend(ProvisioningProgress.Success(ip, deviceInfo))
                        scope.close()
                    }
                }
                "failed" -> fail(status.reason ?: "Device could not join the WiFi network.")
                "connecting" -> scope.trySend(ProvisioningProgress.WaitingForWifi)
                // "idle" and unknown states are ignored.
            }
        }

        val callback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        scope.trySend(ProvisioningProgress.Preparing)
                        gatt.requestMtu(247)
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> fail("Disconnected from device.")
                }
            }

            override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
                gatt.discoverServices()
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                val service = gatt.getService(BleProvisioningProtocol.SERVICE_UUID)
                if (service == null) {
                    fail("This device is not a Rozakos provisioning device.")
                    return
                }
                val infoChar = service.getCharacteristic(BleProvisioningProtocol.DEVICE_INFO_UUID)
                if (infoChar != null) {
                    gatt.readCharacteristic(infoChar)
                } else {
                    enableStatusNotifications(gatt, service)
                }
            }

            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray,
                status: Int,
            ) {
                if (characteristic.uuid == BleProvisioningProtocol.DEVICE_INFO_UUID &&
                    status == BluetoothGatt.GATT_SUCCESS
                ) {
                    deviceInfo = runCatching { BleProvisioningProtocol.parseDeviceInfo(value) }.getOrNull()
                }
                val service = gatt.getService(BleProvisioningProtocol.SERVICE_UUID) ?: return
                enableStatusNotifications(gatt, service)
            }

            @Deprecated("Kept for API < 33, which calls the 3-argument overload.")
            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int,
            ) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    @Suppress("DEPRECATION")
                    onCharacteristicRead(gatt, characteristic, characteristic.value ?: ByteArray(0), status)
                }
            }

            override fun onDescriptorWrite(
                gatt: BluetoothGatt,
                descriptor: BluetoothGattDescriptor,
                status: Int,
            ) {
                if (descriptor.uuid != BleProvisioningProtocol.CCCD_UUID) return
                scope.trySend(ProvisioningProgress.SendingCredentials)
                val service = gatt.getService(BleProvisioningProtocol.SERVICE_UUID) ?: return
                val credChar = service.getCharacteristic(BleProvisioningProtocol.CREDENTIALS_UUID)
                if (credChar == null) {
                    fail("Device cannot accept WiFi credentials.")
                    return
                }
                writeCharacteristicCompat(
                    gatt = gatt,
                    characteristic = credChar,
                    value = BleProvisioningProtocol.credentialsPayload(ssid, password),
                    writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
                )
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int,
            ) {
                if (characteristic.uuid != BleProvisioningProtocol.CREDENTIALS_UUID) return
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    scope.trySend(ProvisioningProgress.WaitingForWifi)
                } else {
                    fail("Failed to send WiFi credentials to the device.")
                }
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray,
            ) {
                if (characteristic.uuid == BleProvisioningProtocol.STATUS_UUID) handleStatus(value)
            }

            @Deprecated("Kept for API < 33, which calls the 2-argument overload.")
            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
            ) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    @Suppress("DEPRECATION")
                    onCharacteristicChanged(gatt, characteristic, characteristic.value ?: ByteArray(0))
                }
            }
        }

        scope.trySend(ProvisioningProgress.Connecting)
        val gatt = device.connectGatt(appContext, false, callback, BluetoothDevice.TRANSPORT_LE)
        awaitClose { gatt?.close() }
    }

    @Suppress("DEPRECATION")
    private fun writeDescriptorCompat(
        gatt: BluetoothGatt,
        descriptor: BluetoothGattDescriptor,
        value: ByteArray,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeDescriptor(descriptor, value)
        } else {
            descriptor.value = value
            gatt.writeDescriptor(descriptor)
        }
    }

    @Suppress("DEPRECATION")
    private fun writeCharacteristicCompat(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        writeType: Int,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(characteristic, value, writeType)
        } else {
            characteristic.writeType = writeType
            characteristic.value = value
            gatt.writeCharacteristic(characteristic)
        }
    }
}
