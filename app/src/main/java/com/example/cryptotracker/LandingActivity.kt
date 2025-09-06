package com.example.cryptotracker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.cryptotracker.SettingsActivity

class LandingActivity : AppCompatActivity() {

    private lateinit var enterButton: Button
    private lateinit var settingsButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landing)

        enterButton = findViewById(R.id.enterButton)
        settingsButton = findViewById(R.id.settingsButton)

        enterButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
}
