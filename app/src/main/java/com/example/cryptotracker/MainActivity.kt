package com.example.cryptotracker

import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.*
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.time.*
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {

    var selectedCoin: String = "bitcoin" // ✅ Promoted to class-level

    private val walletAddresses = mapOf(
        "bitcoin" to "bc1ql49ydapnjafl5t2cp9zqpjwe6pdgmxy98859v2",
        "ethereum" to "0x36b6751586614d647d8a3f495e82bdcf250914c8",
        "solana" to "9dEScN75Ww1JabV8L6oBYY1JLgCE1S81MrBxoaP9k3ic",
        "chainlink" to "0xf795e4c6ff15afe8b2ed9c2a6c3a6c634c58f65e"
    )

    private lateinit var priceTextView: TextView
    private lateinit var balanceTextView: TextView

    private lateinit var bitcoinButton: ImageButton
    private lateinit var ethereumButton: ImageButton
    private lateinit var solanaButton: ImageButton
    private lateinit var chainlinkButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        priceTextView = findViewById(R.id.priceTextView)
        balanceTextView = findViewById(R.id.balanceTextView)

        bitcoinButton = findViewById(R.id.bitcoinButton)
        ethereumButton = findViewById(R.id.ethereumButton)
        solanaButton = findViewById(R.id.solanaButton)
        chainlinkButton = findViewById(R.id.chainlinkButton)

        bitcoinButton.setOnClickListener {
            selectedCoin = "bitcoin"
            loadCoinData(selectedCoin)
        }

        ethereumButton.setOnClickListener {
            selectedCoin = "ethereum"
            loadCoinData(selectedCoin)
        }

        solanaButton.setOnClickListener {
            selectedCoin = "solana"
            loadCoinData(selectedCoin)
        }

        chainlinkButton.setOnClickListener {
            selectedCoin = "chainlink"
            loadCoinData(selectedCoin)
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        BottomTabHandler.wireTabs(
            context = this,
            bottomNav = bottomNav,
            priceTextView = priceTextView,
            balanceTextView = balanceTextView,
            walletAddresses = walletAddresses,
            getSelectedCoin = { selectedCoin } // ✅ Lambda to expose current coin
        )
    }

    // ... rest of your MainActivity code remains unchanged


    fun loadCoinData(coinId: String) {
        CoroutineScope(Dispatchers.Main).launch {
            priceTextView.text = "Loading ${coinId.uppercase()} price..."
            balanceTextView.text = "Loading wallet balance..."

            delay(1000)

            val price = try {
                fetchPrice(coinId)
            } catch (e: HttpException) {
                if (e.code() == 429) "Rate limit hit" else "Error"
            } catch (e: Exception) {
                "Error"
            }

            val address = walletAddresses[coinId] ?: ""
            val balance: Double = try {
                when (coinId) {
                    "bitcoin" -> fetchWalletBalance(address)
                    "ethereum" -> EthereumFetcher.fetchBalance(address)
                    "solana" -> SolanaFetcher.fetchBalance(address)
                    "chainlink" -> ChainlinkFetcher.fetchBalance(address)
                    else -> 0.0
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Balance fetch failed for $coinId", e)
                0.0
            }

            if (coinId == "bitcoin" && price != "Error" && price != "Rate limit hit") {
                val priceValue = price.replace(",", "").toDoubleOrNull()
                priceValue?.let {
                    val fakePurchasePrice = 40000.0
                    val profit = (it - fakePurchasePrice) * balance
                    val profitText = "Profit: ${String.format("%,.2f", profit)} NZD"
                    priceTextView.text = "BTC Price (NZD): $price\n$profitText"
                } ?: run {
                    priceTextView.text = "BTC Price (NZD): $price\nProfit: (invalid)"
                }

                val zone = ZoneId.of("Pacific/Auckland")
                val today = LocalDate.now(zone)
                val last10Days = (1..10).map { today.minusDays(it.toLong()) }
                val formatter = DateTimeFormatter.ofPattern("MMM dd")

                val history = fetchPriceHistoryAtNoon(coinId)
                val historyMap = history.associate { it.first to it.second }

                val historyText = last10Days.map { date ->
                    val formattedDate = formatter.format(date)
                    val price = historyMap[formattedDate] ?: 0.0
                    "$formattedDate: ${String.format("%,.2f", price)} NZD"
                }.joinToString("\n")

                balanceTextView.text = "Wallet Balance: $balance BTC\n\nLast 10 Days:\n$historyText"
            } else if (coinId == "ethereum" && price != "Error" && price != "Rate limit hit") {
                val priceValue = price.replace(",", "").toDoubleOrNull()
                priceValue?.let {
                    val fakePurchasePrice = 3000.0
                    val profit = (it - fakePurchasePrice) * balance
                    val profitText = "Profit: ${String.format("%,.2f", profit)} NZD"
                    priceTextView.text = "ETH Price (NZD): $price\n$profitText"
                } ?: run {
                    priceTextView.text = "ETH Price (NZD): $price\nProfit: (invalid)"
                }

                val zone = ZoneId.of("Pacific/Auckland")
                val today = LocalDate.now(zone)
                val last10Days = (1..10).map { today.minusDays(it.toLong()) }
                val formatter = DateTimeFormatter.ofPattern("MMM dd")

                val history = EthereumFetcher.fetchPriceHistory()
                val historyMap = history.associate { it.first to it.second }

                val historyText = last10Days.map { date ->
                    val formattedDate = formatter.format(date)
                    val price = historyMap[formattedDate] ?: 0.0
                    "$formattedDate: ${String.format("%,.2f", price)} NZD"
                }.joinToString("\n")

                balanceTextView.text = "Wallet Balance: $balance ETH\n\nLast 10 Days:\n$historyText"
            } else {
                priceTextView.text = "${coinId.uppercase()} Price (NZD): $price"

                val zone = ZoneId.of("Pacific/Auckland")
                val today = LocalDate.now(zone)
                val last10Days = (1..10).map { today.minusDays(it.toLong()) }
                val formatter = DateTimeFormatter.ofPattern("MMM dd")

                val history = when (coinId) {
                    "solana" -> SolanaFetcher.fetchPriceHistory()
                    "chainlink" -> ChainlinkFetcher.fetchPriceHistory()
                    else -> emptyList()
                }

                val historyMap = history.associate { it.first to it.second }

                val historyText = last10Days.map { date ->
                    val formattedDate = formatter.format(date)
                    val price = historyMap[formattedDate] ?: 0.0
                    "$formattedDate: ${String.format("%,.2f", price)} NZD"
                }.joinToString("\n")

                balanceTextView.text = "Wallet Balance: $balance ${coinId.uppercase()}\n\nLast 10 Days:\n$historyText"
            }
        }
    }

    interface CoinGeckoService {
        @GET("simple/price")
        suspend fun getPrices(
            @Query("ids") ids: String,
            @Query("vs_currencies") vs: String
        ): Map<String, Map<String, Double>>

        @GET("coins/{id}/market_chart")
        suspend fun getMarketChart(
            @Path("id") coinId: String,
            @Query("vs_currency") vsCurrency: String = "nzd",
            @Query("days") days: Int = 10
        ): MarketChartResponse
    }

    interface BlockstreamService {
        @GET("address/{address}")
        suspend fun getAddressInfo(@Path("address") address: String): AddressInfoResponse

        @GET("address/{address}/txs")
        suspend fun getTransactions(@Path("address") address: String): List<Transaction>
    }

    //  NEW: Ethplorer API interface
    interface EthplorerService { //
        @GET("getAddressInfo") //
        suspend fun getEthplorerInfo( //
            @Query("apiKey") apiKey: String = "freekey", //
            @Query("address") address: String //
        ): EthplorerResponse //
    } //

    //  NEW: Ethplorer response model
    data class EthplorerResponse( //
        val ETH: EthData? //
    ) //

    data class EthData(val balance: Double) //


    data class AddressInfoResponse(
        val chain_stats: ChainStats
    )

    data class ChainStats(
        val funded_txo_sum: Long,
        val spent_txo_sum: Long
    )

    data class MarketChartResponse(
        val prices: List<List<Double>>
    )

    data class Transaction(
        val status: Status,
        val vin: List<TxInput>,
        val vout: List<TxOutput>
    )

    data class Status(val block_time: Long)
    data class TxInput(val prevout: TxOutput?)
    data class TxOutput(val value: Long, val scriptpubkey_address: String)

    private suspend fun fetchPrice(coinId: String): String = withContext(Dispatchers.IO) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.coingecko.com/api/v3/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(CoinGeckoService::class.java)
        val response = service.getPrices(ids = coinId, vs = "nzd")
        val nzdPrice = response[coinId]?.get("nzd") ?: 0.0
        String.format("%,.2f", nzdPrice)
    }

    private suspend fun fetchWalletBalance(address: String): Double = withContext(Dispatchers.IO) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://blockstream.info/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(BlockstreamService::class.java)
        val response: AddressInfoResponse = service.getAddressInfo(address)

        val satoshis = response.chain_stats.funded_txo_sum - response.chain_stats.spent_txo_sum
        satoshis / 100_000_000.0
    }

    private suspend fun fetchPriceHistoryAtNoon(coinId: String): List<Pair<String, Double>> = withContext(Dispatchers.IO) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.coingecko.com/api/v3/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(CoinGeckoService::class.java)

        val response = try {
            service.getMarketChart(coinId)
        } catch (e: HttpException) {
            Log.e("fetchPriceHistoryAtNoon", "HTTP ${e.code()} - ${e.message()}")
            return@withContext emptyList()
        } catch (e: Exception) {
            Log.e("fetchPriceHistoryAtNoon", "Unexpected error", e)
            return@withContext emptyList()
        }

        val zone = ZoneId.of("Pacific/Auckland")
        val formatter = DateTimeFormatter.ofPattern("MMM dd")

        response.prices.mapNotNull { entry ->
            val timestamp = entry[0].toLong()
            val price = entry[1]
            val date = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
            formatter.format(date) to price
        }
    }
    }