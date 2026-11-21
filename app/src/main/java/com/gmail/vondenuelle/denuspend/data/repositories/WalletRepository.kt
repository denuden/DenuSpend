package com.gmail.vondenuelle.denuspend.data.repositories

import com.gmail.vondenuelle.denuspend.data.remote.error.CannotCreateTransactionException
import com.gmail.vondenuelle.denuspend.data.remote.models.wallet.request.WalletRequest
import com.gmail.vondenuelle.denuspend.data.remote.services.wallet.WalletService
import com.gmail.vondenuelle.denuspend.di.qualifiers.FirebaseWallet
import com.gmail.vondenuelle.denuspend.di.qualifiers.IoDispatcher
import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionModel
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletModel
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineDispatcher
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

    // A balance of ₱0 or below is fine (an empty pocket, or a card that's owed), only the name is required
    private fun validate(request: WalletRequest): WalletRequest {
        if (request.name.isBlank()) {
            throw CannotCreateTransactionException("Give the wallet a name")
        }
        return request.copy(name = request.name.trim())
    }
}
