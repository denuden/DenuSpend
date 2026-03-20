package com.gmail.vondenuelle.denuspend.ui.add

import com.gmail.vondenuelle.denuspend.data.remote.models.transaction.request.TransactionRequest
import com.gmail.vondenuelle.denuspend.data.remote.models.transaction.request.TransactionsForDayRequest

sealed class AddScreenEvents {
    //Income Screen
    data class OnTitleChanged(val value : String) : AddScreenEvents()
    data class OnDescriptionChanged(val value : String) : AddScreenEvents()
    data class OnAmountChanged(val value : Long) : AddScreenEvents()
    data class OnCategoryChanged(val value : String) : AddScreenEvents()

    data class OnAddTransaction(val request : TransactionRequest) : AddScreenEvents()
    object OnGetTransactionOverview: AddScreenEvents()
    object OnGetDailyTransactionHistory: AddScreenEvents()
    data class OnGetTransactionsForDay(val request : TransactionsForDayRequest): AddScreenEvents()



    object OnNavigateToIncomeScreen : AddScreenEvents()
    object OnNavigateToRecentTransactions : AddScreenEvents()
    object OnNavigateToExpenseScreen : AddScreenEvents()
}