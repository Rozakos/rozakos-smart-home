package com.rozakos.home.domain.repository

import com.rozakos.home.data.protocol.DeviceInfo
import com.rozakos.home.domain.model.ConnectionStatus
import com.rozakos.home.domain.model.Device

/** Result of one reachability check; [info] is set when the device speaks the protocol. */
data class DeviceCheck(
    val status: ConnectionStatus,
    val info: DeviceInfo? = null,
)

interface DeviceStatusChecker {
    suspend fun check(device: Device): DeviceCheck
}
