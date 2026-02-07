package com.gmail.vondenuelle.denuspend.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Buggy, do not use, it overlaps composables
 */
@Composable
fun WebViewScreen(url: String) {
    AndroidView(factory = { context ->
        WebView(context).apply {
            webViewClient = WebViewClient()
            loadUrl(url)
        }
    })
}

/**
 * Go to specified URL, parse an action if an app can open it outside the application
 */

fun Context.goURL(url: String) : Boolean{
    return try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        val chooser = Intent.createChooser(intent, "Open with")
        startActivity(chooser) // This will display a list of apps that can handle the action
        true
    } catch (e: ActivityNotFoundException) {
        e.printStackTrace()
        Toast.makeText(this, "No application can handle this request. Please install a web browser", Toast.LENGTH_SHORT).show()
        false
    }
}
