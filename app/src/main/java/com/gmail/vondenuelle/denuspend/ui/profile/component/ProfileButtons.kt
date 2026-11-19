package com.gmail.vondenuelle.denuspend.ui.profile.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.clickableDelayed


@Preview
@Composable
private fun ProfileButtonsPreview() {
    DenuSpendTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize() // Fill the whole preview area
                .background(Color.Blue) // Set the entire background to blue
                .padding(40.dp), // Now, create inner padding for the content
            color = Color.Transparent // Make the Surface itself transparent
        ) {
            ProfileButtons(
                onEditProfile = {  },
                onUpdatePassword = {  },
                onUpdateEmail = {  },
                onDeleteAccount = {  },
                onSignOut = {  }
            )
        }
    }
}

@Composable
fun ProfileButtons(
    modifier: Modifier = Modifier,
    onEditProfile: () -> Unit,
    onUpdatePassword: () -> Unit,
    onUpdateEmail: () -> Unit,
    onDeleteAccount: () -> Unit,
    onSignOut: () -> Unit,
) {
    ElevatedCard(
        shape = MaterialTheme.shapes.medium.copy(
            topStart = MaterialTheme.shapes.extraLarge.topStart,
            topEnd = MaterialTheme.shapes.extraLarge.topEnd,
            bottomStart = CornerSize(0.dp),
            bottomEnd = CornerSize(0.dp)
        ),
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
    ) {

        val scrollState = rememberScrollState()
        val showFade = scrollState.canScrollForward

        Box {

            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(horizontal = 8.dp, vertical = 16.dp)
            ) {
                SectionLabel(stringResource(R.string.profile_section_profile))
                ProfileRow(
                    icon = Icons.Filled.Person,
                    label = stringResource(R.string.profile_row_edit_profile),
                    onClick = onEditProfile
                )

                Spacer(modifier = Modifier.height(16.dp))
                SectionLabel(stringResource(R.string.profile_section_account))
                ProfileRow(
                    icon = Icons.Filled.Lock,
                    label = stringResource(R.string.profile_row_update_password),
                    onClick = onUpdatePassword
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ProfileRow(
                    icon = Icons.Filled.Email,
                    label = stringResource(R.string.profile_row_update_email),
                    onClick = onUpdateEmail
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ProfileRow(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    label = stringResource(R.string.profile_row_sign_out),
                    onClick = onSignOut
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ProfileRow(
                    icon = Icons.Filled.DeleteForever,
                    label = stringResource(R.string.profile_row_delete_account),
                    onClick = onDeleteAccount,
                    color = MaterialTheme.colorScheme.error
                )

                // Extra bottom spacing improves scroll perception
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 🔥 Fading Edge Overlay
            if (showFade) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.15f)
                                )
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun ProfileRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    color: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = { Icon(imageVector = icon, contentDescription = null) },
        trailingContent = {
            Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            headlineColor = color,
            leadingIconColor = color,
            trailingIconColor = color,
        ),
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .clickableDelayed { onClick() }
    )
}

// The full-width button every Profile sheet ends with, so they all look the same
@Composable
fun ProfileSheetButton(
    text: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        Text(text)
    }
}
