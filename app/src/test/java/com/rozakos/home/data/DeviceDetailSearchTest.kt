package com.rozakos.home.data

import com.rozakos.home.data.market.MarketSymbol
import com.rozakos.home.data.market.SymbolSearcher
import com.rozakos.home.data.protocol.*
import com.rozakos.home.domain.model.*
import com.rozakos.home.domain.repository.*
import com.rozakos.home.ui.device.DeviceDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceDetailSearchTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setMain() = Dispatchers.setMain(dispatcher)
    @After fun resetMain() = Dispatchers.resetMain()

    @Test fun selectionAddsTheSymbolToADraftAndRefreshCannotEraseIt() = runTest {
        val api = FakeApi()
        val vm = DeviceDetailViewModel(Repository(), checker, api, SymbolSearcher { listOf(NVIDIA) })
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        vm.load("oled"); advanceUntilIdle()
        vm.refresh(); runCurrent() // A network refresh is in flight when the result is selected.
        vm.searchSymbols("Nvidia")
        vm.addSearchResult(NVIDIA)
        vm.addSearchResult(NVIDIA) // Duplicate selection must not duplicate the symbol.
        advanceUntilIdle()
        assertEquals(listOf("INTC", "NVDA"), vm.uiState.value.watchlist!!.draft.symbols)
        assertEquals("Nvidia", vm.uiState.value.symbolSearch.query)
        assertEquals(0, api.writes)
        vm.saveWatchlist(); advanceUntilIdle()
        assertEquals(listOf("INTC", "NVDA"), api.settings.symbols)
        assertEquals(1, api.writes)
    }

    @Test fun switchingDevicesMidRefreshNeverShowsThePreviousDevicesData() = runTest {
        val repo = object : DeviceRepository by Repository() {
            override val devices = MutableStateFlow(listOf(
                Device("oled", "OLED", DeviceCategory.StockTickers, DeviceTypeId.OledStockTicker, "192.168.1.58"),
                Device("cyd", "CYD", DeviceCategory.StockTickers, DeviceTypeId.OledStockTicker, "192.168.1.59")))
        }
        val slowChecker = object : DeviceStatusChecker {
            override suspend fun check(device: Device): DeviceCheck {
                delay(if (device.id == "oled") 1000 else 100)
                return DeviceCheck(ConnectionStatus.Online, DeviceInfo(device.id, device.customName, "oled_stock_ticker",
                    null, null, "/settings", setOf(DeviceInfo.CAP_SETTINGS_API)))
            }
        }
        val vm = DeviceDetailViewModel(repo, slowChecker, FakeApi(), SymbolSearcher { emptyList() })
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        vm.load("oled"); advanceTimeBy(300); runCurrent() // OLED still checking.
        vm.load("cyd"); advanceUntilIdle()
        assertEquals("cyd", vm.uiState.value.device!!.id)
        assertEquals("cyd", vm.uiState.value.info!!.id)
        assertFalse(vm.uiState.value.checking)
    }

    @Test fun rapidQueriesCancelOldResultsAndClearingSearchClearsTheList() = runTest {
        val vm = DeviceDetailViewModel(Repository(), checker, FakeApi(), SymbolSearcher { query ->
            delay(if (query == "Nvidia") 1000 else 10)
            listOf(if (query == "Nvidia") NVIDIA else APPLE)
        })
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        vm.searchSymbols("Nvidia"); advanceTimeBy(200); runCurrent()
        vm.searchSymbols("Apple"); advanceUntilIdle()
        assertEquals("AAPL", vm.uiState.value.symbolSearch.results.single().symbol)
        assertNull(vm.uiState.value.symbolSearch.error)
        vm.searchSymbols(""); runCurrent()
        assertTrue(vm.uiState.value.symbolSearch.results.isEmpty())
        assertFalse(vm.uiState.value.symbolSearch.loading)
    }

    @Test fun directoryFailureOffersAManualFallbackWithoutLosingTheQuery() = runTest {
        val vm = DeviceDetailViewModel(Repository(), checker, FakeApi(), SymbolSearcher { error("Missing catalog") })
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        vm.searchSymbols("Nvidia"); advanceUntilIdle()
        assertEquals("Nvidia", vm.uiState.value.symbolSearch.query)
        assertNotNull(vm.uiState.value.symbolSearch.error)
        assertFalse(vm.uiState.value.symbolSearch.loading)
    }

    @Test fun savingIsSingleFlightAndDraftCannotChangeUnderAnInFlightSave() = runTest {
        var saved = StockTickerSettings(listOf("INTC"), 30, 16)
        var writes = 0
        val api = object : DeviceApi() {
            override suspend fun stockSettings(ip: String) = saved
            override suspend fun state(ip: String): DeviceState? = null
            override suspend fun saveStockSettings(ip: String, settings: StockTickerSettings) {
                writes++; delay(500); saved = settings
            }
        }
        val vm = DeviceDetailViewModel(Repository(), checker, api, SymbolSearcher { listOf(NVIDIA) })
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        vm.load("oled"); advanceUntilIdle()
        vm.addSearchResult(NVIDIA)
        vm.saveWatchlist(); vm.saveWatchlist()
        vm.removeSymbol("INTC"); vm.setRefreshSeconds(3600); vm.discardWatchlistChanges()
        advanceUntilIdle()
        assertEquals(1, writes)
        assertEquals(listOf("INTC", "NVDA"), saved.symbols)
        assertEquals(30, saved.refreshSeconds)
    }

    @Test fun failedSaveKeepsTheDraftAndLetsTheUserRetryOrEdit() = runTest {
        val api = object : DeviceApi() {
            override suspend fun stockSettings(ip: String) = StockTickerSettings(listOf("INTC"), 30, 16)
            override suspend fun state(ip: String): DeviceState? = null
            override suspend fun saveStockSettings(ip: String, settings: StockTickerSettings) { error("Device unreachable") }
        }
        val vm = DeviceDetailViewModel(Repository(), checker, api, SymbolSearcher { listOf(NVIDIA) })
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        vm.load("oled"); advanceUntilIdle(); vm.addSearchResult(NVIDIA)
        vm.saveWatchlist(); advanceUntilIdle()
        assertFalse(vm.uiState.value.watchlist!!.saving)
        assertTrue(vm.uiState.value.watchlist!!.dirty)
        assertTrue(vm.uiState.value.watchlist!!.message!!.contains("Couldn't save"))
        vm.addSearchResult(APPLE); vm.setRefreshSeconds(3600); runCurrent()
        assertEquals(listOf("INTC", "NVDA", "AAPL"), vm.uiState.value.watchlist!!.draft.symbols)
        assertEquals(3600, vm.uiState.value.watchlist!!.draft.refreshSeconds)
        assertEquals("Apple Inc.", vm.uiState.value.symbolNames["AAPL"])
    }

    private class FakeApi : DeviceApi() {
        var settings = StockTickerSettings(listOf("INTC"), 30, 16)
        var writes = 0
        override suspend fun stockSettings(ip: String) = settings
        override suspend fun state(ip: String): DeviceState? = null
        override suspend fun saveStockSettings(ip: String, settings: StockTickerSettings) {
            this.settings = settings; writes++
        }
    }

    private class Repository : DeviceRepository {
        override val devices = MutableStateFlow(listOf(Device("oled", "OLED", DeviceCategory.StockTickers,
            DeviceTypeId.OledStockTicker, "192.168.1.58")))
        override suspend fun addDevice(device: Device) = Unit
        override suspend fun removeDevice(deviceId: String) = Unit
        override suspend fun renameDevice(deviceId: String, name: String) = Unit
        override suspend fun updateLastSeen(deviceId: String, epochMillis: Long) = Unit
        override suspend fun updateIdentity(deviceId: String, ipAddress: String, uid: String?, firmware: String?) = Unit
        override suspend fun updateRoom(deviceId: String, room: String?) = Unit
    }

    companion object {
        private val NVIDIA = MarketSymbol("NVDA", "NVIDIA Corporation", "NASDAQ", "Stock")
        private val APPLE = MarketSymbol("AAPL", "Apple Inc.", "NASDAQ", "Stock")
        private val checker = object : DeviceStatusChecker {
            override suspend fun check(device: Device): DeviceCheck {
                delay(200)
                return DeviceCheck(ConnectionStatus.Online, DeviceInfo("oled", "OLED", "oled_stock_ticker",
                    null, null, "/settings", setOf(DeviceInfo.CAP_SETTINGS_API)))
            }
        }
    }
}
