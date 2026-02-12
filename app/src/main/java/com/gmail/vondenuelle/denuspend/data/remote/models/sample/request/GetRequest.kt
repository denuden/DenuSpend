package com.gmail.vondenuelle.denuspend.data.remote.models.sample.request

import androidx.annotation.Keep
import java.io.File

/**
 * Use @Keep annotation so unused properties won't get
 * obfuscated or minified(removed) or even renamed to
 * reduce resources by proguard
 */

@Keep
data class GetRequest(
    val id : Int? = null,
    val frontImageFile : File? = null,
    val backImageFile : File? = null,
    val userId : String? = null
)