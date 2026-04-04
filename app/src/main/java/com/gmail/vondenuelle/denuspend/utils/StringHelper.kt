package com.gmail.vondenuelle.denuspend.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.YearMonth
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

    fun formatPesoFromDouble(amount: Double): String {
        val formatter = DecimalFormat("#,##0.00")
        return "₱${formatter.format(amount)}"
    }
}

fun getMonthByIndex(index : Int) : String{
     return when(index) {
        0 -> "Jan"
        1 -> "Feb"
        2 -> "Mar"
        3 -> "Apr"
        4 -> "May"
        5 -> "Jun"
        6 -> "Jul"
        7 -> "Aug"
        8 -> "Sep"
        9 -> "Oct"
        10 -> "Nov"
        11 -> "Dec"
         else -> ""
     }
}

fun getDaysInMonth(monthIndex: Int, year: Int): Int {
    val month = monthIndex + 1 // YearMonth uses 1-12
    return YearMonth.of(year, month).lengthOfMonth()
}