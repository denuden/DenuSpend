package com.gmail.vondenuelle.denuspend.utils

import com.gmail.vondenuelle.denuspend.domain.models.wallet.RecurringChargeModel
import java.time.LocalDate
import java.time.YearMonth

/**
 * The schedule math for subscriptions and installments. Nothing here is stored except what the repository saves
 * (chargeCount, nextChargeDate, amount); the screens work the rest out from the doc and the card's statement day.
 */
object RecurringChargeHelper {

    // Date k of the schedule, counted from startDate. Installments land on the card's statement day (29–31 clamp to the month's end).
    // Subscriptions step from the anchor each time, so plusMonths(k) keeps a Jan 31 start on the 31st when the month has one.
    fun dateAt(charge: RecurringChargeModel, k: Int, statementDay: Int): LocalDate {
        val start = CardStatementHelper.toLocalDate(charge.startDate)
        if (charge.isInstallment) return CardStatementHelper.closingDate(YearMonth.from(start).plusMonths(k.toLong()), statementDay)
        val steps = k.toLong() * charge.repeatEvery
        return when (charge.repeatUnit) {
            RecurringChargeModel.UNIT_DAY -> start.plusDays(steps)
            RecurringChargeModel.UNIT_WEEK -> start.plusWeeks(steps)
            RecurringChargeModel.UNIT_YEAR -> start.plusYears(steps)
            else -> start.plusMonths(steps)
        }
    }

    // How many dates fall before today: a subscription that started in the past is never back-charged, it continues from today
    fun datesBefore(charge: RecurringChargeModel, today: LocalDate, statementDay: Int): Int {
        var k = 0
        while (dateAt(charge, k, statementDay).isBefore(today)) k++
        return k
    }

    fun nextDate(charge: RecurringChargeModel, statementDay: Int): LocalDate = dateAt(charge, charge.chargeCount, statementDay)

    // An installment's first month: the next statement day, today included (a charge on the statement day goes on that statement)
    fun firstInstallmentDate(statementDay: Int, today: LocalDate = LocalDate.now()): LocalDate {
        val thisMonth = CardStatementHelper.closingDate(YearMonth.from(today), statementDay)
        return if (today.isAfter(thisMonth)) CardStatementHelper.closingDate(YearMonth.from(today).plusMonths(1), statementDay) else thisMonth
    }

    // ₱10,000 over 3 months = ₱3,333.33 a month; the last month absorbs the rounding (₱3,333.34)
    fun monthlyAmount(totalAmount: Long, totalCount: Int): Long = if (totalCount > 0) totalAmount / totalCount else 0L

    // The amount for date k: a subscription's amount, or the installment month (the very last month takes the rounding)
    fun amountAt(charge: RecurringChargeModel, k: Int): Long {
        if (!charge.isInstallment) return charge.amount
        val monthly = monthlyAmount(charge.totalAmount, charge.totalCount)
        val month = charge.paidBefore + k + 1
        return if (month == charge.totalCount) charge.totalAmount - monthly * (charge.totalCount - 1) else monthly
    }

    fun monthsLeft(charge: RecurringChargeModel): Int =
        (charge.totalCount - charge.paidBefore - charge.chargeCount).coerceAtLeast(0)

    // What's still to come on an installment: every month that isn't paid or charged yet, with the last month's rounding
    fun stillToPay(charge: RecurringChargeModel): Long {
        if (!charge.isInstallment || monthsLeft(charge) == 0) return 0L
        val used = charge.paidBefore + charge.chargeCount
        return charge.totalAmount - monthlyAmount(charge.totalAmount, charge.totalCount) * used
    }

    fun lastInstallmentDate(charge: RecurringChargeModel, statementDay: Int): LocalDate =
        dateAt(charge, (charge.totalCount - charge.paidBefore - 1).coerceAtLeast(0), statementDay)

    // "Installments to come" on a card: banks hold the whole installment against the limit, so "Available" subtracts it too
    fun installmentsToCome(charges: List<RecurringChargeModel>, walletId: String): Long =
        charges.filter { it.walletId == walletId && it.isInstallment && it.active }.sumOf { stillToPay(it) }
}
