package com.rozakos.home.domain.usecase

import com.rozakos.home.domain.model.Device
import com.rozakos.home.domain.model.DeviceCategory
import com.rozakos.home.domain.model.DeviceTypeId
import com.rozakos.home.domain.repository.DeviceRepository
import java.util.UUID

class AddDeviceUseCase(
    private val repository: DeviceRepository,
) {
    suspend operator fun invoke(
        name: String,
        category: DeviceCategory,
        type: DeviceTypeId,
        ipAddress: String,
        room: String?,
    ) {
        repository.addDevice(
            Device(
                id = UUID.randomUUID().toString(),
                customName = name.trim(),
                category = category,
                type = type,
                ipAddress = ipAddress.trim(),
                room = room?.trim()?.takeIf { it.isNotBlank() },
            ),
        )
    }
}
