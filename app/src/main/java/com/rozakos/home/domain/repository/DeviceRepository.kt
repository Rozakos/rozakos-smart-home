package com.rozakos.home.domain.repository

import com.rozakos.home.domain.model.Device
import kotlinx.coroutines.flow.Flow

interface DeviceRepository {
    val devices: Flow<List<Device>>

    suspend fun addDevice(device: Device)
    suspend fun removeDevice(deviceId: String)
    suspend fun renameDevice(deviceId: String, name: String)
    suspend fun updateLastSeen(deviceId: String, epochMillis: Long)
}
