package com.rozakos.home.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.ui.graphics.vector.ImageVector

data class DeviceType(
    val id: DeviceTypeId,
    val category: DeviceCategory,
    val displayName: String,
    val icon: ImageVector,
    val webUiPath: String,
    val setupNetworkPrefix: String?,
    val setupUrl: String?,
    val setupInstructions: List<String>,
) {
    fun webUiUrl(ipAddress: String): String = "http://$ipAddress$webUiPath"
}

object DeviceTypeRegistry {
    val cydStockTicker = DeviceType(
        id = DeviceTypeId.CydStockTicker,
        category = DeviceCategory.StockTickers,
        displayName = "CYD Stock Ticker",
        icon = Icons.AutoMirrored.Outlined.TrendingUp,
        webUiPath = "/settings",
        setupNetworkPrefix = "CYD-Setup-",
        setupUrl = "http://192.168.4.1",
        setupInstructions = listOf(
            "Power on the CYD Stock Ticker.",
            "Join the WiFi network named CYD-Setup-XXXX from Android WiFi settings.",
            "Return here and open the setup portal.",
            "Connect the device to your home WiFi, then add it by IP address.",
        ),
    )

    val all: List<DeviceType> = listOf(cydStockTicker)

    fun byId(id: DeviceTypeId): DeviceType =
        all.firstOrNull { it.id == id } ?: cydStockTicker

    fun byCategory(category: DeviceCategory): List<DeviceType> =
        all.filter { it.category == category }

    fun placeholderIcon(): ImageVector = Icons.Outlined.Devices
}
