package com.rozakos.home.ui.adddevice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rozakos.home.data.device.DeviceDiscoveryService
import com.rozakos.home.data.device.DiscoveredDevice
import com.rozakos.home.domain.model.DeviceCategory
import com.rozakos.home.domain.model.DeviceTypeId
import com.rozakos.home.domain.model.DeviceTypeRegistry
import com.rozakos.home.domain.repository.DeviceRepository
import com.rozakos.home.domain.usecase.AddDeviceUseCase
import com.rozakos.home.ui.util.isLikelyIpv4
import com.rozakos.home.ui.util.normalizeIp
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddDeviceUiState(
    val selectedCategory: DeviceCategory = DeviceCategory.StockTickers,
    val selectedType: DeviceTypeId = DeviceTypeId.CydStockTicker,
    val name: String = DeviceTypeRegistry.cydStockTicker.displayName,
    val ipAddress: String = "",
    val room: String = "",
    val error: String? = null,
    val saved: Boolean = false,
    val scanning: Boolean = false,
    /** Devices on the LAN that are not in the app yet. */
    val nearby: List<DiscoveredDevice> = emptyList(),
    val scannedOnce: Boolean = false,
)

class AddDeviceViewModel(
    private val addDevice: AddDeviceUseCase,
    private val discovery: DeviceDiscoveryService,
    private val repository: DeviceRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddDeviceUiState())
    val uiState: StateFlow<AddDeviceUiState> = _uiState
    private var scanJob: Job? = null

    init {
        scan()
    }

    fun scan() {
        if (scanJob?.isActive == true) return
        _uiState.update { it.copy(scanning = true, nearby = emptyList()) }
        scanJob = viewModelScope.launch {
            val known = repository.devices.first()
            discovery.discover()
                .catch { /* no WiFi / NSD unavailable: the manual path still works */ }
                .onCompletion { _uiState.update { it.copy(scanning = false, scannedOnce = true) } }
                .collect { found ->
                    val fresh = found.filterNot { d ->
                        known.any { k -> (d.uid != null && k.uid == d.uid) || k.ipAddress == d.ipAddress }
                    }
                    _uiState.update { it.copy(nearby = fresh) }
                }
        }
    }

    fun addDiscovered(device: DiscoveredDevice, name: String, room: String) {
        val type = DeviceTypeRegistry.byId(device.type)
        viewModelScope.launch {
            addDevice(
                name = name.ifBlank { device.name },
                category = type.category,
                type = type.id,
                ipAddress = device.ipAddress,
                room = room,
                uid = device.uid,
                firmware = device.firmware,
            )
            _uiState.update { it.copy(saved = true) }
        }
    }

    fun selectCategory(category: DeviceCategory) {
        val firstType = DeviceTypeRegistry.byCategory(category).firstOrNull() ?: return
        _uiState.update {
            it.copy(
                selectedCategory = category,
                selectedType = firstType.id,
                name = if (it.name.isBlank() || DeviceTypeRegistry.all.any { t -> t.displayName == it.name }) firstType.displayName else it.name,
                error = null,
            )
        }
    }

    fun selectType(type: DeviceTypeId) {
        val t = DeviceTypeRegistry.byId(type)
        _uiState.update {
            it.copy(
                selectedType = type,
                name = if (it.name.isBlank() || DeviceTypeRegistry.all.any { d -> d.displayName == it.name }) t.displayName else it.name,
                error = null,
            )
        }
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
