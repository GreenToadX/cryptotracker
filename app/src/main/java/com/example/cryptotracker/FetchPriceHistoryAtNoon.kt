package com.example.cryptotracker

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private var cachedHistory: List<Pair<String, Double>>? = null
private var lastFetchTime: Long = 0
private const val CACHE_TTL_MS = 60_000 // 1 minute
private val fetchLock = Mutex()

suspend fun fetchPriceHistoryAtNoon(coinId: String = "bitcoin"): List<Pair<String, Double>> = withContext(Dispatchers.IO) {
    fetchLock.withLock {
        val now = System.currentTimeMillis()
        if (cachedHistory != null && now - lastFetchTime < CACHE_TTL_MS) {
            Log.d("PriceHistory", "Returning cached history for $coinId")
            return@withLock cachedHistory!!
        }

        Log.d("PriceHistory", "Calling CoinGecko for $coinId 10-day history")

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.coingecko.com/api/v3/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(CoinGeckoService::class.java)

        val chart = try {
            service.getMarketChart(coinId = coinId)
        } catch (e: HttpException) {
            Log.e("PriceHistory", "HTTP ${e.code()} error for $coinId: ${e.message()}")
            return@withLock emptyList()
        } catch (e: Exception) {
            Log.e("PriceHistory", "Unexpected error for $coinId: ${e.localizedMessage}")
            return@withLock emptyList()
        }

        Log.d("PriceHistory", "Received ${chart.prices.size} entries for $coinId")

        chart.prices.take(5).forEachIndexed { i, point ->
            Log.d("PriceHistory", "Raw[$i]: ${point.joinToString()}")
        }

        val zone = ZoneId.of("Pacific/Auckland")
        val formatter = DateTimeFormatter.ofPattern("dd MMM")
        val grouped = mutableMapOf<String, Pair<String, Double>>()

        chart.prices.forEachIndexed { index, point ->
            if (point.size < 2) {
                Log.w("PriceHistory", "Entry[$index] malformed: $point")
                return@forEachIndexed
            }

            val timestamp = try {
                point[0].toDouble().toLong()
            } catch (e: Exception) {
                Log.e("PriceHistory", "Failed to parse timestamp at index $index: ${point[0]}", e)
                return@forEachIndexed
            }

            val price = point[1]
            val date = Instant.ofEpochMilli(timestamp).atZone(zone).format(formatter)

            if (!grouped.containsKey(date)) {
                grouped[date] = date to price
                Log.d("PriceHistory", "Selected $date → $price")
            }
        }

        if (grouped.isEmpty()) {
            Log.w("PriceHistory", "No daily entries parsed for $coinId")
        }

        cachedHistory = grouped.values.toList()
        lastFetchTime = now
        return@withLock cachedHistory!!
    }
}