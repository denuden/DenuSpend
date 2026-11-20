package com.gmail.vondenuelle.denuspend.data.repositories

import com.gmail.vondenuelle.denuspend.data.remote.models.budget.request.GetBudgetSummaryRequest
import com.gmail.vondenuelle.denuspend.data.remote.services.budget.BudgetService
import com.gmail.vondenuelle.denuspend.di.qualifiers.FirebaseBudget
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
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetTotalSummaryModel
import com.google.firebase.Timestamp
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Date
import javax.inject.Inject

@ViewModelScoped
class BudgetRepository @Inject constructor(
    @FirebaseBudget private val budgetService: BudgetService,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {
    /** [customStartMillis]/[customEndMillis] are only used for CUSTOM. They are the date range picker's UTC-midnight millis. */
    suspend fun getBudgetSummary(
        filter: BudgetChartFilter,
        customStartMillis: Long? = null,
        customEndMillis: Long? = null,
    ) : Flow<BudgetTotalSummaryModel> {

        val todayStart = LocalDate.now().atStartOfDay()
        val customStart = customStartMillis?.let { utcMillisToLocalDate(it) } ?: LocalDate.now()
        val customEnd = customEndMillis?.let { utcMillisToLocalDate(it) } ?: customStart

        val startDateTime: LocalDateTime = when (filter) {
            THIS_MONTH -> todayStart.withDayOfMonth(1)
            LAST_3_DAYS -> todayStart.minusDays(2)
            LAST_7_DAYS -> todayStart.minusDays(6)
            LAST_15_DAYS -> todayStart.minusDays(14)
            LAST_30_DAYS -> todayStart.minusDays(29)
            LAST_3_MONTHS -> todayStart.minusMonths(3)
            LAST_6_MONTHS -> todayStart.minusMonths(6)
            THIS_YEAR -> todayStart.withDayOfYear(1)
            CUSTOM -> customStart.atStartOfDay()
        }

        // End of the last day, not "now". A fixed "now" leaves out transactions added while the listener is live.
        val lastDay = if (filter == CUSTOM) customEnd else LocalDate.now()
        val endDateTime: LocalDateTime = lastDay.plusDays(1).atStartOfDay().minusNanos(1)

        val request = GetBudgetSummaryRequest(
            startDate = toTimestamp(startDateTime),
            endDate = toTimestamp(endDateTime)
        )
        return budgetService.getBudgetSummary(request)
            .flowOn(dispatcher)
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
