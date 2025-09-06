package com.example.cryptotracker

object CoinRegistry {
    val supportedCoins = listOf(
        CoinItem("bitcoin", "BTC", R.drawable.bitcoin_btc_logo),
        CoinItem("ethereum", "ETH", R.drawable.ethereum_eth_logo),
        CoinItem("solana", "SOL", R.drawable.solana_sol_logo),
        CoinItem("chainlink", "LINK", R.drawable.chainlink_link_logo),
       // CoinItem("litecoin", "LTC", R.drawable.litecoin_ltc_logo),
        //CoinItem("dogecoin", "DOGE", R.drawable.dogecoin_doge_logo),
        CoinItem("cardano", "ADA", R.drawable.cardano_ada_logo)
        // Add more coins here
    )
}