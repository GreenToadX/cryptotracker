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
        walletAddresses: Map<String, String>
    ) {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.tab1 -> {
                    priceTextView.text = "Loading BTC price..."
                    balanceTextView.text = ""

                    if (context is MainActivity) {
                        context.loadCoinData("bitcoin")
                    }
                    true
                }
                R.id.tab2 -> {
                    CoroutineScope(Dispatchers.Main).launch {
                        priceTextView.text = "Loading BTC balance history..."
                        balanceTextView.text = ""

                        val address = walletAddresses["bitcoin"] ?: return@launch
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

                            "${date.format(formatter)}: ${String.format("%.5f", knownBalance)} BTC"
                        }.joinToString("\n")

                        balanceTextView.text = "BTC Balance History:\n$balanceText"
                    }
                    true
                }
                R.id.tab3 -> {
                    priceTextView.text = "SOL tab tapped (not yet wired)"
                    balanceTextView.text = ""
                    true
                }
                R.id.tab4 -> {
                    priceTextView.text = "LINK tab tapped (not yet wired)"
                    balanceTextView.text = ""
                    true
                }
                else -> false
            }
        }

        bottomNav.selectedItemId = R.id.tab1
    }
}