package com.example.cryptotracker

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class BitcoinTransactionFragment : Fragment(R.layout.fragment_bitcoin_transaction) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BitcoinTxAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.txRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.setHasFixedSize(true)

        fetchTransactions()
    }

    private fun fetchTransactions() {
        val address = arguments?.getString("address") ?: return
        val url = "https://blockchain.info/rawaddr/$address"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = URL(url).readText()
                val json = JSONObject(response)
                val txs = json.getJSONArray("txs")

                val txList = mutableListOf<BitcoinTx>()
                for (i in 0 until txs.length()) {
                    val tx = txs.getJSONObject(i)
                    val time = tx.getLong("time") * 1000
                    val date = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(time))

                    val outputs = tx.getJSONArray("out")
                    val value = outputs.getJSONObject(0).getLong("value") / 100000000.0
                    val isIncoming = tx.getJSONArray("inputs")
                        .getJSONObject(0)
                        .getJSONObject("prev_out")
                        .getString("addr") != address

                    txList.add(
                        BitcoinTx(
                            date = date,
                            amount = String.format("%.8f", value),
                            isIncoming = isIncoming
                        )
                    )
                }

                withContext(Dispatchers.Main) {
                    adapter = BitcoinTxAdapter(txList)
                    recyclerView.adapter = adapter
                }

            } catch (e: Exception) {
                Log.e("BitcoinFragment", "Error fetching BTC transactions", e)
            }
        }
    }

    companion object {
        fun newInstance(address: String): BitcoinTransactionFragment {
            val fragment = BitcoinTransactionFragment()
            val args = Bundle()
            args.putString("address", address)
            fragment.arguments = args
            return fragment
        }
    }
}