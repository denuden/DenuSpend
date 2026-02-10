package com.gmail.vondenuelle.denuspend.di.modules

import androidx.datastore.core.DataStore
import com.gmail.vondenuelle.denuspend.data.storage.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In order to use, inject this in viewmodel to get the in memory cache of data store
 */
@Singleton
class TokenProvider @Inject constructor(
    dataStore: DataStore<UserPreferences>
) {

    /**
     * @Volatile means:
     * - Always read the **latest** value written by ANY thread
     * - Prevents the CPU from caching old values
     * - Ensures visibility across threads
     *
     * -> Without @Volatile, one thread could update `token`, but 
     *    another thread (like OkHttp interceptor thread) might still
     *    see the old value due to memory caching.
     */

    @Volatile private var uid: String? = null
    @Volatile private var name: String? = null
    @Volatile private var email: String? = null
    @Volatile private var isEmailVerified: Boolean? = false

    init {
        /**
         * We launch a coroutine that ALWAYS listens to DataStore changes.
         * Whenever the token in DataStore updates:
         *   → we update `token` in RAM immediately.
         *
         * This means:
         * - tokenProvider.getToken() is O(1)
         * - We never read from disk inside OkHttp
         * - No more runBlocking()
         */
        CoroutineScope(Dispatchers.IO).launch {
            dataStore.data.collect { prefs ->
                // atomic WRITE of a single variable
                uid = prefs.uid
                name = prefs.name
                email = prefs.email
                isEmailVerified = prefs.isEmailVerified
            }
        }
    }

    /**
     * Atomic READ:
     * - Reading a single variable is safe
     * - @Volatile guarantees the latest value is returned
     */
    fun getName(): String = "Welcome back, ${name.orEmpty().ifEmpty { "User" } }"
}
