package com.example.cryptotracker

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import android.util.Log

suspend fun fetchPriceHistoryAtNoon(coinId: String): List<Pair<String, Double>> =
    withContext(Dispatchers.IO) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.coingecko.com/api/v3/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(MainActivity.CoinGeckoService::class.java)

        val response = try {
            service.getMarketChart(coinId)
        } catch (e: HttpException) {
            Log.e("PriceFetch", "HTTP ${e.code()} - ${e.message()}")
            return@withContext emptyList()
        } catch (e: Exception) {
            Log.e("PriceFetch", "Unexpected error", e)
            return@withContext emptyList()
        }

        val zone = ZoneId.of("Pacific/Auckland")
        val formatter = DateTimeFormatter.ofPattern("MMM dd")
        val today = LocalDate.now(zone)

        response.prices
            .map { entry ->
                val timestamp = entry[0].toLong()
                val price = entry[1]
                val dateTime = Instant.ofEpochMilli(timestamp).atZone(zone)
                dateTime to price
            }
            .groupBy { it.first.toLocalDate() }
            .filterKeys { it.isBefore(today) }
            .mapNotNull { (date, entries) ->
                val targetTime = date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
                val closest = entries.minByOrNull { (dt, _) ->
                    kotlin.math.abs(dt.toInstant().toEpochMilli() - targetTime)
                }
                closest?.let { formatter.format(it.first) to it.second }
            }
    }