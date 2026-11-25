package com.gmail.vondenuelle.denuspend.domain.models.budget

import com.gmail.vondenuelle.denuspend.domain.models.wallet.RecurringChargeModel
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletModel
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletMovementModel
import com.gmail.vondenuelle.denuspend.utils.RecurringChargeHelper
import java.time.LocalDate

// Budget's "Bills & subscriptions" card: what the auto-charge posted to cards in the range. Never part of "Spent".
data class BudgetBillsModel(
    val hasCards: Boolean = false, // no cards and nothing charged = the section is hidden
    val total: Long = 0L,
    val chargeCount: Int = 0,
    val cardCount: Int = 0,
    val rows: List<BillRow> = emptyList(),
    val nextUp: NextBill? = null,
)

// One subscription or installment and everything it charged in the range
data class BillRow(
    val walletId: String,
    val cardName: String, // "" = the card was closed
    val name: String,
    val total: Long,
    val count: Int,
    val isInstallment: Boolean,
    val lastNote: String, // "6 of 24" on installments
    val schedule: RecurringChargeModel?, // null = the schedule was deleted
    val nextDate: LocalDate?, // active ones on a card that still exists
)

data class NextBill(val name: String, val cardName: String, val amount: Long, val date: LocalDate)

// Grouped by the schedule each charge came from. "Next up" is only worked out when the range reaches today.
fun buildBudgetBills(
    charges: List<WalletMovementModel>,
    wallets: List<WalletModel>,
    schedules: List<RecurringChargeModel>,
    includesToday: Boolean,
): BudgetBillsModel {
    val walletsById = wallets.associateBy { it.docId }
    val schedulesById = schedules.associateBy { it.docId }

    val rows = charges.groupBy { it.recurringId.ifEmpty { it.title } }.values.map { list ->
        val latest = list.maxBy { it.date }
        val schedule = schedulesById[latest.recurringId]
        val card = walletsById[latest.walletId]
        BillRow(
            walletId = latest.walletId,
            cardName = card?.name ?: "",
            name = schedule?.name ?: latest.title,
            total = -list.sumOf { it.amount },
            count = list.size,
            isInstallment = latest.presetKey == WalletMovementModel.PRESET_INSTALLMENT,
            lastNote = latest.note,
            schedule = schedule,
            nextDate = if (schedule != null && schedule.active && card != null) RecurringChargeHelper.nextDate(schedule, card.statementDay) else null,
        )
    }.sortedByDescending { it.total }

    val nextUp = if (!includesToday) null else schedules.filter { it.active }.mapNotNull { schedule ->
        val card = walletsById[schedule.walletId] ?: return@mapNotNull null
        NextBill(
            name = schedule.name,
            cardName = card.name,
            amount = RecurringChargeHelper.amountAt(schedule, schedule.chargeCount),
            date = RecurringChargeHelper.nextDate(schedule, card.statementDay),
        )
    }.minByOrNull { it.date.toEpochDay() }

    return BudgetBillsModel(
        hasCards = wallets.any { it.isCreditCard },
        total = rows.sumOf { it.total },
        chargeCount = charges.size,
        cardCount = charges.map { it.walletId }.distinct().size,
        rows = rows,
        nextUp = nextUp,
    )
}
