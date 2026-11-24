package com.gmail.vondenuelle.denuspend

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    private fun handleIntent(intent: Intent?) {
        intent?.data?.let { uri ->
            Log.d("handleintent", uri.toString())
            val intent = EmailActionActivity.newIntent(this@MainActivity, uri.toString())
            startActivity(intent)
        }
    }


    // Same instance AppRoot gets from hiltViewModel(), since both are scoped to this Activity
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate. Keeps the system splash up until the session check
        // decides Auth / Wallet setup / Main, so AppRoot's Splash destination is never really seen.
        installSplashScreen().setKeepOnScreenCondition {
            mainViewModel.session.value == SessionState.Loading
        }
        super.onCreate(savedInstanceState)
        // Only on a fresh start. A rotation recreates the Activity with the same intent, which would re-open
        // an email link whose one-time code was already used ("Verification failed").
        if (savedInstanceState == null) handleIntent(intent)
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
