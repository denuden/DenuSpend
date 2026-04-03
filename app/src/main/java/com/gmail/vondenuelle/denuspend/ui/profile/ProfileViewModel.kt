package com.gmail.vondenuelle.denuspend.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gmail.vondenuelle.denuspend.data.remote.error.ErrorModel
import com.gmail.vondenuelle.denuspend.data.remote.error.NoUserException
import com.gmail.vondenuelle.denuspend.data.remote.error.ReAuthenticateException
import com.gmail.vondenuelle.denuspend.data.remote.models.auth.request.LoginRequest
import com.gmail.vondenuelle.denuspend.data.remote.models.profile.request.UpdateEmailRequest
import com.gmail.vondenuelle.denuspend.data.remote.models.profile.request.UpdatePasswordRequest
import com.gmail.vondenuelle.denuspend.data.remote.models.profile.request.UpdateProfileRequest
import com.gmail.vondenuelle.denuspend.data.repositories.ProfileRepository
import com.gmail.vondenuelle.denuspend.navigation.AppRootScreens
import com.gmail.vondenuelle.denuspend.navigation.AuthScreens
import com.gmail.vondenuelle.denuspend.navigation.NavBehavior
import com.gmail.vondenuelle.denuspend.utils.OneTimeEvents
import com.gmail.vondenuelle.denuspend.utils.OneTimeEvents.OnNavigate
import com.gmail.vondenuelle.denuspend.utils.OneTimeEvents.OnPopBackStack
import com.gmail.vondenuelle.denuspend.utils.OneTimeEvents.ShowToast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
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
                        val user = profileRepository.getUserProfile()
                        _stateFlow.update {
                            it.copy(
                                profile = user,
                                name = user.name.orEmpty(),
                                photo = user.photo.orEmpty()
                            )
                        }
                    } catch (e: Exception) {
                        onError(e)
                    }
                }
            }

            is ProfileScreenEvents.OnChangeName ->
                _stateFlow.update { it.copy(name = event.value) }

            is ProfileScreenEvents.OnChangePhoto ->
                _stateFlow.update { it.copy(photo = event.value) }

            ProfileScreenEvents.OnPopBackStack ->
                sendEvent(OnPopBackStack)

            ProfileScreenEvents.OnSaveProfileChanges -> {
                viewModelScope.launch {
                    _stateFlow.update { it.copy(isLoading = true) }
                    try {
                        profileRepository.updateUserProfile(
                            request = UpdateProfileRequest(
                                name = _stateFlow.value.name,
                                photoUri = _stateFlow.value.photo
                            )
                        )
                        _stateFlow.update { it.copy(isLoading = false) }
                        sendEvent(ShowToast("Profile updated successfully"))
                        _stateFlow.update { it.copy(showEditDialog = false) }
                        onEvent(ProfileScreenEvents.OnGetUserProfile)

                    } catch (e: Exception) {
                        _stateFlow.update { it.copy(isLoading = false) }
                        onError(e)
                    }
                }
            }

            ProfileScreenEvents.OnSendEmailVerification ->
                viewModelScope.launch {
                    try {
                        _stateFlow.update { it.copy(isLoading = true) }
                        profileRepository.sendEmailVerification()
                        sendEvent(ShowToast("Email verification sent"))
                        _stateFlow.update { it.copy(isLoading = false) }
                    } catch (e: Exception) {
                        _stateFlow.update { it.copy(isLoading = false) }
                        onError(e)
                    }
                }

            is ProfileScreenEvents.OnSignOut -> {
                viewModelScope.launch {
                    try {
                        profileRepository.logout()
                        sendEvent(
                            OnNavigate(
                                AppRootScreens.AuthTopLevel,
                            )
                        )
                    } catch (e: Exception) {
                        onError(e)
                    }
                }
            }

            is ProfileScreenEvents.OnShowEditDialog -> _stateFlow.update { it.copy(showEditDialog = event.value) }
            is ProfileScreenEvents.OnShowMediaOptionDialog -> _stateFlow.update {
                it.copy(
                    showMediaOptionDialog = event.value
                )
            }

            is ProfileScreenEvents.OnShowDeleteAccountDialog -> _stateFlow.update {
                it.copy(
                    showDeleteAccountDialog = event.value,
                    typeOfEvent = DELETE
                )
            }

            is ProfileScreenEvents.OnDeleteAccount -> {
                viewModelScope.launch {
                    try {
                        _stateFlow.update { it.copy(isLoading = true) }
                        profileRepository.deleteAccount()
                        sendEvent(
                            OnNavigate(
                                AppRootScreens.AuthTopLevel,
                            )
                        )
                        _stateFlow.update {
                            it.copy(
                                isLoading = false,
                                showDeleteAccountDialog = false,
                                typeOfEvent = "",
                                email = "",
                                password = ""
                            )
                        }
                    } catch (e: Exception) {
                        _stateFlow.update {
                            it.copy(
                                isLoading = false,
                                showCredentialsDialog = false
                            )
                        }
                        onError(e)
                    }
                }
            }

            is ProfileScreenEvents.OnChangeReEnterPassword -> {
                _stateFlow.update { it.copy(reEnterPassword = event.value) }
            }

            is ProfileScreenEvents.OnChangeNewPassword -> {
                _stateFlow.update { it.copy(newPassword = event.value) }
            }

            ProfileScreenEvents.OnSavePasswordChanges -> {
                viewModelScope.launch {
                    try {
                        _stateFlow.update { it.copy(isLoading = true) }
                        profileRepository.updatePassword(
                            UpdatePasswordRequest(
                                newPassword = _stateFlow.value.newPassword,
                                reEnterPassword = _stateFlow.value.reEnterPassword,
                            )
                        )
                        sendEvent(ShowToast("Password has been updated"))
                        _stateFlow.update {
                            it.copy(
                                isLoading = false,
                                showUpdatePasswordDialog = false,
                                typeOfEvent = "",
                                email = "",
                                password = "",
                                reEnterPassword = "",
                                newPassword = ""
                            )
                        }
                    } catch (_: ReAuthenticateException) {
                        _stateFlow.update {
                            it.copy(
                                showCredentialsDialog = true,
                            )
                        }
                    } catch (e: Exception) {
                        _stateFlow.update { it.copy(isLoading = false) }
                        onError(e)
                    }
                }
            }

            is ProfileScreenEvents.OnShowUpdatePasswordDialog -> _stateFlow.update {
                it.copy(
                    showUpdatePasswordDialog = event.value,
                    typeOfEvent = UPDATE_PASSWORD
                )
            }

            is ProfileScreenEvents.OnShowUpdateEmailDialog -> _stateFlow.update {
                it.copy(
                    showUpdateEmailDialog = event.value,
                    typeOfEvent = UPDATE_EMAIL
                )
            }

            is ProfileScreenEvents.OnShowCredentialsDialog -> _stateFlow.update {
                it.copy(
                    showCredentialsDialog = event.value
                )
            }

            is ProfileScreenEvents.OnChangeEmail -> _stateFlow.update { it.copy(email = event.value) }
            is ProfileScreenEvents.OnChangeNewEmail -> _stateFlow.update { it.copy(newEmail = event.value) }
            is ProfileScreenEvents.OnChangePassword -> _stateFlow.update { it.copy(password = event.value) }

            is ProfileScreenEvents.OnValidateCredentials -> {
                viewModelScope.launch {
                    try {
                        _stateFlow.update { it.copy(isLoading = true) }

                        profileRepository.reauthenticateUser(
                            LoginRequest(
                                email = _stateFlow.value.email,
                                password = _stateFlow.value.password
                            )
                        )
                        _stateFlow.update {
                            it.copy(
                                showCredentialsDialog = false,
                            )
                        }

                        when (_stateFlow.value.typeOfEvent) {
                            UPDATE_PASSWORD -> {
                                onEvent(ProfileScreenEvents.OnSavePasswordChanges)
                            }

                            UPDATE_EMAIL -> {
                                onEvent(ProfileScreenEvents.OnSaveEmailChanges)
                            }
                        }
                    } catch (e: Exception) {
                        _stateFlow.update { it.copy(isLoading = false) }
                        onError(e)
                    }
                }
            }

            is ProfileScreenEvents.OnSaveEmailChanges -> {
                viewModelScope.launch {
                    try {
                        _stateFlow.update { it.copy(isLoading = true) }

                        profileRepository.updateEmail(
                            UpdateEmailRequest(
                                email = _stateFlow.value.newEmail,
                            )
                        )
                        sendEvent(ShowToast("Check your email ${_stateFlow.value.newEmail} to verify and complete your update."))
                        _stateFlow.update {
                            it.copy(
                                isLoading = false,
                                showUpdateEmailDialog = false,
                                typeOfEvent = "",
                                email = "",
                                newEmail = "",
                                password = "",
                            )
                        }
                    } catch (_: ReAuthenticateException) {
                        _stateFlow.update {
                            it.copy(
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
            is HttpException -> {
                val statusCode = e.code()
                val errorBody = e.response()?.errorBody()
                val gson = Gson()
                val type = object : TypeToken<ErrorModel>() {}.type
                val errorResponse: ErrorModel? = gson.fromJson(errorBody?.charStream(), type)

                // Example: Handle specific status
                when (statusCode) {
                    401 -> {
//                        sendEvent(OneTimeEvents.OnNavigate(AuthScreens.LoginNavigation))
                        return
                    }
                }

                //if this is not null, then there is a message regarding bad request of params
                if (errorResponse?.errors != null) {
                    _stateFlow.update {
                        it.copy(
                        )
                    }
                    sendEvent(OneTimeEvents.ShowInputError(errorResponse.errors))
                } else if (errorResponse?.message != null) {
                    sendEvent(OneTimeEvents.ShowError(errorResponse.message))
                }
            }

            is NoUserException -> {
                //send to login
                //remove stored creds
                sendEvent(OneTimeEvents.ShowToast("No user is signed in"))
                logout()
                sendEvent(
                    OneTimeEvents.OnNavigate(
                        AppRootScreens.AuthTopLevel,
                    )
                )
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

    companion object {
        const val UPDATE_PASSWORD = "UPDATE_PASSWORD"
        const val UPDATE_EMAIL = "UPDATE_EMAIL"
        const val DELETE = "DELETE"
    }
}