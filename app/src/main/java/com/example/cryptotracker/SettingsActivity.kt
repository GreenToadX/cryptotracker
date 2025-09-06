package com.example.cryptotracker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.zxing.integration.android.IntentIntegrator

class SettingsActivity : AppCompatActivity() {

    private lateinit var walletRecyclerView: RecyclerView
    private lateinit var saveButton: Button
    private lateinit var scanButton: Button
    private lateinit var walletInputs: MutableList<WalletInput>
    private lateinit var adapter: WalletInputAdapter

    private val prefs by lazy { getSharedPreferences("wallets", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        walletRecyclerView = findViewById(R.id.walletRecyclerView)
        saveButton = findViewById(R.id.saveButton)
        scanButton = findViewById(R.id.scanButton)

        walletInputs = CoinRegistry.supportedCoins.map { coin ->
            WalletInput(coin.id, coin.symbol, WalletManager.getWallet(this, coin.id))
        }.toMutableList()

        adapter = WalletInputAdapter(walletInputs)
        walletRecyclerView.layoutManager = LinearLayoutManager(this)
        walletRecyclerView.adapter = adapter

        saveButton.setOnClickListener {
            val updatedWallets = walletInputs.associate { it.coinId to it.address }
            WalletManager.saveWallets(this, updatedWallets)
            Toast.makeText(this, "Wallet addresses saved", Toast.LENGTH_SHORT).show()
        }

        @Suppress("DEPRECATION")
        scanButton.setOnClickListener {
            val integrator = IntentIntegrator(this)
            integrator.setPrompt("Scan wallet QR code")
            integrator.setBeepEnabled(true)
            integrator.setOrientationLocked(true)
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            integrator.initiateScan()
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && result.contents != null) {
            val scanned = result.contents.trim()

            val matched = walletInputs.find { input ->
                when (input.coinId) {
                    "bitcoin" -> scanned.startsWith("bc1") || scanned.startsWith("1") || scanned.startsWith("3")
                    "ethereum", "chainlink" -> scanned.startsWith("0x") && scanned.length == 42
                    "solana" -> scanned.length in 32..44 && scanned.all { it.isLetterOrDigit() }
                    else -> false
                }
            }

            if (matched != null) {
                matched.address = scanned
                adapter.notifyItemChanged(walletInputs.indexOf(matched))
            } else {
                Toast.makeText(this, "Unrecognized wallet format", Toast.LENGTH_SHORT).show()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}