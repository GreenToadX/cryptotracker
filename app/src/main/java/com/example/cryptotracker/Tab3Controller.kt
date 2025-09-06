package com.example.cryptotracker

class Tab3Controller(
    private val rootView: View,
    private val fragment: FiatToggleable
) {
    fun wireToggleButtons() {
        val btnHistorical = rootView.findViewById<ImageButton>(R.id.btnHistoricalNzd)
        val btnCurrent = rootView.findViewById<Button>(R.id.btnCurrentNzd)

        btnHistorical.setOnClickListener {
            fragment.showHistoricalNzd()
            btnCurrent.visibility = View.VISIBLE
        }

        btnCurrent.setOnClickListener {
            fragment.showCurrentNzd()
        }
    }
}
