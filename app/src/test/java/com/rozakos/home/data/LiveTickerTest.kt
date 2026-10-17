package com.rozakos.home.data

import com.rozakos.home.data.protocol.DeviceApi
import com.rozakos.home.data.protocol.DeviceInfo
import com.rozakos.home.data.protocol.DeviceState
import com.rozakos.home.domain.model.DeviceTypeId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Opt-in hardware test: ROZAKOS_TEST_DEVICE_IP=192.168.x.x ./gradlew testDebugUnitTest. */
class LiveTickerTest {
    @Test fun nativeClientDiscoversReadsAndEditsRealTicker() = runBlocking {
        val ip = System.getenv("ROZAKOS_TEST_DEVICE_IP").orEmpty()
        assumeTrue("Set ROZAKOS_TEST_DEVICE_IP to run against hardware", ip.isNotBlank())
        val api = DeviceApi(port = System.getenv("ROZAKOS_TEST_DEVICE_PORT")?.toInt() ?: 80,
            readTimeoutMs = 8_000)
        val info = api.info(ip)
        assertEquals(DeviceTypeId.OledStockTicker, DeviceTypeId.fromIdOrNull(info.type))
        assertTrue(info.has(DeviceInfo.CAP_STATE) && info.has(DeviceInfo.CAP_SETTINGS_API))
        val original = api.stockSettings(ip)
        try {
            val draft = original.copy(symbols = listOf("AAPL", "NVDA"), refreshSeconds = 30)
            api.saveStockSettings(ip, draft)
            val saved = api.stockSettings(ip)
            assertEquals(draft.symbols, saved.symbols)
            assertEquals(draft.refreshSeconds, saved.refreshSeconds)
            val state = api.state(ip) as DeviceState.Quotes
            assertEquals(draft.symbols, state.quotes.map { it.symbol })
        } finally {
            api.saveStockSettings(ip, original)
        }
    }
}
