package com.gmail.vondenuelle.denuspend.domain.models.budget

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

// monthly_history/{uid}_{yyyyMM}: one month's totals, written in the same batch as the daily doc on add/edit/delete.
// Insights reads these for ranges over 31 days instead of every daily doc. Amounts are positive cents.
@Keep
data class MonthlyHistoryModel(
    @get:Exclude //excludes this from getting stored in firebase
    val docId: String = "",
    val userId: String = "",
    // Start of the 1st day of the month (device time zone)
    val month: Timestamp = Timestamp.now(),
    val expenseByCategory: Map<String, Long> = emptyMap(),
    val allowanceByCategory: Map<String, Long> = emptyMap(),
    val countByCategory: Map<String, Long> = emptyMap(),
    // Category -> "MON".."SUN" -> spent on that weekday this month
    val weekdayExpenseByCategory: Map<String, Map<String, Long>> = emptyMap(),
)
