package com.gmail.vondenuelle.denuspend.data.remote.services.budget

import com.gmail.vondenuelle.denuspend.data.remote.models.budget.request.GetBudgetSummaryRequest
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetTotalSummaryModel
import kotlinx.coroutines.flow.Flow

interface BudgetService {
    suspend fun getBudgetSummary(request : GetBudgetSummaryRequest) : Flow<BudgetTotalSummaryModel>
}