package com.example.cryptotracker

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import com.example.cryptotracker.MarketChartResponse
interface CoinGeckoService {
    @GET("simple/price")
    public suspend fun getPrices(
        @Query("ids") ids: String,
        @Query("vs_currencies") vs: String
    ): Map<String, Map<String, Double>>

    @GET("coins/{id}/market_chart")
    suspend fun getMarketChart(
        @Path("id") coinId: String,
        @Query("vs_currency") vsCurrency: String = "nzd",
        @Query("days") days: Int = 10
    ): MarketChartResponse
}