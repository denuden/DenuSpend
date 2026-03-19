package com.gmail.vondenuelle.denuspend.data.remote.services.transaction

import com.gmail.vondenuelle.denuspend.data.remote.error.NoUserException
import com.gmail.vondenuelle.denuspend.data.remote.models.transaction.request.TransactionRequest
import com.gmail.vondenuelle.denuspend.data.remote.models.transaction.request.TransactionsForDayRequest
import com.gmail.vondenuelle.denuspend.data.remote.models.transaction.response.TransactionOverviewResponse
import com.gmail.vondenuelle.denuspend.domain.models.transaction.DailyHistoryModel
import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class FirebaseTransactionServiceImpl @Inject constructor(
    private val firebaseAuth : FirebaseAuth,
    private val firebaseFireStore : FirebaseFirestore
) : TransactionService {
    companion object {
        //Transaction collection
        const val TRANSACTION = "transactions"
        const val DAILY_HISTORY = "daily_history"

        val utcFormatter = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    override suspend fun addTransaction(transactionRequest: TransactionRequest): TransactionModel {
        val currentUser = firebaseAuth.currentUser
            ?: throw NoUserException("User not logged in")
        val userId = currentUser.uid

        val timestamp = Timestamp.now()

        val transaction = TransactionModel(
            userId = userId,
            title = transactionRequest.title,
            description = transactionRequest.description,
            amount = transactionRequest.amount,
            category = transactionRequest.category,
            date = timestamp
        )

        val todayId = utcFormatter.format(Date())

        val dailyDocRef = firebaseFireStore.collection(DAILY_HISTORY)
            .document("${userId}_$todayId")

        try {
            // 1. Add transaction to daily subcollection
            val transactionDocRef = dailyDocRef.collection(TRANSACTION)
                .add(transaction)
                .await()

            val savedTransaction = transaction.copy(docId = transactionDocRef.id)

            // 2. Upsert daily totals
            dailyDocRef
                .set(
                    mapOf(
                        "userId" to userId,
                        "date" to timestamp,
                        "totalIncome" to if (transaction.amount > 0) FieldValue.increment(transaction.amount) else FieldValue.increment(0),
                        "totalExpense" to if (transaction.amount < 0) FieldValue.increment(
                            abs(
                                transaction.amount
                            )
                        ) else FieldValue.increment(0)
                    ),
                    SetOptions.merge()
                ).await()

            return savedTransaction
        } catch (e : Exception) {
            throw  e
        }
    }
    override fun getDailyTransactionHistory(limit: Long?): Flow<List<DailyHistoryModel>> = callbackFlow {
        val currentUser = firebaseAuth.currentUser
            ?: throw NoUserException("User not logged in")
        val userId = currentUser.uid

        var query: Query = firebaseFireStore.collection(DAILY_HISTORY)
            .whereEqualTo("userId", userId)
            .orderBy("date", Query.Direction.DESCENDING)

        if (limit != null) {
            query = query.limit(limit)
        }

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val data = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject<DailyHistoryModel>()?.copy(
                    docId = doc.id
                )
            } ?: emptyList()

            trySend(data)
        }

        awaitClose { listener.remove() }
    }

    override fun getTransactionsForDay(request: TransactionsForDayRequest): Flow<List<TransactionModel>> = callbackFlow {
        val currentUser = firebaseAuth.currentUser
            ?: throw NoUserException("User not logged in")

        val dailyDocRef = firebaseFireStore.collection(DAILY_HISTORY).document(request.dailyDocId)

        var query: Query = dailyDocRef.collection(TRANSACTION)
            .orderBy("date", Query.Direction.DESCENDING)

        if (request.limit != null) {
            query = query.limit(request.limit)
        }

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val data = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject<TransactionModel>()?.copy(docId = doc.id)
            } ?: emptyList()

            trySend(data)
        }

        awaitClose { listener.remove() }
    }

    override fun getTodayOverview(): Flow<TransactionOverviewResponse>  {
        val currentUser = firebaseAuth.currentUser
            ?: throw NoUserException("User not logged in")
        val userId = currentUser.uid

        val todayId = utcFormatter.format(Date())

        val dailyDocRef = firebaseFireStore
            .collection(DAILY_HISTORY)
            .document("${userId}_$todayId")

        // Listen to the daily doc
        val dailyFlow = callbackFlow<DailyHistoryModel?> {
            val listener = dailyDocRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val data = snapshot?.toObject<DailyHistoryModel>()
                    ?.copy(docId = snapshot.id)

                trySend(data)
            }

            awaitClose { listener.remove() }
        }


        val transactionFlow = callbackFlow<List<TransactionModel>> {
            val listener = dailyDocRef.collection(TRANSACTION)
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    val data = snapshot?.documents?.mapNotNull {
                        it.toObject<TransactionModel>()?.copy(docId = it.id)
                    } ?: emptyList()

                    trySend(data)
                }

            awaitClose { listener.remove() }
        }

        return kotlinx.coroutines.flow.combine(dailyFlow, transactionFlow) { daily, txs ->
            TransactionOverviewResponse(daily, txs)
        }
    }
}