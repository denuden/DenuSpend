package com.gmail.vondenuelle.denuspend.ui.profile.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.ui.common.components.AppTextField
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.AsyncImageWithErrorHandler
import com.gmail.vondenuelle.denuspend.utils.clickableDelayed

@Composable
fun ProfileEdit(
    modifier: Modifier = Modifier,
    name: String,
    nameError: String,
    onChangeName: (String) -> Unit,
    photo: String,
    onSave: () -> Unit,
    onChangePhoto: () -> Unit,
) {
    val state = rememberScrollState()
    // Shows the person icon instead of a broken image. Resets when the photo changes.
    var photoFailed by remember(photo) { mutableStateOf(false) }

    Column(
        modifier = modifier
            .verticalScroll(state)
            .padding(start = 24.dp, end = 24.dp, bottom = 32.dp)
    ) {
        Text(stringResource(R.string.profile_edit_title), style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickableDelayed {
                    onChangePhoto()
                }
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(color = MaterialTheme.colorScheme.primaryContainer),
            ) {
                if (photo.isNotEmpty() && !photoFailed) {
                    AsyncImageWithErrorHandler(
                        model = photo.toUri(),
                        modifier = Modifier.size(160.dp),
                        shouldShowEnlargeButton = false,
                        onError = { photoFailed = true },
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
            IconButton(
                onClick = {
                    onChangePhoto()
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                ),
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = stringResource(R.string.profile_cd_change_photo)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))


        // Name
        AppTextField(
            value = name,
            onValueChange = onChangeName,
            label = stringResource(R.string.profile_lbl_name),
            placeholder = stringResource(R.string.profile_hint_name),
            leadingIcon = Icons.Filled.Person,
            error = nameError,
            imeAction = ImeAction.Done,
            onImeAction = onSave,
        )

        Spacer(modifier = Modifier.height(24.dp))
        ProfileSheetButton(
            text = stringResource(R.string.profile_btn_save),
            onClick = onSave
        )
    }
}

@Preview
@Composable
private fun ProfileEditPreview() {
    DenuSpendTheme {
        androidx.compose.material3.Surface {
            ProfileEdit(
                name = "",
                nameError = "",
                onChangeName = {},
                photo = "",
                onChangePhoto = {},
                onSave = {},
            )
        }
    }
}
