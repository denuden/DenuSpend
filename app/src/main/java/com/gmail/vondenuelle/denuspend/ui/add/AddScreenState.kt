package com.gmail.vondenuelle.denuspend.ui.add

import com.gmail.vondenuelle.denuspend.domain.models.transaction.AllowanceTemplateModel
import com.gmail.vondenuelle.denuspend.domain.models.transaction.DailyHistoryModel
import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionModel

data class AddScreenState(
    val isLoading : Boolean = false,
    val isListLoading : Boolean = false,
    val isSummaryLoading : Boolean = false,
    val isSheetLoading : Boolean = false,
    val isRefreshing : Boolean = false, //pull to refresh on the Add tab
    val isLoadingMoreHistory : Boolean = false,
    val hasMoreHistory : Boolean = false,

//    Income Screen
    val transactionTitle : String = "",
    val transactionDescription : String = "",
    val transactionCategory : String = "",
    val transactionAmount : Long = 0L,

//    Edit mode: the original transaction (null = adding a new one)
    val editingTransaction : TransactionModel? = null,
    val editingDailyDocId : String = "",

    val transactionList : List<TransactionModel> = emptyList(),
    val dailyHistory : DailyHistoryModel = DailyHistoryModel(),
    val dailyHistoryList : List<DailyHistoryModel> = emptyList(),
    val perDayTransactionList : List<TransactionModel> = emptyList(),

//    Allowance templates
    val allowanceTemplates : List<AllowanceTemplateModel> = emptyList(),
    // Saving replaces the whole list, so saves wait for the first load instead of overwriting templates not loaded yet
    val hasLoadedTemplates : Boolean = false,
    // The template open in the editor sheet (null = closed, empty id = a new one)
    val templateDraft : AllowanceTemplateModel? = null,
    )
