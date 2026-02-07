package com.gmail.vondenuelle.denuspend.data.remote.error

import androidx.annotation.Keep

@Keep
data class ErrorModel(
    val status : Int? = null,
    val type : String? = null,
    val messages : ErrorData? = null,
    val message : String? = null,
    val error : String? = null,
    val errors : ErrorData? = null,
)

@Keep
data class ErrorData(
    var q : List<String>? = null,
    var type : List<String>? = null,
    var score : List<String>? = null,
    var max_score : List<String>? = null,
    var min_score : List<String>? = null,
    var status : List<String>? = null,
    var rating : List<String>? = null,
    var sfw : List<String>? = null,
    var genres : List<String>? = null,
    var order_by : List<String>? = null,
    var sort : List<String>? = null,
    var start_date : List<String>? = null,
    var end_date : List<String>? = null,
)