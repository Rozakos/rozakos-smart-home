package com.rozakos.home.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.ui.graphics.vector.ImageVector

data class DeviceType(
    val id: DeviceTypeId,
    val category: DeviceCategory,
    val displayName: String,
    val description: String,
    val icon: ImageVector,
    val webUiPath: String,
    val setupNetworkPrefix: String?,
    val setupUrl: String?,
    val setupInstructions: List<String>,
    /** Supports the BLE WiFi provisioning flow (docs/ble-provisioning-protocol.md). */
    val bleProvisioning: Boolean = false,
) {
    fun webUiUrl(ipAddress: String): String = "http://$ipAddress$webUiPath"
}

object DeviceTypeRegistry {
    val cydStockTicker = DeviceType(
        id = DeviceTypeId.CydStockTicker,
        category = DeviceCategory.StockTickers,
        displayName = "CYD Stock Ticker",
        description = "2.8\" touch display with live quotes, charts and your portfolio.",
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
        bleProvisioning = true,
    )

    val cydFollowerTracker = DeviceType(
        id = DeviceTypeId.CydFollowerTracker,
        category = DeviceCategory.Displays,
        displayName = "CYD Follower Tracker",
        description = "Desk display for YouTube and Twitch follower counts with growth charts.",
        icon = Icons.Outlined.Groups,
        webUiPath = "/settings",
        setupNetworkPrefix = "CYD-Setup-",
        setupUrl = "http://192.168.4.1",
        setupInstructions = listOf(
            "Power on the Follower Tracker.",
            "Join the WiFi network named CYD-Setup-XXXX from Android WiFi settings.",
            "Return here and open the setup portal.",
            "Connect it to your home WiFi; it then shows up under Nearby devices.",
        ),
    )

    val nucleoStockTicker = DeviceType(
        id = DeviceTypeId.NucleoStockTicker,
        category = DeviceCategory.StockTickers,
        displayName = "STM32 Stock Ticker",
        description = "4.3\" STM32F746 Discovery ticker on Ethernet or WiFi, with price alerts.",
        icon = Icons.Outlined.Memory,
        webUiPath = "/",
        setupNetworkPrefix = null,
        setupUrl = null,
        setupInstructions = listOf(
            "Plug the board into your router with an Ethernet cable (or set up its ESP-01 WiFi).",
            "It gets an address over DHCP and shows up under Nearby devices.",
        ),
    )

    val dotMatrixStockTicker = DeviceType(
        id = DeviceTypeId.DotMatrixStockTicker,
        category = DeviceCategory.StockTickers,
        displayName = "Dot-Matrix Stock Ticker",
        description = "Two-row LED ticker with live stock and crypto quotes.",
        icon = Icons.Outlined.GridOn,
        webUiPath = "/settings",
        setupNetworkPrefix = "Ticker-Setup-",
        setupUrl = "http://192.168.4.1",
        setupInstructions = listOf(
            "Power on the Dot-Matrix Stock Ticker.",
            "Join Ticker-Setup-XXXX from Android WiFi settings.",
            "Open the setup portal and connect it to your WiFi.",
            "Return to your normal WiFi; the ticker appears under Nearby devices.",
        ),
    )

    val oledStockTicker = dotMatrixStockTicker.copy(
        id = DeviceTypeId.OledStockTicker,
        displayName = "OLED Stock Ticker",
        icon = Icons.Outlined.SmartDisplay,
        description = "Compact OLED display with live stock and crypto quotes.",
        setupInstructions = listOf(
            "Power on the OLED Stock Ticker.",
            "Join Ticker-Setup-XXXX from Android WiFi settings.",
            "Open the setup portal and connect it to your WiFi.",
            "Return to your normal WiFi; the ticker appears under Nearby devices.",
        ),
    )

    val all: List<DeviceType> = listOf(cydStockTicker, nucleoStockTicker, dotMatrixStockTicker, oledStockTicker, cydFollowerTracker)

    fun byId(id: DeviceTypeId): DeviceType =
        all.firstOrNull { it.id == id } ?: cydStockTicker

    fun byCategory(category: DeviceCategory): List<DeviceType> =
        all.filter { it.category == category }

    fun placeholderIcon(): ImageVector = Icons.Outlined.Devices
}
