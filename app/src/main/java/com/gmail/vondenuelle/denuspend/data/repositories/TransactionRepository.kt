package com.gmail.vondenuelle.denuspend.data.repositories

import com.gmail.vondenuelle.denuspend.data.remote.error.CannotCreateTransactionException
import com.gmail.vondenuelle.denuspend.data.remote.error.CannotUpdateTransactionException
import com.gmail.vondenuelle.denuspend.data.remote.models.transaction.request.EditTransactionRequest
import com.gmail.vondenuelle.denuspend.data.remote.models.transaction.request.TransactionRequest
import com.gmail.vondenuelle.denuspend.data.remote.models.transaction.request.TransactionsForDayRequest
import com.gmail.vondenuelle.denuspend.data.remote.services.transaction.TransactionService
import com.gmail.vondenuelle.denuspend.di.qualifiers.FirebaseTransaction
import com.gmail.vondenuelle.denuspend.di.qualifiers.IoDispatcher
import com.gmail.vondenuelle.denuspend.domain.models.transaction.AllowanceTemplateModel
import com.gmail.vondenuelle.denuspend.domain.models.transaction.DailyHistoryModel
import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionModel
import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionOverviewModel
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
        val missing = missingFieldsMessage(transactionRequest.amount, transactionRequest.category, transactionRequest.title)
        if (missing != null) {
            throw CannotCreateTransactionException(missing)
        } else {
            transactionService.addTransaction(transactionRequest.copy(description = transactionRequest.description.ifEmpty { "---" }))
        }
    }

    // Names what's missing, in form order, e.g. "Please add an amount and a category"; null = all filled
    private fun missingFieldsMessage(amount: Long, category: String, title: String): String? {
        val missing = listOfNotNull(
            "an amount".takeIf { amount == 0L },
            "a category".takeIf { category.isEmpty() },
            "a title".takeIf { title.isEmpty() },
        )
        if (missing.isEmpty()) return null
        val list = if (missing.size == 1) missing[0] else missing.dropLast(1).joinToString(", ") + " and " + missing.last()
        return "Please add $list"
    }

    suspend fun getTransactionById(docId: String): TransactionModel {
        return transactionService.getTransactionById(docId)
    }

    suspend fun updateTransaction(request: EditTransactionRequest) {
        val missing = missingFieldsMessage(request.amount, request.category, request.title)
        if (missing != null) {
            throw CannotUpdateTransactionException(missing)
        } else {
            transactionService.updateTransaction(request.copy(description = request.description.ifEmpty { "---" }))
        }
    }

    suspend fun deleteTransaction(transaction: TransactionModel, dailyDocId: String) {
        transactionService.deleteTransaction(transaction, dailyDocId)
    }

//    fun getAllTransactions(limit : Long?) : Flow<List<TransactionModel>>{
//        return transactionService.getAllTransactions(limit).flowOn(dispatcher)
//    }
//
//    suspend fun getTransactionById(id: String) : TransactionModel{
//        return transactionService.getTransactionById(id)
//    }
//
//    suspend fun getSummaryOfDailyTransactions() : TransactionSummaryModel {
//        return transactionService.getSummaryOfDailyTransactions()
//    }

    fun getTodayDailyHistory(): Flow<DailyHistoryModel?> {
        return transactionService.getTodayDailyHistory()
            .flowOn(dispatcher)
    }

    fun getTodayOverview(): Flow<TransactionOverviewModel> {
        return transactionService.getTodayOverview()
            .flowOn(dispatcher)
    }
    fun getDailyTransactionHistory(limit : Long?): Flow<List<DailyHistoryModel>> {
        return transactionService.getDailyTransactionHistory(limit)
            .flowOn(dispatcher)
    }

    fun getTransactionsForDay(request: TransactionsForDayRequest) :  Flow<List<TransactionModel>> {
        return transactionService.getTransactionsForDay(request)
            .flowOn(dispatcher)
    }

    fun getAllowanceTemplates(): Flow<List<AllowanceTemplateModel>> {
        return transactionService.getAllowanceTemplates()
            .flowOn(dispatcher)
    }

    // [templates] is the full list to keep, so adding, editing and deleting a template all save through here
    suspend fun saveAllowanceTemplates(templates: List<AllowanceTemplateModel>) {
        val invalid = templates.any { template ->
            template.name.isBlank() ||
                template.items.isEmpty() ||
                template.items.any { it.category.isEmpty() || it.amount <= 0L }
        }
        if (invalid) {
            throw CannotCreateTransactionException("Give the template a name, and pick a category and an amount for every row")
        }
        transactionService.saveAllowanceTemplates(templates.map { it.copy(name = it.name.trim()) })
    }

    // Each row becomes one allowance for today, titled with the template's name
    suspend fun applyAllowanceTemplate(template: AllowanceTemplateModel) {
        if (template.items.isEmpty()) {
            throw CannotCreateTransactionException("This template has no allowances to add")
        }
        transactionService.addTransactions(
            template.items.map {
                TransactionRequest(
                    title = template.name,
                    description = "---",
                    amount = it.amount,
                    category = it.category
                )
            }
        )
    }
}