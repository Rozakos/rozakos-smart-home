package com.rozakos.home.data.device

import com.rozakos.home.data.local.DeviceDataStore
import com.rozakos.home.domain.model.Device
import com.rozakos.home.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class LocalDeviceRepository(
    private val dataStore: DeviceDataStore,
) : DeviceRepository {
    override val devices: Flow<List<Device>> = dataStore.devices

    override suspend fun addDevice(device: Device) {
        val current = dataStore.devices.first()
        dataStore.saveDevices(current + device)
    }

    override suspend fun removeDevice(deviceId: String) {
        val current = dataStore.devices.first()
        dataStore.saveDevices(current.filterNot { it.id == deviceId })
    }

    override suspend fun renameDevice(deviceId: String, name: String) {
        val current = dataStore.devices.first()
        dataStore.saveDevices(
            current.map { device ->
                if (device.id == deviceId) device.copy(customName = name.trim()) else device
            },
        )
    }

    override suspend fun updateLastSeen(deviceId: String, epochMillis: Long) {
        val current = dataStore.devices.first()
        dataStore.saveDevices(
            current.map { device ->
                if (device.id == deviceId) device.copy(lastSeenEpochMillis = epochMillis) else device
            },
        )
    }

    override suspend fun updateIdentity(deviceId: String, ipAddress: String, uid: String?, firmware: String?) {
        val current = dataStore.devices.first()
        dataStore.saveDevices(
            current.map { device ->
                if (device.id == deviceId) {
                    device.copy(
                        ipAddress = ipAddress,
                        uid = uid ?: device.uid,
                        firmware = firmware ?: device.firmware,
                    )
                } else {
                    device
                }
            },
        )
    }

    override suspend fun updateRoom(deviceId: String, room: String?) {
        val current = dataStore.devices.first()
        dataStore.saveDevices(
            current.map { device ->
                if (device.id == deviceId) device.copy(room = room?.trim()?.takeIf { it.isNotBlank() }) else device
            },
        )
    }
}
