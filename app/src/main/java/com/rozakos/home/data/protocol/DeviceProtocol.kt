package com.rozakos.home.data.protocol

import org.json.JSONArray
import org.json.JSONObject

/**
 * Rozakos local device protocol, v1 (docs/rozakos-local-protocol.md).
 *
 * Every Rozakos device on the LAN serves plain HTTP on port 80:
 *  - `GET /api/device-info`  identity + capabilities (always)
 *  - `GET /api/state`        live data, shape depends on `type` (capability "state")
 *  - `GET|POST /api/settings` JSON settings (capability "settings_api")
 * and ESP32 devices also advertise `_rozakos._tcp` over mDNS.
 */
data class DeviceInfo(
    val id: String,
    val name: String,
    val type: String,
    val firmware: String?,
    val mac: String?,
    val webUiPath: String,
    val capabilities: Set<String>,
) {
    fun has(capability: String) = capability in capabilities

    companion object {
        const val CAP_STATE = "state"
        const val CAP_SETTINGS_API = "settings_api"
    }
}

data class Quote(
    val symbol: String,
    val price: Double?,
    val changePercent: Double?,
    val fresh: Boolean,
)

data class FollowerAccount(
    val platform: String,
    val handle: String,
    val displayName: String,
    val followers: Long?,
    val delta: Long?,
)

sealed interface DeviceState {
    data class Quotes(val quotes: List<Quote>, val marketState: String?) : DeviceState
    data class Followers(val accounts: List<FollowerAccount>) : DeviceState
}

data class StockTickerSettings(
    val symbols: List<String>,
    val refreshSeconds: Int,
    val maxSymbols: Int,
)

object DeviceProtocol {
    fun parseInfo(body: String): DeviceInfo {
        val json = JSONObject(body)
        val caps = json.optJSONArray("capabilities") ?: JSONArray()
        return DeviceInfo(
            id = json.getString("id"),
            name = json.optString("name").ifBlank { json.getString("id") },
            type = json.getString("type"),
            firmware = json.optString("fw").takeIf { it.isNotBlank() },
            mac = json.optString("mac").takeIf { it.isNotBlank() },
            webUiPath = json.optString("webUiPath").ifBlank { "/" },
            capabilities = (0 until caps.length()).map { caps.getString(it) }.toSet(),
        )
    }

    fun parseState(body: String): DeviceState? {
        val json = JSONObject(body)
        json.optJSONArray("quotes")?.let { arr ->
            return DeviceState.Quotes(
                quotes = (0 until arr.length()).map { i ->
                    val q = arr.getJSONObject(i)
                    Quote(
                        symbol = q.getString("symbol"),
                        price = q.optDoubleOrNull("price"),
                        changePercent = q.optDoubleOrNull("change_pct"),
                        fresh = q.optBoolean("fresh", true),
                    )
                },
                marketState = json.optString("market_state").takeIf { it.isNotBlank() },
            )
        }
        json.optJSONArray("accounts")?.let { arr ->
            return DeviceState.Followers(
                (0 until arr.length()).map { i ->
                    val a = arr.getJSONObject(i)
                    FollowerAccount(
                        platform = a.optString("platform"),
                        handle = a.optString("handle"),
                        displayName = a.optString("display_name").ifBlank { a.optString("handle") },
                        followers = a.optLongOrNull("followers")?.takeIf { it >= 0 },
                        delta = a.optLongOrNull("delta"),
                    )
                },
            )
        }
        return null
    }

    fun parseStockSettings(body: String): StockTickerSettings {
        val json = JSONObject(body)
        val arr = json.optJSONArray("symbols") ?: JSONArray()
        return StockTickerSettings(
            symbols = (0 until arr.length()).map { arr.getString(it) },
            refreshSeconds = json.optInt("refresh_s", 60),
            maxSymbols = json.optInt("max_symbols", 8),
        )
    }

    fun encodeStockSettings(settings: StockTickerSettings): String =
        JSONObject()
            .put("symbols", JSONArray(settings.symbols))
            .put("refresh_s", settings.refreshSeconds)
            .toString()

    /** Ticker symbols as the firmware accepts them: A-Z 0-9 . - ^, 1..12 chars. */
    fun normalizeSymbol(input: String): String? {
        val s = input.trim().uppercase()
        return s.takeIf { it.length in 1..12 && it.all { c -> c.isLetterOrDigit() || c in ".-^" } }
    }

    private fun JSONObject.optDoubleOrNull(key: String): Double? =
        if (has(key) && !isNull(key)) optDouble(key).takeIf { !it.isNaN() } else null

    private fun JSONObject.optLongOrNull(key: String): Long? =
        if (has(key) && !isNull(key)) optLong(key) else null
}
