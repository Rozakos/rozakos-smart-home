package com.rozakos.home.data.device

import com.rozakos.home.data.protocol.DeviceApi
import com.rozakos.home.data.protocol.DeviceInfo
import com.rozakos.home.domain.model.ConnectionStatus
import com.rozakos.home.domain.model.Device
import com.rozakos.home.domain.model.DeviceTypeRegistry
import com.rozakos.home.domain.repository.DeviceCheck
import com.rozakos.home.domain.repository.DeviceStatusChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Online = the device answers `GET /api/device-info` (and it is the same device: a
 * different uid at that address means DHCP handed the IP to something else). Firmware
 * that predates the protocol is probed via its web UI instead.
 */
class HttpDeviceStatusChecker(
    private val api: DeviceApi,
) : DeviceStatusChecker {
    override suspend fun check(device: Device): DeviceCheck {
        val info: DeviceInfo? = runCatching { api.info(device.ipAddress) }.getOrNull()
        if (info != null) {
            val sameDevice = device.uid == null || device.uid == info.id
            return DeviceCheck(if (sameDevice) ConnectionStatus.Online else ConnectionStatus.Offline, info.takeIf { sameDevice })
        }
        return DeviceCheck(if (webUiAnswers(device)) ConnectionStatus.Online else ConnectionStatus.Offline, null)
    }

    private suspend fun webUiAnswers(device: Device): Boolean = withContext(Dispatchers.IO) {
        val type = DeviceTypeRegistry.byId(device.type)
        listOf(type.webUiUrl(device.ipAddress), "http://${device.ipAddress}/").any { url ->
            runCatching {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 1_500
                connection.readTimeout = 1_500
                connection.requestMethod = "GET"
                connection.responseCode.also { connection.disconnect() }
            }.getOrNull() in 200..399
        }
    }
}
