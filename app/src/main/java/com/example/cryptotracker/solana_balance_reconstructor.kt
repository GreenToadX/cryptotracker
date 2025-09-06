package com.example.cryptotracker

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.time.LocalDate
import java.time.ZoneId

// Import shared models
import com.example.cryptotracker.TransactionRecord
import com.example.cryptotracker.AccountData
import com.example.cryptotracker.NativeTransfer

interface HeliusTransactionService {
    @GET("v0/addresses/{address}/transactions")
    suspend fun getRawTransactions(
        @Path("address") address: String,
        @Query("api-key") apiKey: String
    ): Response<String>
}

suspend fun fetchSolanaBalanceOnDate(address: String, targetDate: LocalDate): Double =
    withContext(Dispatchers.IO) {
        try {
            if (address.isBlank() || address.length < 32) {
                Log.e("SolanaBalanceReconstructor", "Invalid address format: '$address'")
                return@withContext 0.0
            }

            val retrofit = Retrofit.Builder()
                .baseUrl("https://api.helius.xyz/")
                .addConverterFactory(ScalarsConverterFactory.create())
                .build()

            val service = retrofit.create(HeliusTransactionService::class.java)

            val response = service.getRawTransactions(
                address = address,
                apiKey = "38ec42e8-6eae-4e90-abf7-0e6c719c92b9"
            )

            if (!response.isSuccessful || response.body().isNullOrBlank()) {
                Log.e("SolanaBalanceReconstructor", "Failed response: ${response.code()}")
                return@withContext 0.0
            }

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            val type = Types.newParameterizedType(List::class.java, TransactionRecord::class.java)
            val adapter = moshi.adapter<List<TransactionRecord>>(type)

            val transactions = adapter.fromJson(response.body()!!) ?: emptyList()

            val targetEpoch = targetDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toEpochSecond()

            val sortedTxs = transactions.sortedBy { it.timestamp }

            var runningBalance = 0L

            for (tx in sortedTxs) {
                if (tx.timestamp > targetEpoch) break

                val delta = tx.accountData?.find { it.account == address }?.nativeBalanceChange ?: 0L
                runningBalance += delta

                Log.d("TxDebug", "Applied tx: ${tx.signature}, ts=${tx.timestamp}, delta=$delta, balance=$runningBalance")
            }

            Log.d("SolanaBalanceReconstructor", "Balance on $targetDate: $runningBalance")
            return@withContext runningBalance / 1_000_000_000.0 // Convert lamports to SOL
        } catch (e: Exception) {
            Log.e("SolanaBalanceReconstructor", "Unexpected error on $targetDate", e)
            e.printStackTrace()
            0.0
        }
    }