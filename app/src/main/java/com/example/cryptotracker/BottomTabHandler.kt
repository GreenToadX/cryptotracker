package com.example.cryptotracker

import android.content.Context
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object BottomTabHandler {

    private var currentTabId: Int = R.id.tab1
    private lateinit var getSelectedCoin: () -> String
    private val balanceHistoryCache = mutableMapOf<String, Map<LocalDate, Double>>()

    fun wireTabs(
        context: Context,
        bottomNav: BottomNavigationView,
        priceTextView: TextView,
        balanceTextView: TextView,
        walletAddresses: Map<String, String>,
        selectedCoinProvider: () -> String
    ) {
        this.getSelectedCoin = selectedCoinProvider
        bottomNav.itemIconTintList = null

        bottomNav.setOnItemSelectedListener { item ->
            currentTabId = item.itemId
            handleTab(context, priceTextView, balanceTextView, walletAddresses)
            true
        }

        handleTab(context, priceTextView, balanceTextView, walletAddresses)
        bottomNav.selectedItemId = currentTabId
    }

    fun refreshCurrentTab(
        context: Context,
        priceTextView: TextView,
        balanceTextView: TextView,
        bottomNav: BottomNavigationView,
        walletAddresses: Map<String, String>
    ) {
        handleTab(context, priceTextView, balanceTextView, walletAddresses)
        bottomNav.selectedItemId = currentTabId
    }

    private fun handleTab(
        context: Context,
        priceTextView: TextView,
        balanceTextView: TextView,
        walletAddresses: Map<String, String>
    ) {
        val coinId = getSelectedCoin().lowercase()

        when (currentTabId) {
            R.id.tab1 -> {
                priceTextView.text = "Loading ${coinId.uppercase()} price..."
                balanceTextView.text = ""
                if (context is MainActivity) {
                    context.loadCoinData(coinId)
                }
            }

            R.id.tab2 -> {
                CoroutineScope(Dispatchers.Main).launch {
                    priceTextView.text = "Loading ${coinId.uppercase()} balance history..."
                    balanceTextView.text = ""

                    val address = walletAddresses[coinId] ?: return@launch
                    val cached = balanceHistoryCache[coinId]
                    val balanceHistory = cached ?: fetchHistoricalBalances(address, coinId).also {
                        balanceHistoryCache[coinId] = it
                    }

                    val zone = ZoneId.of("Pacific/Auckland")
                    val today = LocalDate.now(zone)
                    val last10Days = (1..10).map { today.minusDays(it.toLong()) }
                    val formatter = DateTimeFormatter.ofPattern("MMM dd")
                    val sortedBalances = balanceHistory.toSortedMap()

                    val balanceText = last10Days.joinToString("\n") { date ->
                        val knownBalance = sortedBalances
                            .filterKeys { it <= date }
                            .maxByOrNull { it.key }
                            ?.value ?: 0.0

                        "${date.format(formatter)}: ${String.format("%.5f", knownBalance)} ${coinId.uppercase()}"
                    }

                    balanceTextView.text = "${coinId.uppercase()} Balance History:\n$balanceText"
                }
            }

            R.id.tab3 -> {
                priceTextView.text = "Loading ${coinId.uppercase()} transactions..."
                balanceTextView.text = ""

                val address = walletAddresses[coinId] ?: return

                if (context is MainActivity) {
                    when (coinId) {
                        "solana" -> {
                            context.supportFragmentManager.beginTransaction()
                                .replace(R.id.fragmentContainer, SolanaTransactionFragment())
                                .commit()
                        }
                        "ethereum", "chainlink" -> {
                            context.supportFragmentManager.beginTransaction()
                                .replace(
                                    R.id.fragmentContainer,
                                    EthereumTransactionFragment.newInstance(address, coinId)
                                )
                                .commit()
                        }
                        "bitcoin" -> {
                            context.supportFragmentManager.beginTransaction()
                                .replace(
                                    R.id.fragmentContainer,
                                    BitcoinTransactionFragment.newInstance(address)
                                )
                                .commit()
                        }
                    }
                }
            }

            R.id.tab4 -> {
                priceTextView.text = "${coinId.uppercase()} Tab 4 tapped (not yet wired)"
                balanceTextView.text = ""
            }
        }
    }
}