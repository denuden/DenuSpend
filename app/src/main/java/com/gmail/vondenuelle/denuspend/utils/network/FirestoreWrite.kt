package com.gmail.vondenuelle.denuspend.utils.network

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "FirestoreWrite"
private const val SERVER_CONFIRM_TIMEOUT_MS = 10_000L

// Use instead of .await() on Firestore writes (set / update / delete / batch.commit).
// Firestore applies a write to the device cache right away and syncs it later, but its Task only finishes when the server
// confirms, which never happens offline, so the LoadingDialog stayed up. Offline we don't wait; online we wait up to 10s so
// real errors (rules, a wallet closed meanwhile) still reach the ViewModel. A queued write that fails later is only logged.
suspend fun Task<*>.awaitWrite(context: Context) {
    val confirmed = NetworkUtil.hasInternetConnection(context) &&
            withTimeoutOrNull(SERVER_CONFIRM_TIMEOUT_MS) { await(); true } == true
    if (!confirmed) {
        addOnFailureListener { Log.e(TAG, "A write saved offline failed when it synced", it) }
    }
}
