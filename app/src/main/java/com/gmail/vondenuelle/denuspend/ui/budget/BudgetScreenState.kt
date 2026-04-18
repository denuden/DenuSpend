package com.gmail.vondenuelle.denuspend.ui.budget

import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetTotalSummaryModel
import com.gmail.vondenuelle.denuspend.ui.budget.components.BudgetChartFilter

data class BudgetScreenState(
    val isLoading : Boolean = false,
    val showDateDialog : Boolean = false,
    val name : String = "",
    val date : String = BudgetChartFilter.THIS_MONTH,
    val budgetTotalSummaryModel: BudgetTotalSummaryModel = BudgetTotalSummaryModel()
)