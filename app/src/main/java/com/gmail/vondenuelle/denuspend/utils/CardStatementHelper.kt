package com.gmail.vondenuelle.denuspend.utils

import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionModel
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletModel
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletMovementModel
import com.google.firebase.Timestamp
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

/**
 * Credit card statement cycles. A cycle runs from the day after one statement day to the next statement day (both included),
 * so a charge made on the statement day goes on the statement that closes that day, like a bank's.
 * Day 29–31 falls on the month's last day in shorter months (31 → Feb 28 → Mar 31).
 */
object CardStatementHelper {

    // start and end are both included; end is the statement (closing) day
    data class Cycle(val start: LocalDate, val end: LocalDate) {
        fun contains(date: LocalDate) = !date.isBefore(start) && !date.isAfter(end)
    }

    // What one cycle's entries add up to. Both are positive amounts.
    data class Totals(val charges: Long, val payments: Long, val chargeCount: Int)

    fun closingDate(month: YearMonth, statementDay: Int): LocalDate =
        month.atDay(statementDay.coerceIn(1, month.lengthOfMonth()))

    /**
     * monthsBack 0 = the open cycle (it closes today or later), 1 = the last closed statement, and so on.
     * The last close is the latest statement day *before* today, so the cycle stays open for the whole statement day.
     */
    fun cycle(statementDay: Int, monthsBack: Int, today: LocalDate = LocalDate.now()): Cycle {
        val thisMonth = YearMonth.from(today)
        val lastClose = closingDate(thisMonth, statementDay).let { if (today.isAfter(it)) it else closingDate(thisMonth.minusMonths(1), statementDay) }
        val endMonth = YearMonth.from(lastClose).plusMonths(1L - monthsBack)
        return Cycle(
            start = closingDate(endMonth.minusMonths(1), statementDay).plusDays(1),
            end = closingDate(endMonth, statementDay)
        )
    }

    fun startOfDay(date: LocalDate): Timestamp =
        Timestamp(Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()))

    fun toLocalDate(timestamp: Timestamp): LocalDate =
        timestamp.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()

    /**
     * Money out = a charge (expenses, other charges, money moved off the card), money in = a payment or credit.
     * Balance corrections count by their sign too, so the statement math always matches the card's real balance.
     */
    fun totals(transactions: List<TransactionModel>, movements: List<WalletMovementModel>): Totals {
        val expenses = transactions.filter { it.amount < 0 }
        val outs = movements.filter { it.amount < 0 }
        return Totals(
            charges = -expenses.sumOf { it.amount } - outs.sumOf { it.amount },
            payments = movements.filter { it.amount > 0 }.sumOf { it.amount },
            chargeCount = expenses.size + outs.size
        )
    }

    /**
     * What the card owed when `cycle` closed: today's owed amount, minus what was charged after the close, plus what was paid after it.
     * `after` = every entry dated after that close (the caller passes them from the same listener).
     */
    fun statementBalance(card: WalletModel, after: Totals): Long =
        -card.balance - after.charges + after.payments

    /**
     * The "Owed now" typed in when the card was added. It's a charge of the cycle the card was added in (the statement day picked vs
     * the day it was added), not of the bill before it. Worked out like a statement balance, so `sinceAdded` must hold every entry
     * from the day it was added (a listener that starts on or before that day). A credit balance entered counts as ₱0.
     */
    fun owedWhenAdded(card: WalletModel, sinceAdded: Totals): Long = statementBalance(card, sinceAdded).coerceAtLeast(0L)

    // "Nov 28", or "Nov 28, 2026" with the year
    fun formatDay(date: LocalDate, withYear: Boolean = false): String =
        date.format(DateTimeFormatter.ofPattern(if (withYear) "MMM d, yyyy" else "MMM d", Locale.getDefault()))

    // "1st", "2nd", "3rd", "28th"… for "28th of every month"
    fun ordinal(day: Int): String {
        val suffix = if (day % 100 in 11..13) "th" else when (day % 10) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
        return "$day$suffix"
    }
}
