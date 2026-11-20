package com.gmail.vondenuelle.denuspend.data.remote.services.transaction

import com.gmail.vondenuelle.denuspend.data.remote.error.DocumentNotFoundException
import com.gmail.vondenuelle.denuspend.data.remote.error.NoUserException
import com.gmail.vondenuelle.denuspend.data.remote.models.transaction.request.EditTransactionRequest
import com.gmail.vondenuelle.denuspend.data.remote.models.transaction.request.TransactionRequest
import com.gmail.vondenuelle.denuspend.data.remote.models.transaction.request.TransactionsForDayRequest
import com.gmail.vondenuelle.denuspend.domain.models.transaction.AllowanceTemplateModel
import com.gmail.vondenuelle.denuspend.domain.models.transaction.AllowanceTemplatesDoc
import com.gmail.vondenuelle.denuspend.domain.models.transaction.DailyHistoryModel
import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionModel
import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionOverviewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
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
        const val ALLOWANCE_TEMPLATES_COLLECTION = "allowance_templates"

        //Fields
        const val TOTAL_INCOME = "totalIncome"
        const val TOTAL_EXPENSE = "totalExpense"
        const val USER_ID = "userId"
        const val DATE = "date"
        const val TITLE = "title"
        const val DESCRIPTION = "description"
        const val AMOUNT = "amount"
        const val CATEGORY = "category"
        const val TEMPLATES = "templates"
        const val EXPENSE_BY_CATEGORY = "expenseByCategory"
        const val ALLOWANCE_BY_CATEGORY = "allowanceByCategory"
        const val COUNT_BY_CATEGORY = "countByCategory"


        val utcFormatter = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    override suspend fun addTransaction(transactionRequest: TransactionRequest): TransactionModel =
        addTransactions(listOf(transactionRequest)).first()

    // Every request lands on today's daily doc in one batch, so a template's allowances move the day totals once.
    override suspend fun addTransactions(requests: List<TransactionRequest>): List<TransactionModel> {
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

        //batch for multiple writes
        val batch = firebaseFireStore.batch()

        val transactions = requests.map { transactionRequest ->
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

            // 1. Write to top-level transactions
            batch.set(transactionRef, transaction)

            // 2. Write to daily subcollection (for UI grouping)
            batch.set(dailyTransactionRef, transaction)

            transaction
        }

        // 3. Update daily totals
        val expensesByCategory = transactions.filter { it.amount < 0 }.groupBy { it.category }
        val allowancesByCategory = transactions.filter { it.amount > 0 }.groupBy { it.category }
        val dailyFields = mutableMapOf<String, Any>(
            USER_ID to userId,
            DATE to timestamp,
            TOTAL_INCOME to FieldValue.increment(transactions.filter { it.amount > 0 }.sumOf { it.amount }),
            TOTAL_EXPENSE to FieldValue.increment(transactions.filter { it.amount < 0 }.sumOf { abs(it.amount) })
        )
        // Per-category totals as nested maps (set + merge reads the keys literally). Empty maps are left out,
        // because merging an empty map would replace the existing one.
        if (expensesByCategory.isNotEmpty()) {
            dailyFields[EXPENSE_BY_CATEGORY] = expensesByCategory.mapValues { (_, list) -> FieldValue.increment(list.sumOf { abs(it.amount) }) }
            dailyFields[COUNT_BY_CATEGORY] = expensesByCategory.mapValues { (_, list) -> FieldValue.increment(list.size.toLong()) }
        }
        if (allowancesByCategory.isNotEmpty()) {
            dailyFields[ALLOWANCE_BY_CATEGORY] = allowancesByCategory.mapValues { (_, list) -> FieldValue.increment(list.sumOf { it.amount }) }
        }
        batch.set(dailyDocRef, dailyFields, SetOptions.merge())


        // Commit all at once (atomic)
        batch.commit().await()

        return transactions
    }

    override fun getAllowanceTemplates(): Flow<List<AllowanceTemplateModel>> = callbackFlow {
        val currentUser = firebaseAuth.currentUser
            ?: throw NoUserException("User not logged in")

        // One doc per user (allowance_templates/{uid}) holding every template, so loading them is a single read
        val listener = firebaseFireStore.collection(ALLOWANCE_TEMPLATES_COLLECTION).document(currentUser.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject<AllowanceTemplatesDoc>()?.templates ?: emptyList())
            }

        awaitClose { listener.remove() }
    }

    // Arrays are replaced as a whole on merge, so this saves the full list (add, edit and delete all go through here)
    override suspend fun saveAllowanceTemplates(templates: List<AllowanceTemplateModel>) {
        val currentUser = firebaseAuth.currentUser
            ?: throw NoUserException("User not logged in")

        firebaseFireStore.collection(ALLOWANCE_TEMPLATES_COLLECTION).document(currentUser.uid)
            .set(mapOf(USER_ID to currentUser.uid, TEMPLATES to templates), SetOptions.merge())
            .await()
    }

    override suspend fun getTransactionById(docId: String): TransactionModel {
        val currentUser = firebaseAuth.currentUser
            ?: throw NoUserException("User not logged in")

        val doc = firebaseFireStore.collection(TRANSACTION).document(docId).get().await()
        val transaction = doc.toObject<TransactionModel>()?.copy(docId = doc.id)

        // Treat another user's doc the same as a missing one
        if (transaction == null || transaction.userId != currentUser.uid) {
            throw DocumentNotFoundException()
        }
        return transaction
    }

    override suspend fun updateTransaction(request: EditTransactionRequest) {
        firebaseAuth.currentUser
            ?: throw NoUserException("User not logged in")

        val transactionRef = firebaseFireStore.collection(TRANSACTION).document(request.docId)
        val dailyDocRef = firebaseFireStore.collection(DAILY_HISTORY).document(request.dailyDocId)
        val dailyTransactionRef = dailyDocRef.collection(TRANSACTION).document(request.docId)

        val fields = mapOf(
            TITLE to request.title,
            DESCRIPTION to request.description,
            AMOUNT to request.amount,
            CATEGORY to request.category
        )

        // Income stays income on edit, so only one total moves, by the difference.
        val totalField = if (request.amount > 0) TOTAL_INCOME else TOTAL_EXPENSE
        val delta = abs(request.amount) - abs(request.oldAmount)

        val dailyFields = mutableMapOf<String, Any>(totalField to FieldValue.increment(delta))
        if (hasCategoryTotals(dailyDocRef)) {
            val categoryField = if (request.amount > 0) ALLOWANCE_BY_CATEGORY else EXPENSE_BY_CATEGORY
            if (request.category == request.oldCategory) {
                dailyFields[categoryField] = mapOf(request.category to FieldValue.increment(delta))
            } else {
                // Category changed: take the old amount off the old category and put the new one on the new category
                dailyFields[categoryField] = mapOf(
                    request.oldCategory to FieldValue.increment(-abs(request.oldAmount)),
                    request.category to FieldValue.increment(abs(request.amount))
                )
                if (request.amount < 0) {
                    dailyFields[COUNT_BY_CATEGORY] = mapOf(
                        request.oldCategory to FieldValue.increment(-1),
                        request.category to FieldValue.increment(1)
                    )
                }
            }
        }

        val batch = firebaseFireStore.batch()
        batch.update(transactionRef, fields)
        batch.update(dailyTransactionRef, fields)
        batch.set(dailyDocRef, dailyFields, SetOptions.merge())
        batch.commit().await()
    }

    override suspend fun deleteTransaction(transaction: TransactionModel, dailyDocId: String) {
        val currentUser = firebaseAuth.currentUser
            ?: throw NoUserException("User not logged in")
        if (transaction.userId != currentUser.uid) throw DocumentNotFoundException()

        val transactionRef = firebaseFireStore.collection(TRANSACTION).document(transaction.docId)
        val dailyDocRef = firebaseFireStore.collection(DAILY_HISTORY).document(dailyDocId)
        val dailyTransactionRef = dailyDocRef.collection(TRANSACTION).document(transaction.docId)

        val totalField = if (transaction.amount > 0) TOTAL_INCOME else TOTAL_EXPENSE

        val dailyFields = mutableMapOf<String, Any>(totalField to FieldValue.increment(-abs(transaction.amount)))
        if (hasCategoryTotals(dailyDocRef)) {
            val categoryField = if (transaction.amount > 0) ALLOWANCE_BY_CATEGORY else EXPENSE_BY_CATEGORY
            dailyFields[categoryField] = mapOf(transaction.category to FieldValue.increment(-abs(transaction.amount)))
            if (transaction.amount < 0) {
                dailyFields[COUNT_BY_CATEGORY] = mapOf(transaction.category to FieldValue.increment(-1))
            }
        }

        // The daily doc is kept even if this was the day's last transaction (it shows as ₱0).
        val batch = firebaseFireStore.batch()
        batch.delete(transactionRef)
        batch.delete(dailyTransactionRef)
        batch.set(dailyDocRef, dailyFields, SetOptions.merge())
        batch.commit().await()
    }

    // Days saved before the per-category fields existed don't have them. Changing those days would create
    // negative category values, so edit and delete only touch the per-category fields when the day already has them.
    private suspend fun hasCategoryTotals(dailyDocRef: DocumentReference): Boolean {
        val day = dailyDocRef.get().await()
        return day.contains(EXPENSE_BY_CATEGORY) || day.contains(ALLOWANCE_BY_CATEGORY)
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

    // Wrapped in flow {} so a missing user throws inside the flow and reaches asResult()'s catch instead of crashing the caller.
    override fun getTodayOverview(): Flow<TransactionOverviewModel> = flow {
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

        emitAll(
            combine(dailyFlow, transactionFlow) { daily, txs ->
                TransactionOverviewModel(daily ?: DailyHistoryModel(), txs)
            }
        )
    }
}