package com.example.cryptotracker

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

data class WalletInput(val coinId: String, val label: String, var address: String)

class WalletInputAdapter(
    private val inputs: List<WalletInput>
) : RecyclerView.Adapter<WalletInputAdapter.InputViewHolder>() {

    inner class InputViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val coinIcon: ImageView = view.findViewById(R.id.coinIcon)
        val coinLabel: TextView = view.findViewById(R.id.coinLabel)
        val addressField: TextInputEditText = view.findViewById(R.id.addressField)
        val inputLayout: TextInputLayout = view.findViewById(R.id.walletInputLayout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InputViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_wallet_input, parent, false)
        return InputViewHolder(view)
    }

    override fun onBindViewHolder(holder: InputViewHolder, position: Int) {
        val input = inputs[position]
        holder.coinLabel.text = input.label
        holder.addressField.setText(input.address)
        holder.addressField.hint = "Enter your ${input.label} address"

        // Optional: animate field when updated via QR
        holder.addressField.setBackgroundColor(0xFF444444.toInt())
        holder.addressField.postDelayed({
            holder.addressField.setBackgroundColor(0x00000000)
        }, 300)

        // Load icon from CoinRegistry
        val iconRes = CoinRegistry.supportedCoins.find { it.id == input.coinId }?.iconResId
        if (iconRes != null) holder.coinIcon.setImageResource(iconRes)

        holder.addressField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                input.address = s.toString()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    override fun getItemCount(): Int = inputs.size
}