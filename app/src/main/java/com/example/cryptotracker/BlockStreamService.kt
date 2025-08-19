package com.example.cryptotracker

import retrofit2.http.GET
import retrofit2.http.Path

interface BlockstreamService {
    @GET("address/{address}")
    suspend fun getAddressInfo(@Path("address") address: String): AddressInfoResponse

    @GET("address/{address}/txs")
    suspend fun getTransactions(@Path("address") address: String): List<Transaction>
}