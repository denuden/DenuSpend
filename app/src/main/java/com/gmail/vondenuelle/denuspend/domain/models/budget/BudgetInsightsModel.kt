package com.gmail.vondenuelle.denuspend.domain.models.budget

import com.gmail.vondenuelle.denuspend.domain.models.transaction.DailyHistoryModel
import com.google.firebase.Timestamp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

// Everything the Insights screen shows for one category and range. Amounts are positive cents, like the daily docs.
// Built on the device from daily docs (up to 31 days) or monthly docs (longer), never stored in Firestore.
data class BudgetInsightsModel(
    val category: String = "",
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate = LocalDate.now(),
    val dayCount: Int = 0,
    val spent: Long = 0L,
    val allowance: Long = 0L,
    val transactionCount: Long = 0L,
    // Spent on every category in the range, for "Food is X% of all your spending"
    val allCategoriesSpent: Long = 0L,
    // One entry per calendar day (days with no doc are ₱0). Empty on long ranges, which only have monthly totals.
    val days: List<InsightDay> = emptyList(),
    // Monday to Sunday
    val weekdays: List<InsightWeekday> = emptyList(),
    val months: List<InsightMonth> = emptyList(),
    // The period right before startDate (same number of days, or of months on long ranges), for "Compared to before".
    // It never starts before tracking did, so days before the first entry don't pull the average down (0 days = nothing to compare).
    val previousStartDate: LocalDate = LocalDate.now(),
    val previousSpent: Long = 0L,
    val previousDayCount: Int = 0,
    // Built from monthly docs: the chart shows months and "over" is counted per month. Set by the builder that read the docs,
    // not worked out from dayCount, since a range that starts on the setup day can be 31 days or fewer and still be monthly.
    val isLongRange: Boolean = false,
) {
    val avgPerDay: Long get() = if (dayCount > 0) spent / dayCount else 0L
    val avgPerTransaction: Long get() = if (transactionCount > 0) spent / transactionCount else 0L
    val previousAvgPerDay: Long get() = if (previousDayCount > 0) previousSpent / previousDayCount else 0L
    val overspentDays: Int get() = days.count { it.isOver }
    val overspentMonths: Int get() = months.count { it.isOver }
    val biggestDay: InsightDay? get() = days.filter { it.spent > 0 }.maxByOrNull { it.spent }

    companion object {
        const val MAX_DAILY_RANGE = 31
    }
}

data class InsightDay(
    val date: LocalDate,
    // The day's daily_history doc id, for loading its transactions. Empty when the day has no doc.
    val dailyDocId: String = "",
    val spent: Long = 0L,
    val allowance: Long = 0L,
    val transactionCount: Long = 0L,
) {
    // Same rule as the Add tab: spending with no allowance also counts as over
    val isOver: Boolean get() = spent > 0 && spent > allowance
}

data class InsightWeekday(
    val dayOfWeek: DayOfWeek,
    val dayCount: Int = 0,
    val spent: Long = 0L,
    // Only known on short ranges (needs each day)
    val overspentDays: Int = 0,
) {
    val avgPerDay: Long get() = if (dayCount > 0) spent / dayCount else 0L
}

data class InsightMonth(
    val month: YearMonth,
    // First and last day of this month inside the range (the current month ends today)
    val firstDay: LocalDate,
    val lastDay: LocalDate,
    val spent: Long = 0L,
    val allowance: Long = 0L,
    val transactionCount: Long = 0L,
) {
    val dayCount: Int get() = ChronoUnit.DAYS.between(firstDay, lastDay).toInt() + 1
    val avgPerDay: Long get() = if (dayCount > 0) spent / dayCount else 0L
    val isOver: Boolean get() = spent > allowance
    val isPartial: Boolean get() = dayCount < month.lengthOfMonth()
}

/**
 * Up to 31 days: adds up the daily docs of [start]..[end] for one [category].
 * [previousDocs] are the docs of [previousStart]..the day before [start].
 */
