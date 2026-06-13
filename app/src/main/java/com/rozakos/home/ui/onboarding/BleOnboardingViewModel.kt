package com.rozakos.home.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rozakos.home.data.ble.BleProvisioningService
import com.rozakos.home.data.ble.ProvisioningCandidate
import com.rozakos.home.data.ble.ProvisioningProgress
import com.rozakos.home.domain.model.DeviceCategory
import com.rozakos.home.domain.model.DeviceTypeId
import com.rozakos.home.domain.usecase.AddDeviceUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

enum class OnboardingStep { Scan, Credentials, Provisioning, Done }

data class BleOnboardingUiState(
    val step: OnboardingStep = OnboardingStep.Scan,
    val scanning: Boolean = false,
    val candidates: List<ProvisioningCandidate> = emptyList(),
    val selected: ProvisioningCandidate? = null,
    val ssid: String = "",
    val password: String = "",
    val progressLabel: String = "",
    val resultIp: String? = null,
    val deviceName: String = "CYD Stock Ticker",
    val room: String = "",
    val error: String? = null,
    val saved: Boolean = false,
)

class BleOnboardingViewModel(
    private val bleService: BleProvisioningService,
    private val addDevice: AddDeviceUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BleOnboardingUiState())
    val uiState: StateFlow<BleOnboardingUiState> = _uiState

    private var scanJob: Job? = null

    fun isBluetoothReady(): Boolean = bleService.isBluetoothReady()

    fun startScan() {
        if (scanJob?.isActive == true) return
        _uiState.update { it.copy(scanning = true, error = null, candidates = emptyList()) }
        scanJob = viewModelScope.launch {
            bleService.scan()
                .catch { e ->
                    _uiState.update { it.copy(scanning = false, error = e.message ?: "Scan failed.") }
                }
                .collect { candidates ->
                    _uiState.update { it.copy(candidates = candidates) }
                }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        _uiState.update { it.copy(scanning = false) }
    }

    fun select(candidate: ProvisioningCandidate) {
        stopScan()
        _uiState.update {
            it.copy(
                selected = candidate,
                step = OnboardingStep.Credentials,
                deviceName = candidate.name,
                error = null,
            )
        }
    }

    fun updateSsid(value: String) = _uiState.update { it.copy(ssid = value, error = null) }
    fun updatePassword(value: String) = _uiState.update { it.copy(password = value, error = null) }
    fun updateDeviceName(value: String) = _uiState.update { it.copy(deviceName = value, error = null) }
    fun updateRoom(value: String) = _uiState.update { it.copy(room = value, error = null) }

    fun backToScan() {
        _uiState.update {
            it.copy(step = OnboardingStep.Scan, selected = null, error = null)
        }
        startScan()
    }

    fun provision() {
        val state = _uiState.value
        val device = state.selected ?: return
        if (state.ssid.isBlank()) {
            _uiState.update { it.copy(error = "Enter your WiFi network name.") }
            return
        }

        _uiState.update {
            it.copy(step = OnboardingStep.Provisioning, error = null, progressLabel = "Connecting…")
        }

        viewModelScope.launch {
            val terminal = withTimeoutOrNull(90_000) {
                var last: ProvisioningProgress? = null
                bleService.provision(device.address, state.ssid, state.password)
                    .catch { e ->
                        _uiState.update {
                            it.copy(step = OnboardingStep.Credentials, error = e.message ?: "Provisioning failed.")
                        }
                    }
                    .collect { progress ->
                        last = progress
                        _uiState.update { it.copy(progressLabel = progress.label()) }
                        if (progress is ProvisioningProgress.Success) {
                            _uiState.update {
                                it.copy(
                                    step = OnboardingStep.Done,
                                    resultIp = progress.ipAddress,
                                    deviceName = progress.info?.name ?: it.deviceName,
                                )
                            }
                        } else if (progress is ProvisioningProgress.Failure) {
                            _uiState.update {
                                it.copy(step = OnboardingStep.Credentials, error = progress.message)
                            }
                        }
                    }
                last
            }

            if (terminal == null) {
                _uiState.update {
                    it.copy(
                        step = OnboardingStep.Credentials,
                        error = "Timed out waiting for the device to join WiFi.",
                    )
                }
            }
        }
    }

    fun saveDevice() {
        val state = _uiState.value
        val ip = state.resultIp ?: return
        if (state.deviceName.isBlank()) {
            _uiState.update { it.copy(error = "Enter a device name.") }
            return
        }
        viewModelScope.launch {
            addDevice(
                name = state.deviceName,
                category = DeviceCategory.StockTickers,
                type = DeviceTypeId.CydStockTicker,
                ipAddress = ip,
                room = state.room,
            )
            _uiState.update { it.copy(saved = true) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        scanJob?.cancel()
    }
}

private fun ProvisioningProgress.label(): String = when (this) {
    ProvisioningProgress.Connecting -> "Connecting to device…"
    ProvisioningProgress.Preparing -> "Preparing device…"
    ProvisioningProgress.SendingCredentials -> "Sending WiFi credentials…"
    ProvisioningProgress.WaitingForWifi -> "Device is joining your WiFi…"
    is ProvisioningProgress.Success -> "Connected."
    is ProvisioningProgress.Failure -> message
}
