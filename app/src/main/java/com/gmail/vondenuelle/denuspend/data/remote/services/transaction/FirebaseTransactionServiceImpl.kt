package com.gmail.vondenuelle.denuspend.data.remote.services.transaction

import com.gmail.vondenuelle.denuspend.data.remote.error.NoUserException
import com.gmail.vondenuelle.denuspend.data.remote.models.transaction.request.TransactionRequest
import com.gmail.vondenuelle.denuspend.data.remote.models.transaction.request.TransactionsForDayRequest
import com.gmail.vondenuelle.denuspend.domain.models.transaction.DailyHistoryModel
import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionModel
import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionOverviewModel
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
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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

        //Fields
        const val TOTAL_INCOME = "totalIncome"
        const val TOTAL_EXPENSE = "totalExpense"
        const val USER_ID = "userId"
        const val DATE = "date"


        val utcFormatter = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    override suspend fun addTransaction(transactionRequest: TransactionRequest): TransactionModel {
        val currentUser = firebaseAuth.currentUser
            ?: throw NoUserException("User not logged in")
        val userId = currentUser.uid
        val timestamp = Timestamp.now()


        val todayId = LocalDate
            .now(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val dailyDocId = "${userId}_$todayId"


        val dailyDocRef = firebaseFireStore.collection(DAILY_HISTORY)
            .document(dailyDocId)
        //create empty doc
        val transactionRef = firebaseFireStore.collection(TRANSACTION).document()
        //create transaction doc inside daily history doc
        val dailyTransactionRef =
            dailyDocRef
                .collection(TRANSACTION)
                .document(transactionRef.id)

        val transaction = TransactionModel(
            docId = transactionRef.id,
            userId = userId,
            title = transactionRequest.title,
            description = transactionRequest.description,
            amount = transactionRequest.amount,
            category = transactionRequest.category,
            date = timestamp
        )

        //batch for multiple writes
        val batch = firebaseFireStore.batch()

        try {
            // 1. Write to top-level transactions
            batch.set(transactionRef, transaction)

            // 2. Write to daily subcollection (for UI grouping)
            batch.set(dailyTransactionRef, transaction)

            // 3. Update daily totals
            batch.set(
                dailyDocRef,
                mapOf(
                    USER_ID to userId,
                    DATE to timestamp,
                    TOTAL_INCOME to if (transaction.amount > 0)
                        FieldValue.increment(transaction.amount)
                    else FieldValue.increment(0),

                    TOTAL_EXPENSE to if (transaction.amount < 0)
                        FieldValue.increment(abs(transaction.amount))
                    else FieldValue.increment(0)
                ),
                SetOptions.merge()
            )


            // Commit all at once (atomic)
            batch.commit().await()

            return transaction
        } catch (e : Exception) {
            throw  e
        }
    }


    override fun getDailyTransactionHistory(limit: Long?): Flow<List<DailyHistoryModel>> = callbackFlow {
        val currentUser = firebaseAuth.currentUser
            ?: throw NoUserException("User not logged in")
        val userId = currentUser.uid

        var query: Query = firebaseFireStore.collection(DAILY_HISTORY)
            .whereEqualTo(USER_ID, userId)
            .orderBy(DATE, Query.Direction.DESCENDING)

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
            .orderBy(DATE, Query.Direction.DESCENDING)

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

    override fun getTodayOverview(): Flow<TransactionOverviewModel>  {
        val currentUser = firebaseAuth.currentUser
            ?: throw NoUserException("User not logged in")
        val userId = currentUser.uid

        val todayId = LocalDate
            .now(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyyMMdd"))

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
                .orderBy(DATE, Query.Direction.DESCENDING)
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
            TransactionOverviewModel(daily, txs)
        }
    }
}