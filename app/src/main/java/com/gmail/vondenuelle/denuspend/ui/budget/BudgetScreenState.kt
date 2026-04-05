package com.gmail.vondenuelle.denuspend.ui.budget

data class BudgetScreenState(
    val isLoading : Boolean = false,
    val showDateDialog : Boolean = false,
    val name : String = ""
)