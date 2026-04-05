package com.gmail.vondenuelle.denuspend.ui.budget

sealed class BudgetScreenEvents {
    data class OnShowDatePicker(val value : Boolean) : BudgetScreenEvents()
    data class OnNavigateToBudgetTransactionScreen(val category : String) : BudgetScreenEvents()
}