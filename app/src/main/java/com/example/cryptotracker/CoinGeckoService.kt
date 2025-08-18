package com.example.cryptotracker

import retrofit2.http.GET
import retrofit2.http.Query

interface CoinGeckoService {
    @GET("simple/price")
    suspend fun getPrices(
        @Query("ids") ids: String,           // e.g., "bitcoin,ethereum,solana,chainlink"
        @Query("vs_currencies") vs: String = "nzd"
    ): Map<String, Map<String, Double>>
}
