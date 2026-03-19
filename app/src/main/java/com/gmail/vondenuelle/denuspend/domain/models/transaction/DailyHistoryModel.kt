package com.gmail.vondenuelle.denuspend.domain.models.transaction

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

data class DailyHistoryModel(
    @get:Exclude //excludes this from getting stored in firebase
    val docId: String = "",
    val userId : String = "",
    val date : Timestamp = Timestamp.now(),
    val expense : Long = 0L,
    val totalBudget : Long = 0L
    )
