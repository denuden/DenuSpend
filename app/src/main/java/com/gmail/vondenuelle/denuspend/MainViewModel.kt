package com.gmail.vondenuelle.denuspend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.gmail.vondenuelle.denuspend.utils.SnackBarController
import com.gmail.vondenuelle.denuspend.utils.SnackbarEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.filter
import com.gmail.vondenuelle.denuspend.data.remote.error.NoUserException
import com.gmail.vondenuelle.denuspend.data.repositories.AuthRepository
import com.gmail.vondenuelle.denuspend.data.repositories.WalletRepository
import com.gmail.vondenuelle.denuspend.data.storage.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SessionState {
    data object Loading : SessionState
    data object LoggedIn : SessionState
    data object LoggedOut : SessionState
    // Signed in, but the mandatory physical wallet isn't set up yet
    data object NeedsWalletSetup : SessionState
}

/**
 * Lives for the whole Activity (created in AppRoot, outside any NavHost).
 * Decides which "world" is shown (Splash / Auth / Wallet setup / Main) and feeds the shared top bar.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context, // only for the auto-charge snackbar text
    private val authRepository: AuthRepository,
    private val walletRepository: WalletRepository,
) : ViewModel() {

    private val _session = MutableStateFlow<SessionState>(SessionState.Loading)
    val session: StateFlow<SessionState> = _session.asStateFlow()

    // Name and photo for the top bar. Updates live when Profile changes them.
    val user: StateFlow<UserPreferences> = authRepository.userPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreferences())

    init {
        checkCurrentUser()
        observeSignOut()
        chargeDueOnLogin()
    }

    // Called by the auth screens only after login/register fully finished (incl. saving the display name)
    fun onLoginOrRegisterCompleted() {
        viewModelScope.launch {
            _session.value = loggedInOrSetup()
        }
    }

    // Called by the setup screen's Finish button
    fun onWalletSetupCompleted() {
        _session.value = SessionState.LoggedIn
    }

    // Main, unless the physical wallet isn't there yet. If the check itself fails (e.g. offline), let them into Main
    // instead of blocking the app; setup shows on the next start that can reach Firestore.
    private suspend fun loggedInOrSetup(): SessionState = try {
        if (walletRepository.needsWalletSetup()) SessionState.NeedsWalletSetup else SessionState.LoggedIn
    } catch (e: Exception) {
        SessionState.LoggedIn
    }

    private fun checkCurrentUser() {
        viewModelScope.launch {
            _session.value = try {
                authRepository.getCurrentUser() // also refreshes the cached user in DataStore
                loggedInOrSetup()
            } catch (e: NoUserException) {
                SessionState.LoggedOut
            } catch (e: Exception) {
                // e.g. offline with an expired token. Firebase still has the user cached, so let them in.
                if (authRepository.hasUser()) SessionState.LoggedIn else SessionState.LoggedOut
            }
        }
    }

    /**
     * Card subscriptions and installments are charged when the app opens (each time the session becomes LoggedIn: a cold start or a login).
     * It runs quietly: offline or a failure just leaves them for the next open. One snackbar names what was charged.
     */
    private fun chargeDueOnLogin() {
        viewModelScope.launch {
            _session.filter { it == SessionState.LoggedIn }.collect {
                val charged = try {
                    walletRepository.chargeDueRecurring()
                } catch (e: Exception) {
                    emptyList()
                }
                if (charged.isEmpty()) return@collect
                val names = charged.joinToString(", ") { it.second }
                val cards = charged.map { it.first }.distinct()
                val message = if (cards.size == 1) context.getString(R.string.main_snackbar_recurring_charged, cards.first(), names)
                else context.getString(R.string.main_snackbar_recurring_charged_many, names)
                SnackBarController.sendEvent(SnackbarEvent(message = message))
            }
        }
    }

    /**
     * Any sign out (Profile sign out, delete account, session expired) ends up here.
     * Sign-ins are ignored on purpose: Firebase signs in mid-registration, before the name is saved.
     */
    private fun observeSignOut() {
        viewModelScope.launch {
            authRepository.observeAuthState().collect { isLoggedIn ->
                if (!isLoggedIn) {
                    _session.value = SessionState.LoggedOut
                    authRepository.clearUserPreferences()
                }
            }
        }
    }
}