fun buildBudgetInsights(
    category: String,
    start: LocalDate,
    end: LocalDate,
    docs: List<DailyHistoryModel>,
    previousDocs: List<DailyHistoryModel>,
    previousStart: LocalDate = start.minusDays(ChronoUnit.DAYS.between(start, end) + 1),
): BudgetInsightsModel {
    val docsByDate = docs.associateBy { it.date.toLocalDate() }

    val days = daysBetween(start, end).map { date ->
        val doc = docsByDate[date]
        InsightDay(
            date = date,
            dailyDocId = doc?.docId.orEmpty(),
            spent = doc?.expenseByCategory?.get(category) ?: 0L,
            allowance = doc?.allowanceByCategory?.get(category) ?: 0L,
            transactionCount = doc?.countByCategory?.get(category) ?: 0L,
        )
    }

    val weekdays = DayOfWeek.entries.map { dayOfWeek ->
        val sameWeekday = days.filter { it.date.dayOfWeek == dayOfWeek }
        InsightWeekday(
            dayOfWeek = dayOfWeek,
            dayCount = sameWeekday.size,
            spent = sameWeekday.sumOf { it.spent },
            overspentDays = sameWeekday.count { it.isOver },
        )
    }

    val months = days.groupBy { YearMonth.from(it.date) }.map { (month, monthDays) ->
        InsightMonth(
            month = month,
            firstDay = monthDays.first().date,
            lastDay = monthDays.last().date,
            spent = monthDays.sumOf { it.spent },
            allowance = monthDays.sumOf { it.allowance },
            transactionCount = monthDays.sumOf { it.transactionCount },
        )
    }

    return BudgetInsightsModel(
        category = category,
        startDate = start,
        endDate = end,
        dayCount = days.size,
        spent = days.sumOf { it.spent },
        allowance = days.sumOf { it.allowance },
        transactionCount = days.sumOf { it.transactionCount },
        allCategoriesSpent = docs.sumOf { doc -> doc.expenseByCategory.values.sum() },
        days = days,
        weekdays = weekdays,
        months = months,
        previousStartDate = previousStart,
        previousSpent = previousDocs.sumOf { it.expenseByCategory[category] ?: 0L },
        previousDayCount = ChronoUnit.DAYS.between(previousStart, start).toInt().coerceAtLeast(0),
    )
}

/**
 * Over 31 days: adds up the monthly docs of [start]'s month..[end]'s month. [start] is the 1st of a month (a monthly doc holds
 * the whole month), or the day tracking started when that's later, so the per-day averages only count tracked days.
 * [previousDocs] are the months from [previousStart] to the one before [start].
 */
fun buildBudgetInsightsFromMonths(
    category: String,
    start: LocalDate,
    end: LocalDate,
    docs: List<MonthlyHistoryModel>,
    previousDocs: List<MonthlyHistoryModel>,
    previousStart: LocalDate,
    previousDayCount: Int,
): BudgetInsightsModel {
    val docsByMonth = docs.associateBy { YearMonth.from(it.month.toLocalDate()) }

    val months = generateSequence(YearMonth.from(start)) { it.plusMonths(1) }
        .takeWhile { !it.isAfter(YearMonth.from(end)) }
        .map { month ->
            val doc = docsByMonth[month]
            InsightMonth(
                month = month,
                firstDay = maxOf(month.atDay(1), start),
                lastDay = minOf(month.atEndOfMonth(), end),
                spent = doc?.expenseByCategory?.get(category) ?: 0L,
                allowance = doc?.allowanceByCategory?.get(category) ?: 0L,
                transactionCount = doc?.countByCategory?.get(category) ?: 0L,
            )
        }.toList()

    val allDays = daysBetween(start, end)
    val weekdays = DayOfWeek.entries.map { dayOfWeek ->
        InsightWeekday(
            dayOfWeek = dayOfWeek,
            dayCount = allDays.count { it.dayOfWeek == dayOfWeek },
            spent = docs.sumOf { it.weekdayExpenseByCategory[category]?.get(dayOfWeek.name.take(3)) ?: 0L },
        )
    }

    return BudgetInsightsModel(
        category = category,
        startDate = start,
        endDate = end,
        dayCount = allDays.size,
        spent = months.sumOf { it.spent },
        allowance = months.sumOf { it.allowance },
        transactionCount = months.sumOf { it.transactionCount },
        allCategoriesSpent = docs.sumOf { doc -> doc.expenseByCategory.values.sum() },
        weekdays = weekdays,
        months = months,
        previousStartDate = previousStart,
        previousSpent = previousDocs.sumOf { it.expenseByCategory[category] ?: 0L },
        previousDayCount = previousDayCount,
        isLongRange = true,
    )
}

// Daily docs store the time of the day's latest add, monthly docs the start of the 1st, so the local date is the day/month they belong to
fun Timestamp.toLocalDate(): LocalDate =
    toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()

private fun daysBetween(start: LocalDate, end: LocalDate): List<LocalDate> =
    generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(end) }.toList()
