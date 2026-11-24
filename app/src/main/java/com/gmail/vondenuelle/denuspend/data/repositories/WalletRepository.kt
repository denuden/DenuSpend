package com.gmail.vondenuelle.denuspend.data.repositories

import com.gmail.vondenuelle.denuspend.data.remote.error.CannotCreateTransactionException
import com.gmail.vondenuelle.denuspend.data.remote.models.wallet.request.WalletMovementRequest
import com.gmail.vondenuelle.denuspend.data.remote.models.wallet.request.WalletRequest
import com.gmail.vondenuelle.denuspend.data.remote.services.wallet.WalletService
import com.gmail.vondenuelle.denuspend.di.qualifiers.FirebaseWallet
import com.gmail.vondenuelle.denuspend.di.qualifiers.IoDispatcher
import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionModel
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletModel
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletMovementModel
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineDispatcher
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

@ViewModelScoped
class WalletRepository @Inject constructor(
    @FirebaseWallet private val walletService: WalletService,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {
    suspend fun needsWalletSetup(): Boolean = walletService.needsWalletSetup()

    suspend fun savePhysicalWallet(request: WalletRequest) {
        walletService.savePhysicalWallet(validate(request))
    }

    suspend fun saveWallet(request: WalletRequest) {
        walletService.saveWallet(validate(request))
    }

    suspend fun closeWallet(docId: String) {
        walletService.closeWallet(docId)
    }

    fun getWallets(): Flow<List<WalletModel>> {
        return walletService.getWallets()
            .flowOn(dispatcher)
    }

    fun getWalletTransactions(walletId: String, limit: Long): Flow<List<TransactionModel>> {
        return walletService.getWalletTransactions(walletId, limit)
            .flowOn(dispatcher)
    }

    suspend fun addMovement(request: WalletMovementRequest) {
        if (request.amount == 0L) throw CannotCreateTransactionException("Enter an amount")
        if (request.title.isBlank()) throw CannotCreateTransactionException("Give it a title")
        if (request.toWalletId == request.walletId) throw CannotCreateTransactionException("Pick a different wallet to move the money to")
        walletService.addMovement(request.copy(title = request.title.trim(), note = request.note.trim()))
    }

    suspend fun deleteMovement(movement: WalletMovementModel) {
        walletService.deleteMovement(movement)
    }

    fun getWalletMovements(walletId: String, limit: Long): Flow<List<WalletMovementModel>> {
        return walletService.getWalletMovements(walletId, limit)
            .flowOn(dispatcher)
    }

    fun getWalletActivitySince(walletId: String, since: Timestamp): Flow<Pair<List<TransactionModel>, List<WalletMovementModel>>> {
        return walletService.getWalletActivitySince(walletId, since)
            .flowOn(dispatcher)
    }

    // A balance of ₱0 or below is fine (an empty pocket, or a card that's owed). The name is required, and a card needs its statement day.
    private fun validate(request: WalletRequest): WalletRequest {
        if (request.name.isBlank()) {
            throw CannotCreateTransactionException("Give the wallet a name")
        }
        if (request.type == WalletModel.TYPE_CREDIT_CARD && request.statementDay !in 1..31) {
            throw CannotCreateTransactionException("Pick the card's statement day")
        }
        return request.copy(name = request.name.trim())
    }
}
