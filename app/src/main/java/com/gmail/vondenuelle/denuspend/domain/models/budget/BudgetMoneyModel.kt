package com.gmail.vondenuelle.denuspend.domain.models.budget

import com.gmail.vondenuelle.denuspend.domain.models.wallet.RecurringChargeModel
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletModel
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletMovementModel

// Budget's real money in and out from the wallet history (not the allowance). Positive cents.
// Expenses aren't in here: they come from the daily/monthly docs (BudgetTotalSummaryModel).
data class BudgetMoneyModel(
    val bills: BudgetBillsModel = BudgetBillsModel(),
    // Money in: salary, gifts, refunds, cashback, typed "Add money"…
    val moneyIn: Long = 0L,
    val moneyInCount: Int = 0,
    // The biggest money-in entry, named on the Money in card
    val biggestIn: WalletMovementModel? = null,
    // Money out that isn't an expense or a card bill: fees, emergencies, money lent, typed "Take out"…
    val otherOut: Long = 0L,
    // Bills + other money out per wallet id (expenses are added from the summary)
    val outByWallet: Map<String, Long> = emptyMap(),
    val wallets: List<WalletModel> = emptyList(),
)

fun buildBudgetMoney(
    movements: List<WalletMovementModel>,
    wallets: List<WalletModel>,
    schedules: List<RecurringChargeModel>,
    includesToday: Boolean,
): BudgetMoneyModel {
    // The query already leaves transfers out by preset, but a transfer whose title was retyped has presetKey "", so check here too
    val real = movements.filter { !it.isTransfer }
    val billKeys = setOf(WalletMovementModel.PRESET_SUBSCRIPTION, WalletMovementModel.PRESET_INSTALLMENT)
    val bills = real.filter { it.presetKey in billKeys }
    val ins = real.filter { it.amount > 0 }
    val outs = real.filter { it.amount < 0 }
    return BudgetMoneyModel(
        bills = buildBudgetBills(bills, wallets, schedules, includesToday),
        moneyIn = ins.sumOf { it.amount },
        moneyInCount = ins.size,
        biggestIn = ins.maxByOrNull { it.amount },
        otherOut = -outs.filter { it.presetKey !in billKeys }.sumOf { it.amount },
        outByWallet = outs.groupBy { it.walletId }.mapValues { (_, list) -> -list.sumOf { it.amount } },
        wallets = wallets,
    )
}
