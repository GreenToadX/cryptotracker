package com.example.cryptotracker

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

private const val ETHERSCAN_API_KEY = "D3HM6P7NH8NMBINBJ7CP1ENXHMHS554ICX"
private const val ETHERSCAN_BASE_URL = "https://api.etherscan.io/"

interface EtherscanTxService {
    @GET("api")
    suspend fun getNormalTxs(
        @Query("module") module: String = "account",
        @Query("action") action: String = "txlist",
        @Query("address") address: String,
        @Query("startblock") startBlock: Long = 0,
        @Query("endblock") endBlock: Long = 99999999,
        @Query("sort") sort: String = "desc",
        @Query("apikey") apiKey: String = ETHERSCAN_API_KEY
    ): Response<EtherscanTxResponse>

    @GET("api")
    suspend fun getTokenTxs(
        @Query("module") module: String = "account",
        @Query("action") action: String = "tokentx",
        @Query("address") address: String,
        @Query("startblock") startBlock: Long = 0,
        @Query("endblock") endBlock: Long = 99999999,
        @Query("sort") sort: String = "desc",
        @Query("apikey") apiKey: String = ETHERSCAN_API_KEY
    ): Response<EtherscanTxResponse>
}

data class EtherscanTxResponse(
    val status: String,
    val message: String,
    val result: List<EtherscanTxItem>
)

data class EtherscanTxItem(
    val blockNumber: String,
    val timeStamp: String,
    val hash: String,
    val nonce: String,
    val blockHash: String,
    val transactionIndex: String,
    val from: String,
    val to: String,
    val value: String?,
    val gas: String,
    val gasPrice: String,
    val isError: String,
    val txreceipt_status: String,
    val input: String,
    val contractAddress: String,
    val cumulativeGasUsed: String,
    val gasUsed: String,
    val confirmations: String,
    val tokenName: String? = null,
    val tokenSymbol: String? = null,
    val tokenDecimal: String? = null
)

object EthereumTransactionFetcher {

    private val retrofit = Retrofit.Builder()
        .baseUrl(ETHERSCAN_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service = retrofit.create(EtherscanTxService::class.java)

    suspend fun fetchRecentEthereumTransactions(address: String): List<String> =
        withContext(Dispatchers.IO) {
            try {
                if (!address.matches(Regex("^0x[a-fA-F0-9]{40}$"))) {
                    Log.e("EthereumTxFetcher", "Invalid Ethereum address: $address")
                    return@withContext emptyList()
                }

                Log.d("EthereumTxFetcher", "Calling Etherscan with address: $address")

                val normalResp = service.getNormalTxs(address = address)
                val tokenResp = service.getTokenTxs(address = address)

                if (!normalResp.isSuccessful || normalResp.body() == null) {
                    val err = normalResp.errorBody()?.string()
                    Log.e("EthereumTxFetcher", "NormalTxs failed: ${normalResp.code()} $err")
                    return@withContext emptyList()
                }

                if (!tokenResp.isSuccessful || tokenResp.body() == null) {
                    val err = tokenResp.errorBody()?.string()
                    Log.e("EthereumTxFetcher", "TokenTxs failed: ${tokenResp.code()} $err")
                    return@withContext emptyList()
                }

                val combined = (normalResp.body()!!.result + tokenResp.body()!!.result)
                    .filter { it.isError == "0" }
                    .filter { it.from.equals(address, true) || it.to.equals(address, true) }
                    .sortedByDescending { it.timeStamp.toLong() }

                val zone = java.time.ZoneId.of("Pacific/Auckland")
                val dateFmt = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                val dustThresholdEth = java.math.BigDecimal("0.00001")

                combined.take(10).map { tx ->
                    val tsMs = tx.timeStamp.toLong() * 1000
                    val dateStr = java.time.Instant.ofEpochMilli(tsMs).atZone(zone).format(dateFmt)
                    val direction = if (tx.from.equals(address, true)) "Sent" else "Received"

                    val amountStr = if (!tx.value.isNullOrBlank() && tx.contractAddress.isBlank()) {
                        val eth = java.math.BigDecimal(tx.value)
                            .divide(java.math.BigDecimal("1000000000000000000"))
                        if (eth < dustThresholdEth) "≈ 0 ETH"
                        else "${eth.stripTrailingZeros().toPlainString()} ETH"
                    } else {
                        val decimals = tx.tokenDecimal?.toIntOrNull() ?: 0
                        val raw = tx.value?.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO
                        val tokenAmt = raw.movePointLeft(decimals)
                        val symbol = tx.tokenSymbol ?: "TOKEN"
                        "${tokenAmt.stripTrailingZeros().toPlainString()} $symbol"
                    }

                    "$dateStr | $direction | $amountStr"
                }

            } catch (e: Exception) {
                Log.e("EthereumTxFetcher", "Failed to fetch txs", e)
                if (e is retrofit2.HttpException) {
                    val errorBody = e.response()?.errorBody()?.string()
                    Log.e("EthereumTxFetcher", "HTTP ${e.code()} error: $errorBody")
                }
                emptyList()
            }
        }
}