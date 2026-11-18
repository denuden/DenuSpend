package com.gmail.vondenuelle.denuspend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gmail.vondenuelle.denuspend.data.remote.error.NoUserException
import com.gmail.vondenuelle.denuspend.data.repositories.AuthRepository
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
}

/**
 * Lives for the whole Activity (created in AppRoot, outside any NavHost).
 * Decides which "world" is shown (Splash / Auth / Main) and feeds the shared top bar.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _session = MutableStateFlow<SessionState>(SessionState.Loading)
    val session: StateFlow<SessionState> = _session.asStateFlow()

    // Name and photo for the top bar. Updates live when Profile changes them.
    val user: StateFlow<UserPreferences> = authRepository.userPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreferences())

    init {
        checkCurrentUser()
        observeSignOut()
    }

    // Called by the auth screens only after login/register fully finished (incl. saving the display name)
    fun onLoginOrRegisterCompleted() {
        _session.value = SessionState.LoggedIn
    }

    private fun checkCurrentUser() {
        viewModelScope.launch {
            _session.value = try {
                authRepository.getCurrentUser() // also refreshes the cached user in DataStore
                SessionState.LoggedIn
            } catch (e: NoUserException) {
                SessionState.LoggedOut
            } catch (e: Exception) {
                // e.g. offline with an expired token. Firebase still has the user cached, so let them in.
                if (authRepository.hasUser()) SessionState.LoggedIn else SessionState.LoggedOut
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
