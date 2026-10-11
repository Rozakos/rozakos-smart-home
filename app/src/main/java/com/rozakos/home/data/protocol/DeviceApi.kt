package com.rozakos.home.data.protocol

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** HTTP client for the Rozakos local device protocol. All calls are LAN-only, plain HTTP. */
open class DeviceApi(
    private val port: Int = 80,
    private val connectTimeoutMs: Int = 1_500,
    private val readTimeoutMs: Int = 3_000,
) {
    open suspend fun info(ip: String, connectTimeoutMs: Int = this.connectTimeoutMs): DeviceInfo =
        DeviceProtocol.parseInfo(get(ip, "/api/device-info", connectTimeoutMs))

    open suspend fun state(ip: String): DeviceState? =
        DeviceProtocol.parseState(get(ip, "/api/state"))

    open suspend fun stockSettings(ip: String): StockTickerSettings =
        DeviceProtocol.parseStockSettings(get(ip, "/api/settings"))

    open suspend fun saveStockSettings(ip: String, settings: StockTickerSettings) {
        post(ip, "/api/settings", DeviceProtocol.encodeStockSettings(settings))
    }

    private fun url(ip: String, path: String) =
        URL(if (port == 80) "http://$ip$path" else "http://$ip:$port$path")

    protected suspend fun get(ip: String, path: String, connectTimeout: Int = connectTimeoutMs): String =
        withContext(Dispatchers.IO) {
            val conn = url(ip, path).openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = connectTimeout
                conn.readTimeout = readTimeoutMs
                conn.setRequestProperty("Accept", "application/json")
                val code = conn.responseCode
                if (code != 200) throw IOException("HTTP $code for $path")
                conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }
        }

    protected suspend fun post(ip: String, path: String, body: String): String =
        withContext(Dispatchers.IO) {
            val conn = url(ip, path).openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = connectTimeoutMs
                conn.readTimeout = readTimeoutMs
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.use { it.write(body.toByteArray()) }
                val code = conn.responseCode
                val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (code !in 200..299) {
                    val message = runCatching { org.json.JSONObject(text).optString("error") }.getOrNull()
                    throw IOException(message?.takeIf { it.isNotBlank() } ?: "HTTP $code")
                }
                text
            } finally {
                conn.disconnect()
            }
        }
}
