package com.gmail.vondenuelle.denuspend.utils

import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionModel
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletModel
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletMovementModel
import com.google.firebase.Timestamp
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
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
     * Balance corrections are added up first and only what's left counts, by its sign, so lowering then raising the owed amount back
     * cancels out instead of landing as a payment plus a charge. Charges − payments stays the same, so statement balances don't change.
     */
    fun totals(transactions: List<TransactionModel>, movements: List<WalletMovementModel>): Totals {
        val expenses = transactions.filter { it.amount < 0 }
        val (corrections, others) = movements.partition { it.presetKey == WalletMovementModel.PRESET_CORRECTION }
        val outs = others.filter { it.amount < 0 }
        val netCorrection = corrections.sumOf { it.amount }
        return Totals(
            charges = -expenses.sumOf { it.amount } - outs.sumOf { it.amount } + (-netCorrection).coerceAtLeast(0L),
            payments = others.filter { it.amount > 0 }.sumOf { it.amount } + netCorrection.coerceAtLeast(0L),
            chargeCount = expenses.size + outs.size + (if (netCorrection < 0) 1 else 0)
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

    const val MAX_DUE_DAYS = 25 // so the due date always lands before the next statement closes (the shortest cycle is 28 days)
    const val DUE_WARNING_DAYS = 5

    enum class DueState { UPCOMING, SOON, OVERDUE, PAID }

    data class DueStatus(val date: LocalDate, val daysLeft: Int, val left: Long, val state: DueState) {
        val needsWarning get() = state == DueState.SOON || state == DueState.OVERDUE
    }

    /**
     * When the last closed statement has to be paid: its closing day + the card's due days. Null when the due days aren't set,
     * or when that statement wasn't tracked (the card was added after it closed). Only a warning: nothing here touches the statements.
     */
    fun dueDate(card: WalletModel, today: LocalDate = LocalDate.now()): LocalDate? {
        if (!card.isCreditCard || card.statementDay !in 1..31 || card.paymentDueDays !in 1..MAX_DUE_DAYS) return null
        val previous = cycle(card.statementDay, 1, today)
        if (toLocalDate(card.createdAt).isAfter(previous.end)) return null
        return previous.end.plusDays(card.paymentDueDays.toLong())
    }

    // From 5 days before the due date until the next statement closes (then it's a new statement with a new due date)
    fun isInDueWindow(card: WalletModel, today: LocalDate = LocalDate.now()): Boolean =
        dueDate(card, today)?.let { !today.isBefore(it.minusDays(DUE_WARNING_DAYS.toLong())) } ?: false

    // "Still unpaid" of the last closed statement = statement balance − paid since = owed now − what was charged since it closed
    fun leftToPay(card: WalletModel, chargesSinceClose: Long): Long = (-card.balance - chargesSinceClose).coerceAtLeast(0L)

    fun dueStatus(card: WalletModel, left: Long, today: LocalDate = LocalDate.now()): DueStatus? {
        val date = dueDate(card, today) ?: return null
        val daysLeft = ChronoUnit.DAYS.between(today, date).toInt()
        val state = when {
            left == 0L -> DueState.PAID
            daysLeft < 0 -> DueState.OVERDUE
            daysLeft <= DUE_WARNING_DAYS -> DueState.SOON
            else -> DueState.UPCOMING
        }
        return DueStatus(date, daysLeft, left, state)
    }

    // For the Wallet tab and Home: null until the card's charges since the close have loaded (only cards inside their due window)
    fun dueStatus(card: WalletModel, chargesSinceClose: Map<String, Long>): DueStatus? =
        chargesSinceClose[card.docId]?.let { dueStatus(card, leftToPay(card, it)) }

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
