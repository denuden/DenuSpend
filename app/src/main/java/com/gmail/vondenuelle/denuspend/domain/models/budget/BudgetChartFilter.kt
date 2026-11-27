package com.gmail.vondenuelle.denuspend.domain.models.budget

import androidx.annotation.StringRes
import com.gmail.vondenuelle.denuspend.R

// The enum is the logic key and the label is only for display, so the text can live in strings.xml
enum class BudgetChartFilter(@StringRes val label: Int) {
    THIS_MONTH(R.string.budget_filter_this_month),
    LAST_3_DAYS(R.string.budget_filter_last_3_days),
    LAST_7_DAYS(R.string.budget_filter_last_7_days),
    LAST_15_DAYS(R.string.budget_filter_last_15_days),
    LAST_30_DAYS(R.string.budget_filter_last_30_days),
    LAST_3_MONTHS(R.string.budget_filter_last_3_months),
    LAST_6_MONTHS(R.string.budget_filter_last_6_months),
    THIS_YEAR(R.string.budget_filter_this_year),
    CUSTOM(R.string.budget_filter_custom), //range picked in a date range dialog
    ;

    // Whole months from the 1st, read from the monthly docs on both the dashboard and Insights
    val isWholeMonths: Boolean get() = this == LAST_3_MONTHS || this == LAST_6_MONTHS || this == THIS_YEAR
}
