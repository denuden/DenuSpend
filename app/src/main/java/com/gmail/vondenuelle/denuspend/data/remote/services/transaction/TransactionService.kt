package com.gmail.vondenuelle.denuspend.data.remote.services.transaction

import com.gmail.vondenuelle.denuspend.data.remote.models.transaction.request.TransactionRequest
import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionModel
import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionSummaryModel
import kotlinx.coroutines.flow.Flow

interface TransactionService {
    suspend fun addTransaction(transactionRequest: TransactionRequest) : TransactionModel
    fun getAllTransactions(limit : Long?): Flow<List<TransactionModel>>
    suspend fun getTransactionById(id: String): TransactionModel

    suspend fun getSummaryOfDailyTransactions() : TransactionSummaryModel

}