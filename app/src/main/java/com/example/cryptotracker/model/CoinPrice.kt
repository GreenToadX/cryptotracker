package com.greentoadx.cryptotracker.model

data class CoinPrice(
    val symbol: String,
    val priceUsd: Double,
    val change24h: Double
)