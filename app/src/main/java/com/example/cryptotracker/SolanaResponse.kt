package com.example.cryptotracker

data class SolanaResponse(
    val result: SolanaResult
)

data class SolanaResult(
    val value: Long // lamports
)