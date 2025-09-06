package com.example.cryptotracker

import com.example.cryptotracker.CoinGeckoService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

suspend fun fetchPrice(coinId: String): String = withContext(Dispatchers.IO) {
    val retrofit = Retrofit.Builder()
        .baseUrl("https://api.coingecko.com/api/v3/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service = retrofit.create(CoinGeckoService::class.java)
    val response = service.getPrices(ids = coinId, vs = "nzd")
    val nzdPrice = response[coinId]?.get("nzd") ?: 0.0
    String.format("%,.2f", nzdPrice)
}
