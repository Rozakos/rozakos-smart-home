package com.rozakos.home.data

import com.rozakos.home.data.device.DiscoveredDevice
import com.rozakos.home.data.device.mergeDiscovered
import com.rozakos.home.data.device.subnetHostsFor
import com.rozakos.home.data.protocol.DeviceInfo
import com.rozakos.home.data.protocol.DeviceProtocol
import com.rozakos.home.data.protocol.DeviceState
import com.rozakos.home.domain.model.DeviceTypeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Bodies below are exactly what the firmware handlers serialize (see docs/rozakos-local-protocol.md). */
class DeviceProtocolTest {
    @Test fun parsesStockTickerDeviceInfo() {
        val info = DeviceProtocol.parseInfo(
            """{"id":"cyd-ticker-ab12","name":"CYD Stock Ticker","type":"cyd_stock_ticker","fw":"1.0.0",
               "mac":"24:6F:28:AA:AB:12","webUiPath":"/settings","capabilities":["web_ui","settings","settings_api","state"]}""",
        )
        assertEquals("cyd-ticker-ab12", info.id)
        assertEquals(DeviceTypeId.CydStockTicker, DeviceTypeId.fromIdOrNull(info.type))
        assertTrue(info.has(DeviceInfo.CAP_STATE) && info.has(DeviceInfo.CAP_SETTINGS_API))
        assertEquals("/settings", info.webUiPath)
    }

    @Test fun parsesNucleoDeviceInfoWithoutMac() {
        val info = DeviceProtocol.parseInfo(
            """{"id":"nucleo-ticker-3f2a","name":"STM32 Stock Ticker","type":"nucleo_stock_ticker","fw":"1.1.0","webUiPath":"/","capabilities":["web_ui","settings","state"]}""",
        )
        assertNull(info.mac)
        assertFalse(info.has(DeviceInfo.CAP_SETTINGS_API))
        assertEquals(DeviceTypeId.NucleoStockTicker, DeviceTypeId.fromIdOrNull(info.type))
    }

    @Test fun unknownTypeIsNotMistakenForATicker() {
        assertNull(DeviceTypeId.fromIdOrNull("rozakos_lamp"))
    }

    @Test fun parsesQuotesWithNullsAndMarketState() {
        val state = DeviceProtocol.parseState(
            """{"quotes":[{"symbol":"AAPL","price":231.40,"change_pct":-1.25,"fresh":true,"ext_price":232.10},
               {"symbol":"NVDA","price":null,"change_pct":null,"fresh":false}],"market_state":"POST","updated":1791580000}""",
        ) as DeviceState.Quotes
        assertEquals(2, state.quotes.size)
        assertEquals(231.40, state.quotes[0].price!!, 1e-9)
        assertEquals(-1.25, state.quotes[0].changePercent!!, 1e-9)
        assertNull(state.quotes[1].price)
        assertFalse(state.quotes[1].fresh)
        assertEquals("POST", state.marketState)
    }

    @Test fun parsesFollowerAccounts() {
        val state = DeviceProtocol.parseState(
            """{"accounts":[{"platform":"youtube","handle":"@mkbhd","display_name":"Marques Brownlee","followers":19200000,"delta":250000},
               {"platform":"twitch","handle":"pokimane","display_name":"pokimane","followers":null}],"updated":1,"range":"1w"}""",
        ) as DeviceState.Followers
        assertEquals(19_200_000L, state.accounts[0].followers)
        assertEquals(250_000L, state.accounts[0].delta)
        assertNull(state.accounts[1].followers)
    }

    @Test fun stockSettingsRoundTrip() {
        val s = DeviceProtocol.parseStockSettings("""{"symbols":["AAPL","BTC-USD"],"refresh_s":60,"max_symbols":16,"has_api_key":true}""")
        assertEquals(listOf("AAPL", "BTC-USD"), s.symbols)
        assertEquals(16, s.maxSymbols)
        val body = org.json.JSONObject(DeviceProtocol.encodeStockSettings(s.copy(symbols = listOf("NVDA"), refreshSeconds = 120)))
        assertEquals("NVDA", body.getJSONArray("symbols").getString(0))
        assertEquals(1, body.getJSONArray("symbols").length())
        assertEquals(120, body.getInt("refresh_s"))
        assertEquals(2, body.length())   // the API token is never sent unless set explicitly
    }

    @Test fun symbolValidationMatchesFirmware() {
        assertEquals("BRK.B", DeviceProtocol.normalizeSymbol(" brk.b "))
        assertEquals("^GSPC", DeviceProtocol.normalizeSymbol("^gspc"))
        assertNull(DeviceProtocol.normalizeSymbol("TOOLONGSYMBOL1"))
        assertNull(DeviceProtocol.normalizeSymbol("AA PL"))
        assertNull(DeviceProtocol.normalizeSymbol(""))
    }

    @Test fun subnetEnumerationSkipsSelfNetworkAndBroadcast() {
        val hosts = subnetHostsFor("192.168.1.37", 24)
        assertEquals(253, hosts.size)
        assertEquals("192.168.1.1", hosts.first())
        assertEquals("192.168.1.254", hosts.last())
        assertFalse("192.168.1.37" in hosts)
        // a /16 would be 65k probes: scan the /24 around us instead
        assertEquals(253, subnetHostsFor("10.0.5.9", 16).size)
        // 10.0.0.0/30 has hosts .1 and .2; we are .1, so only .2 is probed
        assertEquals(listOf("10.0.0.2"), subnetHostsFor("10.0.0.1", 30))
    }

    @Test fun mergeKeepsOneEntryPerDevice() {
        val mdns = DiscoveredDevice("Desk ticker", "192.168.1.50", DeviceTypeId.CydStockTicker, "cyd-ticker-ab12", "1.0.0", DiscoveredDevice.Source.Mdns)
        val scan = mdns.copy(name = "CYD Stock Ticker", source = DiscoveredDevice.Source.SubnetScan)
        val other = DiscoveredDevice("STM32 Stock Ticker", "192.168.1.60", DeviceTypeId.NucleoStockTicker, "nucleo-ticker-3f2a", "1.1.0", DiscoveredDevice.Source.SubnetScan)
        var list = mergeDiscovered(emptyList(), mdns)
        list = mergeDiscovered(list, scan)
        list = mergeDiscovered(list, other)
        assertEquals(2, list.size)
        assertEquals("Desk ticker", list.first { it.uid == "cyd-ticker-ab12" }.name)
        // same device seen again at a new address (DHCP renew) updates in place
        list = mergeDiscovered(list, mdns.copy(ipAddress = "192.168.1.77"))
        assertEquals(2, list.size)
        assertEquals("192.168.1.77", list.first { it.uid == "cyd-ticker-ab12" }.ipAddress)
    }
}
