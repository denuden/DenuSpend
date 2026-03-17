package com.gmail.vondenuelle.denuspend.ui.add

data class AddScreenState(
    val isLoading : Boolean = false,

//    Income Screen
    val transactionTitle : String = "",
    val transactionDescription : String = "",
    val transactionCategory : String = "",
    val transactionAmount : Long = 0L,

    )
