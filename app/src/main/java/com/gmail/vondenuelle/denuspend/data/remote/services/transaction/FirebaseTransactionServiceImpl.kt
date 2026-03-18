package com.gmail.vondenuelle.denuspend.data.remote.services.transaction

import android.util.Log
import com.gmail.vondenuelle.denuspend.data.remote.error.DocumentNotFoundException
import com.gmail.vondenuelle.denuspend.data.remote.error.NoUserException
import com.gmail.vondenuelle.denuspend.data.remote.error.ReturnedNullException
import com.gmail.vondenuelle.denuspend.data.remote.models.transaction.request.TransactionRequest
import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionModel
import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionSummaryModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import com.google.type.Date
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.ZoneId
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseTransactionServiceImpl @Inject constructor(
    private val firebaseAuth : FirebaseAuth,
    private val firebaseFireStore : FirebaseFirestore
) : TransactionService {
    companion object {
        //Transaction collection
        const val TRANSACTION = "transactions"
    }

    override suspend fun addTransaction(transactionRequest: TransactionRequest): TransactionModel {
        val currentUser = firebaseAuth.currentUser
        val id = currentUser?.uid ?: throw NoUserException("User not logged in")

        val transaction = TransactionModel(
            userId = id,
            title = transactionRequest.title,
            description = transactionRequest.description,
            amount = transactionRequest.amount,
            category = transactionRequest.category,
             date = Timestamp.now()
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

    override fun getAllTransactions(limit : Long?): Flow<List<TransactionModel>> = callbackFlow {
        val currentUser = firebaseAuth.currentUser
        val id = currentUser?.uid ?: throw NoUserException("User not logged in")

        val today = java.time.LocalDate.now()
        val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = today.atTime(23, 59, 59, 999_000_000)
            .atZone(ZoneId.systemDefault())
            .toInstant()

        val startTimestamp = Timestamp(java.util.Date.from(startOfDay))
        val endTimestamp = Timestamp(java.util.Date.from(endOfDay))

        var query: Query = firebaseFireStore.collection(TRANSACTION)
            .whereEqualTo("userId", id)
            .whereGreaterThanOrEqualTo("date", startTimestamp)
            .whereLessThanOrEqualTo("date", endTimestamp)
            .orderBy("date", Query.Direction.DESCENDING)

        if (limit != null) {
            query = query.limit(limit)
        }

        val listener = query
            .addSnapshotListener { snapshot, error ->
                Log.d("Firestore", "Snapshot received with ${snapshot?.size()} docs, limit=$limit")

                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val data = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject<TransactionModel>()?.copy(docId = doc.id)
                } ?: emptyList()

                trySend(data)
            }

        awaitClose {
            listener.remove()
        }
    }

    override suspend fun getSummaryOfDailyTransactions(): TransactionSummaryModel {
        val currentUser = firebaseAuth.currentUser
        val id = currentUser?.uid ?: throw NoUserException("User not logged in")

        val today = java.time.LocalDate.now()
        val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = today.atTime(23, 59, 59, 999_000_000)
            .atZone(ZoneId.systemDefault())
            .toInstant()

        val startTimestamp = Timestamp(java.util.Date.from(startOfDay))
        val endTimestamp = Timestamp(java.util.Date.from(endOfDay))

        val query: Query = firebaseFireStore.collection(TRANSACTION)
            .whereEqualTo("userId", id)
            .whereGreaterThanOrEqualTo("date", startTimestamp)
            .whereLessThanOrEqualTo("date", endTimestamp)
            .orderBy("date", Query.Direction.DESCENDING)

        val doc = query.get().await()
        var totalExpense = 0L
        var totalIncome = 0L

        doc.documents.forEach  {
            val model = it.toObject<TransactionModel>()?.copy(docId = it.id)
            if (model != null) {
                if(model.amount < 0) {
                    totalExpense += model.amount
                } else {
                    totalIncome += model.amount
                }
            }
        }

        return TransactionSummaryModel(
            totalExpense = totalExpense,
            totalIncome = totalIncome
        )
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