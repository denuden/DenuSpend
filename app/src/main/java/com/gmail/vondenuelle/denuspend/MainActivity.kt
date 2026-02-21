package com.gmail.vondenuelle.denuspend

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.datastore.core.DataStore
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.gmail.vondenuelle.denuspend.data.storage.UserPreferences
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.ActionCodeResult
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.ActionCodeUrl
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var firebaseAuth : FirebaseAuth
    @Inject lateinit var dataStore : DataStore<UserPreferences>

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    private fun handleIntent(intent: Intent?) {
        intent?.data?.let { uri ->
            handleActionCode(uri.toString())
            Log.d("gwgw", "Received link: $uri")
        }
    }

    private fun handleActionCode(link: String) {
        val actionCodeUrl = ActionCodeUrl.parseLink(link)
        val mode = actionCodeUrl?.operation
        val oobCode = actionCodeUrl?.code

//        https://denu-spend.firebaseapp.com/__/auth/links?link=https://denu-spend.firebaseapp.com/__/auth/action?apiKey%3DAIzaSyAGoM0A1jfMqsTE9-m5UlQFZF4bQJDbTmY%26mode%3DverifyEmail%26oobCode%3D98bvY3pHYSDFiy77JeJSwiNH2_6-uZ3IcvpmzlbBtkkAAAGce6FsRA%26continueUrl%3Dhttps://denu-spend.firebaseapp.com%26lang%3Den
        when (mode) {
            ActionCodeResult.VERIFY_EMAIL -> {
                firebaseAuth.applyActionCode(oobCode.orEmpty())
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Email verified!
                            lifecycleScope.launch {
                                repeatOnLifecycle(Lifecycle.State.STARTED){
                                    dataStore.updateData {
                                        it.copy(isEmailVerified = true)
                                    }
                                }
                            }
                            Toast.makeText(this@MainActivity, "Email is verified", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@MainActivity, "Email cannot be verified", Toast.LENGTH_SHORT).show()

                        }
                    }
            }
            ActionCodeResult.PASSWORD_RESET -> {
                // Prompt user to enter new password, then confirm with oobCode
            }
            else -> {
                Log.w("Auth", "Unhandled mode: $mode")
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            DenuSpendTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color = MaterialTheme.colorScheme.surface)
                ) {
                    AppRoot()
                }
            }
        }
    }
}