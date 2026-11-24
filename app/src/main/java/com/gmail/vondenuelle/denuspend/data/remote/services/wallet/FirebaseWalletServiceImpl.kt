package com.gmail.vondenuelle.denuspend.data.remote.services.wallet

import com.gmail.vondenuelle.denuspend.data.remote.error.NoUserException
import com.gmail.vondenuelle.denuspend.data.remote.models.wallet.request.WalletMovementRequest
import com.gmail.vondenuelle.denuspend.data.remote.models.wallet.request.WalletRequest
import com.gmail.vondenuelle.denuspend.data.remote.services.transaction.FirebaseTransactionServiceImpl.Companion.DATE
import com.gmail.vondenuelle.denuspend.data.remote.services.transaction.FirebaseTransactionServiceImpl.Companion.TRANSACTION
import com.gmail.vondenuelle.denuspend.data.remote.services.transaction.FirebaseTransactionServiceImpl.Companion.USER_ID
import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionModel
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletModel
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletMovementModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import com.gmail.vondenuelle.denuspend.utils.network.awaitWrite
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class FirebaseWalletServiceImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firebaseFireStore: FirebaseFirestore,
    @ApplicationContext private val context: Context, // only to check the connection before waiting on a write
) : WalletService {
    companion object {
        const val WALLETS = "wallets"

        //Fields
        const val NAME = "name"
        const val COLOR_INDEX = "colorIndex"
        const val ICON_KEY = "iconKey"
        const val BALANCE = "balance"
        const val CREATED_AT = "createdAt"
        const val CREDIT_LIMIT = "creditLimit"
        const val STATEMENT_DAY = "statementDay"
        const val WALLET_ID = "walletId" // on transactions and wallet_movements
        const val WALLET_MOVEMENTS = "wallet_movements"

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
        firebaseFireStore.collection(WALLETS).document(physicalWalletId(currentUser.uid)).set(wallet).awaitWrite(context)
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
                createdAt = Timestamp.now(),
                type = request.type,
                creditLimit = request.creditLimit,
                statementDay = request.statementDay,
            )
            firebaseFireStore.collection(WALLETS).document().set(wallet).awaitWrite(context)
        } else {
            // The balance moves by what the user changed since the sheet opened, so an expense saved meanwhile isn't overwritten.
            // That change is also logged as a "Balance correction" entry, in the same batch.
            // The type (wallet or card) is set when the wallet is added and never written here, so a card's history always starts as a card.
            val delta = request.balance - request.originalBalance
            val fields = mutableMapOf<String, Any>(
                NAME to request.name,
                COLOR_INDEX to request.colorIndex,
                ICON_KEY to request.iconKey,
                CREDIT_LIMIT to request.creditLimit,
                STATEMENT_DAY to request.statementDay,
            )
            val batch = firebaseFireStore.batch()
            if (delta != 0L) {
                fields[BALANCE] = FieldValue.increment(delta)
                batch.set(
                    firebaseFireStore.collection(WALLET_MOVEMENTS).document(),
                    WalletMovementModel(
                        userId = currentUser.uid,
                        walletId = request.docId,
                        amount = delta,
                        title = request.correctionTitle,
                        presetKey = WalletMovementModel.PRESET_CORRECTION,
                        date = Timestamp.now()
                    )
                )
            }
            batch.update(walletRef(request.docId), fields)
            batch.commit().awaitWrite(context)
        }
    }

    // One batch with the wallet's increment, like expenses. A wallet closed meanwhile fails the whole batch (NOT_FOUND), so nothing half-saves.
    // A transfer is two docs, "{transferId}_out" and "{transferId}_in", so each wallet's list stays a single query.
    override suspend fun addMovement(request: WalletMovementRequest) {
        val currentUser = firebaseAuth.currentUser
            ?: throw NoUserException("User not logged in")

        val movements = firebaseFireStore.collection(WALLET_MOVEMENTS)
        val now = Timestamp.now()
        val base = WalletMovementModel(
            userId = currentUser.uid,
            walletId = request.walletId,
            amount = request.amount,
            title = request.title,
            presetKey = request.presetKey,
            note = request.note,
            date = now
        )
        val batch = firebaseFireStore.batch()

        if (request.toWalletId.isEmpty()) {
            batch.set(movements.document(), base)
            batch.update(walletRef(request.walletId), BALANCE, FieldValue.increment(request.amount))
        } else {
            val transferId = movements.document().id
            val moved = abs(request.amount)
            batch.set(
                movements.document("${transferId}_out"),
                base.copy(amount = -moved, otherWalletId = request.toWalletId, transferId = transferId)
            )
            batch.set(
                movements.document("${transferId}_in"),
                base.copy(walletId = request.toWalletId, amount = moved, otherWalletId = request.walletId, transferId = transferId)
            )
            batch.update(walletRef(request.walletId), BALANCE, FieldValue.increment(-moved))
            batch.update(walletRef(request.toWalletId), BALANCE, FieldValue.increment(moved))
        }
        batch.commit().awaitWrite(context)
    }

    // Puts the money back. A transfer deletes both halves. A wallet that was closed since is skipped (one get() each), like expenses.
    override suspend fun deleteMovement(movement: WalletMovementModel) {
        firebaseAuth.currentUser ?: throw NoUserException("User not logged in")

        val movements = firebaseFireStore.collection(WALLET_MOVEMENTS)
        // (walletId, amount it added) for each side to reverse
        val sides = if (movement.isTransfer) {
            listOf(movement.walletId to movement.amount, movement.otherWalletId to -movement.amount)
        } else {
            listOf(movement.walletId to movement.amount)
        }

        val batch = firebaseFireStore.batch()
        if (movement.isTransfer) {
            batch.delete(movements.document("${movement.transferId}_out"))
            batch.delete(movements.document("${movement.transferId}_in"))
        } else {
            batch.delete(movements.document(movement.docId))
        }
        sides.forEach { (walletId, amount) ->
            if (walletRef(walletId).get().await().exists()) {
                batch.update(walletRef(walletId), BALANCE, FieldValue.increment(-amount))
            }
        }
        batch.commit().awaitWrite(context)
    }

    // Needs a composite index: wallet_movements (userId ASC, walletId ASC, date DESC). The first run logs a link that creates it.
    override fun getWalletMovements(walletId: String, limit: Long): Flow<List<WalletMovementModel>> = callbackFlow {
        val currentUser = firebaseAuth.currentUser
            ?: throw NoUserException("User not logged in")

        val listener = firebaseFireStore.collection(WALLET_MOVEMENTS)
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
                    doc.toObject<WalletMovementModel>()?.copy(docId = doc.id)
                } ?: emptyList()

                trySend(data)
            }

        awaitClose { listener.remove() }
    }

    // Everything on one wallet dated on or after `since`, from both collections, for the card statements. No limit, since it's a month or a few of one card.
    // Uses the same (userId, walletId, date DESC) indexes as the two lists, so no new index.
    override fun getWalletActivitySince(walletId: String, since: Timestamp): Flow<Pair<List<TransactionModel>, List<WalletMovementModel>>> = flow {
        val currentUser = firebaseAuth.currentUser
            ?: throw NoUserException("User not logged in")

        val transactions = rangeListener(TRANSACTION, currentUser.uid, walletId, since) { doc ->
            doc.toObject<TransactionModel>()?.copy(docId = doc.id)
        }
        val movements = rangeListener(WALLET_MOVEMENTS, currentUser.uid, walletId, since) { doc ->
            doc.toObject<WalletMovementModel>()?.copy(docId = doc.id)
        }
        emitAll(combine(transactions, movements) { t, m -> t to m })
    }

    private fun <T> rangeListener(
        collection: String,
        uid: String,
        walletId: String,
        since: Timestamp,
        map: (DocumentSnapshot) -> T?,
    ): Flow<List<T>> = callbackFlow {
        val listener = firebaseFireStore.collection(collection)
            .whereEqualTo(USER_ID, uid)
            .whereEqualTo(WALLET_ID, walletId)
            .whereGreaterThanOrEqualTo(DATE, since)
            .orderBy(DATE, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.mapNotNull(map) ?: emptyList())
            }

        awaitClose { listener.remove() }
    }

    private fun walletRef(walletId: String) = firebaseFireStore.collection(WALLETS).document(walletId)

    // Only the wallet doc goes. Transactions keep their walletId; edit/delete skip the balance math for a wallet that's gone.
    override suspend fun closeWallet(docId: String) {
        val currentUser = firebaseAuth.currentUser
            ?: throw NoUserException("User not logged in")
        if (docId == physicalWalletId(currentUser.uid)) throw IllegalArgumentException("The physical wallet can't be closed")

        firebaseFireStore.collection(WALLETS).document(docId).delete().awaitWrite(context)
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
