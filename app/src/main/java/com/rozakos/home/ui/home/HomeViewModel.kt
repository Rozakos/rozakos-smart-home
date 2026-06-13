package com.rozakos.home.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rozakos.home.domain.model.ConnectionStatus
import com.rozakos.home.domain.model.Device
import com.rozakos.home.domain.repository.DeviceRepository
import com.rozakos.home.domain.repository.DeviceStatusChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DeviceCardState(
    val device: Device,
    val status: ConnectionStatus = ConnectionStatus.Unknown,
)

data class HomeUiState(
    val devices: List<DeviceCardState> = emptyList(),
)

class HomeViewModel(
    private val repository: DeviceRepository,
    private val statusChecker: DeviceStatusChecker,
) : ViewModel() {
    private val statuses = MutableStateFlow<Map<String, ConnectionStatus>>(emptyMap())

    val uiState: StateFlow<HomeUiState> = repository.devices
        .combine(statuses) { devices, currentStatuses ->
            HomeUiState(
                devices = devices.map { device ->
                    DeviceCardState(
                        device = device,
                        status = currentStatuses[device.id] ?: ConnectionStatus.Unknown,
                    )
                },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun refreshStatuses() {
        viewModelScope.launch {
            uiState.value.devices.forEach { card ->
                launch {
                    val status = statusChecker.check(card.device)
                    statuses.value = statuses.value + (card.device.id to status)
                    if (status == ConnectionStatus.Online) {
                        repository.updateLastSeen(card.device.id, System.currentTimeMillis())
                    }
                }
            }
        }
    }

    fun rename(deviceId: String, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.renameDevice(deviceId, name)
        }
    }

    fun remove(deviceId: String) {
        viewModelScope.launch {
            repository.removeDevice(deviceId)
        }
    }
}
