package com.example.cryptotracker

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

interface CoinDataFetcher {
    suspend fun fetchBalance(address: String): Double
    suspend fun fetchPriceHistory(): List<Pair<String, Double>>
}

object EthereumFetcher : CoinDataFetcher {

    interface EtherscanService {
        @GET("api")
        suspend fun getBalance(
            @Query("module") module: String = "account",
            @Query("action") action: String = "balance",
            @Query("address") address: String,
            @Query("tag") tag: String = "latest",
            @Query("apikey") apiKey: String = "D3HM6P7NH8NMBINBJ7CP1ENXHMHS554ICX"
        ): EtherscanResponse
    }

    data class EtherscanResponse(val status: String, val message: String, val result: String)

    override suspend fun fetchBalance(address: String): Double = withContext(Dispatchers.IO) {
        try {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://api.etherscan.io/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service = retrofit.create(EtherscanService::class.java)
            val response = service.getBalance(address = address)

            println("Raw wei from Etherscan: ${response.result}")

            val wei = response.result.toBigDecimalOrNull() ?: return@withContext 0.0
            val eth = wei.divide(BigDecimal("1000000000000000000"))
            String.format("%.4f", eth).toDouble()
        } catch (e: Exception) {
            println("EthereumFetcher error: ${e.localizedMessage}")
            throw e
        }
    }

    override suspend fun fetchPriceHistory(): List<Pair<String, Double>> = withContext(Dispatchers.IO) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.coingecko.com/api/v3/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(MainActivity.CoinGeckoService::class.java)

        val response = try {
            service.getMarketChart("ethereum")
        } catch (e: HttpException) {
            println("EthereumFetcher HTTP ${e.code()} - ${e.message()}")
            return@withContext emptyList()
        } catch (e: Exception) {
            println("EthereumFetcher unexpected error: ${e.localizedMessage}")
            return@withContext emptyList()
        }

        val zone = ZoneId.of("Pacific/Auckland")
        val formatter = DateTimeFormatter.ofPattern("MMM dd")

        response.prices.mapNotNull { entry ->
            val timestamp = entry[0].toLong()
            val price = entry[1]
            val date = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
            formatter.format(date) to price
        }
    }
}

object SolanaFetcher : CoinDataFetcher {

    private const val API_KEY = "38ec42e8-6eae-4e90-abf7-0e6c719c92b9"
    private const val BASE_URL = "https://api.helius.xyz/"

    interface HeliusService {
        @GET("v0/addresses/{address}/balances")
        suspend fun getBalance(
            @Path("address") address: String,
            @Query("api-key") apiKey: String
        ): HeliusResponse
    }

    data class HeliusResponse(val nativeBalance: Long)

    override suspend fun fetchBalance(address: String): Double = withContext(Dispatchers.IO) {
        try {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service = retrofit.create(HeliusService::class.java)
            val response = service.getBalance(address, API_KEY)

            val sol = BigDecimal(response.nativeBalance).divide(BigDecimal("1000000000"))
            sol.setScale(4, RoundingMode.HALF_UP).toDouble()
        } catch (e: Exception) {
            println("SolanaFetcher error: ${e.localizedMessage}")
            0.0
        }
    }

    override suspend fun fetchPriceHistory(): List<Pair<String, Double>> = withContext(Dispatchers.IO) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.coingecko.com/api/v3/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(MainActivity.CoinGeckoService::class.java)

        val response = try {
            service.getMarketChart("solana")
        } catch (e: HttpException) {
            println("SolanaFetcher HTTP ${e.code()} - ${e.message()}")
            return@withContext emptyList()
        } catch (e: Exception) {
            println("SolanaFetcher unexpected error: ${e.localizedMessage}")
            return@withContext emptyList()
        }

        val zone = ZoneId.of("Pacific/Auckland")
        val formatter = DateTimeFormatter.ofPattern("MMM dd")

        response.prices.mapNotNull { entry ->
            val timestamp = entry[0].toLong()
            val price = entry[1]
            val date = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
            formatter.format(date) to price
        }
    }
}

object ChainlinkFetcher : CoinDataFetcher {

    interface ChainlinkService {
        @GET("api")
        suspend fun getBalance(
            @Query("module") module: String = "account",
            @Query("action") action: String = "tokenbalance",
            @Query("contractaddress") contract: String = "0x514910771AF9Ca656af840dff83E8264EcF986CA",
            @Query("address") address: String,
            @Query("tag") tag: String = "latest",
            @Query("apikey") apiKey: String = "D3HM6P7NH8NMBINBJ7CP1ENXHMHS554ICX"
        ): ChainlinkResponse
    }

    data class ChainlinkResponse(val status: String, val message: String, val result: String)

    override suspend fun fetchBalance(address: String): Double = withContext(Dispatchers.IO) {
        try {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://api.etherscan.io/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service = retrofit.create(ChainlinkService::class.java)
            val response = service.getBalance(address = address)

            val link = response.result.toBigDecimalOrNull()?.divide(BigDecimal("1000000000000000000")) ?: BigDecimal.ZERO
            String.format("%.4f", link).toDouble()
        } catch (e: Exception) {
            println("ChainlinkFetcher error: ${e.localizedMessage}")
            throw e
        }
    }

    override suspend fun fetchPriceHistory(): List<Pair<String, Double>> = withContext(Dispatchers.IO) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.coingecko.com/api/v3/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(MainActivity.CoinGeckoService::class.java)

        val response = try {
            service.getMarketChart("chainlink")
        } catch (e: HttpException) {
            println("ChainlinkFetcher HTTP ${e.code()} - ${e.message()}")
            return@withContext emptyList()
        } catch (e: Exception) {
            println("ChainlinkFetcher unexpected error: ${e.localizedMessage}")
            return@withContext emptyList()
        }

        val zone = ZoneId.of("Pacific/Auckland")
        val formatter = DateTimeFormatter.ofPattern("MMM dd")

        response.prices.mapNotNull { entry ->
            val timestamp = entry[0].toLong()
            val price = entry[1]
            val date = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
            formatter.format(date) to price
        }
    }
}