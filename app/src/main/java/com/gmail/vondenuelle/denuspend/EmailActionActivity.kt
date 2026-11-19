package com.gmail.vondenuelle.denuspend

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Password
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import com.gmail.vondenuelle.denuspend.data.remote.models.auth.request.LoginRequest
import com.gmail.vondenuelle.denuspend.data.storage.UserPreferences
import com.gmail.vondenuelle.denuspend.ui.auth.AuthScreenEvents
import com.gmail.vondenuelle.denuspend.ui.common.components.AppTextField
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.google.firebase.auth.ActionCodeResult
import com.google.firebase.auth.ActionCodeUrl
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import androidx.core.net.toUri


@AndroidEntryPoint
class EmailActionActivity : ComponentActivity() {
    @Inject
    lateinit var firebaseAuth: FirebaseAuth
    @Inject
    lateinit var dataStore: DataStore<UserPreferences>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri = intent?.getStringExtra(URI)
        Log.d("gwgw", uri.toString())

        setContent {
            DenuSpendTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (uri != null && uri.isNotBlank()) {
                        val outerUri = uri.toUri()
                        val encodedInnerLink = outerUri.getQueryParameter("link")
                        val decodedInnerLink = encodedInnerLink?.let {
                            Uri.decode(it)
                        }
                        val actionUri = decodedInnerLink?.toUri()

                        AuthActionScreen(
                            actionUri,
                            firebaseAuth,
                            dataStore
                        ) {
                            // After handling, navigate back to main or finish
                            finish()
                        }
                    } else {
                        Text("No action link found")
                    }
                }
            }
        }
    }


    companion object {
        const val URI = "URI"
        fun newIntent(context: Context, uri: String): Intent {
            val intent = Intent(context, EmailActionActivity::class.java)
            return intent.apply {
                putExtra(URI, uri)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AuthActionScreen(
    actionUri: Uri?,
    firebaseAuth: FirebaseAuth,
    dataStore: DataStore<UserPreferences>,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()


    val mode = actionUri?.getQueryParameter("mode")
    val oobCode = actionUri?.getQueryParameter("oobCode")


    Column(
        modifier = Modifier.safeContentPadding()
    ) {
        when (mode) {
            "verifyEmail" -> {
                LaunchedEffect(Unit) {
                    firebaseAuth.applyActionCode(oobCode.orEmpty())
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                scope.launch {
                                    dataStore.updateData {
                                        it.copy(isEmailVerified = true)
                                    }
                                    //Reload current user in cache to update email verified
                                    firebaseAuth.currentUser!!.reload().await()
                                }

                                Toast.makeText(context, "Email verified!", Toast.LENGTH_SHORT)
                                    .show()
                            } else {
                                Toast.makeText(context, "Verification failed", Toast.LENGTH_SHORT)
                                    .show()
                            }
                            onComplete()
                        }
                }
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                ) {
                    ContainedLoadingIndicator()
                    Spacer(modifier = Modifier.padding(6.dp))
                    Text("Verifying your email…")
                }
            }

            "verifyAndChangeEmail" -> {
                LaunchedEffect(Unit) {
                    firebaseAuth.applyActionCode(oobCode.orEmpty())
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                scope.launch {
                                    dataStore.updateData {
                                        it.copy(isEmailVerified = true)
                                    }
                                    //Reload current user in cache to update email verified
                                    firebaseAuth.currentUser!!.reload().await()
                                }

                                Toast.makeText(context, "Email verified!", Toast.LENGTH_SHORT)
                                    .show()
                                onComplete()
                            } else {
                                Toast.makeText(context, "Verification failed", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                }
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                ) {
                    ContainedLoadingIndicator()
                    Spacer(modifier = Modifier.padding(6.dp))
                    Text("Verifying your email…")
                }
            }

            "resetPassword" -> {
                var newPassword by remember { mutableStateOf("") }
                var passwordError by remember { mutableStateOf("") }

                Column {
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier.height(56.dp)
                    ) {
                        IconButton(
                            onClick = {
                                onComplete()
                            },
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = null,
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.padding(16.dp),
                    ) {
                        AppTextField(
                            value = newPassword,
                            onValueChange = {
                                newPassword = it
                            },
                            label = stringResource(R.string.lbl_new_password),
                            placeholder = stringResource(R.string.hint_password_reset),
                            leadingIcon = Icons.Filled.Password,
                            error = passwordError,
                            isPassword = true,
                            imeAction = ImeAction.Done,
                            onImeAction = {
                                    firebaseAuth.confirmPasswordReset(oobCode.orEmpty(), newPassword)
                                        .addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                Toast.makeText(
                                                    context,
                                                    "Password reset successful",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                onComplete()
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "Password reset failed",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                            },
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                firebaseAuth.confirmPasswordReset(oobCode.orEmpty(), newPassword)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            Toast.makeText(
                                                context,
                                                "Password reset successful",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            onComplete()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Password reset failed: ${task.exception?.localizedMessage}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.extraLarge,
                            elevation = ButtonDefaults.buttonElevation(0.dp),
                            contentPadding = PaddingValues(vertical = 16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                stringResource(R.string.btn_reset_password).uppercase(),
                            )
                        }
                    }

                }
            }

            else -> {
                Text("Unhandled action mode")
            }
        }
    }


}

