package com.gmail.vondenuelle.denuspend.data.repositories

import com.gmail.vondenuelle.denuspend.data.remote.models.budget.request.GetBudgetSummaryRequest
import com.gmail.vondenuelle.denuspend.data.remote.services.budget.BudgetService
import com.gmail.vondenuelle.denuspend.data.remote.services.wallet.WalletService
import com.gmail.vondenuelle.denuspend.di.qualifiers.FirebaseBudget
import com.gmail.vondenuelle.denuspend.di.qualifiers.FirebaseWallet
import com.gmail.vondenuelle.denuspend.di.qualifiers.IoDispatcher
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetBillsModel
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
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetTotalSummaryModel
import com.gmail.vondenuelle.denuspend.domain.models.budget.MonthlyHistoryModel
import com.gmail.vondenuelle.denuspend.domain.models.budget.buildBudgetBills
import com.gmail.vondenuelle.denuspend.domain.models.budget.buildBudgetInsights
import com.gmail.vondenuelle.denuspend.domain.models.budget.buildBudgetInsightsFromMonths
import com.gmail.vondenuelle.denuspend.domain.models.budget.toLocalDate
import com.google.firebase.Timestamp
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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
    /** [customStartMillis]/[customEndMillis] are only used for CUSTOM. They are the date range picker's UTC-midnight millis. */
    suspend fun getBudgetSummary(
        filter: BudgetChartFilter,
        customStartMillis: Long? = null,
        customEndMillis: Long? = null,
    ) : Flow<BudgetTotalSummaryModel> {
        val (firstDay, lastDay) = rangeFor(filter, customStartMillis, customEndMillis)
        return budgetService.getBudgetSummary(requestFor(firstDay, lastDay))
            .flowOn(dispatcher)
    }

    // "Bills & subscriptions": the range's card charges, named with the wallets and schedules (the same listeners the Wallet tab runs)
    fun getBudgetBills(
        filter: BudgetChartFilter,
        customStartMillis: Long? = null,
        customEndMillis: Long? = null,
    ): Flow<BudgetBillsModel> {
        val (firstDay, lastDay) = rangeFor(filter, customStartMillis, customEndMillis)
        val request = requestFor(firstDay, lastDay)
        val includesToday = !lastDay.isBefore(LocalDate.now())
        return combine(
            walletService.getBillCharges(request.startDate, request.endDate),
            walletService.getWallets(),
            walletService.getRecurringCharges(),
        ) { charges, wallets, schedules -> buildBudgetBills(charges, wallets, schedules, includesToday) }
            .flowOn(dispatcher)
    }

    // Home's month card: this month's monthly_history doc (device time zone, same as the doc id the batch writes)
    fun getMonthlyHistory(month: YearMonth): Flow<MonthlyHistoryModel?> {
        return budgetService.getMonthlyHistory(month.format(DateTimeFormatter.ofPattern("yyyyMM")))
            .flowOn(dispatcher)
    }

    /**
     * Insights for one [category]. Up to 31 days it reads the daily docs; longer ranges read the monthly docs and
     * start on the 1st of the first month (a monthly doc holds the whole month). Either way the previous period
     * (same number of days or months) comes from the same query, so it's one listener.
     */
    fun getBudgetInsights(
        category: String,
        filter: BudgetChartFilter,
        customStartMillis: Long? = null,
        customEndMillis: Long? = null,
    ): Flow<BudgetInsightsModel> {
        val (firstDay, lastDay) = rangeFor(filter, customStartMillis, customEndMillis)
        val dayCount = ChronoUnit.DAYS.between(firstDay, lastDay).toInt() + 1

        val insights = if (dayCount <= BudgetInsightsModel.MAX_DAILY_RANGE) {
            val previousStart = firstDay.minusDays(dayCount.toLong())
            budgetService.getDailyHistoryInRange(requestFor(previousStart, lastDay)).map { docs ->
                val (previous, current) = docs.partition { it.date.toLocalDate().isBefore(firstDay) }
                buildBudgetInsights(category, firstDay, lastDay, current, previous)
            }
        } else {
            val monthStart = firstDay.withDayOfMonth(1)
            val monthCount = ChronoUnit.MONTHS.between(YearMonth.from(monthStart), YearMonth.from(lastDay)) + 1
            val previousStart = monthStart.minusMonths(monthCount)
            val previousDayCount = ChronoUnit.DAYS.between(previousStart, monthStart).toInt()
            budgetService.getMonthlyHistoryInRange(requestFor(previousStart, lastDay)).map { docs ->
                val (previous, current) = docs.partition { it.month.toLocalDate().isBefore(monthStart) }
                buildBudgetInsightsFromMonths(category, monthStart, lastDay, current, previous, previousDayCount)
            }
        }
        return insights.flowOn(dispatcher)
    }

    /** Drill-in on a long range: the daily docs of one [month], up to [lastDay] (the range's end), for its daily bars. */
    fun getBudgetInsightsMonthDays(
        category: String,
        month: YearMonth,
        lastDay: LocalDate,
    ): Flow<BudgetInsightsModel> {
        val first = month.atDay(1)
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
            LAST_3_MONTHS -> today.minusMonths(3)
            LAST_6_MONTHS -> today.minusMonths(6)
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
