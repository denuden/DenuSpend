package com.gmail.vondenuelle.denuspend.data.repositories

import com.gmail.vondenuelle.denuspend.data.remote.models.budget.request.GetBudgetSummaryRequest
import com.gmail.vondenuelle.denuspend.data.remote.services.budget.BudgetService
import com.gmail.vondenuelle.denuspend.di.qualifiers.FirebaseBudget
import com.gmail.vondenuelle.denuspend.di.qualifiers.IoDispatcher
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetTotalSummaryModel
import com.gmail.vondenuelle.denuspend.ui.budget.components.BudgetChartFilter.LAST_15_DAYS
import com.gmail.vondenuelle.denuspend.ui.budget.components.BudgetChartFilter.LAST_30_DAYS
import com.gmail.vondenuelle.denuspend.ui.budget.components.BudgetChartFilter.LAST_3_MONTHS
import com.gmail.vondenuelle.denuspend.ui.budget.components.BudgetChartFilter.LAST_6_MONTHS
import com.gmail.vondenuelle.denuspend.ui.budget.components.BudgetChartFilter.LAST_7_DAYS
import com.gmail.vondenuelle.denuspend.ui.budget.components.BudgetChartFilter.THIS_MONTH
import com.gmail.vondenuelle.denuspend.ui.budget.components.BudgetChartFilter.THIS_YEAR
import com.google.firebase.Timestamp
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject

@ViewModelScoped
class BudgetRepository @Inject constructor(
    @FirebaseBudget private val budgetService: BudgetService,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {
    suspend fun getBudgetSummary(
        date: String
    ) : Flow<BudgetTotalSummaryModel> {

        val now = LocalDateTime.now()
        val todayStart = now.toLocalDate().atStartOfDay()

        val startDateTime: LocalDateTime
        val endDateTime: LocalDateTime = now

        when (date) {
           THIS_MONTH -> {
               startDateTime = now.withDayOfMonth(1).toLocalDate().atStartOfDay()
           }

           LAST_7_DAYS -> {
               startDateTime = todayStart.minusDays(6)
           }

           LAST_15_DAYS -> {
               startDateTime = todayStart.minusDays(14)
           }

           LAST_30_DAYS -> {
               startDateTime = todayStart.minusDays(29)
           }
           LAST_3_MONTHS -> {
               startDateTime = todayStart.minusMonths(3)
           }
           LAST_6_MONTHS -> {
               startDateTime = todayStart.minusMonths(6)
           }
           THIS_YEAR -> {
               startDateTime = now.withDayOfYear(1).toLocalDate().atStartOfDay()
           }
           else -> {
               startDateTime = todayStart
           }
        }

        val request = GetBudgetSummaryRequest(
            startDate = toTimestamp(startDateTime),
            endDate = toTimestamp(endDateTime)
        )
        return budgetService.getBudgetSummary(request)
            .flowOn(dispatcher)
    }

    fun toTimestamp(localDateTime: LocalDateTime): Timestamp {
        val instant = localDateTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
        return Timestamp(Date.from(instant))
    }
}
