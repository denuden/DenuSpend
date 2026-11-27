package com.gmail.vondenuelle.denuspend.data.repositories

import com.gmail.vondenuelle.denuspend.data.remote.models.budget.request.GetBudgetSummaryRequest
import com.gmail.vondenuelle.denuspend.data.remote.services.budget.BudgetService
import com.gmail.vondenuelle.denuspend.data.remote.services.wallet.WalletService
import com.gmail.vondenuelle.denuspend.di.qualifiers.FirebaseBudget
import com.gmail.vondenuelle.denuspend.di.qualifiers.FirebaseWallet
import com.gmail.vondenuelle.denuspend.di.qualifiers.IoDispatcher
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetChartFilter
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetChartFilter.CUSTOM
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetChartFilter.LAST_15_DAYS
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetChartFilter.LAST_30_DAYS
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetChartFilter.LAST_3_DAYS
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetChartFilter.LAST_3_MONTHS
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetChartFilter.LAST_6_MONTHS
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetChartFilter.LAST_7_DAYS
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetChartFilter.THIS_MONTH
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetChartFilter.THIS_YEAR
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetInsightsModel
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetMoneyModel
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetTotalSummaryModel
import com.gmail.vondenuelle.denuspend.domain.models.budget.MonthlyHistoryModel
import com.gmail.vondenuelle.denuspend.domain.models.budget.buildBudgetMoney
import com.gmail.vondenuelle.denuspend.domain.models.budget.summaryFromDays
import com.gmail.vondenuelle.denuspend.domain.models.budget.summaryFromMonths
import com.gmail.vondenuelle.denuspend.domain.models.budget.buildBudgetInsights
import com.gmail.vondenuelle.denuspend.domain.models.budget.buildBudgetInsightsFromMonths
import com.gmail.vondenuelle.denuspend.domain.models.budget.toLocalDate
import com.google.firebase.Timestamp
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date
import javax.inject.Inject

