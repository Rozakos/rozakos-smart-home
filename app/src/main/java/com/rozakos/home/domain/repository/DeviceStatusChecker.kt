package com.rozakos.home.domain.repository

import com.rozakos.home.domain.model.ConnectionStatus
import com.rozakos.home.domain.model.Device

interface DeviceStatusChecker {
    suspend fun check(device: Device): ConnectionStatus
}
