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
    // 1.7.0: spent per wallet id (expenses only), for Budget's "By wallet"
    val expenseByWallet: Map<String, Long> = emptyMap(),
    // 1.7.0: category -> day of month ("01".."31") -> DayMark (ok / over / unplanned). Set (not added) on every save of that day,
    // so long ranges can count bad days without reading every daily doc. Months before 1.7.0 get it from a one-time backfill.
    val dayStatusByCategory: Map<String, Map<String, String>> = emptyMap(),
)

// How one category did on one day. Spending with no allowance is "unplanned", not "over": no limit was broken,
// but the money wasn't planned either. Both count as a bad day.
object DayMark {
    const val OK = "ok"
    const val OVER = "over"
    const val UNPLANNED = "unplanned"

    fun of(spent: Long, allowance: Long): String = when {
        spent <= 0L -> OK
        allowance <= 0L -> UNPLANNED
        spent > allowance -> OVER
        else -> OK
    }
}
