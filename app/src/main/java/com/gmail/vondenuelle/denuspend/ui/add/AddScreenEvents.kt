package com.gmail.vondenuelle.denuspend.ui.add

import com.gmail.vondenuelle.denuspend.domain.models.transaction.AllowanceTemplateModel
import com.gmail.vondenuelle.denuspend.data.remote.models.transaction.request.TransactionRequest
import com.gmail.vondenuelle.denuspend.data.remote.models.transaction.request.TransactionsForDayRequest
import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionModel

sealed class AddScreenEvents {
    //Income Screen
    data class OnTitleChanged(val value : String) : AddScreenEvents()
    data class OnDescriptionChanged(val value : String) : AddScreenEvents()
    data class OnAmountChanged(val value : Long) : AddScreenEvents()
    data class OnCategoryChanged(val value : String) : AddScreenEvents()
    data class OnWalletChanged(val walletId : String) : AddScreenEvents() // "" = no wallet
    object OnGetWallets : AddScreenEvents()

    data class OnAddTransaction(val request : TransactionRequest) : AddScreenEvents()
    object OnGetTransactionOverview: AddScreenEvents()
    data object OnRefreshOverview: AddScreenEvents()
    object OnGetDailyTransactionHistory: AddScreenEvents()
    object OnLoadMoreHistory: AddScreenEvents()
    data class OnGetTransactionsForDay(val request : TransactionsForDayRequest): AddScreenEvents()

    //Edit / Delete
    data class OnLoadTransactionForEdit(val docId : String, val dailyDocId : String) : AddScreenEvents()
    object OnUpdateTransaction : AddScreenEvents()
    data class OnDeleteTransaction(val transaction : TransactionModel, val dailyDocId : String) : AddScreenEvents()
    data class OnNavigateToEditTransaction(val transaction : TransactionModel, val dailyDocId : String) : AddScreenEvents()



    //Allowance templates
    object OnGetAllowanceTemplates : AddScreenEvents()
    data class OnOpenTemplateEditor(val template : AllowanceTemplateModel?) : AddScreenEvents() // null = new template
    object OnCloseTemplateEditor : AddScreenEvents()
    data class OnTemplateDraftChanged(val draft : AllowanceTemplateModel) : AddScreenEvents()
    object OnSaveTemplateDraft : AddScreenEvents()
    data class OnDeleteAllowanceTemplate(val template : AllowanceTemplateModel) : AddScreenEvents()
    data class OnApplyAllowanceTemplate(val template : AllowanceTemplateModel) : AddScreenEvents()
    object OnNavigateToAllowanceTemplates : AddScreenEvents()

    object OnNavigateToIncomeScreen : AddScreenEvents()
    object OnNavigateToRecentTransactions : AddScreenEvents()
    object OnNavigateToExpenseScreen : AddScreenEvents()
}