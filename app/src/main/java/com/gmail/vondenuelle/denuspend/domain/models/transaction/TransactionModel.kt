package com.gmail.vondenuelle.denuspend.domain.models.transaction

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

@Keep
data class TransactionModel(
    @get:Exclude //excludes this from getting stored in firebase
    var docId : String = "",
    var userId : String = "",
    val title : String = "",
    val description : String = "---", //optional
    val date: Timestamp = Timestamp.Companion.now(),
    val amount : Long = 0L,
    val category : String = "",
)