package com.gmail.vondenuelle.denuspend.ui.budget

import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetChartFilter

sealed class BudgetScreenEvents {
    data object OnGetBudgetSummary : BudgetScreenEvents()
    data class OnChangeFilter(val filter : BudgetChartFilter) : BudgetScreenEvents()
    data class OnShowRangePicker(val value : Boolean) : BudgetScreenEvents()
    data class OnCustomRangeSelected(val startMillis : Long, val endMillis : Long) : BudgetScreenEvents()
    data class OnShowDatePicker(val value : Boolean) : BudgetScreenEvents()
    data class OnNavigateToBudgetTransactionScreen(val category : String) : BudgetScreenEvents()
}
