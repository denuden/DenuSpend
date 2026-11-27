package com.gmail.vondenuelle.denuspend.domain.models.budget

import androidx.annotation.Keep
import com.gmail.vondenuelle.denuspend.domain.models.transaction.DailyHistoryModel
import java.time.LocalDate
import java.time.YearMonth

// Keyed by category name (same names as CategoryDropdownButtonModel.list). Amounts are positive cents.
// totals/counts are expenses, allowances are the positive (allowance) transactions.
// Built on the device from daily docs (short ranges) or monthly docs (Last 3/6 Months, This Year).
@Keep
data class BudgetTotalSummaryModel(
    val totals : Map<String, Long> = emptyMap(),
    val counts : Map<String, Int> = emptyMap(),
    val totalExpense : Long = 0L,
    val allowances : Map<String, Long> = emptyMap(),
    val totalAllowance : Long = 0L,
    val dayCount : Int = 0,
    // Per category: days it went over its allowance, and days it had spending with no allowance (DayMark)
    val overDays : Map<String, Int> = emptyMap(),
    val unplannedDays : Map<String, Int> = emptyMap(),
    // Days where at least one category went over or was unplanned
    val badDays : Int = 0,
    // false = monthly docs without day marks (months before 1.7.0 that weren't backfilled), so the day counts above are unknown
    val hasDayCounts : Boolean = true,
    // Spent per wallet id (expenses only)
    val expenseByWallet : Map<String, Long> = emptyMap(),
    // The range's first and last day (set by the repository)
    val startDate : LocalDate? = null,
    val endDate : LocalDate? = null,
)

fun summaryFromDays(days: List<DailyHistoryModel>, dayCount: Int): BudgetTotalSummaryModel {
    val totals = sumMaps(days.map { it.expenseByCategory })
    val allowances = sumMaps(days.map { it.allowanceByCategory })
    // Each day's marks come straight from its own category maps
    val marks = days.map { day ->
        (day.expenseByCategory.keys + day.allowanceByCategory.keys).associateWith { category ->
            DayMark.of(day.expenseByCategory[category] ?: 0L, day.allowanceByCategory[category] ?: 0L)
        }
    }
    return BudgetTotalSummaryModel(
        totals = totals,
        counts = sumMaps(days.map { it.countByCategory }).mapValues { it.value.toInt() },
        totalExpense = totals.values.sum(),
        allowances = allowances,
        totalAllowance = allowances.values.sum(),
        dayCount = dayCount,
        overDays = countMarks(marks, DayMark.OVER),
        unplannedDays = countMarks(marks, DayMark.UNPLANNED),
        badDays = marks.count { day -> day.values.any { it != DayMark.OK } },
        expenseByWallet = sumMaps(days.map { it.expenseByWallet }),
    )
}

// Months are whole, so every mark in each doc is inside the range (the current month has none after today)
fun summaryFromMonths(months: List<MonthlyHistoryModel>, dayCount: Int): BudgetTotalSummaryModel {
    val totals = sumMaps(months.map { it.expenseByCategory })
    val allowances = sumMaps(months.map { it.allowanceByCategory })
    // Turn category -> day -> mark into one map per day (keyed by month + day), like summaryFromDays
    val marks = months.flatMap { month ->
        val ym = YearMonth.from(month.month.toLocalDate())
        month.dayStatusByCategory.flatMap { (category, byDay) -> byDay.map { (day, mark) -> Triple("$ym-$day", category, mark) } }
    }.groupBy({ it.first }, { it.second to it.third }).values.map { it.toMap() }
    return BudgetTotalSummaryModel(
        totals = totals,
        counts = sumMaps(months.map { it.countByCategory }).mapValues { it.value.toInt() },
        totalExpense = totals.values.sum(),
        allowances = allowances,
        totalAllowance = allowances.values.sum(),
        dayCount = dayCount,
        overDays = countMarks(marks, DayMark.OVER),
        unplannedDays = countMarks(marks, DayMark.UNPLANNED),
        badDays = marks.count { day -> day.values.any { it != DayMark.OK } },
        // Every month with spending needs marks, or one 1.7.0 save would make unmarked older months look like "no bad days"
        hasDayCounts = months.all { it.dayStatusByCategory.isNotEmpty() || it.expenseByCategory.isEmpty() },
        expenseByWallet = sumMaps(months.map { it.expenseByWallet }),
    )
}

// How many days each category had [mark]
private fun countMarks(days: List<Map<String, String>>, mark: String): Map<String, Int> =
    days.flatMap { day -> day.filterValues { it == mark }.keys }.groupingBy { it }.eachCount()

fun sumMaps(maps: List<Map<String, Long>>): Map<String, Long> =
    maps.flatMap { it.entries }
        .groupBy({ it.key }, { it.value })
        .mapValues { (_, values) -> values.sum() }
