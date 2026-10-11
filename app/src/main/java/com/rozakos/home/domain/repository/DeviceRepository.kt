package com.rozakos.home.domain.repository

import com.rozakos.home.domain.model.Device
import kotlinx.coroutines.flow.Flow

interface DeviceRepository {
    val devices: Flow<List<Device>>

    suspend fun addDevice(device: Device)
    suspend fun removeDevice(deviceId: String)
    suspend fun renameDevice(deviceId: String, name: String)
    suspend fun updateLastSeen(deviceId: String, epochMillis: Long)
    /** Record what the device reported about itself (and where it now lives). */
    suspend fun updateIdentity(deviceId: String, ipAddress: String, uid: String?, firmware: String?)
    suspend fun updateRoom(deviceId: String, room: String?)
}
