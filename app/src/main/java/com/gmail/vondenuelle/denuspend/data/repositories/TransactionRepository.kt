package com.gmail.vondenuelle.denuspend.data.repositories

import com.gmail.vondenuelle.denuspend.data.remote.error.CannotCreateTransactionException
import com.gmail.vondenuelle.denuspend.data.remote.models.transaction.request.TransactionRequest
import com.gmail.vondenuelle.denuspend.data.remote.services.transaction.TransactionService
import com.gmail.vondenuelle.denuspend.di.qualifiers.FirebaseTransaction
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
class TransactionRepository @Inject constructor(
    @FirebaseTransaction private val transactionService: TransactionService
) {
    suspend fun addTransaction(transactionRequest: TransactionRequest) {
        if (transactionRequest.amount == 0L ||
            transactionRequest.category.isEmpty() ||
            transactionRequest.description.isEmpty() ||
            transactionRequest.title.isEmpty()) {
            throw CannotCreateTransactionException("Input fields are empty or invalid. Please make sure your inputs are correct")
        } else {
            transactionService.addTransaction(transactionRequest)
        }
    }

    suspend fun getAllTransactions() {
        transactionService.getAllTransactions()
    }

    suspend fun getTransactionById(id: String) {
        transactionService.getTransactionById(id)
    }
}