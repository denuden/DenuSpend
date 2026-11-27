package com.gmail.vondenuelle.denuspend.domain.models.budget

import com.gmail.vondenuelle.denuspend.domain.models.transaction.DailyHistoryModel
import com.google.firebase.Timestamp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

// Everything the Insights screen shows for one category and range. Amounts are positive cents, like the daily docs.
// Built on the device from daily docs (up to 31 days, and every custom range) or monthly docs (whole-month filters), never stored.
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
    // One entry per calendar day (days with no doc are ₱0). Empty on monthly ranges, which only have monthly totals.
    val days: List<InsightDay> = emptyList(),
    // Monday to Sunday
    val weekdays: List<InsightWeekday> = emptyList(),
    val months: List<InsightMonth> = emptyList(),
    // The period right before startDate (same number of days, or of months on long ranges), for "Compared to before".
    // It never starts before tracking did, so days before the first entry don't pull the average down (0 days = nothing to compare).
    val previousStartDate: LocalDate = LocalDate.now(),
    val previousSpent: Long = 0L,
    val previousDayCount: Int = 0,
    // Long ranges chart months and list "Month by month". Set by the builder: a custom range over 31 days reads days but still charts months.
    val isLongRange: Boolean = false,
    // Days it went over its allowance / had spending with no allowance. From the days themselves, or from the monthly docs' day marks.
    val overDays: Int = 0,
    val unplannedDays: Int = 0,
    // false = monthly docs without day marks (months before 1.7.0 that weren't backfilled), so only whole months can be judged
    val hasDayCounts: Boolean = true,
) {
    val avgPerDay: Long get() = roundDiv(spent, dayCount.toLong())
    val avgPerTransaction: Long get() = roundDiv(spent, transactionCount)
    val previousAvgPerDay: Long get() = roundDiv(previousSpent, previousDayCount.toLong())
    val badDays: Int get() = overDays + unplannedDays
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
    // Same rule as the day marks: over a set allowance, or spending with no allowance at all (unplanned)
    val mark: String get() = DayMark.of(spent, allowance)
    val isOver: Boolean get() = mark == DayMark.OVER
    val isUnplanned: Boolean get() = mark == DayMark.UNPLANNED
}

data class InsightWeekday(
    val dayOfWeek: DayOfWeek,
    val dayCount: Int = 0,
    val spent: Long = 0L,
    // Over or unplanned days on this weekday (0 when the range has no day counts)
    val badDays: Int = 0,
) {
    val avgPerDay: Long get() = roundDiv(spent, dayCount.toLong())
}

data class InsightMonth(
    val month: YearMonth,
    // First and last day of this month inside the range (the current month ends today)
    val firstDay: LocalDate,
    val lastDay: LocalDate,
    val spent: Long = 0L,
    val allowance: Long = 0L,
    val transactionCount: Long = 0L,
    val overDays: Int = 0,
    val unplannedDays: Int = 0,
) {
    val dayCount: Int get() = ChronoUnit.DAYS.between(firstDay, lastDay).toInt() + 1
    val avgPerDay: Long get() = roundDiv(spent, dayCount.toLong())
    val isOver: Boolean get() = spent > allowance
    val isPartial: Boolean get() = dayCount < month.lengthOfMonth()
    val badDays: Int get() = overDays + unplannedDays
}

/**
 * Adds up the daily docs of [start]..[end] for one [category]. Over 31 days (custom ranges only) it still charts months.
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
            badDays = sameWeekday.count { it.isOver || it.isUnplanned },
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
            overDays = monthDays.count { it.isOver },
            unplannedDays = monthDays.count { it.isUnplanned },
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
        isLongRange = days.size > BudgetInsightsModel.MAX_DAILY_RANGE,
        overDays = days.count { it.isOver },
        unplannedDays = days.count { it.isUnplanned },
    )
}

/**
 * Whole-month filters: adds up the monthly docs of [start]'s month..[end]'s month. [start] is the 1st of a month, or the day
 * tracking started when that's later, so the per-day averages only count tracked days.
 * Bad days come from each doc's day marks (category -> "dd" -> mark). [previousDocs] are the months from [previousStart] to the one before [start].
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

    // This category's marked days inside the range, as dates
    val marks: Map<LocalDate, String> = docs.flatMap { doc ->
        val ym = YearMonth.from(doc.month.toLocalDate())
        doc.dayStatusByCategory[category].orEmpty().mapNotNull { (day, mark) ->
            val date = day.toIntOrNull()?.takeIf { it in 1..ym.lengthOfMonth() }?.let { ym.atDay(it) }
            date?.takeIf { !it.isBefore(start) && !it.isAfter(end) }?.let { it to mark }
        }
    }.toMap()
    // Marks exist once a month was backfilled (or saved on 1.7.0). Every month with spending needs them, or one new save would make
    // the older, unmarked months count as "no bad days". Without them the range can only judge whole months.
    val hasMarks = docs.all { it.dayStatusByCategory.isNotEmpty() || it.expenseByCategory.isEmpty() }

    val months = generateSequence(YearMonth.from(start)) { it.plusMonths(1) }
        .takeWhile { !it.isAfter(YearMonth.from(end)) }
        .map { month ->
            val doc = docsByMonth[month]
            val monthMarks = marks.filterKeys { YearMonth.from(it) == month }.values
            InsightMonth(
                month = month,
                firstDay = maxOf(month.atDay(1), start),
                lastDay = minOf(month.atEndOfMonth(), end),
                spent = doc?.expenseByCategory?.get(category) ?: 0L,
                allowance = doc?.allowanceByCategory?.get(category) ?: 0L,
                transactionCount = doc?.countByCategory?.get(category) ?: 0L,
                overDays = monthMarks.count { it == DayMark.OVER },
                unplannedDays = monthMarks.count { it == DayMark.UNPLANNED },
            )
        }.toList()

    val allDays = daysBetween(start, end)
    val weekdays = DayOfWeek.entries.map { dayOfWeek ->
        InsightWeekday(
            dayOfWeek = dayOfWeek,
            dayCount = allDays.count { it.dayOfWeek == dayOfWeek },
            spent = docs.sumOf { it.weekdayExpenseByCategory[category]?.get(dayOfWeek.name.take(3)) ?: 0L },
            badDays = marks.count { (date, mark) -> date.dayOfWeek == dayOfWeek && mark != DayMark.OK },
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
        overDays = marks.values.count { it == DayMark.OVER },
        unplannedDays = marks.values.count { it == DayMark.UNPLANNED },
        hasDayCounts = hasMarks,
    )
}

// Rounded to the nearest cent (or day/percent), instead of cutting off: ₱228.888 → ₱228.89. Positive values only.
fun roundDiv(value: Long, by: Long): Long = if (by > 0) (value + by / 2) / by else 0L

// Daily docs store the time of the day's latest add, monthly docs the start of the 1st, so the local date is the day/month they belong to
fun Timestamp.toLocalDate(): LocalDate =
    toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()

private fun daysBetween(start: LocalDate, end: LocalDate): List<LocalDate> =
    generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(end) }.toList()