@ViewModelScoped
class BudgetRepository @Inject constructor(
    @FirebaseBudget private val budgetService: BudgetService,
    @FirebaseWallet private val walletService: WalletService,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {
    /**
     * The dashboard's totals, built on the device. Last 3/6 Months and This Year read the monthly docs (3–12 reads, whole months),
     * the rest the daily docs (1 read per day with entries). Insights reads the same docs for the same filter, so both screens match.
     * [customStartMillis]/[customEndMillis] are only used for CUSTOM. They are the date range picker's UTC-midnight millis.
     */
    fun getBudgetSummary(
        filter: BudgetChartFilter,
        customStartMillis: Long? = null,
        customEndMillis: Long? = null,
    ) : Flow<BudgetTotalSummaryModel> = flow {
        val (firstDay, lastDay) = rangeFor(filter, customStartMillis, customEndMillis)
        val setupDay = walletService.getTrackingStart()?.toLocalDate()
        val request = requestFor(firstDay, lastDay)
        // Days before tracking started don't count toward "N of M days"
        fun countedDays(oldestDoc: LocalDate?, byMonth: Boolean): Int {
            val start = maxOf(firstDay, trackedFrom(setupDay, oldestDoc, lastDay, byMonth))
            return (ChronoUnit.DAYS.between(start, lastDay).toInt() + 1).coerceAtLeast(0)
        }
        val summary = if (filter.isWholeMonths) {
            budgetService.getMonthlyHistoryInRange(request).map { months ->
                summaryFromMonths(months, countedDays(months.minOfOrNull { it.month.toLocalDate() }, byMonth = true))
            }
        } else {
            budgetService.getDailyHistoryInRange(request).map { days ->
                summaryFromDays(days, countedDays(days.minOfOrNull { it.date.toLocalDate() }, byMonth = false))
            }
        }
        emitAll(summary.map { it.copy(startDate = firstDay, endDate = lastDay) })
    }.flowOn(dispatcher)

    // Real money in and out from the wallet history, plus "Bills & subscriptions", for the same range as the summary.
    // One movements listener plus the wallets and schedules listeners the Wallet tab already runs.
    fun getBudgetMoney(
        filter: BudgetChartFilter,
        customStartMillis: Long? = null,
        customEndMillis: Long? = null,
    ): Flow<BudgetMoneyModel> {
        val (firstDay, lastDay) = rangeFor(filter, customStartMillis, customEndMillis)
        val request = requestFor(firstDay, lastDay)
        val includesToday = !lastDay.isBefore(LocalDate.now())
        return combine(
            walletService.getMoneyMovements(request.startDate, request.endDate),
            walletService.getWallets(),
            walletService.getRecurringCharges(),
        ) { movements, wallets, schedules -> buildBudgetMoney(movements, wallets, schedules, includesToday) }
            .flowOn(dispatcher)
    }

    // Home's month card: this month's monthly_history doc (device time zone, same as the doc id the batch writes)
    fun getMonthlyHistory(month: YearMonth): Flow<MonthlyHistoryModel?> {
        return budgetService.getMonthlyHistory(month.format(DateTimeFormatter.ofPattern("yyyyMM")))
            .flowOn(dispatcher)
    }

    /**
     * Insights for one [category]. Up to 31 days, and every custom range, it reads the daily docs; Last 3/6 Months and This Year
     * read the monthly docs (whole months, like the dashboard). Either way the previous period
     * (same number of days or months) comes from the same query, so it's one listener.
     * Neither starts before tracking did, so empty days before the first entry don't lower the averages (see [trackedFrom]).
     */
    fun getBudgetInsights(
        category: String,
        filter: BudgetChartFilter,
        customStartMillis: Long? = null,
        customEndMillis: Long? = null,
    ): Flow<BudgetInsightsModel> = flow {
        val (firstDay, lastDay) = rangeFor(filter, customStartMillis, customEndMillis)
        val dayCount = ChronoUnit.DAYS.between(firstDay, lastDay).toInt() + 1
        val setupDay = walletService.getTrackingStart()?.toLocalDate()

        // The filter picks days or months once, here. Last 3/6 Months and This Year are whole months (This Year has one bar in January).
        // Custom always reads its exact days: a monthly doc holds whole months, which counted days outside a mid-month custom range.
        val byDay = filter == CUSTOM || (!filter.isWholeMonths && dayCount <= BudgetInsightsModel.MAX_DAILY_RANGE)
        val insights = if (byDay) {
            val previousStart = firstDay.minusDays(dayCount.toLong())
            budgetService.getDailyHistoryInRange(requestFor(previousStart, lastDay)).map { docs ->
                val tracked = trackedFrom(setupDay, docs.minOfOrNull { it.date.toLocalDate() }, lastDay, byMonth = false)
                val start = maxOf(firstDay, tracked)
                val (previous, current) = docs.partition { it.date.toLocalDate().isBefore(start) }
                buildBudgetInsights(category, start, lastDay, current, previous, previousStart = minOf(maxOf(previousStart, tracked), start))
            }
        } else {
            val monthStart = firstDay.withDayOfMonth(1)
            val monthCount = ChronoUnit.MONTHS.between(YearMonth.from(monthStart), YearMonth.from(lastDay)) + 1
            val previousStart = monthStart.minusMonths(monthCount)
            budgetService.getMonthlyHistoryInRange(requestFor(previousStart, lastDay)).map { docs ->
                val tracked = trackedFrom(setupDay, docs.minOfOrNull { it.month.toLocalDate() }, lastDay, byMonth = true)
                val start = maxOf(monthStart, tracked)
                // Months are whole, so the split is by month; the "before" days start where tracking did
                val (previous, current) = docs.partition { YearMonth.from(it.month.toLocalDate()).isBefore(YearMonth.from(start)) }
                val beforeStart = minOf(maxOf(previousStart, tracked), monthStart)
                val beforeDays = ChronoUnit.DAYS.between(beforeStart, monthStart).toInt()
                buildBudgetInsightsFromMonths(category, start, lastDay, current, previous, beforeStart, beforeDays)
            }
        }
        emitAll(insights)
    }.flowOn(dispatcher)

    /**
     * The first tracked day: the wallet setup day, unless a loaded doc is older (accounts that tracked before wallets existed),
     * then that doc's day. Monthly docs are dated the 1st, so they only count when they're from an earlier month.
     * An unknown setup day (offline) or one after the range ends (an old custom range) leaves the range as it is.
     */
    private fun trackedFrom(setupDay: LocalDate?, oldestDoc: LocalDate?, lastDay: LocalDate, byMonth: Boolean): LocalDate {
        if (setupDay == null || setupDay.isAfter(lastDay)) return LocalDate.MIN
        if (oldestDoc == null) return setupDay
        val docIsOlder = if (byMonth) YearMonth.from(oldestDoc).isBefore(YearMonth.from(setupDay)) else oldestDoc.isBefore(setupDay)
        return if (docIsOlder) oldestDoc else setupDay
    }

    /** Drill-in on a long range: the daily docs of one [month], up to [lastDay] (the range's end), for its daily bars. */
    fun getBudgetInsightsMonthDays(
        category: String,
        month: YearMonth,
        lastDay: LocalDate,
        // A custom range can start mid-month, so its first month opens on the range's first day
        firstDay: LocalDate = month.atDay(1),
    ): Flow<BudgetInsightsModel> {
        val first = maxOf(month.atDay(1), firstDay)
        val last = minOf(month.atEndOfMonth(), lastDay)
        return budgetService.getDailyHistoryInRange(requestFor(first, last))
            .map { docs -> buildBudgetInsights(category, first, last, docs, emptyList()) }
            .flowOn(dispatcher)
    }

    // The filter's first and last calendar day. Shared by the dashboard and Insights.
    private fun rangeFor(
        filter: BudgetChartFilter,
        customStartMillis: Long?,
        customEndMillis: Long?,
    ): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now()
        val customStart = customStartMillis?.let { utcMillisToLocalDate(it) } ?: today
        val customEnd = customEndMillis?.let { utcMillisToLocalDate(it) } ?: customStart

        val firstDay = when (filter) {
            THIS_MONTH -> today.withDayOfMonth(1)
            LAST_3_DAYS -> today.minusDays(2)
            LAST_7_DAYS -> today.minusDays(6)
            LAST_15_DAYS -> today.minusDays(14)
            LAST_30_DAYS -> today.minusDays(29)
            // This month and the ones before it, as whole months, so the dashboard and Insights cover the same days
            LAST_3_MONTHS -> today.minusMonths(2).withDayOfMonth(1)
            LAST_6_MONTHS -> today.minusMonths(5).withDayOfMonth(1)
            THIS_YEAR -> today.withDayOfYear(1)
            CUSTOM -> customStart
        }
        val lastDay = if (filter == CUSTOM) customEnd else today
        return firstDay to lastDay
    }

    // Start of the first day to the end of the last day, not "now". A fixed "now" leaves out transactions added while the listener is live.
    private fun requestFor(firstDay: LocalDate, lastDay: LocalDate): GetBudgetSummaryRequest {
        val endDateTime: LocalDateTime = lastDay.plusDays(1).atStartOfDay().minusNanos(1)
        return GetBudgetSummaryRequest(
            startDate = toTimestamp(firstDay.atStartOfDay()),
            endDate = toTimestamp(endDateTime)
        )
    }

    // The picker stores the picked day as midnight UTC, so read it back in UTC to get the same calendar day
    private fun utcMillisToLocalDate(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

    fun toTimestamp(localDateTime: LocalDateTime): Timestamp {
        val instant = localDateTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
        return Timestamp(Date.from(instant))
    }
}
