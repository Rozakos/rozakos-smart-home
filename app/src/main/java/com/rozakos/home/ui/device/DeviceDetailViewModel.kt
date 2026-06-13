package com.rozakos.home.ui.device

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

data class DeviceDetailUiState(
    val device: Device? = null,
    val status: ConnectionStatus = ConnectionStatus.Unknown,
    val checking: Boolean = false,
)

class DeviceDetailViewModel(
    private val repository: DeviceRepository,
    private val statusChecker: DeviceStatusChecker,
) : ViewModel() {
    private val selectedDeviceId = MutableStateFlow("")
    private val status = MutableStateFlow(ConnectionStatus.Unknown)
    private val checking = MutableStateFlow(false)

    val uiState: StateFlow<DeviceDetailUiState> =
        combine(repository.devices, selectedDeviceId, status, checking) { devices, id, currentStatus, isChecking ->
            DeviceDetailUiState(
                device = devices.firstOrNull { it.id == id },
                status = currentStatus,
                checking = isChecking,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DeviceDetailUiState())

    fun load(deviceId: String) {
        selectedDeviceId.value = deviceId
        refresh()
    }

    fun refresh() {
        val device = uiState.value.device ?: return
        viewModelScope.launch {
            checking.value = true
            val result = statusChecker.check(device)
            status.value = result
            checking.value = false
            if (result == ConnectionStatus.Online) {
                repository.updateLastSeen(device.id, System.currentTimeMillis())
            }
        }
    }
}
