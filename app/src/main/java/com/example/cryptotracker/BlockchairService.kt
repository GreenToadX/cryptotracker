package com.example.cryptotracker

import retrofit2.http.GET
import retrofit2.http.Path

interface BlockchairService {
    @GET("bitcoin/dashboards/address/{address}")
    suspend fun getBTCData(@Path("address") address: String): BTCResponse
}
