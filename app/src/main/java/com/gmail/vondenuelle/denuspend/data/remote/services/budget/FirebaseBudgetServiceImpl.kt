package com.gmail.vondenuelle.denuspend.data.remote.services.budget

import com.gmail.vondenuelle.denuspend.data.remote.error.NoUserException
import com.gmail.vondenuelle.denuspend.data.remote.models.budget.request.GetBudgetSummaryRequest
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetTotalSummaryModel
import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import kotlin.math.abs

class FirebaseBudgetServiceImpl @Inject constructor(
    private val firebaseAuth : FirebaseAuth,
    private val firebaseFireStore : FirebaseFirestore
) : BudgetService {

    companion object {
        //Transaction collection
        const val TRANSACTION = "transactions"
        const val DAILY_HISTORY = "daily_history"

        const val TOTAL_INCOME = "totalIncome"
        const val TOTAL_EXPENSE = "totalExpense"
        const val USER_ID = "userId"
        const val DATE = "date"

    }


    override suspend fun getBudgetSummary(
        request: GetBudgetSummaryRequest
    ): Flow<BudgetTotalSummaryModel> = callbackFlow {

        val currentUser = firebaseAuth.currentUser
            ?: throw NoUserException("User not logged in")

        val query = firebaseFireStore
            .collection(TRANSACTION)
            .whereEqualTo(USER_ID, currentUser.uid)
            .whereGreaterThanOrEqualTo(DATE, request.startDate)
            .whereLessThanOrEqualTo(DATE, request.endDate)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val transactions = snapshot?.documents?.mapNotNull {
                it.toObject(TransactionModel::class.java)
            } ?: emptyList()

            var foodTotal = 0L
            var foodCount = 0

            var entertainmentTotal = 0L
            var entertainmentCount = 0

            var householdTotal = 0L
            var householdCount = 0

            var transportationTotal = 0L
            var transportationCount = 0

            var workEducationTotal = 0L
            var workEducationCount = 0

            var healthcareTotal = 0L
            var healthcareCount = 0

            var personalTotal = 0L
            var personalCount = 0

            var familyTotal = 0L
            var familyCount = 0

            var othersTotal = 0L
            var othersCount = 0

            var totalExpense = 0L

            for (transaction in transactions) {
                //from negative to positive
                val amount = abs(transaction.amount)

                when (transaction.category) {
                    "Food" -> {
                        foodTotal += amount
                        totalExpense += amount
                        foodCount++
                    }

                    "Entertainment" -> {
                        entertainmentTotal += amount
                        totalExpense += amount
                        entertainmentCount++
                    }

                    "Household" -> {
                        householdTotal += amount
                        totalExpense += amount
                        householdCount++
                    }

                    "Transportation" -> {
                        transportationTotal += amount
                        totalExpense += amount
                        transportationCount++
                    }

                    "Work/Education" -> {
                        workEducationTotal += amount
                        totalExpense += amount
                        workEducationCount++
                    }

                    "Healthcare" -> {
                        healthcareTotal += amount
                        totalExpense += amount
                        healthcareCount++
                    }

                    "Personal" -> {
                        personalTotal += amount
                        totalExpense += amount
                        personalCount++
                    }

                    "Family" -> {
                        familyTotal += amount
                        totalExpense += amount
                        familyCount++
                    }

                    else -> {
                        othersTotal += amount
                        totalExpense += amount
                        othersCount++
                    }
                }
            }

            val summary = BudgetTotalSummaryModel(
                food = foodTotal,
                foodCount = foodCount,
                entertainment = entertainmentTotal,
                entertainmentCount = entertainmentCount,
                household = householdTotal,
                householdCount = householdCount,
                transportation = transportationTotal,
                transportationCount = transportationCount,
                workEducation = workEducationTotal,
                workEducationCount = workEducationCount,
                healthcare = healthcareTotal,
                healthcareCount = healthcareCount,
                personal = personalTotal,
                personalCount = personalCount,
                family = familyTotal,
                familyCount = familyCount,
                others = othersTotal,
                othersCount = othersCount,
                totalExpense = totalExpense
            )

            trySend(summary)
        }

        awaitClose { listener.remove() }
    }

}