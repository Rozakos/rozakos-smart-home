package com.rozakos.home.ui.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rozakos.home.data.protocol.DeviceApi
import com.rozakos.home.data.protocol.DeviceInfo
import com.rozakos.home.data.protocol.DeviceProtocol
import com.rozakos.home.data.protocol.DeviceState
import com.rozakos.home.data.protocol.StockTickerSettings
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
import kotlinx.coroutines.launch

/** Native watchlist editor state; [saved] is what the device has, [draft] what the user edits. */
data class WatchlistEditor(
    val saved: StockTickerSettings,
    val draft: StockTickerSettings,
    val newSymbol: String = "",
    val saving: Boolean = false,
    val message: String? = null,
) {
    val dirty: Boolean get() = saved != draft
}

data class DeviceDetailUiState(
    val device: Device? = null,
    val status: ConnectionStatus = ConnectionStatus.Unknown,
    val checking: Boolean = false,
    val info: DeviceInfo? = null,
    val state: DeviceState? = null,
    val watchlist: WatchlistEditor? = null,
)

private data class Live(
    val status: ConnectionStatus = ConnectionStatus.Unknown,
    val checking: Boolean = false,
    val info: DeviceInfo? = null,
    val state: DeviceState? = null,
    val watchlist: WatchlistEditor? = null,
)

class DeviceDetailViewModel(
    private val repository: DeviceRepository,
    private val statusChecker: DeviceStatusChecker,
    private val api: DeviceApi,
) : ViewModel() {
    private val selectedDeviceId = MutableStateFlow("")
    private val live = MutableStateFlow(Live())

    val uiState: StateFlow<DeviceDetailUiState> =
        combine(repository.devices, selectedDeviceId, live) { devices, id, l ->
            DeviceDetailUiState(
                device = devices.firstOrNull { it.id == id },
                status = l.status,
                checking = l.checking,
                info = l.info,
                state = l.state,
                watchlist = l.watchlist,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DeviceDetailUiState())

    fun load(deviceId: String) {
        selectedDeviceId.value = deviceId
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val device = repository.devices.first().firstOrNull { it.id == selectedDeviceId.value } ?: return@launch
            live.update { it.copy(checking = true) }
            val check = statusChecker.check(device)
            val info = check.info
            var state: DeviceState? = null
            var watchlist: WatchlistEditor? = live.value.watchlist?.takeIf { it.dirty }
            if (check.status == ConnectionStatus.Online) {
                repository.updateLastSeen(device.id, System.currentTimeMillis())
                if (info != null) repository.updateIdentity(device.id, device.ipAddress, info.id, info.firmware)
                if (info?.has(DeviceInfo.CAP_STATE) == true) {
                    state = runCatching { api.state(device.ipAddress) }.getOrNull()
                }
                if (watchlist == null && info?.has(DeviceInfo.CAP_SETTINGS_API) == true) {
                    watchlist = runCatching { api.stockSettings(device.ipAddress) }.getOrNull()
                        ?.let { WatchlistEditor(saved = it, draft = it) }
                }
            }
            live.value = Live(check.status, false, info, state, watchlist)
        }
    }

    // ---- watchlist editing ------------------------------------------------------------
    fun updateNewSymbol(text: String) = editWatchlist { it.copy(newSymbol = text.uppercase(), message = null) }

    fun addSymbol() = editWatchlist { w ->
        val symbol = DeviceProtocol.normalizeSymbol(w.newSymbol)
        when {
            symbol == null -> w.copy(message = "Use 1-12 letters, digits, '.', '-' or '^'.")
            symbol in w.draft.symbols -> w.copy(message = "$symbol is already on the list.", newSymbol = "")
            w.draft.symbols.size >= w.draft.maxSymbols -> w.copy(message = "The device shows up to ${w.draft.maxSymbols} symbols.")
            else -> w.copy(draft = w.draft.copy(symbols = w.draft.symbols + symbol), newSymbol = "", message = null)
        }
    }

    fun removeSymbol(symbol: String) = editWatchlist { w ->
        if (w.draft.symbols.size <= 1) w.copy(message = "Keep at least one symbol.")
        else w.copy(draft = w.draft.copy(symbols = w.draft.symbols - symbol), message = null)
    }

    fun moveSymbol(symbol: String, delta: Int) = editWatchlist { w ->
        val list = w.draft.symbols.toMutableList()
        val from = list.indexOf(symbol)
        val to = from + delta
        if (from < 0 || to !in list.indices) return@editWatchlist w
        list.add(to, list.removeAt(from))
        w.copy(draft = w.draft.copy(symbols = list))
    }

    fun setRefreshSeconds(seconds: Int) = editWatchlist { w ->
        w.copy(draft = w.draft.copy(refreshSeconds = seconds.coerceIn(15, 3600)))
    }

    fun discardWatchlistChanges() = editWatchlist { it.copy(draft = it.saved, message = null, newSymbol = "") }

    fun saveWatchlist() {
        val device = uiState.value.device ?: return
        val editor = live.value.watchlist ?: return
        viewModelScope.launch {
            editWatchlist { it.copy(saving = true, message = null) }
            val result = runCatching {
                api.saveStockSettings(device.ipAddress, editor.draft)
                api.stockSettings(device.ipAddress)
            }
            result.onSuccess { fresh ->
                live.update { it.copy(watchlist = WatchlistEditor(saved = fresh, draft = fresh, message = "Saved to the device.")) }
                runCatching { api.state(device.ipAddress) }.getOrNull()?.let { s -> live.update { it.copy(state = s) } }
            }.onFailure { e ->
                editWatchlist { it.copy(saving = false, message = "Couldn't save: ${e.message ?: "device unreachable"}") }
            }
        }
    }

    fun setRoom(room: String) {
        val device = uiState.value.device ?: return
        viewModelScope.launch { repository.updateRoom(device.id, room) }
    }

    private fun editWatchlist(change: (WatchlistEditor) -> WatchlistEditor) {
        live.update { l -> l.watchlist?.let { l.copy(watchlist = change(it)) } ?: l }
    }
}
