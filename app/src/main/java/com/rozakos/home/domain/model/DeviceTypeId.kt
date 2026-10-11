package com.rozakos.home.domain.model

/** Matches the `type` field of a device's `GET /api/device-info` and mDNS TXT record. */
enum class DeviceTypeId(val id: String) {
    CydStockTicker("cyd_stock_ticker"),
    CydFollowerTracker("cyd_follower_tracker"),
    NucleoStockTicker("nucleo_stock_ticker");

    companion object {
        fun fromId(id: String): DeviceTypeId =
            entries.firstOrNull { it.id == id } ?: CydStockTicker

        fun fromIdOrNull(id: String?): DeviceTypeId? =
            entries.firstOrNull { it.id == id }
    }
}
