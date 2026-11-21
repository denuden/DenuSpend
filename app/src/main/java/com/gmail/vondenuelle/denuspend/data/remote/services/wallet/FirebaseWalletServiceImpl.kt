package com.gmail.vondenuelle.denuspend.data.remote.services.wallet

import com.gmail.vondenuelle.denuspend.data.remote.error.NoUserException
import com.gmail.vondenuelle.denuspend.data.remote.models.wallet.request.WalletRequest
import com.gmail.vondenuelle.denuspend.data.remote.services.transaction.FirebaseTransactionServiceImpl.Companion.DATE
import com.gmail.vondenuelle.denuspend.data.remote.services.transaction.FirebaseTransactionServiceImpl.Companion.TRANSACTION
import com.gmail.vondenuelle.denuspend.data.remote.services.transaction.FirebaseTransactionServiceImpl.Companion.USER_ID
import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionModel
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseWalletServiceImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firebaseFireStore: FirebaseFirestore
) : WalletService {
    companion object {
        const val WALLETS = "wallets"

        //Fields
        const val NAME = "name"
        const val COLOR_INDEX = "colorIndex"
        const val ICON_KEY = "iconKey"
        const val BALANCE = "balance"
        const val CREATED_AT = "createdAt"
        const val WALLET_ID = "walletId" // on transactions

        fun physicalWalletId(uid: String) = "$uid${WalletModel.PHYSICAL_SUFFIX}"
    }

    // The physical wallet doc doubles as the "setup done" flag.
    // A "missing" answer from the offline cache isn't trusted, so an offline start never shows setup to someone who already did it
    // (saving setup again would overwrite their real balance).
    override suspend fun needsWalletSetup(): Boolean {
        val currentUser = firebaseAuth.currentUser
            ?: throw NoUserException("User not logged in")

        val doc = firebaseFireStore.collection(WALLETS).document(physicalWalletId(currentUser.uid)).get().await()
        return !doc.exists() && !doc.metadata.isFromCache
    }

    // set() on the fixed id, so pressing Next again in setup just saves over it instead of making a second physical wallet
    override suspend fun savePhysicalWallet(request: WalletRequest) {
        val currentUser = firebaseAuth.currentUser
            ?: throw NoUserException("User not logged in")

        val wallet = WalletModel(
            userId = currentUser.uid,
            name = request.name,
            colorIndex = request.colorIndex,
            iconKey = request.iconKey,
            balance = request.balance,
            createdAt = Timestamp.now()
        )
        firebaseFireStore.collection(WALLETS).document(physicalWalletId(currentUser.uid)).set(wallet).await()
    }

    // New wallet (empty docId) or an edit. An edit only touches the editable fields, so createdAt stays.
    override suspend fun saveWallet(request: WalletRequest) {
        val currentUser = firebaseAuth.currentUser
            ?: throw NoUserException("User not logged in")

        if (request.docId.isEmpty()) {
            val wallet = WalletModel(
                userId = currentUser.uid,
                name = request.name,
                colorIndex = request.colorIndex,
                iconKey = request.iconKey,
                balance = request.balance,
                createdAt = Timestamp.now()
            )
            firebaseFireStore.collection(WALLETS).document().set(wallet).await()
        } else {
            firebaseFireStore.collection(WALLETS).document(request.docId).update(
                mapOf(
                    NAME to request.name,
                    COLOR_INDEX to request.colorIndex,
                    ICON_KEY to request.iconKey,
                    BALANCE to request.balance
                )
            ).await()
        }
    }

    // Only the wallet doc goes. Transactions keep their walletId; edit/delete skip the balance math for a wallet that's gone.
    override suspend fun closeWallet(docId: String) {
        val currentUser = firebaseAuth.currentUser
            ?: throw NoUserException("User not logged in")
        if (docId == physicalWalletId(currentUser.uid)) throw IllegalArgumentException("The physical wallet can't be closed")

        firebaseFireStore.collection(WALLETS).document(docId).delete().await()
    }

    // Small list, so it's sorted on the device: physical wallet first, then oldest first (no composite index needed)
    override fun getWallets(): Flow<List<WalletModel>> = callbackFlow {
        val currentUser = firebaseAuth.currentUser
            ?: throw NoUserException("User not logged in")

        val listener = firebaseFireStore.collection(WALLETS)
            .whereEqualTo(USER_ID, currentUser.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val data = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject<WalletModel>()?.copy(docId = doc.id)
                } ?: emptyList()

                trySend(data.sortedWith(compareByDescending<WalletModel> { it.isPhysical }.thenBy { it.createdAt }))
            }

        awaitClose { listener.remove() }
    }

    // Needs a composite index: transactions (userId ASC, walletId ASC, date DESC). The first run logs a link that creates it.
    override fun getWalletTransactions(walletId: String, limit: Long): Flow<List<TransactionModel>> = callbackFlow {
        val currentUser = firebaseAuth.currentUser
            ?: throw NoUserException("User not logged in")

        val listener = firebaseFireStore.collection(TRANSACTION)
            .whereEqualTo(USER_ID, currentUser.uid)
            .whereEqualTo(WALLET_ID, walletId)
            .orderBy(DATE, Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val data = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject<TransactionModel>()?.copy(docId = doc.id)
                } ?: emptyList()

                trySend(data)
            }

        awaitClose { listener.remove() }
    }
}
