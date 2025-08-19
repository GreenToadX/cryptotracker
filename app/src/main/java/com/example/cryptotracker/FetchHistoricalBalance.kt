package com.example.cryptotracker

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

suspend fun fetchHistoricalBalances(address: String): Map<LocalDate, Double> =
    withContext(Dispatchers.IO) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://blockstream.info/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(BlockstreamService::class.java)
        val transactions = service.getTransactions(address)
        val info = service.getAddressInfo(address)

        val zone = ZoneId.of("Pacific/Auckland")
        val today = LocalDate.now(zone)
        val last10Days = (0..9).map { today.minusDays(it * 10L) }

        val txMap = mutableMapOf<LocalDate, Double>()
        val sortedTxs = transactions.sortedBy { it.status.block_time }

        val initialBalance =
            (info.chain_stats.funded_txo_sum - info.chain_stats.spent_txo_sum) / 100_000_000.0
        var runningBalance = initialBalance

        // Rewind through transactions to reconstruct historical balances
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

        // Fill in missing days by propagating last known balance forward
        val fullMap = mutableMapOf<LocalDate, Double>()
        var lastKnown = initialBalance
        last10Days.forEach { date ->
            val fallback = txMap.filterKeys { it <= date }.maxByOrNull { it.key }?.value
            lastKnown = fallback ?: lastKnown
            fullMap[date] = lastKnown
        }

        fullMap
    }