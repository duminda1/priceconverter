package com.pockettoolsstudio.pocketcurrency.util

import java.text.NumberFormat
import java.util.Locale

object AmountInputFormatter {
    fun filterInput(input: String): String {
        val filtered = input.filter { it.isDigit() || it == '.' || it == ',' }
        val lastSeparatorIndex = filtered.lastIndexOfAny(charArrayOf('.', ','))
        if (lastSeparatorIndex == -1) {
            return filtered
        }
        val beforeSeparator = filtered.substring(0, lastSeparatorIndex)
            .replace(".", "")
            .replace(",", "")
        val separator = filtered[lastSeparatorIndex]
        val afterSeparator = filtered.substring(lastSeparatorIndex + 1)
            .replace(".", "")
            .replace(",", "")
        return beforeSeparator + separator + afterSeparator
    }

    fun formatAmount(amount: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 0
            isGroupingUsed = false
        }
        return formatter.format(amount)
    }

    fun parseInput(input: String): Double? {
        val filtered = input.filter { it.isDigit() || it == '.' || it == ',' }
        if (filtered.isBlank()) {
            return null
        }
        val lastSeparatorIndex = filtered.lastIndexOfAny(charArrayOf('.', ','))
        val normalized = if (lastSeparatorIndex == -1) {
            filtered
        } else {
            val beforeSeparator = filtered.substring(0, lastSeparatorIndex)
                .replace(".", "")
                .replace(",", "")
            val afterSeparator = filtered.substring(lastSeparatorIndex + 1)
                .replace(".", "")
                .replace(",", "")
            if (beforeSeparator.isEmpty() && afterSeparator.isEmpty()) {
                return null
            }
            if (afterSeparator.isEmpty()) {
                "$beforeSeparator."
            } else {
                "$beforeSeparator.$afterSeparator"
            }
        }
        return normalized.toDoubleOrNull()
    }
}
