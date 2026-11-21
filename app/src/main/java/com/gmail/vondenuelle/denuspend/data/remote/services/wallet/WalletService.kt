package com.gmail.vondenuelle.denuspend.data.remote.services.wallet

import com.gmail.vondenuelle.denuspend.data.remote.models.wallet.request.WalletRequest
import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionModel
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletModel
import kotlinx.coroutines.flow.Flow

interface WalletService {
    suspend fun needsWalletSetup(): Boolean
    suspend fun savePhysicalWallet(request: WalletRequest)
    suspend fun saveWallet(request: WalletRequest)
    suspend fun closeWallet(docId: String)
    fun getWallets(): Flow<List<WalletModel>>
    fun getWalletTransactions(walletId: String, limit: Long): Flow<List<TransactionModel>>
}
