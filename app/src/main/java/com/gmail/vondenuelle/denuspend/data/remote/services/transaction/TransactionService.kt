package com.gmail.vondenuelle.denuspend.data.remote.services.transaction

import com.gmail.vondenuelle.denuspend.data.remote.models.transaction.request.TransactionRequest
import com.gmail.vondenuelle.denuspend.data.remote.models.transaction.request.TransactionsForDayRequest
import com.gmail.vondenuelle.denuspend.data.remote.models.transaction.response.TransactionOverviewResponse
import com.gmail.vondenuelle.denuspend.domain.models.transaction.DailyHistoryModel
import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionModel
import kotlinx.coroutines.flow.Flow

interface TransactionService {
    suspend fun addTransaction(transactionRequest: TransactionRequest) : TransactionModel
    fun getDailyTransactionHistory(limit : Long?) : Flow<List<DailyHistoryModel>>

    fun getTransactionsForDay(request: TransactionsForDayRequest): Flow<List<TransactionModel>>

    fun getTodayOverview() : Flow<TransactionOverviewResponse>


}