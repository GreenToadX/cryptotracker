package com.example.cryptotracker

data class BTCResponse(
    val data: Map<String, BTCData>
)

data class BTCData(
    val address: BTCAddress
)

data class BTCAddress(
    val balance: Long // in satoshis
)
