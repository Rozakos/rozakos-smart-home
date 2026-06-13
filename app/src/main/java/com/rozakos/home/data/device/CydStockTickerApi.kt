package com.rozakos.home.data.device

data class CydStockTickerSettings(
    val symbols: List<String>,
    val refreshIntervalSeconds: Int,
    val bearerToken: String,
)

interface CydStockTickerApi {
    suspend fun getSettings(ipAddress: String): CydStockTickerSettings
    suspend fun saveSettings(ipAddress: String, settings: CydStockTickerSettings)
}

class TodoCydStockTickerApi : CydStockTickerApi {
    override suspend fun getSettings(ipAddress: String): CydStockTickerSettings {
        // TODO: Implement once firmware exposes a native endpoint, e.g. GET /api/settings.
        error("Native CYD settings endpoint is not available yet. Use WebUI fallback.")
    }

    override suspend fun saveSettings(ipAddress: String, settings: CydStockTickerSettings) {
        // TODO: Implement once firmware exposes a native endpoint, e.g. PUT /api/settings.
        error("Native CYD settings endpoint is not available yet. Use WebUI fallback.")
    }
}
