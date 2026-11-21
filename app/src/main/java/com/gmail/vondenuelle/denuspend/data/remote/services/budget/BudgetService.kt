package com.gmail.vondenuelle.denuspend.data.remote.services.budget

import com.gmail.vondenuelle.denuspend.data.remote.models.budget.request.GetBudgetSummaryRequest
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetTotalSummaryModel
import com.gmail.vondenuelle.denuspend.domain.models.budget.MonthlyHistoryModel
import com.gmail.vondenuelle.denuspend.domain.models.transaction.DailyHistoryModel
import kotlinx.coroutines.flow.Flow

interface BudgetService {
    suspend fun getBudgetSummary(request : GetBudgetSummaryRequest) : Flow<BudgetTotalSummaryModel>
    // Insights: the raw docs of a range, added up on the device (daily for up to 31 days, monthly for longer)
    fun getDailyHistoryInRange(request : GetBudgetSummaryRequest) : Flow<List<DailyHistoryModel>>
    fun getMonthlyHistoryInRange(request : GetBudgetSummaryRequest) : Flow<List<MonthlyHistoryModel>>
    // Home: one month's doc by id ([yearMonth] = "yyyyMM"). Emits null when nothing was added that month yet.
    fun getMonthlyHistory(yearMonth : String) : Flow<MonthlyHistoryModel?>
}