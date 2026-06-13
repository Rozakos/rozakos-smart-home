package com.rozakos.home.domain.model

enum class DeviceTypeId(val id: String) {
    CydStockTicker("cyd_stock_ticker");

    companion object {
        fun fromId(id: String): DeviceTypeId =
            entries.firstOrNull { it.id == id } ?: CydStockTicker
    }
}
