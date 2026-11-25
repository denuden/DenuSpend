package com.gmail.vondenuelle.denuspend.domain.models.wallet

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

/**
 * A subscription or an installment on a credit card (recurring_charges). Both are an amount that repeats on a schedule;
 * an installment stops after totalCount months and is charged on the card's statement day.
 * Date k of the schedule = startDate + k cycles (see RecurringChargeHelper), so a Jan 31 start goes Feb 28 → Mar 31.
 */
@Keep
data class RecurringChargeModel(
    @get:Exclude
    var docId : String = "",
    val userId : String = "",
    val walletId : String = "",
    val kind : String = KIND_SUBSCRIPTION,
    val name : String = "",
    val amount : Long = 0L, // cents per charge. Installments: the regular month (the last one absorbs the rounding)
    val repeatEvery : Int = 1,
    val repeatUnit : String = UNIT_MONTH, // installments are always 1 month
    val startDate : Timestamp = Timestamp.now(), // the schedule's anchor (an installment's first remaining month)
    val chargeCount : Int = 0, // dates already used up: charged, or before the day it was added (subscriptions are never back-charged)
    val totalCount : Int = 0, // installments: months in total (24). 0 = never ends
    val paidBefore : Int = 0, // installments: months already paid before it was added (4). Never charged, they're already in "Amount owed"
    val totalAmount : Long = 0L, // installments: the whole price with interest (₱36,000)
    val nextChargeDate : Timestamp = Timestamp.now(), // stored so the auto-charge can find what's due without working it out per doc
    val active : Boolean = true, // false = paused, paid off or its card was closed
    val endedReason : String = "", // "" (paused by the user), PAID_OFF or WALLET_CLOSED
    val createdAt : Timestamp = Timestamp.now(),
) {
    @get:Exclude
    val isInstallment : Boolean
        get() = kind == KIND_INSTALLMENT

    companion object {
        const val KIND_SUBSCRIPTION = "subscription"
        const val KIND_INSTALLMENT = "installment"

        const val UNIT_DAY = "day"
        const val UNIT_WEEK = "week"
        const val UNIT_MONTH = "month"
        const val UNIT_YEAR = "year"

        const val ENDED_PAID_OFF = "paid_off"
        const val ENDED_WALLET_CLOSED = "wallet_closed"
    }
}
