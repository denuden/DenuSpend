package com.gmail.vondenuelle.denuspend.ui.add

import com.gmail.vondenuelle.denuspend.data.remote.models.transaction.request.TransactionRequest

sealed class AddScreenEvents {
    //Income Screen
    data class OnTitleChanged(val value : String) : AddScreenEvents()
    data class OnDescriptionChanged(val value : String) : AddScreenEvents()
    data class OnAmountChanged(val value : Long) : AddScreenEvents()
    data class OnCategoryChanged(val value : String) : AddScreenEvents()

    data class OnAddTransaction(val request : TransactionRequest) : AddScreenEvents()
    object OnGetAllTransactions : AddScreenEvents()
    object OnGetSummaryOfDailyTransactions : AddScreenEvents()



    object OnNavigateToIncomeScreen : AddScreenEvents()
    object OnNavigateToRecentTransactions : AddScreenEvents()
    object OnNavigateToExpenseScreen : AddScreenEvents()
}