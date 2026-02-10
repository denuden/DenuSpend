package com.gmail.vondenuelle.denuspend.ui.auth.screen

import android.graphics.BlurMaskFilter
import android.graphics.RectF
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavOptions
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.navigation.NavBehavior
import com.gmail.vondenuelle.denuspend.navigation.NavigationScreens
import com.gmail.vondenuelle.denuspend.ui.auth.AuthScreenEvents
import com.gmail.vondenuelle.denuspend.ui.auth.AuthScreenState
import com.gmail.vondenuelle.denuspend.ui.auth.AuthViewModel
import com.gmail.vondenuelle.denuspend.ui.common.dialog.ErrorDialog
import com.gmail.vondenuelle.denuspend.ui.common.dialog.LoadingDialog
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.ObserveAsEvents
import com.gmail.vondenuelle.denuspend.utils.OneTimeEvents

@Composable
fun RegisterScreen(
    onNavigate: (NavigationScreens, NavOptions?) -> Unit,
    onPopBackStack: () -> Unit,
    viewModel : AuthViewModel = hiltViewModel()
) {
    var error by remember { mutableStateOf("") }
    val context = LocalContext.current
    ObserveAsEvents(viewModel.channel) { event ->
        when(event){
            is OneTimeEvents.OnNavigate -> {
                val options = NavOptions.Builder().apply {
                    when (event.behavior) {
                        NavBehavior.ClearAll -> {
                            setPopUpTo(0, inclusive = true)
                            setLaunchSingleTop(true)
                        }
                        is NavBehavior.PopUpTo -> {
                            setPopUpTo(event.behavior.destination, inclusive = event.behavior.inclusive)
                        }
                        NavBehavior.None -> Unit
                    }
                }.build()

                onNavigate(event.route, options)
            }
            is OneTimeEvents.ShowError -> error = event.msg
            is OneTimeEvents.ShowToast -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            else -> Unit
        }
    }

    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    LoadingDialog(
        text = "Signing you up...",
        showDialog = state.isSigningUp
    ) { }


    ErrorDialog(
        text = error,
        showDialog = error.isNotEmpty()
    ) { error = "" }

    RegisterScreenContent(
        state = state,
        onEvent = viewModel::onEvent
    )
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun RegisterScreenContent(
    modifier: Modifier = Modifier,
    state: AuthScreenState,
    onEvent: (AuthScreenEvents) -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 30.dp)
                .verticalScroll(state = scrollState )
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.register_title),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.register_subtitle),
                textAlign = TextAlign.Center
            )

//            ============= FIELDS
            Spacer(modifier = Modifier.height(48.dp))

            val fieldColors = TextFieldDefaults.colors().copy(
                focusedIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
            val fieldBorder = Modifier.border(
                1.dp, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.error
            )


            // Username
            Text(stringResource(R.string.register_lbl_name), modifier = Modifier.align(Alignment.Start))
            TextField(
                value = state.email,
                onValueChange = {
                    onEvent(AuthScreenEvents.OnChangeNameField(it))
                },
                label = {
                    Text(text = stringResource(R.string.register_hint_name))
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = stringResource(R.string.register_hint_name)
                    )
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Text
                ),
                shape = MaterialTheme.shapes.large,
                colors = fieldColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (state.emailError.isNotEmpty()) fieldBorder else Modifier
                    )
            )


            // Username
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.register_lbl_email), modifier = Modifier.align(Alignment.Start))
            TextField(
                value = state.email,
                onValueChange = {
                    onEvent(AuthScreenEvents.OnChangeEmailField(it))
                },
                label = {
                    Text(text = stringResource(R.string.login_hint_email))
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Email,
                        contentDescription = stringResource(R.string.login_hint_email)
                    )
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Email
                ),
                shape = MaterialTheme.shapes.large,
                colors = fieldColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (state.emailError.isNotEmpty()) fieldBorder else Modifier
                    )
            )

            // Password
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.register_lbl_password), modifier = Modifier.align(Alignment.Start))
            TextField(
                value = state.password,
                onValueChange = {
                    onEvent(AuthScreenEvents.OnChangePasswordField(it))
                },
                label = {
                    Text(text = stringResource(R.string.register_hint_password))
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Password,
                        contentDescription = stringResource(R.string.register_hint_password)
                    )
                },
                keyboardActions = KeyboardActions(
                    onDone = {
//                        TODO
                    }
                ),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Password
                ),
                shape = MaterialTheme.shapes.large,
                colors = fieldColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (state.emailError.isNotEmpty()) fieldBorder else Modifier
                    )
            )

            //Forgot password and remember me
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = state.shouldRememberMe,
                        onCheckedChange = { onEvent(AuthScreenEvents.OnChangeRememberMeCheckBox(it)) }
                    )
                    Text(stringResource(R.string.lbl_remember_me))
                }

                TextButton(
                    onClick = {
//                        TODO
                    }
                ) {
                    Text(stringResource(R.string.lbl_forgot_password))
                }
            }


            val colorPrimary = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
                    .drawBehind {
                        val glowRadius = 50.dp.toPx()
                        val spread = 12.dp.toPx()

                        val paint = Paint().asFrameworkPaint().apply {
                            color = colorPrimary.toArgb()
                            maskFilter = BlurMaskFilter(
                                glowRadius,
                                BlurMaskFilter.Blur.NORMAL
                            )
                        }

                        drawIntoCanvas { canvas ->
                            val rect = RectF(
                                spread,
                                spread,
                                size.width - spread,
                                size.height - spread
                            )

                            canvas.nativeCanvas.drawRoundRect(
                                rect,
                                32.dp.toPx(), // corner radius X
                                32.dp.toPx(), // corner radius Y
                                paint
                            )
                        }
                    }
            ) {
                Button(
                    onClick = { /* TODO */ },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    elevation = ButtonDefaults.buttonElevation(0.dp),
                    contentPadding = PaddingValues(vertical = 20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        stringResource(R.string.register_btn_create).uppercase(),
                        fontSize = 16.sp
                    )
                }
            }


            // =============== OTHER SIGN INS
            Spacer(modifier = Modifier.height(40.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(R.string.lbl_other_signin_methods),
                    modifier = Modifier.padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f)
                )
            }


            Spacer(modifier = Modifier.height(24.dp))


            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                FilledTonalButton(
                    shape = MaterialTheme.shapes.extraLarge,
                    onClick = {
                        //TODO
                    },

                    ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_google_neutral_rd_na),
                            modifier = Modifier.size(30.dp),
                            contentDescription = null,
                            tint = Color.Unspecified
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.lbl_google))
                    }
                }

                Spacer(Modifier.width(8.dp))

                FilledTonalButton(
                    shape = MaterialTheme.shapes.extraLarge,
                    onClick = {
                        //TODO
                    },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_facebook_logo),
                            modifier = Modifier.size(30.dp),
                            contentDescription = null,
                            tint = Color.Unspecified
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.lbl_facebook))
                    }
                }

                Spacer(Modifier.width(8.dp))
            }

            Spacer(Modifier.height(8.dp))

            FilledTonalButton(
                shape = MaterialTheme.shapes.extraLarge,
                onClick = {
                    //TODO
                },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhoneAndroid,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.lbl_contact_number))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.register_navigate_to_login))
                TextButton(
                    onClick = {
                        onEvent(AuthScreenEvents.OnNavigateToLogin)
                    }
                ) {
                    Text(stringResource(R.string.lbl_sign_in), fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}

@Preview
@Composable
private fun RegisterScreen() {
    DenuSpendTheme {
        RegisterScreenContent(
            state = AuthScreenState()
        )
    }
}