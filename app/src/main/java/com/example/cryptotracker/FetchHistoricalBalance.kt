package com.example.cryptotracker

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

suspend fun fetchHistoricalBalances(address: String, coinId: String): Map<LocalDate, Double> =
    withContext(Dispatchers.IO) {
        val zone = ZoneId.of("Pacific/Auckland")
        val today = LocalDate.now(zone)
        val last10Days = (0..9).map { today.minusDays(it * 10L) }

        return@withContext when (coinId.lowercase()) {
            "solana" -> {
                val map = mutableMapOf<LocalDate, Double>()
                for (date in last10Days) {
                    val balance = try {
                        fetchSolanaBalanceOnDate(address, date)
                    } catch (e: Exception) {
                        Log.e("SolanaHistory", "Error on $date", e)
                        0.0
                    }
                    map[date] = balance
                }
                map
            }

            "bitcoin" -> {
                try {
                    val retrofit = Retrofit.Builder()
                        .baseUrl("https://blockstream.info/api/")
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()

                    val service = retrofit.create(MainActivity.BlockstreamService::class.java)
                    val transactions = service.getTransactions(address)
                    val info = service.getAddressInfo(address)

                    val txMap = mutableMapOf<LocalDate, Double>()
                    val sortedTxs = transactions.sortedBy { it.status.block_time }

                    val initialBalance =
                        (info.chain_stats.funded_txo_sum - info.chain_stats.spent_txo_sum) / 100_000_000.0
                    var runningBalance = initialBalance

                    sortedTxs.reversed().forEach { tx ->
                        val date = Instant.ofEpochSecond(tx.status.block_time).atZone(zone).toLocalDate()

                        tx.vout.forEach {
                            if (it.scriptpubkey_address == address) {
                                runningBalance -= it.value / 100_000_000.0
                            }
                        }

                        tx.vin.mapNotNull { it.prevout }.forEach {
                            if (it.scriptpubkey_address == address) {
                                runningBalance += it.value / 100_000_000.0
                            }
                        }

                        txMap[date] = runningBalance
                    }

                    val fullMap = mutableMapOf<LocalDate, Double>()
                    var lastKnown = initialBalance
                    last10Days.forEach { date ->
                        val fallback = txMap.filterKeys { it <= date }.maxByOrNull { it.key }?.value
                        lastKnown = fallback ?: lastKnown
                        fullMap[date] = lastKnown
                    }

                    fullMap
                } catch (e: HttpException) {
                    Log.e("BitcoinHistory", "HTTP ${e.code()} - ${e.message()}")
                    emptyMap()
                } catch (e: Exception) {
                    Log.e("BitcoinHistory", "Unexpected error", e)
                    emptyMap()
                }
            }

            else -> {
                Log.w("BalanceHistory", "Unsupported coin: $coinId")
                emptyMap()
            }
        }
    }