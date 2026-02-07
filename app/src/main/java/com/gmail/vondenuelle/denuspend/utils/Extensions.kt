package com.gmail.vondenuelle.denuspend.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.debugInspectorInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


fun String.debug(message: String) {
    Log.d(this, message)
}

/**
 * Clear Focus from a composable like textfield
 */
fun clearFocus(focusManager: FocusManager){
    focusManager.clearFocus()
}

/**
 * Modified modifier.clickable where 1second delay is imposed
 * to avoid spam clicks
 */
@Composable
fun Modifier.clickableDelayed(
    delayMillis: Long = 1000L,
    onClick: () -> Unit
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "clickableDelayed"
        properties["delayMillis"] = delayMillis
        properties["onClick"] = onClick
    }
) {
    var isClickable by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

   clickable(enabled = isClickable) {
        if (isClickable) {
            isClickable = false
            onClick()
            // Restore clickable state after the delay
            scope.launch {
                delay(delayMillis)
                isClickable = true
            }
        }
    }
}


/**
 * Go to specified URL, parse an action if an app can open it outside the application
 */
fun Context.goIntent(url: String) {
    try {
        val myIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(myIntent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, "No application can handle this request. Please install a web browser", Toast.LENGTH_SHORT).show()
        e.printStackTrace()
    }
}


/**
 * Return specified string if null, variation of orEmpty()
 */
fun String?.orEmpty(string: String): String = this ?: string
