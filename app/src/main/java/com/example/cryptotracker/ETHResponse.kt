package com.example.cryptotracker

data class ETHResponse(
    val status: String,
    val message: String,
    val result: String // balance in wei
)