package com.example.cryptotracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class CoinItem(val id: String, val symbol: String, val iconResId: Int)

class CoinAdapter(
    private val coins: List<CoinItem>,
    private val onCoinClick: (CoinItem) -> Unit
) : RecyclerView.Adapter<CoinAdapter.CoinViewHolder>() {

    inner class CoinViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageButton = view.findViewById(R.id.coinIcon)
        val label: TextView = view.findViewById(R.id.coinLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CoinViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_coin_button, parent, false)
        return CoinViewHolder(view)
    }

    override fun onBindViewHolder(holder: CoinViewHolder, position: Int) {
        val coin = coins[position]
        holder.icon.setImageResource(coin.iconResId)
        holder.label.text = coin.symbol
        holder.icon.setOnClickListener { onCoinClick(coin) }
    }

    override fun getItemCount(): Int = coins.size
}