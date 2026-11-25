package com.gmail.vondenuelle.denuspend.data.remote.services.wallet

import com.gmail.vondenuelle.denuspend.data.remote.error.NoUserException
import com.gmail.vondenuelle.denuspend.data.remote.models.wallet.request.WalletMovementRequest
import com.gmail.vondenuelle.denuspend.data.remote.models.wallet.request.WalletRequest
import com.gmail.vondenuelle.denuspend.data.remote.services.transaction.FirebaseTransactionServiceImpl.Companion.DATE
import com.gmail.vondenuelle.denuspend.data.remote.services.transaction.FirebaseTransactionServiceImpl.Companion.TRANSACTION
import com.gmail.vondenuelle.denuspend.data.remote.services.transaction.FirebaseTransactionServiceImpl.Companion.USER_ID
import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionModel
import com.gmail.vondenuelle.denuspend.domain.models.wallet.RecurringChargeModel
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletModel
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletMovementModel
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.utils.CardStatementHelper
import com.gmail.vondenuelle.denuspend.utils.RecurringChargeHelper
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Transaction
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.CancellationException
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
        const val PRESET_KEY = "presetKey"
        const val RECURRING_CHARGES = "recurring_charges"
        private const val MAX_CHARGES_PER_OPEN = 60

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

    // One get() of the physical wallet (usually from the cache, since the Wallet tab listens to it). Any failure is just "unknown".
    override suspend fun getTrackingStart(): Timestamp? {
        val currentUser = firebaseAuth.currentUser ?: return null
        return try {
            firebaseFireStore.collection(WALLETS).document(physicalWalletId(currentUser.uid)).get().await()
                .toObject<WalletModel>()?.createdAt
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
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

    // A handful per user, so all of them are read with one userId filter and split per card on the device (no composite index)
    override fun getRecurringCharges(): Flow<List<RecurringChargeModel>> = callbackFlow {
        val currentUser = firebaseAuth.currentUser
            ?: throw NoUserException("User not logged in")

        val listener = firebaseFireStore.collection(RECURRING_CHARGES)
            .whereEqualTo(USER_ID, currentUser.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val data = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject<RecurringChargeModel>()?.copy(docId = doc.id)
                } ?: emptyList()
                trySend(data.sortedBy { it.createdAt })
            }

        awaitClose { listener.remove() }
    }

    // Needs a composite index: wallet_movements (userId ASC, presetKey ASC, date ASC). The first run logs a link that creates it.
    // Reads only the posted charges, so a deleted charge or schedule and a closed card still add up right.
    override fun getBillCharges(start: Timestamp, end: Timestamp): Flow<List<WalletMovementModel>> = callbackFlow {
        val currentUser = firebaseAuth.currentUser
            ?: throw NoUserException("User not logged in")

        val listener = firebaseFireStore.collection(WALLET_MOVEMENTS)
            .whereEqualTo(USER_ID, currentUser.uid)
            .whereIn(PRESET_KEY, listOf(WalletMovementModel.PRESET_SUBSCRIPTION, WalletMovementModel.PRESET_INSTALLMENT))
            .whereGreaterThanOrEqualTo(DATE, start)
            .whereLessThanOrEqualTo(DATE, end)
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

    // New (empty docId) = the whole doc. An edit only renames it: the schedule is fixed once saved, so an edit can never
    // make the auto-charge post a date twice or skip one (to change it, delete it and add it again).
    override suspend fun saveRecurringCharge(charge: RecurringChargeModel) {
        val currentUser = firebaseAuth.currentUser
            ?: throw NoUserException("User not logged in")

        if (charge.docId.isEmpty()) {
            firebaseFireStore.collection(RECURRING_CHARGES).document()
                .set(charge.copy(userId = currentUser.uid, createdAt = Timestamp.now())).awaitWrite(context)
        } else {
            firebaseFireStore.collection(RECURRING_CHARGES).document(charge.docId).update("name", charge.name).awaitWrite(context)
        }
    }

    // Charges it already posted stay on the card (they're real charges); only the schedule goes
    override suspend fun deleteRecurringCharge(docId: String) {
        firebaseFireStore.collection(RECURRING_CHARGES).document(docId).delete().awaitWrite(context)
    }

    /**
     * The auto-charge, on app open. Each due subscription / installment gets its own transaction that re-reads the doc, posts every date
     * from its next one up to today (dated on the due day, so missed ones land on the right statement), moves the card's balance and
     * moves chargeCount forward, all in one commit. Two phones opening at once can't both post: Firestore retries the second
     * transaction, which then sees the new chargeCount and finds nothing due. The fixed movement id "{id}_{yyyyMMdd}" plus the
     * wallet_movements "update: if false" rule is the backstop. Offline, the transactions fail and nothing is charged until next time.
     */
    override suspend fun chargeDueRecurring(today: LocalDate): List<Pair<String, String>> {
        val currentUser = firebaseAuth.currentUser
            ?: throw NoUserException("User not logged in")

        val startOfTomorrow = CardStatementHelper.startOfDay(today.plusDays(1))
        val thisMonth = YearMonth.from(today)
        val due = firebaseFireStore.collection(RECURRING_CHARGES)
            .whereEqualTo(USER_ID, currentUser.uid)
            .get().await()
            .documents.mapNotNull { doc -> doc.toObject<RecurringChargeModel>()?.copy(docId = doc.id) }
            .filter { charge ->
                // An installment's stored date has the statement day from when it was last charged, and the card's day can be edited
                // since. Its month never changes, so it's checked by month; the transaction works out the exact day (nothing if not yet).
                charge.active && if (charge.isInstallment) !YearMonth.from(CardStatementHelper.toLocalDate(charge.nextChargeDate)).isAfter(thisMonth)
                else charge.nextChargeDate < startOfTomorrow
            }

        val charged = mutableListOf<Pair<String, String>>()
        for (charge in due) {
            val result = try {
                firebaseFireStore.runTransaction { tx -> chargeInTransaction(tx, charge.docId, currentUser.uid, today) }.await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: FirebaseFirestoreException) {
                // Offline: stop, the rest is charged on a later open. Anything else only skips this one.
                if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) break else continue
            } catch (e: Exception) {
                continue
            }
            result?.let { charged += it }
        }
        return charged
    }

    // Returns (card name, charge name) when something was posted. All reads happen before any write, as transactions require.
    private fun chargeInTransaction(tx: Transaction, chargeId: String, uid: String, today: LocalDate): Pair<String, String>? {
        val chargeRef = firebaseFireStore.collection(RECURRING_CHARGES).document(chargeId)
        val charge = tx.get(chargeRef).toObject<RecurringChargeModel>()?.copy(docId = chargeId) ?: return null
        if (!charge.active) return null
        val cardRef = walletRef(charge.walletId)
        val card = tx.get(cardRef).toObject<WalletModel>()

        // The card was closed: stop the schedule instead of charging nothing forever
        if (card == null) {
            tx.update(chargeRef, mapOf("active" to false, "endedReason" to RecurringChargeModel.ENDED_WALLET_CLOSED))
            return null
        }

        // Every date from the next one up to today. Capped per open, so a long-missed daily one can't overflow the transaction.
        val limit = minOf(MAX_CHARGES_PER_OPEN, if (charge.isInstallment) RecurringChargeHelper.monthsLeft(charge) else MAX_CHARGES_PER_OPEN)
        val ks = (charge.chargeCount until charge.chargeCount + limit)
            .takeWhile { k -> !RecurringChargeHelper.dateAt(charge, k, card.statementDay).isAfter(today) }
        if (ks.isEmpty()) return null

        var total = 0L
        ks.forEach { k ->
            val date = RecurringChargeHelper.dateAt(charge, k, card.statementDay)
            val amount = RecurringChargeHelper.amountAt(charge, k)
            total += amount
            tx.set(
                firebaseFireStore.collection(WALLET_MOVEMENTS).document("${chargeId}_${date.format(DateTimeFormatter.BASIC_ISO_DATE)}"),
                WalletMovementModel(
                    userId = uid,
                    walletId = charge.walletId,
                    amount = -amount,
                    title = charge.name,
                    presetKey = if (charge.isInstallment) WalletMovementModel.PRESET_INSTALLMENT else WalletMovementModel.PRESET_SUBSCRIPTION,
                    // "5 of 24" on the statement for installments
                    note = if (charge.isInstallment) context.getString(R.string.wallet_lbl_installment_of, charge.paidBefore + k + 1, charge.totalCount) else "",
                    date = CardStatementHelper.startOfDay(date),
                    recurringId = chargeId,
                )
            )
        }
        tx.update(cardRef, BALANCE, FieldValue.increment(-total))

        val newCount = ks.last() + 1
        val updated = charge.copy(chargeCount = newCount)
        val isPaidOff = charge.isInstallment && RecurringChargeHelper.monthsLeft(updated) == 0
        tx.update(
            chargeRef,
            mapOf(
                "chargeCount" to newCount,
                "nextChargeDate" to CardStatementHelper.startOfDay(RecurringChargeHelper.nextDate(updated, card.statementDay)),
                "active" to !isPaidOff,
                "endedReason" to if (isPaidOff) RecurringChargeModel.ENDED_PAID_OFF else "",
            )
        )
        return card.name to charge.name
    }
}
