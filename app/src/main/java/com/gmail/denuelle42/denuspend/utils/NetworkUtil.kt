package com.gmail.denuelle42.denuspend.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class NetworkUtil {
    companion object {

        /*
         * Check if the device has internet connection or not, either wifi, cellular or ethernet
         */
        fun hasInternetConnection(context: Context): Boolean {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
            return when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        }
    }
}


/**
 * Pass a mime type to a request body for multipart form
 */
fun File?.asNetWorkRequestBody(mimeType: String): RequestBody {
    if(this == null) return "".toRequestBody("text/plain".toMediaTypeOrNull())
    return this.asRequestBody(mimeType.toMediaTypeOrNull())
}

/**
 * Make a request body from a string
 */
fun String?.createPartFromString(): RequestBody {
    return this.orEmpty().toRequestBody("text/plain".toMediaTypeOrNull())
}

const val ALL_IMAGE_MIME_TYPE = "image/*"


