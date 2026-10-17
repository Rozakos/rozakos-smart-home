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
import kotlinx.coroutines.CancellationException
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
    val scanError: String? = null,
    val alreadyAddedCount: Int = 0,
    val saving: Boolean = false,
    val nameError: String? = null,
    val ipError: String? = null,
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
        _uiState.update { it.copy(scanning = true, nearby = emptyList(), scanError = null, alreadyAddedCount = 0) }
        scanJob = viewModelScope.launch {
            val known = repository.devices.first()
            discovery.discover()
                .catch { _uiState.update { it.copy(scanError = "Couldn't scan this network. Check Wi-Fi, then try again or use Manual.") } }
                .onCompletion { _uiState.update { it.copy(scanning = false, scannedOnce = true) } }
                .collect { found ->
                    val fresh = found.filterNot { d ->
                        known.any { k -> (d.uid != null && k.uid == d.uid) || k.ipAddress == d.ipAddress }
                    }
                    _uiState.update { it.copy(nearby = fresh, alreadyAddedCount = found.size - fresh.size) }
                }
        }
    }

    fun stopScan() { scanJob?.cancel(); scanJob = null }

    fun addDiscovered(device: DiscoveredDevice, name: String, room: String) {
        val type = DeviceTypeRegistry.byId(device.type)
        persist {
            addDevice(
                name = name.ifBlank { device.name },
                category = type.category,
                type = type.id,
                ipAddress = device.ipAddress,
                room = room,
                uid = device.uid,
                firmware = device.firmware,
            )
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
        _uiState.update { it.copy(name = value, error = null, nameError = null) }
    }

    fun updateIpAddress(value: String) {
        _uiState.update { it.copy(ipAddress = value, error = null, ipError = null) }
    }

    fun updateRoom(value: String) {
        _uiState.update { it.copy(room = value, error = null) }
    }

    fun save() {
        val state = _uiState.value
        val ip = normalizeIp(state.ipAddress)
        val nameError = if (state.name.isBlank()) "Enter a device name." else null
        val ipError = if (!isLikelyIpv4(ip)) "Use an IPv4 address such as 192.168.1.50." else null
        _uiState.update { it.copy(nameError = nameError, ipError = ipError, error = null) }
        if (nameError != null || ipError != null) return
        persist {
            addDevice(
                name = state.name,
                category = state.selectedCategory,
                type = state.selectedType,
                ipAddress = ip,
                room = state.room,
            )
        }
    }

    private fun persist(action: suspend () -> Unit) {
        if (_uiState.value.saving || _uiState.value.saved) return
        _uiState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                action()
                _uiState.update { it.copy(saved = true) }
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (error: Exception) {
                _uiState.update { it.copy(error = error.message ?: "Couldn't add the device. Try again.") }
            } finally { _uiState.update { it.copy(saving = false) } }
        }
    }
}
