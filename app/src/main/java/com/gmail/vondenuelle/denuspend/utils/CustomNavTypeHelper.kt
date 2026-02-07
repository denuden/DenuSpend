package com.gmail.vondenuelle.denuspend.utils

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.navigation.NavType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


/**
 * This function helper serves the purpose of passing full data class type (complex data)
 * to new Serializable data classes for type safety navigation compose
 *  refer https://medium.com/@edmiro/type-safety-in-navigation-compose-23c03e3d74a5
 *  https://developer.android.com/guide/navigation/design/kotlin-dsl#custom-types
 *
 *  Some commented values are from philipp lackner
 *  https://www.youtube.com/watch?v=qBxaZ071N0c&t=1s
 *
 *  uses:
 *
 *   composable<SampleScreens.SampleNavigation>
 *       (typeMap =
 *       mapOf(typeOf<SampleModel>() to
 *       parcelableType<SampleModel>())
 *       ) {}
 */
inline fun <reified T : Parcelable> parcelableType(
    isNullableAllowed: Boolean = false,
    json: Json = Json,
) = object : NavType<T>(isNullableAllowed = isNullableAllowed) {

    override fun get(bundle: Bundle, key: String) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bundle.getParcelable(key, T::class.java)
        } else {
            @Suppress("DEPRECATION")
            bundle.getParcelable(key)
        }
//    Json.decodeFromString<T>(bundle.getString(key) ?: "")

    override fun parseValue(value: String): T = json.decodeFromString(value)

    override fun serializeAsValue(value: T): String = Uri.encode(Json.encodeToString(value))

    override fun put(bundle: Bundle, key: String, value: T) =
        bundle.putParcelable(key, value)
//    bundle.putString(key, Json.encodeToString(value))
}