package com.gmail.vondenuelle.denuspend.ui.add

sealed class AddScreenEvents {
    //Income Screen
    data class OnIncomeTitleChanged(val value : String) : AddScreenEvents()
    data class OnIncomeDescriptionChanged(val value : String) : AddScreenEvents()
    data class OnIncomeAmountChanged(val value : Long) : AddScreenEvents()

    object OnNavigateToIncomeScreen : AddScreenEvents()
    object OnNavigateToExpenseScreen : AddScreenEvents()
}