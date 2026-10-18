package com.rozakos.home.domain.usecase

import com.rozakos.home.domain.model.Device
import com.rozakos.home.domain.model.DeviceCategory
import com.rozakos.home.domain.model.DeviceTypeId
import com.rozakos.home.domain.repository.DeviceRepository
import java.util.UUID
import kotlinx.coroutines.flow.first

class AddDeviceUseCase(
    private val repository: DeviceRepository,
) {
    suspend operator fun invoke(
        name: String,
        category: DeviceCategory,
        type: DeviceTypeId,
        ipAddress: String,
        room: String?,
        uid: String? = null,
        firmware: String? = null,
    ) {
        val existing = repository.devices.first()
        require(existing.none { it.matches(ipAddress, uid) }) {
            "This device is already in your home. Open its existing card instead."
        }
        repository.addDevice(
            Device(
                id = UUID.randomUUID().toString(),
                customName = name.trim(),
                category = category,
                type = type,
                ipAddress = ipAddress.trim(),
                room = room?.trim()?.takeIf { it.isNotBlank() },
                uid = uid,
                firmware = firmware,
            ),
        )
    }
}
