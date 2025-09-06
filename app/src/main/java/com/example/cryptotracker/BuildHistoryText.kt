package com.example.cryptotracker

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Creates a nicely‑formatted 10‑day price‑history string. */
fun buildHistoryText(history: List<Pair<String, Double>>): String {
    val zone = ZoneId.of("Pacific/Auckland")
    val today = LocalDate.now(zone)
    val last10Days = (1..10).map { today.minusDays(it.toLong()) }
    val formatter = DateTimeFormatter.ofPattern("dd MMM")

    // Turn the list into a map for O(1) look‑ups
    val historyMap = history.associate { it.first to it.second }

    return last10Days.joinToString("\n") { date ->
        val formatted = formatter.format(date)
        val price = historyMap[formatted] ?: 0.0
        "$formatted: ${String.format("%,.2f", price)} NZD"
    }
}