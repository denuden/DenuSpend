package com.gmail.vondenuelle.denuspend.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale



object CurrencyUtils {

    // Convert cents (Long) -> formatted string with commas and 2 decimals
    fun formatCents(cents: Long): String {
        val amount = cents / 100.0
        val formatter = DecimalFormat("#,##0.00")
        return formatter.format(amount)
    }

    // Clean input string -> Long cents
    fun parseInputToCents(input: String): Long {
        // Keep only digits
        val digits = input.filter { it.isDigit() }
        return digits.toLongOrNull() ?: 0L
    }
}