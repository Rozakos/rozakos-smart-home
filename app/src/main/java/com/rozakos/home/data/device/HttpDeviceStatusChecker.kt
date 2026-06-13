package com.rozakos.home.data.device

import com.rozakos.home.domain.model.ConnectionStatus
import com.rozakos.home.domain.model.Device
import com.rozakos.home.domain.model.DeviceTypeRegistry
import com.rozakos.home.domain.repository.DeviceStatusChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class HttpDeviceStatusChecker : DeviceStatusChecker {
    override suspend fun check(device: Device): ConnectionStatus = withContext(Dispatchers.IO) {
        val type = DeviceTypeRegistry.byId(device.type)
        val urls = listOf(type.webUiUrl(device.ipAddress), "http://${device.ipAddress}/")

        for (url in urls) {
            val status = runCatching {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 1_500
                connection.readTimeout = 1_500
                connection.instanceFollowRedirects = true
                connection.requestMethod = "GET"
                connection.responseCode
            }.getOrNull()

            if (status != null && status in 200..399) {
                return@withContext ConnectionStatus.Online
            }
        }

        ConnectionStatus.Offline
    }
}
