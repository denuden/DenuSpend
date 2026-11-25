package com.gmail.vondenuelle.denuspend.data.remote.services.wallet

import com.gmail.vondenuelle.denuspend.data.remote.models.wallet.request.WalletMovementRequest
import com.gmail.vondenuelle.denuspend.data.remote.models.wallet.request.WalletRequest
import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionModel
import com.gmail.vondenuelle.denuspend.domain.models.wallet.RecurringChargeModel
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletModel
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletMovementModel
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface WalletService {
    suspend fun needsWalletSetup(): Boolean
    // The day tracking started: when the physical wallet was set up. null when it can't be read (offline, no setup yet)
    suspend fun getTrackingStart(): Timestamp?
    suspend fun savePhysicalWallet(request: WalletRequest)
    suspend fun saveWallet(request: WalletRequest)
    suspend fun closeWallet(docId: String)
    fun getWallets(): Flow<List<WalletModel>>
    fun getWalletTransactions(walletId: String, limit: Long): Flow<List<TransactionModel>>

    // Money in / out that isn't an expense (wallet_movements)
    suspend fun addMovement(request: WalletMovementRequest)
    suspend fun deleteMovement(movement: WalletMovementModel)
    fun getWalletMovements(walletId: String, limit: Long): Flow<List<WalletMovementModel>>

    // Credit card statements: expenses + money in/out on one wallet from `since` on
    fun getWalletActivitySince(walletId: String, since: Timestamp): Flow<Pair<List<TransactionModel>, List<WalletMovementModel>>>

    // Credit cards: subscriptions and installments (recurring_charges)
    fun getRecurringCharges(): Flow<List<RecurringChargeModel>>
    // Budget's "Bills & subscriptions": the auto charges posted to any card between start and end
    fun getBillCharges(start: Timestamp, end: Timestamp): Flow<List<WalletMovementModel>>
    suspend fun saveRecurringCharge(charge: RecurringChargeModel)
    suspend fun deleteRecurringCharge(docId: String)
    // Auto-charge on app open: posts every due date and returns (card name, charge name) for each charge it posted
    suspend fun chargeDueRecurring(today: LocalDate): List<Pair<String, String>>
}
