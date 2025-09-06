package com.example.cryptotracker

import android.view.View
import android.widget.Button
import android.widget.ImageButton

class Tab3Controller(
    private val rootView: View,
    private val fragment: FiatToggleable
) {
    fun wireToggleButtons() {
        val btnHistorical = rootView.findViewById<ImageButton>(R.id.btnHistoricalNzd)
        val btnCurrent = rootView.findViewById<Button>(R.id.btnCurrentNzd)

        btnHistorical.setOnClickListener {
            fragment.showHistoricalNzd()
            btnHistorical.visibility = View.GONE
            btnCurrent.visibility = View.VISIBLE
        }

        btnCurrent.setOnClickListener {
            fragment.showCurrentNzd()
            btnCurrent.visibility = View.GONE
            btnHistorical.visibility = View.VISIBLE
        }
    }
}