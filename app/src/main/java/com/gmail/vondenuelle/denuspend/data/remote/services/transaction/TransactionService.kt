package com.gmail.vondenuelle.denuspend.data.remote.services.transaction

import com.gmail.vondenuelle.denuspend.data.remote.models.transaction.request.EditTransactionRequest
import com.gmail.vondenuelle.denuspend.data.remote.models.transaction.request.TransactionRequest
import com.gmail.vondenuelle.denuspend.data.remote.models.transaction.request.TransactionsForDayRequest
import com.gmail.vondenuelle.denuspend.domain.models.transaction.AllowanceTemplateModel
import com.gmail.vondenuelle.denuspend.domain.models.transaction.DailyHistoryModel
import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionModel
import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionOverviewModel
import kotlinx.coroutines.flow.Flow

interface TransactionService {
    suspend fun addTransaction(transactionRequest: TransactionRequest): TransactionModel
    suspend fun addTransactions(requests: List<TransactionRequest>): List<TransactionModel>
    suspend fun getTransactionById(docId: String): TransactionModel
    suspend fun updateTransaction(request: EditTransactionRequest)
    suspend fun deleteTransaction(transaction: TransactionModel, dailyDocId: String)
    fun getDailyTransactionHistory(limit: Long?): Flow<List<DailyHistoryModel>>

    fun getTransactionsForDay(request: TransactionsForDayRequest): Flow<List<TransactionModel>>

    fun getTodayOverview(): Flow<TransactionOverviewModel>

    fun getAllowanceTemplates(): Flow<List<AllowanceTemplateModel>>
    suspend fun saveAllowanceTemplates(templates: List<AllowanceTemplateModel>)


}