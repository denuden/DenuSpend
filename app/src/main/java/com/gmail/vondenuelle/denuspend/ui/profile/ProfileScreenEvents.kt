package com.gmail.vondenuelle.denuspend.ui.profile

import com.gmail.vondenuelle.denuspend.data.remote.models.auth.request.LoginRequest

sealed class ProfileScreenEvents {
    object OnGetUserProfile : ProfileScreenEvents()

    data class OnChangeName(val value: String) : ProfileScreenEvents()
    data class OnChangePhoto(val value: String) : ProfileScreenEvents()
    data class OnChangeNewPassword(val value: String) : ProfileScreenEvents()
    data class OnChangeReEnterPassword(val value: String) : ProfileScreenEvents()
    data class OnChangePassword(val value: String) : ProfileScreenEvents()
    data class OnChangeEmail(val value: String) : ProfileScreenEvents()
    data class OnChangeNewEmail(val value: String) : ProfileScreenEvents()

    data class OnShowMediaOptionDialog(val value: Boolean) : ProfileScreenEvents()
    data class OnShowEditDialog(val value: Boolean) : ProfileScreenEvents()
    data class OnShowUpdatePasswordDialog(val value: Boolean) : ProfileScreenEvents()
    data class OnShowDeleteAccountDialog(val value: Boolean) : ProfileScreenEvents()
    data class OnShowCredentialsDialog(val value: Boolean) : ProfileScreenEvents()
    data class OnShowUpdateEmailDialog(val value: Boolean) : ProfileScreenEvents()

    object OnValidateCredentials : ProfileScreenEvents()

    object OnSendEmailVerification : ProfileScreenEvents()
    object OnDeleteAccount : ProfileScreenEvents()
    object OnSignOut : ProfileScreenEvents()
    object OnSaveProfileChanges : ProfileScreenEvents()
    object OnSavePasswordChanges : ProfileScreenEvents()
    object OnSaveEmailChanges : ProfileScreenEvents()
    object OnPopBackStack : ProfileScreenEvents()
}