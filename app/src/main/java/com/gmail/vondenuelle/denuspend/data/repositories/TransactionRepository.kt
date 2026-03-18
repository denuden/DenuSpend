package com.gmail.vondenuelle.denuspend.data.repositories

import com.gmail.vondenuelle.denuspend.data.remote.error.CannotCreateTransactionException
import com.gmail.vondenuelle.denuspend.data.remote.models.transaction.request.TransactionRequest
import com.gmail.vondenuelle.denuspend.data.remote.services.transaction.TransactionService
import com.gmail.vondenuelle.denuspend.di.qualifiers.FirebaseTransaction
import com.gmail.vondenuelle.denuspend.di.qualifiers.IoDispatcher
import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionModel
import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionSummaryModel
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

@ViewModelScoped
class TransactionRepository @Inject constructor(
    @FirebaseTransaction private val transactionService: TransactionService,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {
    suspend fun addTransaction(transactionRequest: TransactionRequest) {
        if (transactionRequest.amount == 0L ||
            transactionRequest.category.isEmpty() ||
            transactionRequest.title.isEmpty()) {
            throw CannotCreateTransactionException("Input fields are empty or invalid. Please make sure your inputs are correct")
        } else {
            transactionService.addTransaction(transactionRequest.copy(description = transactionRequest.description.ifEmpty { "---" }))
        }
    }

    fun getAllTransactions(limit : Long?) : Flow<List<TransactionModel>>{
        return transactionService.getAllTransactions(limit).flowOn(dispatcher)
    }

    suspend fun getTransactionById(id: String) : TransactionModel{
        return transactionService.getTransactionById(id)
    }

    suspend fun getSummaryOfDailyTransactions() : TransactionSummaryModel {
        return transactionService.getSummaryOfDailyTransactions()
    }
}