package com.example.cryptotracker

import android.util.Log
import com.example.cryptotracker.BlockchairService
import com.example.cryptotracker.CoinGeckoService

class CryptoRepository(
    private val blockchair: BlockchairService,
    private val coingecko: CoinGeckoService
) {
    suspend fun getBTCValue(address: String): Double {
        try {
            Log.d("CryptoRepository", "Calling Blockchair API for address: $address")
            val response = blockchair.getBTCData(address)
            Log.d("CryptoRepository", "Blockchair response: $response")

            val balanceSats = response.data[address]?.address?.balance ?: 0L
            Log.d("CryptoRepository", "Balance in sats: $balanceSats")

            val btc = balanceSats / 100_000_000.0
            Log.d("CryptoRepository", "Balance in BTC: $btc")

            Log.d("CryptoRepository", "Calling CoinGecko API for bitcoin price")
            val priceMap = coingecko.getPrices("bitcoin")
            Log.d("CryptoRepository", "CoinGecko price map: $priceMap")

            val price = priceMap["bitcoin"]?.get("nzd") ?: 0.0
            Log.d("CryptoRepository", "Bitcoin price in NZD: $price")

            val value = btc * price
            Log.d("CryptoRepository", "Final BTC value in NZD: $value")

            return value
        } catch (e: Exception) {
            Log.e("CryptoRepository", "Error in getBTCValue", e)
            throw e
        }
    }
}