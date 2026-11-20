package com.gmail.vondenuelle.denuspend.domain.models.budget

import androidx.annotation.Keep

// Keyed by category name (same names as CategoryDropdownButtonModel.list). Amounts are positive cents.
// totals/counts are expenses, allowances are the positive (allowance) transactions.
@Keep
data class BudgetTotalSummaryModel(
    val totals : Map<String, Long> = emptyMap(),
    val counts : Map<String, Int> = emptyMap(),
    val totalExpense : Long = 0L,
    val allowances : Map<String, Long> = emptyMap(),
    val totalAllowance : Long = 0L,
)
