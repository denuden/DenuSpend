package com.gmail.vondenuelle.denuspend.utils.media

import android.net.Uri
import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class MediaPickerUiState(
    val selectedOption : SelectedOption? = null,
    val idUri : Uri? = null,
    val addressUri : Uri? = null,

    val savedToGalleryUri : Uri? = null,

    val files : List<Uri> =  emptyList(),
    val fileHolder : Uri? = null,
    val singleFile : Uri? = null,
) : Parcelable