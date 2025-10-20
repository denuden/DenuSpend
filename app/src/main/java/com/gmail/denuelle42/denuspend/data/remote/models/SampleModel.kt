package com.gmail.denuelle42.denuspend.data.remote.models

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Keep
@Serializable
@Parcelize
data class SampleModel(
    val id : Int? = null,
    val nam : String? = null
)  : Parcelable
