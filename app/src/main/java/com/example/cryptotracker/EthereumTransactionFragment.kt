package com.example.cryptotracker

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class EthereumTransactionFragment : Fragment(R.layout.fragment_ethereum_transaction) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: EthereumTxAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.ethRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.setHasFixedSize(true)

        fetchTransactions()
    }

    private fun fetchTransactions() {
        val address = arguments?.getString("address") ?: return
        val coinId = arguments?.getString("coin") ?: "ethereum"
        val url = "https://api.etherscan.io/api?module=account&action=txlist&address=$address&sort=desc&apikey=D3HM6P7NH8NMBINBJ7CP1ENXHMHS554ICX"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = URL(url).readText()
                val json = JSONObject(response)
                val status = json.optString("status")
                val message = json.optString("message")

                if (status != "1") {
                    Log.e("EthereumFragment", "API error: $message")
                    return@launch
                }

                val result = json.getJSONArray("result")
                val txList = mutableListOf<EthereumTx>()

                for (i in 0 until result.length()) {
                    val tx = result.getJSONObject(i)
                    if (tx.getString("isError") == "0") {
                        txList.add(
                            EthereumTx(
                                date = formatTimestamp(tx.getString("timeStamp")),
                                amount = formatEth(tx.getString("value")),
                                isIncoming = tx.getString("to").equals(address, ignoreCase = true)
                            )
                        )
                    }
                }

                withContext(Dispatchers.Main) {
                    adapter = EthereumTxAdapter(txList, coinId)
                    recyclerView.adapter = adapter
                }

            } catch (e: Exception) {
                Log.e("EthereumFragment", "Error fetching transactions", e)
            }
        }
    }

    private fun formatTimestamp(ts: String): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(Date(ts.toLong() * 1000))
    }

    private fun formatEth(value: String): String {
        val eth = BigDecimal(value).divide(BigDecimal("1000000000000000000"))
        return eth.setScale(4, RoundingMode.DOWN).toPlainString()
    }

    companion object {
        fun newInstance(address: String, coin: String): EthereumTransactionFragment {
            val fragment = EthereumTransactionFragment()
            val args = Bundle()
            args.putString("address", address)
            args.putString("coin", coin)
            fragment.arguments = args
            return fragment
        }
    }
}