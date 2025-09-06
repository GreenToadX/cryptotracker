package com.example.cryptotracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import android.util.Log

class SolanaTransactionFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TransactionAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.solana_transaction_fragment, container, false)
        recyclerView = view.findViewById(R.id.transactionRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = TransactionAdapter()
        recyclerView.adapter = adapter

        recyclerView.addItemDecoration(
            DividerItemDecoration(context, DividerItemDecoration.VERTICAL).apply {
                setDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.divider)!!)
            }
        )

        fetchAndDisplayTransactions()

        return view
    }

    private fun fetchAndDisplayTransactions() {
        val address = MainActivity.walletAddresses.getOrElse("solana") {
            Log.w("SolanaFragment", "Solana wallet address not found or not initialized")
            return
        }

        lifecycleScope.launch {
            val transactions = fetchRecentSolanaTransactions(address)
            adapter.submitList(transactions)
        }
    }

    private class TransactionAdapter : ListAdapter<String, TransactionAdapter.TxViewHolder>(
        object : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(oldItem: String, newItem: String) = oldItem == newItem
            override fun areContentsTheSame(oldItem: String, newItem: String) = oldItem == newItem
        }
    ) {
        inner class TxViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val dateText: TextView = view.findViewById(R.id.dateText)
            val amountText: TextView = view.findViewById(R.id.amountText)
            val coinIcon: ImageView = view.findViewById(R.id.coinIcon)
            val directionIcon: ImageView = view.findViewById(R.id.directionIcon)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TxViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.solana_transaction_fragment_item, parent, false)
            return TxViewHolder(view)
        }

        override fun onBindViewHolder(holder: TxViewHolder, position: Int) {
            val parts = getItem(position).split(" | ")
            val date = parts[0]
            val direction = parts[1]
            val amount = parts[2]

            holder.dateText.text = date
            holder.amountText.text = amount
            holder.coinIcon.setImageResource(R.drawable.solana_sol_logo)

            if (direction == "Sent") {
                holder.directionIcon.setImageResource(R.drawable.solana_red)
                val red = ContextCompat.getColor(holder.itemView.context, android.R.color.holo_red_dark)
                holder.amountText.setTextColor(red)
            } else {
                holder.directionIcon.setImageResource(R.drawable.solana_green)
                val green = ContextCompat.getColor(holder.itemView.context, android.R.color.holo_green_dark)
                holder.amountText.setTextColor(green)
            }

            // Fade-in animation
            holder.itemView.alpha = 0f
            holder.itemView.animate().alpha(1f).setDuration(300).start()
        }
    }
}