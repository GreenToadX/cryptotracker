package com.example.cryptotracker

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    val blockchairService: BlockchairService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.blockchair.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BlockchairService::class.java)
    }

    val coingeckoService: CoinGeckoService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.coingecko.com/api/v3/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CoinGeckoService::class.java)
    }
}