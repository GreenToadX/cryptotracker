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

object CoinGeckoThrottledFetcher {

    private const val CACHE_TTL_MS = 60_000L

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.coingecko.com/api/v3/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service = retrofit.create(CoinGeckoService::class.java)

    private val cacheMap = mutableMapOf<String, List<Pair<String, Double>>>()
    private val lastFetchMap = mutableMapOf<String, Long>()
    private val lockMap = mutableMapOf<String, Mutex>()
    private val queryCountMap = mutableMapOf<String, Int>()

    suspend fun fetch(coinId: String): List<Pair<String, Double>> = withContext(Dispatchers.IO) {
        val lock = synchronized(lockMap) {
            lockMap.getOrPut(coinId) { Mutex() }
        }

        lock.withLock {
            val now = System.currentTimeMillis()
            val cached = cacheMap[coinId]
            val lastFetch = lastFetchMap[coinId] ?: 0

            if (cached != null && now - lastFetch < CACHE_TTL_MS) {
                Log.d("ThrottledFetcher", "Returning cached history for $coinId")
                return@withLock cached
            }

            queryCountMap[coinId] = queryCountMap.getOrDefault(coinId, 0) + 1
            Log.d("ThrottledFetcher", "$coinId API query count = ${queryCountMap[coinId]}")
            Log.d("ThrottledFetcher", "Calling CoinGecko for $coinId 10-day history")

            val chart = try {
                service.getMarketChart(coinId)
            } catch (e: HttpException) {
                Log.e("ThrottledFetcher", "HTTP ${e.code()} error for $coinId: ${e.message()}")
                return@withLock emptyList()
            } catch (e: Exception) {
                Log.e("ThrottledFetcher", "Unexpected error for $coinId: ${e.localizedMessage}")
                return@withLock emptyList()
            }

            Log.d("ThrottledFetcher", "Received ${chart.prices.size} entries for $coinId")
            chart.prices.take(5).forEachIndexed { i, point ->
                Log.d("ThrottledFetcher", "Raw[$i]: ${point.joinToString()}")
            }

            val zone = ZoneId.of("Pacific/Auckland")
            val formatter = DateTimeFormatter.ofPattern("dd MMM")
            val grouped = mutableMapOf<String, Pair<String, Double>>()

            chart.prices.forEachIndexed { index, point ->
                if (point.size < 2) {
                    Log.w("ThrottledFetcher", "Entry[$index] malformed: $point")
                    return@forEachIndexed
                }

                val timestamp = try {
                    point[0].toDouble().toLong()
                } catch (e: Exception) {
                    Log.e("ThrottledFetcher", "Failed to parse timestamp at index $index: ${point[0]}", e)
                    return@forEachIndexed
                }

                val price = point[1]
                val date = Instant.ofEpochMilli(timestamp).atZone(zone).format(formatter)

                if (!grouped.containsKey(date)) {
                    grouped[date] = date to price
                    Log.d("ThrottledFetcher", "Selected $date → $price")
                }
            }

            if (grouped.isEmpty()) {
                Log.w("ThrottledFetcher", "No daily entries parsed for $coinId")
            }

            val result = grouped.values.toList()
            cacheMap[coinId] = result
            lastFetchMap[coinId] = now
            return@withLock result
        }
    }
}