package com.gmail.vondenuelle.denuspend.domain.models.transaction

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

@Keep
data class DailyHistoryModel(
    @get:Exclude //excludes this from getting stored in firebase
    val docId: String = "",
    val userId : String = "",
    val date : Timestamp = Timestamp.now(),
    val totalExpense : Long = 0L,
    val totalIncome : Long = 0L,
    // Per category, positive cents (counts = number of expenses). Budget adds these up per day instead of reading every transaction.
    // Empty on days saved before these fields existed.
    val expenseByCategory : Map<String, Long> = emptyMap(),
    val allowanceByCategory : Map<String, Long> = emptyMap(),
    val countByCategory : Map<String, Long> = emptyMap(),
    val inMonthly : Boolean = false, // read only by edit/delete in the service; here so toObject() stops warning about it
    )
