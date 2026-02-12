package com.gmail.vondenuelle.denuspend.domain.models

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable


//From remote like json, or unfiltered data
@Keep
@Serializable
@Parcelize
data class SampleModel(
    val id : Int? = null,
    val nam : String? = null
)  : Parcelable
