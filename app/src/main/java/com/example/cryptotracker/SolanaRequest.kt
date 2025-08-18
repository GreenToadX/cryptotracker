package com.example.cryptotracker

data class SolanaRequest(
    val jsonrpc: String = "2.0",
    val method: String = "getBalance",
    val params: List<String>,
    val id: Int = 1
)