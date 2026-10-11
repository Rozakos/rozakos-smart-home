package com.rozakos.home.data

import com.rozakos.home.data.protocol.DeviceApi
import com.rozakos.home.data.protocol.DeviceState
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

/** DeviceApi against a fake device speaking the protocol over real HTTP on localhost. */
class DeviceApiTest {
    private lateinit var server: ServerSocket
    private var symbols = mutableListOf("AAPL", "NVDA")
    private var refresh = 60
    private lateinit var api: DeviceApi

    @Before fun start() {
        server = ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))
        thread(isDaemon = true) {
            while (!server.isClosed) {
                val socket = runCatching { server.accept() }.getOrNull() ?: break
                thread(isDaemon = true) { socket.use { serve(it) } }
            }
        }
        api = DeviceApi(port = server.localPort)
    }

    @After fun stop() = server.close()

    /** Minimal HTTP/1.0-style handler: one request per connection. */
    private fun serve(socket: Socket) {
        val input = socket.getInputStream().bufferedReader()
        val requestLine = input.readLine() ?: return
        val (method, path) = requestLine.split(" ").let { it[0] to it[1] }
        var length = 0
        while (true) {
            val line = input.readLine() ?: break
            if (line.isEmpty()) break
            if (line.lowercase().startsWith("content-length:")) length = line.substringAfter(":").trim().toInt()
        }
        val body = CharArray(length).also { var read = 0; while (read < length) read += input.read(it, read, length - read) }.concatToString()
        val (code, reply) = route(method, path, body)
        val bytes = reply.toByteArray()
        socket.getOutputStream().apply {
            write("HTTP/1.1 $code X\r\nContent-Type: application/json\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray())
            write(bytes)
            flush()
        }
    }

    private fun route(method: String, path: String, body: String): Pair<Int, String> = when (path) {
        "/api/device-info" -> 200 to """{"id":"cyd-ticker-ab12","name":"CYD Stock Ticker","type":"cyd_stock_ticker","fw":"1.0.0","webUiPath":"/settings","capabilities":["web_ui","settings","settings_api","state"]}"""
        "/api/state" -> 200 to """{"quotes":[${symbols.joinToString(",") { """{"symbol":"$it","price":100.5,"change_pct":0.5,"fresh":true}""" }}]}"""
        "/api/settings" -> if (method == "POST") {
            val json = JSONObject(body)
            val arr = json.getJSONArray("symbols")
            val next = (0 until arr.length()).map { arr.getString(it) }
            if (next.isEmpty()) {
                400 to """{"error":"keep at least one symbol"}"""
            } else {
                symbols = next.toMutableList()
                refresh = json.getInt("refresh_s")
                200 to """{"ok":true}"""
            }
        } else {
            200 to """{"symbols":[${symbols.joinToString(",") { "\"$it\"" }}],"refresh_s":$refresh,"max_symbols":16}"""
        }
        else -> 404 to """{"error":"not found"}"""
    }

    @Test fun infoStateAndSettingsRoundTrip() = runTest {
        assertEquals("cyd-ticker-ab12", api.info("127.0.0.1").id)
        val settings = api.stockSettings("127.0.0.1")
        api.saveStockSettings("127.0.0.1", settings.copy(symbols = listOf("TSLA", "AAPL"), refreshSeconds = 120))
        assertEquals(listOf("TSLA", "AAPL"), api.stockSettings("127.0.0.1").symbols)
        assertEquals(120, refresh)
        val state = api.state("127.0.0.1") as DeviceState.Quotes
        assertEquals(listOf("TSLA", "AAPL"), state.quotes.map { it.symbol })
    }

    @Test fun deviceErrorMessageIsSurfaced() = runTest {
        val settings = api.stockSettings("127.0.0.1")
        try {
            api.saveStockSettings("127.0.0.1", settings.copy(symbols = emptyList()))
            fail("expected an error")
        } catch (e: IOException) {
            assertEquals("keep at least one symbol", e.message)
        }
    }

    @Test fun unreachableDeviceFailsFast() = runTest {
        val start = System.currentTimeMillis()
        val ok = runCatching { DeviceApi(port = 1).info("127.0.0.1", connectTimeoutMs = 500) }.isSuccess
        assertTrue(!ok && System.currentTimeMillis() - start < 3_000)
    }
}
