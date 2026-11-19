package com.gmail.vondenuelle.denuspend.ui.profile

import android.content.Context
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.data.remote.error.NoUserException
import com.gmail.vondenuelle.denuspend.data.remote.error.ReAuthenticateException
import com.gmail.vondenuelle.denuspend.data.remote.models.auth.request.LoginRequest
import com.gmail.vondenuelle.denuspend.data.remote.models.profile.request.UpdateEmailRequest
import com.gmail.vondenuelle.denuspend.data.remote.models.profile.request.UpdatePasswordRequest
import com.gmail.vondenuelle.denuspend.data.remote.models.profile.request.UpdateProfileRequest
import com.gmail.vondenuelle.denuspend.data.repositories.ProfileRepository
import com.gmail.vondenuelle.denuspend.utils.OneTimeEvents
import com.gmail.vondenuelle.denuspend.utils.OneTimeEvents.OnPopBackStack
import com.gmail.vondenuelle.denuspend.utils.OneTimeEvents.ShowToast
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    // Application context (not an Activity), only used to read strings.xml, so it can't leak a screen
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val TAG = ProfileViewModel::class.java.simpleName

    private val _channel = Channel<OneTimeEvents>()
    val channel = _channel.receiveAsFlow()

    private val _stateFlow = MutableStateFlow<ProfileScreenState>(ProfileScreenState())
    val stateFlow = _stateFlow.asStateFlow()

    fun onEvent(event: ProfileScreenEvents) {
        when (event) {
            ProfileScreenEvents.OnGetUserProfile -> {
                viewModelScope.launch {
                    try {
                        // Only the profile here. This runs on every ON_START (e.g. back from the camera),
                        // so copying into the edit fields would wipe what the user typed. OnShowEditDialog fills them.
                        val user = profileRepository.getUserProfile()
                        _stateFlow.update { it.copy(profile = user) }
                    } catch (e: Exception) {
                        onError(e)
                    }
                }
            }

            is ProfileScreenEvents.OnChangeName ->
                _stateFlow.update { it.copy(name = event.value, nameError = "") }

            is ProfileScreenEvents.OnChangePhoto ->
                _stateFlow.update { it.copy(photo = event.value) }

            ProfileScreenEvents.OnPopBackStack ->
                sendEvent(OnPopBackStack)

            ProfileScreenEvents.OnSaveProfileChanges -> {
                if (_stateFlow.value.isLoading) return // ignore double taps while saving
                val nameError = if (_stateFlow.value.name.isBlank()) context.getString(R.string.error_name_required) else ""
                _stateFlow.update { it.copy(nameError = nameError) }
                if (nameError.isNotEmpty()) return

                _stateFlow.update { it.copy(isLoading = true) }
                viewModelScope.launch {
                    try {
                        profileRepository.updateUserProfile(
                            request = UpdateProfileRequest(
                                name = _stateFlow.value.name,
                                photoUri = _stateFlow.value.photo
                            )
                        )
                        _stateFlow.update { it.copy(isLoading = false) }
                        sendEvent(ShowToast(context.getString(R.string.profile_toast_profile_updated)))
                        _stateFlow.update { it.copy(showEditDialog = false) }
                        onEvent(ProfileScreenEvents.OnGetUserProfile)

                    } catch (e: Exception) {
                        _stateFlow.update { it.copy(isLoading = false) }
                        onError(e)
                    }
                }
            }

            ProfileScreenEvents.OnSendEmailVerification -> {
                if (_stateFlow.value.isLoading) return
                _stateFlow.update { it.copy(isLoading = true) }
                viewModelScope.launch {
                    try {
                        profileRepository.sendEmailVerification()
                        sendEvent(ShowToast(context.getString(R.string.profile_toast_verification_sent)))
                        _stateFlow.update { it.copy(isLoading = false) }
                    } catch (e: Exception) {
                        _stateFlow.update { it.copy(isLoading = false) }
                        onError(e)
                    }
                }
            }

            is ProfileScreenEvents.OnShowSignOutDialog -> _stateFlow.update { it.copy(showSignOutDialog = event.value) }

            // MainViewModel sees the sign out and AppRoot swaps to the auth screens
            is ProfileScreenEvents.OnSignOut -> {
                _stateFlow.update { it.copy(showSignOutDialog = false) }
                logout()
            }

            // Opening starts from the saved profile, so edits from a dismissed sheet don't come back
            is ProfileScreenEvents.OnShowEditDialog -> _stateFlow.update {
                if (event.value) {
                    it.copy(
                        showEditDialog = true,
                        name = it.profile?.name.orEmpty(),
                        nameError = "",
                        photo = it.profile?.photo.orEmpty()
                    )
                } else {
                    it.copy(showEditDialog = false)
                }
            }
            is ProfileScreenEvents.OnShowMediaOptionDialog -> _stateFlow.update {
                it.copy(
                    showMediaOptionDialog = event.value
                )
            }

            is ProfileScreenEvents.OnShowDeleteAccountDialog -> _stateFlow.update {
                it.copy(showDeleteAccountDialog = event.value)
            }

            // Step 2 of deleting: closes the info dialog and opens the password + slide sheet
            is ProfileScreenEvents.OnShowDeleteConfirmSheet -> _stateFlow.update {
                it.copy(
                    showDeleteAccountDialog = false,
                    showDeleteConfirmSheet = event.value,
                    password = ""
                )
            }

            is ProfileScreenEvents.OnDeleteAccount -> {
                val current = _stateFlow.value
                if (current.isLoading) return
                _stateFlow.update { it.copy(isLoading = true) }
                viewModelScope.launch {
                    try {
                        // Deleting also signs out, so AppRoot swaps to the auth screens on its own
                        profileRepository.deleteAccount(
                            LoginRequest(
                                email = current.profile?.email.orEmpty(),
                                password = current.password
                            )
                        )
                        _stateFlow.update {
                            it.copy(
                                isLoading = false,
                                showDeleteConfirmSheet = false,
                                password = ""
                            )
                        }
                    } catch (e: Exception) {
                        // Sheet stays open so the user can retry; the slider springs back once loading ends
                        _stateFlow.update { it.copy(isLoading = false) }
                        onError(e)
                    }
                }
            }

            is ProfileScreenEvents.OnChangeReEnterPassword -> {
                _stateFlow.update { it.copy(reEnterPassword = event.value, reEnterPasswordError = "") }
            }

            is ProfileScreenEvents.OnChangeNewPassword -> {
                _stateFlow.update { it.copy(newPassword = event.value, newPasswordError = "") }
            }

            ProfileScreenEvents.OnSavePasswordChanges -> {
                val current = _stateFlow.value
                if (current.isLoading) return
                val newPasswordError =
                    if (current.newPassword.isBlank()) context.getString(R.string.error_password_required) else ""
                val reEnterPasswordError =
                    if (current.reEnterPassword.trim() != current.newPassword.trim()) context.getString(R.string.profile_error_passwords_mismatch) else ""
                _stateFlow.update { it.copy(newPasswordError = newPasswordError, reEnterPasswordError = reEnterPasswordError) }
                if (newPasswordError.isNotEmpty() || reEnterPasswordError.isNotEmpty()) return

                _stateFlow.update { it.copy(isLoading = true) }
                viewModelScope.launch {
                    try {
                        profileRepository.updatePassword(
                            UpdatePasswordRequest(
                                newPassword = _stateFlow.value.newPassword,
                                reEnterPassword = _stateFlow.value.reEnterPassword,
                            )
                        )
                        sendEvent(ShowToast(context.getString(R.string.profile_toast_password_updated)))
                        _stateFlow.update {
                            it.copy(
                                isLoading = false,
                                showUpdatePasswordDialog = false,
                                pendingAction = PendingAction.NONE,
                                password = "",
                                reEnterPassword = "",
                                newPassword = ""
                            )
                        }
                    } catch (_: ReAuthenticateException) {
                        _stateFlow.update {
                            it.copy(
                                isLoading = false,
                                showCredentialsDialog = true,
                            )
                        }
                    } catch (e: Exception) {
                        _stateFlow.update { it.copy(isLoading = false) }
                        onError(e)
                    }
                }
            }

            // Both sheets start empty every time they open or close
            is ProfileScreenEvents.OnShowUpdatePasswordDialog -> _stateFlow.update {
                it.copy(
                    showUpdatePasswordDialog = event.value,
                    pendingAction = if (event.value) PendingAction.UPDATE_PASSWORD else PendingAction.NONE,
                    newPassword = "",
                    newPasswordError = "",
                    reEnterPassword = "",
                    reEnterPasswordError = "",
                )
            }

            is ProfileScreenEvents.OnShowUpdateEmailDialog -> _stateFlow.update {
                it.copy(
                    showUpdateEmailDialog = event.value,
                    pendingAction = if (event.value) PendingAction.UPDATE_EMAIL else PendingAction.NONE,
                    newEmail = "",
                    emailError = "",
                )
            }

            // Clears the typed password whenever the sheet opens or closes
            is ProfileScreenEvents.OnShowCredentialsDialog -> _stateFlow.update {
                it.copy(
                    showCredentialsDialog = event.value,
                    password = "",
                    passwordError = ""
                )
            }

            is ProfileScreenEvents.OnChangeNewEmail -> _stateFlow.update { it.copy(newEmail = event.value, emailError = "") }
            is ProfileScreenEvents.OnChangePassword -> _stateFlow.update { it.copy(password = event.value, passwordError = "") }

            is ProfileScreenEvents.OnValidateCredentials -> {
                if (_stateFlow.value.isLoading) return
                val passwordError =
                    if (_stateFlow.value.password.isBlank()) context.getString(R.string.error_password_required) else ""
                _stateFlow.update { it.copy(passwordError = passwordError) }
                if (passwordError.isNotEmpty()) return

                _stateFlow.update { it.copy(isLoading = true) }
                viewModelScope.launch {
                    try {
                        profileRepository.reauthenticateUser(
                            LoginRequest(
                                email = _stateFlow.value.profile?.email.orEmpty(),
                                password = _stateFlow.value.password
                            )
                        )
                        // isLoading goes false so the retried save passes its double-tap guard.
                        // The retry sets it back right away with no suspension in between, so the dialog doesn't flicker.
                        _stateFlow.update {
                            it.copy(
                                isLoading = false,
                                showCredentialsDialog = false,
                                password = "",
                            )
                        }

                        when (_stateFlow.value.pendingAction) {
                            PendingAction.UPDATE_PASSWORD -> {
                                onEvent(ProfileScreenEvents.OnSavePasswordChanges)
                            }

                            PendingAction.UPDATE_EMAIL -> {
                                onEvent(ProfileScreenEvents.OnSaveEmailChanges)
                            }

                            PendingAction.NONE -> {}
                        }
                    } catch (e: Exception) {
                        _stateFlow.update { it.copy(isLoading = false) }
                        onError(e)
                    }
                }
            }

            is ProfileScreenEvents.OnSaveEmailChanges -> {
                val current = _stateFlow.value
                if (current.isLoading) return
                val newEmail = current.newEmail.trim()
                val emailError = when {
                    newEmail.isBlank() -> context.getString(R.string.error_email_required)
                    !Patterns.EMAIL_ADDRESS.matcher(newEmail).matches() -> context.getString(R.string.error_email_invalid)
                    newEmail.equals(current.profile?.email, ignoreCase = true) -> context.getString(R.string.profile_error_same_email)
                    else -> ""
                }
                _stateFlow.update { it.copy(emailError = emailError) }
                if (emailError.isNotEmpty()) return

                _stateFlow.update { it.copy(isLoading = true) }
                viewModelScope.launch {
                    try {
                        profileRepository.updateEmail(
                            UpdateEmailRequest(
                                email = newEmail,
                            )
                        )
                        sendEvent(ShowToast(context.getString(R.string.profile_toast_email_update_sent, newEmail)))
                        _stateFlow.update {
                            it.copy(
                                isLoading = false,
                                showUpdateEmailDialog = false,
                                pendingAction = PendingAction.NONE,
                                newEmail = "",
                                password = "",
                            )
                        }
                    } catch (_: ReAuthenticateException) {
                        _stateFlow.update {
                            it.copy(
                                isLoading = false,
                                showCredentialsDialog = true,
                            )
                        }
                    } catch (e: Exception) {
                        _stateFlow.update { it.copy(isLoading = false) }
                        onError(e)
                    }
                }
            }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            try {
                profileRepository.logout()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    private fun onError(e: Throwable?) {
        when (e) {
            is NoUserException -> {
                // Signing out is enough. AppRoot swaps to the auth screens on its own.
                sendEvent(OneTimeEvents.ShowToast(context.getString(R.string.error_no_user_signed_in)))
                logout()
            }

            else -> {
                sendEvent(OneTimeEvents.ShowError(e?.message.orEmpty()))
            }
        }
    }

    private fun sendEvent(event: OneTimeEvents) {
        viewModelScope.launch {
            _channel.send(event)
        }
    }
}