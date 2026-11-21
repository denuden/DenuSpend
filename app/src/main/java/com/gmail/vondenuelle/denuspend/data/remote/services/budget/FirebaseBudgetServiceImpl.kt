package com.gmail.vondenuelle.denuspend.data.remote.services.budget

import com.gmail.vondenuelle.denuspend.data.remote.error.NoUserException
import com.gmail.vondenuelle.denuspend.data.remote.models.budget.request.GetBudgetSummaryRequest
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetTotalSummaryModel
import com.gmail.vondenuelle.denuspend.domain.models.budget.MonthlyHistoryModel
import com.gmail.vondenuelle.denuspend.domain.models.transaction.DailyHistoryModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class FirebaseBudgetServiceImpl @Inject constructor(
    private val firebaseAuth : FirebaseAuth,
    private val firebaseFireStore : FirebaseFirestore
) : BudgetService {

    companion object {
        //Daily history collection
        const val DAILY_HISTORY = "daily_history"
        const val USER_ID = "userId"
        const val DATE = "date"
        const val MONTHLY_HISTORY = "monthly_history"
        const val MONTH = "month"

    }


    override suspend fun getBudgetSummary(
        request: GetBudgetSummaryRequest
    ): Flow<BudgetTotalSummaryModel> = callbackFlow {

        val currentUser = firebaseAuth.currentUser
            ?: throw NoUserException("User not logged in")
        // One doc per day (daily_history) instead of one per transaction, so a range costs at most one read per day
        val query = firebaseFireStore
            .collection(DAILY_HISTORY)
            .whereEqualTo(USER_ID, currentUser.uid)
            .whereGreaterThanOrEqualTo(DATE, request.startDate)
            .whereLessThanOrEqualTo(DATE, request.endDate)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val days = snapshot?.documents
                ?.mapNotNull { it.toObject(DailyHistoryModel::class.java) }
                ?: emptyList()

            // Add up each day's per-category maps. Days saved before these maps existed add nothing.
            val totals = sumByCategory(days.map { it.expenseByCategory })
            val allowances = sumByCategory(days.map { it.allowanceByCategory })

            val summary = BudgetTotalSummaryModel(
                totals = totals,
                counts = sumByCategory(days.map { it.countByCategory }).mapValues { it.value.toInt() },
                totalExpense = totals.values.sum(),
                allowances = allowances,
                totalAllowance = allowances.values.sum()
            )

            trySend(summary)
        }

        awaitClose { listener.remove() }
    }

    // Same query as getBudgetSummary (so the same index), but returns each day instead of the sum
    override fun getDailyHistoryInRange(
        request: GetBudgetSummaryRequest
    ): Flow<List<DailyHistoryModel>> = callbackFlow {
        val currentUser = firebaseAuth.currentUser
            ?: throw NoUserException("User not logged in")
        val query = firebaseFireStore
            .collection(DAILY_HISTORY)
            .whereEqualTo(USER_ID, currentUser.uid)
            .whereGreaterThanOrEqualTo(DATE, request.startDate)
            .whereLessThanOrEqualTo(DATE, request.endDate)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val days = snapshot?.documents
                ?.mapNotNull { doc -> doc.toObject(DailyHistoryModel::class.java)?.copy(docId = doc.id) }
                ?: emptyList()
            trySend(days)
        }

        awaitClose { listener.remove() }
    }

    // One doc per month. Needs a composite index (userId + month); the first run logs a link that creates it.
    override fun getMonthlyHistoryInRange(
        request: GetBudgetSummaryRequest
    ): Flow<List<MonthlyHistoryModel>> = callbackFlow {
        val currentUser = firebaseAuth.currentUser
            ?: throw NoUserException("User not logged in")
        val query = firebaseFireStore
            .collection(MONTHLY_HISTORY)
            .whereEqualTo(USER_ID, currentUser.uid)
            .whereGreaterThanOrEqualTo(MONTH, request.startDate)
            .whereLessThanOrEqualTo(MONTH, request.endDate)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val months = snapshot?.documents
                ?.mapNotNull { doc -> doc.toObject(MonthlyHistoryModel::class.java)?.copy(docId = doc.id) }
                ?: emptyList()
            trySend(months)
        }

        awaitClose { listener.remove() }
    }

    // Read by id ("{uid}_yyyyMM", the same id the transaction batch writes), so it needs no index and costs 1 read
    override fun getMonthlyHistory(yearMonth: String): Flow<MonthlyHistoryModel?> = callbackFlow {
        val currentUser = firebaseAuth.currentUser
            ?: throw NoUserException("User not logged in")
        val docRef = firebaseFireStore
            .collection(MONTHLY_HISTORY)
            .document("${currentUser.uid}_$yearMonth")

        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.toObject(MonthlyHistoryModel::class.java)?.copy(docId = snapshot.id))
        }

        awaitClose { listener.remove() }
    }

    private fun sumByCategory(maps: List<Map<String, Long>>): Map<String, Long> =
        maps.flatMap { it.entries }
            .groupBy({ it.key }, { it.value })
            .mapValues { (_, values) -> values.sum() }
}
