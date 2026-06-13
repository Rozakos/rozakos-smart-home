package com.rozakos.home.ui.adddevice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rozakos.home.domain.model.DeviceCategory
import com.rozakos.home.domain.model.DeviceTypeId
import com.rozakos.home.domain.usecase.AddDeviceUseCase
import com.rozakos.home.ui.util.isLikelyIpv4
import com.rozakos.home.ui.util.normalizeIp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddDeviceUiState(
    val selectedCategory: DeviceCategory = DeviceCategory.StockTickers,
    val selectedType: DeviceTypeId = DeviceTypeId.CydStockTicker,
    val name: String = "CYD Stock Ticker",
    val ipAddress: String = "",
    val room: String = "",
    val error: String? = null,
    val saved: Boolean = false,
)

class AddDeviceViewModel(
    private val addDevice: AddDeviceUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddDeviceUiState())
    val uiState: StateFlow<AddDeviceUiState> = _uiState

    fun selectCategory(category: DeviceCategory) {
        _uiState.update { it.copy(selectedCategory = category, error = null) }
    }

    fun selectType(type: DeviceTypeId) {
        _uiState.update { it.copy(selectedType = type, error = null) }
    }

    fun updateName(value: String) {
        _uiState.update { it.copy(name = value, error = null) }
    }

    fun updateIpAddress(value: String) {
        _uiState.update { it.copy(ipAddress = value, error = null) }
    }

    fun updateRoom(value: String) {
        _uiState.update { it.copy(room = value, error = null) }
    }

    fun save() {
        val state = _uiState.value
        val ip = normalizeIp(state.ipAddress)
        when {
            state.name.isBlank() -> {
                _uiState.update { it.copy(error = "Enter a device name.") }
                return
            }

            !isLikelyIpv4(ip) -> {
                _uiState.update { it.copy(error = "Enter a valid local IPv4 address, for example 192.168.1.50.") }
                return
            }
        }

        viewModelScope.launch {
            addDevice(
                name = state.name,
                category = state.selectedCategory,
                type = state.selectedType,
                ipAddress = ip,
                room = state.room,
            )
            _uiState.update { it.copy(saved = true) }
        }
    }
}
