package com.rozakos.home.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rozakos.home.data.device.DeviceDiscoveryService
import com.rozakos.home.data.protocol.DeviceApi
import com.rozakos.home.data.protocol.DeviceInfo
import com.rozakos.home.data.protocol.DeviceState
import com.rozakos.home.domain.model.ConnectionStatus
import com.rozakos.home.domain.model.Device
import com.rozakos.home.domain.repository.DeviceRepository
import com.rozakos.home.domain.repository.DeviceStatusChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

data class DeviceCardState(
    val device: Device,
    val status: ConnectionStatus = ConnectionStatus.Unknown,
    val state: DeviceState? = null,
)

/** Devices grouped like a smart-home app: one section per room, unassigned last. */
data class RoomSection(val room: String?, val devices: List<DeviceCardState>)

data class HomeUiState(
    val devices: List<DeviceCardState> = emptyList(),
    val refreshing: Boolean = false,
) {
    val online: Int get() = devices.count { it.status == ConnectionStatus.Online }
    val offline: Int get() = devices.count { it.status == ConnectionStatus.Offline }
    val sections: List<RoomSection>
        get() = devices.groupBy { it.device.room }
            .entries
            .sortedWith(compareBy({ it.key == null }, { it.key?.lowercase() }))
            .map { RoomSection(it.key, it.value) }
}

class HomeViewModel(
    private val repository: DeviceRepository,
    private val statusChecker: DeviceStatusChecker,
    private val api: DeviceApi,
    private val discovery: DeviceDiscoveryService,
) : ViewModel() {
    private val statuses = MutableStateFlow<Map<String, ConnectionStatus>>(emptyMap())
    private val states = MutableStateFlow<Map<String, DeviceState>>(emptyMap())
    private val refreshing = MutableStateFlow(false)

    val uiState: StateFlow<HomeUiState> =
        combine(repository.devices, statuses, states, refreshing) { devices, st, live, busy ->
            HomeUiState(
                devices = devices.map { device ->
                    DeviceCardState(
                        device = device,
                        status = st[device.id] ?: ConnectionStatus.Unknown,
                        state = live[device.id],
                    )
                },
                refreshing = busy,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun refreshStatuses() {
        if (refreshing.value) return
        viewModelScope.launch {
            refreshing.value = true
            repository.devices.first().map { device -> launch { refreshOne(device) } }.joinAll()
            refreshing.value = false
        }
    }

    private suspend fun refreshOne(device: Device) {
        var check = statusChecker.check(device)
        var current = device
        if (check.status == ConnectionStatus.Offline && device.uid != null) {
            // DHCP may have moved it: look it up by its id over mDNS.
            discovery.locate(device.uid)?.takeIf { it != device.ipAddress }?.let { newIp ->
                repository.updateIdentity(device.id, newIp, null, null)
                current = device.copy(ipAddress = newIp)
                check = statusChecker.check(current)
            }
        }
        statuses.update { it + (device.id to check.status) }
        if (check.status != ConnectionStatus.Online) return
        repository.updateLastSeen(device.id, System.currentTimeMillis())
        check.info?.let { info ->
            if (info.id != current.uid || info.firmware != current.firmware) {
                repository.updateIdentity(device.id, current.ipAddress, info.id, info.firmware)
            }
            if (info.has(DeviceInfo.CAP_STATE)) {
                runCatching { api.state(current.ipAddress) }.getOrNull()?.let { live ->
                    states.update { it + (device.id to live) }
                }
            }
        }
    }

    fun rename(deviceId: String, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.renameDevice(deviceId, name) }
    }

    fun remove(deviceId: String) {
        viewModelScope.launch { repository.removeDevice(deviceId) }
    }
}
