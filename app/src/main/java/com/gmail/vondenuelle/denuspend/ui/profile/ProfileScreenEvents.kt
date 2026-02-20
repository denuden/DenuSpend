package com.gmail.vondenuelle.denuspend.ui.profile

sealed class ProfileScreenEvents {
   object OnGetUserProfile : ProfileScreenEvents()

   data class OnChangeName(val value : String) : ProfileScreenEvents()
   data class OnChangePhoto(val value : String) : ProfileScreenEvents()

   object OnSendEmailVerification : ProfileScreenEvents()
   object OnSaveChanges : ProfileScreenEvents()
   object OnPopBackStack : ProfileScreenEvents()
}