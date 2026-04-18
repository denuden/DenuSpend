package com.gmail.vondenuelle.denuspend.data.remote.models.budget.request

import com.google.firebase.Timestamp

data class GetBudgetSummaryRequest(
    val startDate: Timestamp,
    val endDate: Timestamp
)