package com.gmail.vondenuelle.denuspend.domain.models.budget

data class BudgetTotalSummaryModel(
    val food : Long = 0L,
    val foodCount : Int = 0,
    val entertainment : Long = 0L,
    val entertainmentCount : Int = 0,
    val household : Long = 0L,
    val householdCount : Int = 0,
    val transportation : Long = 0L,
    val transportationCount : Int = 0,
    val workEducation : Long = 0L,
    val workEducationCount : Int = 0,
    val healthcare : Long = 0L,
    val healthcareCount : Int = 0,
    val personal : Long = 0L,
    val personalCount : Int = 0,
    val family : Long = 0L,
    val familyCount : Int = 0,
    val others : Long = 0L,
    val othersCount : Int = 0,

    val totalExpense : Long = 0L
)
