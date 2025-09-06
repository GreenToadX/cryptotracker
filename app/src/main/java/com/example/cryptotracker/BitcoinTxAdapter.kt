package com.example.cryptotracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class BitcoinTx(
    val date: String,
    val amount: String,
    val isIncoming: Boolean
)

class BitcoinTxAdapter(
    private val txList: List<BitcoinTx>
) : RecyclerView.Adapter<BitcoinTxAdapter.TxViewHolder>() {

    class TxViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateText: TextView = view.findViewById(R.id.dateText)
        val amountText: TextView = view.findViewById(R.id.amountText)
        val directionIcon: ImageView = view.findViewById(R.id.directionIcon)
        val coinIcon: ImageView = view.findViewById(R.id.coinIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TxViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bitcoin_transaction, parent, false)
        return TxViewHolder(view)
    }

    override fun onBindViewHolder(holder: TxViewHolder, position: Int) {
        val tx = txList[position]

        holder.dateText.text = tx.date
        holder.amountText.text = "${tx.amount} BTC"

        holder.directionIcon.setImageResource(
            if (tx.isIncoming) R.drawable.bitcoin_green else R.drawable.bitcoin_red
        )

        holder.coinIcon.setImageResource(R.drawable.bitcoin_btc_logo)
    }

    override fun getItemCount(): Int = txList.size
}