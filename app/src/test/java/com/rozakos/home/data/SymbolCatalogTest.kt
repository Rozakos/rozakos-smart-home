package com.rozakos.home.data

import com.rozakos.home.data.market.MarketSymbol
import com.rozakos.home.data.market.SymbolCatalog
import com.rozakos.home.data.market.findSymbols
import com.rozakos.home.data.market.parseCryptoDirectory
import com.rozakos.home.data.market.parseNasdaqDirectory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SymbolCatalogTest {
    @get:Rule val folder = TemporaryFolder()
    private val entries = listOf(
        MarketSymbol("NVDL", "GraniteShares 2x Long NVDA Daily ETF", "NASDAQ", "ETF"),
        MarketSymbol("NVDA", "NVIDIA Corporation", "NASDAQ", "Stock"),
        MarketSymbol("AAPL", "Apple Inc.", "NASDAQ", "Stock"),
        MarketSymbol("AMD", "Advanced Micro Devices", "NASDAQ", "Stock"),
        MarketSymbol("BTC-USD", "Bitcoin", "USD", "Crypto"),
        MarketSymbol("BTCO", "Invesco Galaxy Bitcoin ETF", "Cboe", "ETF"),
        MarketSymbol("NOK", "Nokia Corporation Sponsored American Depositary Shares", "NYSE", "Stock"),
    )

    @Test fun companyNamesSymbolsAndCryptoFindTheExpectedListing() {
        assertEquals("NVDA", findSymbols(entries, "nvidia").first().symbol)
        assertEquals("NVDA", findSymbols(entries, " nvda ").first().symbol)
        assertEquals("AAPL", findSymbols(entries, "Apple").first().symbol)
        assertEquals("NOK", findSymbols(entries, "Nokia").first().symbol)
        assertEquals("BTC-USD", findSymbols(entries, "Bitcoin").first().symbol)
        assertEquals("AMD", findSymbols(entries, "advanced micro").first().symbol)
    }

    @Test fun blankUnknownAndBoundedQueriesDoNotOfferArbitraryTickers() {
        assertTrue(findSymbols(entries, "  ").isEmpty())
        assertTrue(findSymbols(entries, "Nonexistent Company 999").isEmpty())
        assertEquals(1, findSymbols(entries, "bitcoin", limit = 1).size)
    }

    @Test fun directoryParsingExcludesTestIssuesAndPreservesExchangeAndFundType() {
        val rows = parseNasdaqDirectory("""
            ACT Symbol|Security Name|Exchange|ETF|Test Issue
            NOK|Nokia Corporation|N|N|N
            VOO|Vanguard S&P 500 ETF|P|Y|N
            TEST|Test Company|N|N|Y
            BAD/../|Invalid|N|N|N
            File Creation Time: 20261017||||
        """.trimIndent())
        assertEquals(listOf("NOK", "VOO"), rows.map { it.symbol })
        assertEquals("NYSE", rows[0].exchange)
        assertEquals("ETF", rows[1].kind)
        assertEquals("NYSE Arca", rows[1].exchange)
    }

    @Test fun cryptoUsesUsdSymbolsAndKeepsFirstMarketCapRankedDuplicate() {
        val rows = parseCryptoDirectory("""[
            {"symbol":"btc","name":"Bitcoin"},
            {"symbol":"btc","name":"Different Bitcoin Token"},
            {"symbol":"eth","name":"Ethereum"}
        ]""")
        assertEquals(listOf("BTC-USD", "ETH-USD"), rows.map { it.symbol })
        assertEquals("Bitcoin", rows.first().name)
    }

    @Test fun bundledSearchWorksOfflineAndBrokenRefreshRetainsIt() = runBlocking {
        val catalog = SymbolCatalog(folder.root, bundledJson = { BUNDLED }, download = { error("Offline") })
        assertEquals("NVDA", catalog.search("Nvidia").single().symbol)
        catalog.refreshIfNeeded()
        assertEquals("NVDA", catalog.search("Nvidia").single().symbol)
        assertFalse(File(folder.root, "catalog.json").exists())
    }

    @Test fun corruptDiskCacheFallsBackToBundledDirectory() = runBlocking {
        File(folder.root, "catalog.json").writeText("not json")
        val catalog = SymbolCatalog(folder.root, bundledJson = { BUNDLED })
        assertEquals("NVDA", catalog.search("nvidia").single().symbol)
    }

    companion object {
        private const val BUNDLED = """[{"symbol":"NVDA","name":"NVIDIA Corporation","exchange":"NASDAQ","kind":"Stock"}]"""
    }
}
