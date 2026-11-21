package com.gmail.vondenuelle.denuspend.data.remote.services.transaction

import com.gmail.vondenuelle.denuspend.data.remote.error.DocumentNotFoundException
import com.gmail.vondenuelle.denuspend.data.remote.error.NoUserException
import com.gmail.vondenuelle.denuspend.data.remote.models.transaction.request.EditTransactionRequest
import com.gmail.vondenuelle.denuspend.data.remote.models.transaction.request.TransactionRequest
import com.gmail.vondenuelle.denuspend.data.remote.models.transaction.request.TransactionsForDayRequest
import com.gmail.vondenuelle.denuspend.data.remote.services.wallet.FirebaseWalletServiceImpl.Companion.BALANCE
import com.gmail.vondenuelle.denuspend.data.remote.services.wallet.FirebaseWalletServiceImpl.Companion.WALLETS
import com.gmail.vondenuelle.denuspend.data.remote.services.wallet.FirebaseWalletServiceImpl.Companion.WALLET_ID
import com.gmail.vondenuelle.denuspend.domain.models.transaction.AllowanceTemplateModel
import com.gmail.vondenuelle.denuspend.domain.models.transaction.AllowanceTemplatesDoc
import com.gmail.vondenuelle.denuspend.domain.models.transaction.DailyHistoryModel
import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionModel
import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionOverviewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
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
        const val ALLOWANCE_TEMPLATES_COLLECTION = "allowance_templates"
        const val MONTHLY_HISTORY = "monthly_history"

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
        const val MONTH = "month"
        const val WEEKDAY_EXPENSE_BY_CATEGORY = "weekdayExpenseByCategory"
        // On the daily doc: true once the day's adds also go to monthly_history. Days without it are skipped on edit/delete.
        const val IN_MONTHLY = "inMonthly"


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


        val today = LocalDate.now(ZoneId.systemDefault())
        val todayId = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val dailyDocId = "${userId}_$todayId"


        val dailyDocRef = firebaseFireStore.collection(DAILY_HISTORY)
            .document(dailyDocId)
        val monthlyDocRef = monthlyDocRef(userId, today)

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
                date = timestamp,
                // Only expenses come out of a wallet; an allowance is a plan, not real money moving
                walletId = if (transactionRequest.amount < 0) transactionRequest.walletId else ""
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
            TOTAL_EXPENSE to FieldValue.increment(transactions.filter { it.amount < 0 }.sumOf { abs(it.amount) }),
            IN_MONTHLY to true
        )
        // Per-category totals as nested maps (set + merge reads the keys literally). Empty maps are left out,
        // because merging an empty map would replace the existing one.
        val categoryFields = mutableMapOf<String, Any>()
        if (expensesByCategory.isNotEmpty()) {
            categoryFields[EXPENSE_BY_CATEGORY] = expensesByCategory.mapValues { (_, list) -> FieldValue.increment(list.sumOf { abs(it.amount) }) }
            categoryFields[COUNT_BY_CATEGORY] = expensesByCategory.mapValues { (_, list) -> FieldValue.increment(list.size.toLong()) }
        }
        if (allowancesByCategory.isNotEmpty()) {
            categoryFields[ALLOWANCE_BY_CATEGORY] = allowancesByCategory.mapValues { (_, list) -> FieldValue.increment(list.sumOf { it.amount }) }
        }
        batch.set(dailyDocRef, dailyFields + categoryFields, SetOptions.merge())

        // 4. Update the month's totals (same category maps, plus expenses per weekday) for Insights' long ranges
        val monthlyFields = mutableMapOf<String, Any>(
            USER_ID to userId,
            MONTH to toTimestamp(today.withDayOfMonth(1))
        )
        monthlyFields.putAll(categoryFields)
        if (expensesByCategory.isNotEmpty()) {
            monthlyFields[WEEKDAY_EXPENSE_BY_CATEGORY] = expensesByCategory.mapValues { (_, list) ->
                mapOf(weekdayKey(today) to FieldValue.increment(list.sumOf { abs(it.amount) }))
            }
        }
        batch.set(monthlyDocRef, monthlyFields, SetOptions.merge())

        // 5. Take the expenses off the wallets they were paid from. update() fails if a wallet was closed meanwhile, so nothing half-saves.
        transactions.filter { it.walletId.isNotEmpty() }.groupBy { it.walletId }.forEach { (walletId, list) ->
            batch.update(walletRef(walletId), BALANCE, FieldValue.increment(-list.sumOf { abs(it.amount) }))
        }


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
        val currentUser = firebaseAuth.currentUser
            ?: throw NoUserException("User not logged in")

        val transactionRef = firebaseFireStore.collection(TRANSACTION).document(request.docId)
        val dailyDocRef = firebaseFireStore.collection(DAILY_HISTORY).document(request.dailyDocId)
        val dailyTransactionRef = dailyDocRef.collection(TRANSACTION).document(request.docId)

        val newWalletId = if (request.amount < 0) request.walletId else ""
        val fields = mapOf(
            TITLE to request.title,
            DESCRIPTION to request.description,
            AMOUNT to request.amount,
            CATEGORY to request.category,
            WALLET_ID to newWalletId
        )

        // Income stays income on edit, so only one total moves, by the difference.
        val totalField = if (request.amount > 0) TOTAL_INCOME else TOTAL_EXPENSE
        val delta = abs(request.amount) - abs(request.oldAmount)

        val dailyFields = mutableMapOf<String, Any>(totalField to FieldValue.increment(delta))
        // The same category changes go to the daily doc and, if the day is counted there, to the month
        val categoryFields = mutableMapOf<String, Any>()
        val day = dailyDocRef.get().await()
        if (hasCategoryTotals(day)) {
            val categoryField = if (request.amount > 0) ALLOWANCE_BY_CATEGORY else EXPENSE_BY_CATEGORY
            if (request.category == request.oldCategory) {
                categoryFields[categoryField] = mapOf(request.category to FieldValue.increment(delta))
            } else {
                // Category changed: take the old amount off the old category and put the new one on the new category
                categoryFields[categoryField] = mapOf(
                    request.oldCategory to FieldValue.increment(-abs(request.oldAmount)),
                    request.category to FieldValue.increment(abs(request.amount))
                )
                if (request.amount < 0) {
                    categoryFields[COUNT_BY_CATEGORY] = mapOf(
                        request.oldCategory to FieldValue.increment(-1),
                        request.category to FieldValue.increment(1)
                    )
                }
            }
        }
        dailyFields.putAll(categoryFields)

        val batch = firebaseFireStore.batch()
        batch.update(transactionRef, fields)
        batch.update(dailyTransactionRef, fields)
        batch.set(dailyDocRef, dailyFields, SetOptions.merge())
        if (isInMonthly(day) && categoryFields.isNotEmpty()) {
            val dayDate = dayFromDailyDocId(request.dailyDocId)
            val monthlyFields = categoryFields.toMutableMap()
            if (request.amount < 0) {
                val weekday = weekdayKey(dayDate)
                monthlyFields[WEEKDAY_EXPENSE_BY_CATEGORY] = if (request.category == request.oldCategory) {
                    mapOf(request.category to mapOf(weekday to FieldValue.increment(delta)))
                } else {
                    mapOf(
                        request.oldCategory to mapOf(weekday to FieldValue.increment(-abs(request.oldAmount))),
                        request.category to mapOf(weekday to FieldValue.increment(abs(request.amount)))
                    )
                }
            }
            batch.set(monthlyDocRef(currentUser.uid, dayDate), monthlyFields, SetOptions.merge())
        }
        // Wallet balances: same wallet moves by the difference; a changed wallet refunds the old one and charges the new one.
        // A closed old wallet is skipped (1 read to check), so this never brings it back.
        val oldWalletOpen = request.oldWalletId.isNotEmpty() && walletRef(request.oldWalletId).get().await().exists()
        if (newWalletId.isNotEmpty() && newWalletId == request.oldWalletId) {
            if (oldWalletOpen) batch.update(walletRef(newWalletId), BALANCE, FieldValue.increment(-delta))
        } else {
            if (oldWalletOpen) batch.update(walletRef(request.oldWalletId), BALANCE, FieldValue.increment(abs(request.oldAmount)))
            if (newWalletId.isNotEmpty()) batch.update(walletRef(newWalletId), BALANCE, FieldValue.increment(-abs(request.amount)))
        }
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
        val categoryFields = mutableMapOf<String, Any>()
        val day = dailyDocRef.get().await()
        if (hasCategoryTotals(day)) {
            val categoryField = if (transaction.amount > 0) ALLOWANCE_BY_CATEGORY else EXPENSE_BY_CATEGORY
            categoryFields[categoryField] = mapOf(transaction.category to FieldValue.increment(-abs(transaction.amount)))
            if (transaction.amount < 0) {
                categoryFields[COUNT_BY_CATEGORY] = mapOf(transaction.category to FieldValue.increment(-1))
            }
        }
        dailyFields.putAll(categoryFields)

        // The daily doc is kept even if this was the day's last transaction (it shows as ₱0).
        val batch = firebaseFireStore.batch()
        batch.delete(transactionRef)
        batch.delete(dailyTransactionRef)
        batch.set(dailyDocRef, dailyFields, SetOptions.merge())
        if (isInMonthly(day) && categoryFields.isNotEmpty()) {
            val dayDate = dayFromDailyDocId(dailyDocId)
            val monthlyFields = categoryFields.toMutableMap()
            if (transaction.amount < 0) {
                monthlyFields[WEEKDAY_EXPENSE_BY_CATEGORY] = mapOf(
                    transaction.category to mapOf(weekdayKey(dayDate) to FieldValue.increment(-abs(transaction.amount)))
                )
            }
            batch.set(monthlyDocRef(currentUser.uid, dayDate), monthlyFields, SetOptions.merge())
        }
        // Give the money back to the wallet it was paid from, unless that wallet was closed (1 read to check)
        if (transaction.amount < 0 && transaction.walletId.isNotEmpty()) {
            val wallet = walletRef(transaction.walletId)
            if (wallet.get().await().exists()) {
                batch.update(wallet, BALANCE, FieldValue.increment(abs(transaction.amount)))
            }
        }
        batch.commit().await()
    }

    private fun walletRef(walletId: String): DocumentReference =
        firebaseFireStore.collection(WALLETS).document(walletId)

    // Days saved before the per-category fields existed don't have them. Changing those days would create
    // negative category values, so edit and delete only touch the per-category fields when the day already has them.
    // Takes the day doc that edit/delete already read, so the monthly check below costs no extra read.
    private fun hasCategoryTotals(day: DocumentSnapshot): Boolean =
        day.contains(EXPENSE_BY_CATEGORY) || day.contains(ALLOWANCE_BY_CATEGORY)

    // Same idea for monthly_history: days added before it existed were never counted in their month, so they're left out.
    private fun isInMonthly(day: DocumentSnapshot): Boolean = day.getBoolean(IN_MONTHLY) == true

    private fun monthlyDocRef(userId: String, day: LocalDate): DocumentReference =
        firebaseFireStore.collection(MONTHLY_HISTORY)
            .document("${userId}_${day.format(DateTimeFormatter.ofPattern("yyyyMM"))}")

    // Daily doc ids are "{uid}_yyyyMMdd", so the last 8 characters are the day
    private fun dayFromDailyDocId(dailyDocId: String): LocalDate =
        LocalDate.parse(dailyDocId.takeLast(8), DateTimeFormatter.BASIC_ISO_DATE)

    // "MON".."SUN", the keys of weekdayExpenseByCategory
    private fun weekdayKey(day: LocalDate): String = day.dayOfWeek.name.take(3)

    private fun toTimestamp(day: LocalDate): Timestamp =
        Timestamp(Date.from(day.atStartOfDay(ZoneId.systemDefault()).toInstant()))


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

    // Only today's daily doc, for the expense/allowance form's "left today" line. It's the same doc the Add tab's
    // getTodayOverview listens to, so the SDK shares that listener and the form usually costs no extra reads.
    override fun getTodayDailyHistory(): Flow<DailyHistoryModel?> = flow {
        val currentUser = firebaseAuth.currentUser
            ?: throw NoUserException("User not logged in")
        val todayId = LocalDate
            .now(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val dailyDocRef = firebaseFireStore
            .collection(DAILY_HISTORY)
            .document("${currentUser.uid}_$todayId")

        emitAll(callbackFlow {
            val listener = dailyDocRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject<DailyHistoryModel>()?.copy(docId = snapshot.id))
            }
            awaitClose { listener.remove() }
        })
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