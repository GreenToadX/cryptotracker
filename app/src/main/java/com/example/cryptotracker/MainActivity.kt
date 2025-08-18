package com.example.cryptotracker

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    private lateinit var priceTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        priceTextView = findViewById(R.id.priceTextView)

        // Replaced lifecycleScope with a simple Global CoroutineScope
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val price = fetchBitcoinPrice()
                priceTextView.text = "BTC Price: $price NZD"
            } catch (e: Exception) {
                priceTextView.text = "Error: ${e.message}"
            }
        }
    }

    private suspend fun fetchBitcoinPrice(): String = withContext(Dispatchers.IO) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.coingecko.com/api/v3/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(CoinGeckoService::class.java)
        val response = service.getPrices(ids = "bitcoin", vs = "nzd")
        val nzdPrice = response["bitcoin"]?.get("nzd") ?: 0.0
        nzdPrice.toString()
    }
}