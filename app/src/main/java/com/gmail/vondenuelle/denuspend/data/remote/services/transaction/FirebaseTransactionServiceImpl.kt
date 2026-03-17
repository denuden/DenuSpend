package com.gmail.vondenuelle.denuspend.data.remote.services.transaction

import com.gmail.vondenuelle.denuspend.data.remote.error.DocumentNotFoundException
import com.gmail.vondenuelle.denuspend.data.remote.error.ReturnedEmptyListException
import com.gmail.vondenuelle.denuspend.data.remote.error.ReturnedNullException
import com.gmail.vondenuelle.denuspend.data.remote.models.transaction.request.TransactionRequest
import com.gmail.vondenuelle.denuspend.domain.models.TransactionModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseTransactionServiceImpl @Inject constructor(
    private val firebaseFireStore : FirebaseFirestore
) : TransactionService {
    companion object {
        //Transaction collection
        const val TRANSACTION = "transactions"
    }

    override suspend fun addTransaction(transactionRequest: TransactionRequest): TransactionModel {
        val transaction = TransactionModel(
            title = transactionRequest.title,
            description = transactionRequest.description,
            amount = transactionRequest.amount,
            category = transactionRequest.category
        )

        try {
            val docRef = firebaseFireStore.collection(TRANSACTION)
                .add(transaction)
                .await()

            return transaction.copy(
                docId = docRef.id,
            )
        } catch (e : Exception) {
            throw  e
        }
    }

    override suspend fun getAllTransactions(): List<TransactionModel> {
        try {
            val snapshot = firebaseFireStore.collection(TRANSACTION)
                .get().await()

            return snapshot.documents.mapNotNull { doc ->
                doc.toObject<TransactionModel>()?.copy(docId = doc.id)
            }

        } catch (e : Exception) {
            throw e
        }
    }

    override suspend fun getTransactionById(id: String): TransactionModel {
        try {
            val snapshot = firebaseFireStore.collection(TRANSACTION)
                .document(id).get().await()

            if (!snapshot.exists()) {
                throw DocumentNotFoundException()
            }

            return snapshot.toObject<TransactionModel>()?.copy(docId = id)
                ?: throw ReturnedNullException()

        } catch (e : Exception) {
            throw e
        }
    }
}