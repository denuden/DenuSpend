package com.gmail.vondenuelle.denuspend.domain.models

import com.google.firebase.firestore.Exclude

data class TransactionModel(
    @get:Exclude //excludes this from getting stored in firebase
    var docId : String? = null,
    val title : String,
    val description : String,
    val amount : Long = 0L,
    val category : String,
)