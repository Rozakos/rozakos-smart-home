package com.rozakos.home.data.device

import com.rozakos.home.domain.model.DeviceType

interface DeviceDiscoveryService {
    suspend fun discover(type: DeviceType): List<DiscoveredDevice>
}

data class DiscoveredDevice(
    val name: String,
    val ipAddress: String,
    val type: DeviceType,
)

class PlaceholderDeviceDiscoveryService : DeviceDiscoveryService {
    override suspend fun discover(type: DeviceType): List<DiscoveredDevice> {
        // TODO: Add subnet scanning and mDNS when firmware exposes /api/device-info.
        return emptyList()
    }
}
