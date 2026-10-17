package com.rozakos.home.data.market

import com.rozakos.home.data.protocol.DeviceProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.Normalizer
import java.util.Locale

data class MarketSymbol(val symbol: String, val name: String, val exchange: String, val kind: String) {
    internal val nameKey = searchKey(name)
}

fun interface SymbolSearcher {
    suspend fun search(query: String): List<MarketSymbol>
    suspend fun namesFor(symbols: List<String>): Map<String, String> = emptyMap()
}

private fun searchKey(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT)
    .replace(Regex("[^a-z0-9]+"), " ").trim()

/** Searches a local directory. Company names and exact symbols rank above related funds. */
fun findSymbols(entries: List<MarketSymbol>, input: String, limit: Int = 25): List<MarketSymbol> {
    val query = input.trim().take(80)
    val key = searchKey(query)
    if (key.isBlank()) return emptyList()
    val words = key.split(' ').filter(String::isNotBlank)
    return entries.mapNotNull { entry ->
        val rank = when {
            entry.symbol.equals(query, ignoreCase = true) -> 0
            entry.nameKey == key -> 1
            entry.nameKey.startsWith(key) -> 2
            entry.symbol.startsWith(query, ignoreCase = true) -> 3
            words.all { word -> entry.nameKey.split(' ').any { it.startsWith(word) } } -> 4
            entry.nameKey.contains(key) -> 5
            else -> return@mapNotNull null
        }
        entry to rank
    }.sortedWith(compareBy({ it.second }, { if (it.first.kind == "ETF") 1 else 0 }, { it.first.symbol }))
        .take(limit).map { it.first }
}

/** Bundled directory works offline on first use; refreshes public metadata at most daily. */
class SymbolCatalog(
    private val directory: File,
    private val bundledJson: () -> String,
    private val download: (String) -> String = ::downloadCatalog,
) : SymbolSearcher {
    private val mutex = Mutex()
    @Volatile private var entries: List<MarketSymbol>? = null
    private val cache get() = File(directory, "catalog.json")

    private suspend fun load(): List<MarketSymbol> = withContext(Dispatchers.IO) {
        mutex.withLock {
            entries ?: (runCatching { decodeCatalog(cache.readText()) }.getOrNull()
                ?: decodeCatalog(bundledJson())).also { entries = it }
        }
    }

    override suspend fun search(query: String): List<MarketSymbol> {
        if (query.isBlank()) return emptyList()
        val snapshot = load()
        return withContext(Dispatchers.Default) { findSymbols(snapshot, query) }
    }

    override suspend fun namesFor(symbols: List<String>): Map<String, String> {
        val requested = symbols.toSet()
        return load().filter { it.symbol in requested }.associate { it.symbol to it.name }
    }

    suspend fun refreshIfNeeded() = withContext(Dispatchers.IO) {
        val original = load()
        directory.mkdirs()
        val attempted = File(directory, "last-attempt")
        if (attempted.exists() && System.currentTimeMillis() - attempted.lastModified() < 86_400_000) return@withContext
        attempted.writeText("")
        // Keep the existing directory if either stock source is unavailable.
        val stocks = runCatching {
            val nasdaq = parseNasdaqDirectory(download(NASDAQ_URL), "NASDAQ")
            val other = parseNasdaqDirectory(download(OTHER_URL))
            check(nasdaq.size >= 1000 && other.size >= 1000)
            nasdaq + other
        }.getOrElse { return@withContext }
        val crypto = runCatching { parseCryptoDirectory(download(CRYPTO_URL)) }
            .getOrNull()?.takeIf { it.isNotEmpty() } ?: original.filter { it.kind == "Crypto" }
        val updated = (stocks + crypto).distinctBy { it.symbol }.sortedBy { it.symbol }
        mutex.withLock {
            val temporary = File(directory, "catalog.tmp")
            temporary.writeText(encodeCatalog(updated))
            check(temporary.renameTo(cache)) { "Could not save the symbol directory" }
            entries = updated
        }
    }

    companion object {
        const val NASDAQ_URL = "https://www.nasdaqtrader.com/dynamic/SymDir/nasdaqlisted.txt"
        const val OTHER_URL = "https://www.nasdaqtrader.com/dynamic/SymDir/otherlisted.txt"
        const val CRYPTO_URL = "https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd&order=market_cap_desc&per_page=250&page=1&sparkline=false"
    }
}

internal fun decodeCatalog(json: String): List<MarketSymbol> {
    val array = JSONArray(json)
    val entries = (0 until array.length()).mapNotNull { index ->
        val row = array.getJSONObject(index)
        val symbol = DeviceProtocol.normalizeSymbol(row.optString("symbol")) ?: return@mapNotNull null
        val name = row.optString("name").trim()
        if (name.isEmpty()) return@mapNotNull null
        MarketSymbol(symbol, name, row.optString("exchange"), row.optString("kind"))
    }.distinctBy { it.symbol }
    check(entries.isNotEmpty()) { "Empty symbol directory" }
    return entries
}

private fun encodeCatalog(entries: List<MarketSymbol>): String = JSONArray().apply {
    entries.forEach { entry -> put(JSONObject().apply {
        put("symbol", entry.symbol); put("name", entry.name)
        put("exchange", entry.exchange); put("kind", entry.kind)
    }) }
}.toString()

internal fun parseNasdaqDirectory(text: String, fixedExchange: String? = null): List<MarketSymbol> {
    val lines = text.lineSequence().iterator()
    if (!lines.hasNext()) return emptyList()
    val header = lines.next().trimStart('\uFEFF').split('|')
    val exchanges = mapOf("A" to "NYSE American", "N" to "NYSE", "P" to "NYSE Arca", "Z" to "Cboe", "V" to "IEX")
    return lines.asSequence().mapNotNull { line ->
        val parts = line.split('|')
        fun field(name: String) = parts.getOrNull(header.indexOf(name)).orEmpty().trim()
        if (field("Test Issue") == "Y") return@mapNotNull null
        val symbol = DeviceProtocol.normalizeSymbol(field("ACT Symbol").ifEmpty { field("Symbol") })
            ?: return@mapNotNull null
        val name = field("Security Name").substringBefore(" - ")
        if (name.isBlank()) return@mapNotNull null
        MarketSymbol(symbol, name, fixedExchange ?: exchanges[field("Exchange")] ?: "U.S.",
            if (field("ETF") == "Y") "ETF" else "Stock")
    }.toList()
}

internal fun parseCryptoDirectory(text: String): List<MarketSymbol> {
    val coins = JSONArray(text)
    return (0 until coins.length()).mapNotNull { index ->
        val coin = coins.getJSONObject(index)
        val symbol = DeviceProtocol.normalizeSymbol(coin.optString("symbol") + "-USD") ?: return@mapNotNull null
        val name = coin.optString("name").trim()
        if (name.isEmpty()) null else MarketSymbol(symbol, name, "USD", "Crypto")
    }.distinctBy { it.symbol }
}

private fun downloadCatalog(url: String): String {
    val connection = URL(url).openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = 6_000
        connection.readTimeout = 6_000
        connection.setRequestProperty("User-Agent", "RozakosHome/0.3.3")
        check(connection.responseCode == 200) { "Directory unavailable" }
        return connection.inputStream.bufferedReader().use { it.readText() }
    } finally {
        connection.disconnect()
    }
}
