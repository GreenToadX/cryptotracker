package com.example.cryptotracker

import com.example.cryptotracker.MainActivity.BlockstreamService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

suspend fun fetchWalletBalance(address: String): Double = withContext(Dispatchers.IO) {
    val retrofit = Retrofit.Builder()
        .baseUrl("https://blockstream.info/api/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service = retrofit.create(BlockstreamService::class.java)
    val response = service.getAddressInfo(address)

    val satoshis = response.chain_stats.funded_txo_sum -
            response.chain_stats.spent_txo_sum
    satoshis / 100_000_000.0
}
