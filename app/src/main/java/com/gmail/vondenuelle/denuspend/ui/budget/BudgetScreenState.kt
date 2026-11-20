package com.gmail.vondenuelle.denuspend.ui.budget

import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetChartFilter
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetTotalSummaryModel

data class BudgetScreenState(
    val isLoading : Boolean = false,
    val showDateDialog : Boolean = false,
    val filter : BudgetChartFilter = BudgetChartFilter.THIS_MONTH,
    val showRangePicker : Boolean = false,
    // Only set once a custom range is picked. UTC-midnight millis, as the date range picker gives them.
    val customStartMillis : Long? = null,
    val customEndMillis : Long? = null,
    val budgetTotalSummaryModel: BudgetTotalSummaryModel = BudgetTotalSummaryModel()
)
