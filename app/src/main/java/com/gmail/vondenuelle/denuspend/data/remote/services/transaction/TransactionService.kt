package com.gmail.vondenuelle.denuspend.data.remote.services.transaction

import com.gmail.vondenuelle.denuspend.data.remote.models.transaction.request.TransactionRequest
import com.gmail.vondenuelle.denuspend.domain.models.TransactionModel

interface TransactionService {
    suspend fun addTransaction(transactionRequest: TransactionRequest) : TransactionModel
    suspend fun getAllTransactions(): List<TransactionModel>
    suspend fun getTransactionById(id: String): TransactionModel
}