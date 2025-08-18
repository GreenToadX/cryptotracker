package com.example.cryptotracker

import retrofit2.http.GET
import retrofit2.http.Query

interface EtherscanService {
    @GET("api")
    suspend fun getETHBalance(
        @Query("module") module: String = "account",
        @Query("action") action: String = "balance",
        @Query("address") address: String,
        @Query("apikey") apiKey: String
    ): ETHResponse
}