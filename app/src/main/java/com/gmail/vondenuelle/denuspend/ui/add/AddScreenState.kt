package com.gmail.vondenuelle.denuspend.ui.add

import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionModel
import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionSummaryModel

data class AddScreenState(
    val isLoading : Boolean = false,
    val isListLoading : Boolean = false,
    val isSummaryLoading : Boolean = false,

//    Income Screen
    val transactionTitle : String = "",
    val transactionDescription : String = "",
    val transactionCategory : String = "",
    val transactionAmount : Long = 0L,

    val transactionList : List<TransactionModel> = emptyList(),
    val transactionSummary : TransactionSummaryModel = TransactionSummaryModel()
    )
