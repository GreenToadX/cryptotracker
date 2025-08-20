package com.example.cryptotracker

import android.content.Context
import android.widget.TextView
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.*
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object BottomTabHandler {

    fun wireTabs(
        context: Context,
        bottomNav: BottomNavigationView,
        priceTextView: TextView,
        balanceTextView: TextView,
        walletAddresses: Map<String, String>,
        getSelectedCoin: () -> String
    ) {
        bottomNav.itemIconTintList = null

        bottomNav.setOnItemSelectedListener { item ->
            val coinId = getSelectedCoin()

            when (item.itemId) {
                R.id.tab1 -> {
                    priceTextView.text = "Loading ${coinId.uppercase()} price..."
                    balanceTextView.text = ""

                    if (context is MainActivity) {
                        context.loadCoinData(coinId)
                    }
                    true
                }

                R.id.tab2 -> {
                    CoroutineScope(Dispatchers.Main).launch {
                        priceTextView.text = "Loading ${coinId.uppercase()} balance history..."
                        balanceTextView.text = ""

                        val address = walletAddresses[coinId] ?: return@launch
                        val balanceHistory = fetchHistoricalBalances(address)

                        val zone = ZoneId.of("Pacific/Auckland")
                        val today = LocalDate.now(zone)
                        val last10Days: List<LocalDate> = (1..10).map { today.minusDays(it * 10L) }
                        val formatter = DateTimeFormatter.ofPattern("MMM dd")

                        val sortedBalances: Map<LocalDate, Double> = balanceHistory.toSortedMap()
                        val balanceText = last10Days.map { date: LocalDate ->
                            val knownBalance = sortedBalances
                                .filterKeys { key: LocalDate -> key <= date }
                                .maxByOrNull { entry -> entry.key }
                                ?.value ?: 0.0

                            "${date.format(formatter)}: ${String.format("%.5f", knownBalance)} ${coinId.uppercase()}"
                        }.joinToString("\n")

                        balanceTextView.text = "${coinId.uppercase()} Balance History:\n$balanceText"
                    }
                    true
                }

                R.id.tab3 -> {
                    priceTextView.text = "${coinId.uppercase()} Tab 3 tapped (not yet wired)"
                    balanceTextView.text = ""
                    true
                }

                R.id.tab4 -> {
                    priceTextView.text = "${coinId.uppercase()} Tab 4 tapped (not yet wired)"
                    balanceTextView.text = ""
                    true
                }

                else -> false
            }
        }

        bottomNav.selectedItemId = R.id.tab1
    }
}