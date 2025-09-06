package com.example.cryptotracker

import android.content.Context
import android.content.SharedPreferences

object WalletManager {

    private const val PREF_NAME = "wallets"

    fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getWallets(context: Context): Map<String, String> {
        val prefs = getPrefs(context)
        return mapOf(
            Pair("bitcoin", prefs.getString("bitcoin", "") ?: ""),
            Pair("ethereum", prefs.getString("ethereum", "") ?: ""),
            Pair("solana", prefs.getString("solana", "") ?: ""),
            Pair("chainlink", prefs.getString("chainlink", "") ?: ""),
            Pair("cardano", prefs.getString("cardano", "") ?: "")
        )
    }

    fun saveWallets(context: Context, wallets: Map<String, String>) {
        val prefs = getPrefs(context)
        prefs.edit().apply {
            wallets.forEach { (key, value) -> putString(key, value.trim()) }
            apply()
        }
    }

    fun getWallet(context: Context, coin: String): String {
        return getPrefs(context).getString(coin, "") ?: ""
    }

    fun setWallet(context: Context, coin: String, address: String) {
        getPrefs(context).edit().putString(coin, address.trim()).apply()
    }
}