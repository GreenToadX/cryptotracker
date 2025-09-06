package com.example.cryptotracker

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.graphics.drawable.AnimatedImageDrawable
import retrofit2.HttpException
import retrofit2.http.GET
import retrofit2.http.Path

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var priceTextView: TextView
    private lateinit var balanceTextView: TextView
    private lateinit var coinSpinner: ImageView
    private lateinit var coinListRecyclerView: RecyclerView

    var selectedCoin: String = ""
    private var lastLoadedCoin: String = ""
    private val priceCache = mutableMapOf<String, String>()
    private val historyCache = mutableMapOf<String, List<Pair<String, Double>>>()

    companion object {
        lateinit var walletAddresses: Map<String, String>
    }

    private val prefs by lazy { getSharedPreferences("wallets", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        walletAddresses = WalletManager.getWallets(this)
        Log.d("MainActivity", "Loaded wallet addresses: $walletAddresses")

        priceTextView = findViewById(R.id.priceTextView)
        balanceTextView = findViewById(R.id.balanceTextView)
        bottomNav = findViewById(R.id.bottomNav)
        coinSpinner = findViewById(R.id.coinSpinner)
        coinListRecyclerView = findViewById(R.id.coinListRecyclerView)

        val coinList = CoinRegistry.supportedCoins.filter { walletAddresses.containsKey(it.id) }

        coinListRecyclerView.layoutManager = LinearLayoutManager(this)
        coinListRecyclerView.adapter = CoinAdapter(coinList) { coin ->
            selectedCoin = coin.id
            refreshAndLoad()
        }

        BottomTabHandler.wireTabs(
            context = this,
            bottomNav = bottomNav,
            priceTextView = priceTextView,
            balanceTextView = balanceTextView,
            walletAddresses = walletAddresses,
            selectedCoinProvider = { selectedCoin }
        )

        supportFragmentManager.registerFragmentLifecycleCallbacks(
            object : FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentViewCreated(
                    fm: FragmentManager,
                    f: Fragment,
                    v: View,
                    savedInstanceState: Bundle?
                ) {
                    if (f is FiatToggleable) {
                        Tab3Controller(v, f).wireToggleButtons()
                    }
                }
            },
            true
        )
    }

    private fun refreshAndLoad() {
        if (selectedCoin == lastLoadedCoin) return
        lastLoadedCoin = selectedCoin

        BottomTabHandler.refreshCurrentTab(
            context = this,
            priceTextView = priceTextView,
            balanceTextView = balanceTextView,
            bottomNav = bottomNav,
            walletAddresses = walletAddresses
        )
        loadCoinData(selectedCoin)
    }

    fun loadCoinData(coinId: String) {
        if (coinId.isBlank()) {
            priceTextView.text = "🪙 Tap a coin to view price"
            balanceTextView.text = ""
            coinSpinner.visibility = View.VISIBLE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                (coinSpinner.drawable as? AnimatedImageDrawable)?.start()
            }
            return
        }

        coinSpinner.visibility = View.GONE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            (coinSpinner.drawable as? AnimatedImageDrawable)?.stop()
        }

        CoroutineScope(Dispatchers.Main).launch {
            priceTextView.text = "Loading ${coinId.uppercase()} price..."
            balanceTextView.text = "Loading wallet balance..."
            delay(500)

            val price = priceCache[coinId] ?: run {
                val fetched = try {
                    fetchPrice(coinId)
                } catch (e: HttpException) {
                    if (e.code() == 429) "Rate limit hit" else "Error"
                } catch (e: Exception) {
                    "Error"
                }
                priceCache[coinId] = fetched
                fetched
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

            if (coinId == "bitcoin" && price !in listOf("Error", "Rate limit hit")) {
                displayWithProfit(
                    coin = "BTC",
                    priceStr = price,
                    balance = balance,
                    fakePurchasePrice = 40000.0,
                    priceHistoryFetcher = ::fetchPriceHistoryAtNoon
                )
            } else if (coinId == "ethereum" && price !in listOf("Error", "Rate limit hit")) {
                displayWithProfit(
                    coin = "ETH",
                    priceStr = price,
                    balance = balance,
                    fakePurchasePrice = 3000.0,
                    priceHistoryFetcher = EthereumFetcher::fetchPriceHistory
                )
            } else {
                val history = historyCache[coinId] ?: run {
                    val fetched = when (coinId) {
                        "solana" -> SolanaFetcher.fetchPriceHistory()
                        "chainlink" -> ChainlinkFetcher.fetchPriceHistory()
                        else -> emptyList()
                    }
                    historyCache[coinId] = fetched
                    fetched
                }

                val historyText = buildHistoryText(history)
                priceTextView.text = "${coinId.uppercase()} Price (NZD): $price"
                balanceTextView.text =
                    "Wallet Balance: $balance ${coinId.uppercase()}\n\nLast 10 Days:\n$historyText"
            }
        }
    }

    private suspend fun displayWithProfit(
        coin: String,
        priceStr: String,
        balance: Double,
        fakePurchasePrice: Double,
        priceHistoryFetcher: suspend () -> List<Pair<String, Double>>
    ) {
        val priceVal = priceStr.replace(",", "").toDoubleOrNull()
        if (priceVal != null) {
            val profit = (priceVal - fakePurchasePrice) * balance
            val profitText = "Profit: ${String.format("%,.2f", profit)} NZD"
            priceTextView.text = "$coin Price (NZD): $priceStr\n$profitText"
        } else {
            priceTextView.text = "$coin Price (NZD): $priceStr\nProfit: (invalid)"
        }

        val history = historyCache[coin] ?: run {
            val fetched = priceHistoryFetcher()
            historyCache[coin] = fetched
            fetched
        }

        val historyText = buildHistoryText(history)
        balanceTextView.text =
            "Wallet Balance: $balance $coin\n\nLast 10 Days:\n$historyText"
    }

    interface BlockstreamService {
        @GET("address/{address}")
        suspend fun getAddressInfo(@Path("address") address: String): AddressInfoResponse

        @GET("address/{address}/txs")
        suspend fun getTransactions(@Path("address") address: String): List<Transaction>
    }

    data class EthData(val balance: Double)
    data class AddressInfoResponse(val chain_stats: ChainStats)
    data class ChainStats(val funded_txo_sum: Long, val spent_txo_sum: Long)
    data class Transaction(val status: Status, val vin: List<TxInput>, val vout: List<TxOutput>)
    data class Status(val block_time: Long)
    data class TxInput(val prevout: TxOutput?)
    data class TxOutput(val value: Long, val scriptpubkey_address: String)
}