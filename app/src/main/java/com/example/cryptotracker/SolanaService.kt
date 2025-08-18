package com.example.cryptotracker

import retrofit2.http.Body
import retrofit2.http.POST

interface SolanaService {
    @POST("v1/getBalance")
    suspend fun getSOLBalance(@Body request: SolanaRequest): SolanaResponse
}