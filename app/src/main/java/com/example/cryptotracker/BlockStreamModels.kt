package com.example.cryptotracker

data class Transaction(
    val status: Status,
    val vin: List<TxInput>,
    val vout: List<TxOutput>
)

data class Status(val block_time: Long)
data class TxInput(val prevout: TxOutput?)
data class TxOutput(val value: Long, val scriptpubkey_address: String)