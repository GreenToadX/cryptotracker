package com.example.cryptotracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class EthereumTx(
    val date: String,
    val amount: String,
    val isIncoming: Boolean
)

class EthereumTxAdapter(
    initialList: List<EthereumTx>,
    private val coinId: String
) : RecyclerView.Adapter<EthereumTxAdapter.TxViewHolder>() {

    private val txList: MutableList<EthereumTx> = initialList.toMutableList()

    class TxViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateText: TextView = view.findViewById(R.id.dateText)
        val amountText: TextView = view.findViewById(R.id.amountText)
        val directionIcon: ImageView = view.findViewById(R.id.directionIcon)
        val coinIcon: ImageView = view.findViewById(R.id.coinIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TxViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ethereum_transaction, parent, false)
        return TxViewHolder(view)
    }

    override fun onBindViewHolder(holder: TxViewHolder, position: Int) {
        val tx = txList[position]
        val isChainlink = coinId.lowercase() == "chainlink"

        holder.dateText.text = tx.date
        holder.amountText.text = "${tx.amount} ${if (isChainlink) "LINK" else "ETH"}"

        holder.directionIcon.setImageResource(
            if (tx.isIncoming)
                if (isChainlink) R.drawable.chainlink_green else R.drawable.ethereum_green
            else
                if (isChainlink) R.drawable.chainlink_red else R.drawable.ethereum_red
        )

        holder.coinIcon.setImageResource(
            if (isChainlink) R.drawable.chainlink_link_logo else R.drawable.ethereum_eth_logo
        )
    }

    override fun getItemCount(): Int = txList.size

    fun updateData(newList: List<EthereumTx>) {
        txList.clear()
        txList.addAll(newList)
        notifyDataSetChanged()
    }
}