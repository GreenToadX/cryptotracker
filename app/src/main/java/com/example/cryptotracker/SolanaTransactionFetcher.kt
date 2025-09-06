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
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import com.example.cryptotracker.TransactionRecord
import com.example.cryptotracker.NativeTransfer

interface SolanaTxService {
    @GET("v0/addresses/{address}/transactions")
    suspend fun getTransactions(
        @Path("address") address: String,
        @Query("api-key") apiKey: String
    ): Response<String>
}

suspend fun fetchRecentSolanaTransactions(address: String): List<String> =
    withContext(Dispatchers.IO) {
        try {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://api.helius.xyz/")
                .addConverterFactory(ScalarsConverterFactory.create())
                .build()

            val service = retrofit.create(SolanaTxService::class.java)

            val response = service.getTransactions(
                address = address,
                apiKey = "38ec42e8-6eae-4e90-abf7-0e6c719c92b9"
            )

            val rawJson = response.body() ?: "null"
            Log.d("SolanaTxFetcher", "Raw JSON for $address: $rawJson")

            if (!response.isSuccessful || rawJson == "null" || rawJson == "[]") {
                Log.e("SolanaTxFetcher", "Empty or failed response for address=$address, code=${response.code()}")
                return@withContext emptyList()
            }

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            val type = Types.newParameterizedType(List::class.java, TransactionRecord::class.java)
            val adapter = moshi.adapter<List<TransactionRecord>>(type)

            val transactions = adapter.fromJson(rawJson) ?: emptyList()

            val zone = ZoneId.of("Pacific/Auckland")
            val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
            val MIN_TRANSFER_LAMPORTS = 1000L // ~0.000001 SOL

            transactions
                .sortedByDescending { it.timestamp }
                .flatMap { tx ->
                    tx.nativeTransfers
                        .onEach { transfer ->
                            Log.d("SolanaTxFetcher", "Parsed transfer: from=${transfer.from}, to=${transfer.to}, amount=${transfer.amount}")
                            if (transfer.amount < MIN_TRANSFER_LAMPORTS) {
                                Log.d("SolanaTxFilter", "Excluded micro-transfer: ${transfer.from} → ${transfer.to}, amount=${transfer.amount}")
                            }
                        }
                        .filter { transfer ->
                            transfer.amount >= MIN_TRANSFER_LAMPORTS &&
                                    transfer.from != transfer.to &&
                                    (transfer.from == address || transfer.to == address)
                        }
                        .map { transfer ->
                            val date = java.time.Instant.ofEpochSecond(tx.timestamp).atZone(zone).toLocalDate()
                            val direction = if (transfer.from == address) "Sent" else "Received"
                            val solAmount = transfer.amount / 1_000_000_000.0

                            "${date.format(formatter)} | $direction | %.4f SOL".format(solAmount)
                        }
                }
                .take(10)
        } catch (e: Exception) {
            Log.e("SolanaTxFetcher", "Error fetching transactions", e)
            emptyList()
        }
    }