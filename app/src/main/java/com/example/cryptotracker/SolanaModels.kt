package com.example.cryptotracker

import com.squareup.moshi.Json

data class NativeTransfer(
    @Json(name = "fromUserAccount") val from: String,
    @Json(name = "toUserAccount") val to: String,
    val amount: Long
)

data class AccountData(
    val account: String,
    val nativeBalanceChange: Long
)

data class TransactionRecord(
    val signature: String,
    val timestamp: Long,
    val nativeTransfers: List<NativeTransfer> = emptyList(),
    val accountData: List<AccountData>? = emptyList()
)