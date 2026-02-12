package com.gmail.vondenuelle.denuspend.ui.profile.component

import android.widget.Space
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
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
    photoError: String,
    onSave: () -> Unit,
    onChangePhoto: () -> Unit,
) {

    Column(modifier = modifier) {
        val fieldBorder = Modifier.border(
            1.dp, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.error
        )
        val fieldColors = TextFieldDefaults.colors().copy(
            focusedIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        )

        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(12.dp)
                .clickableDelayed {
                    onChangePhoto()
                }
        ) {
            AsyncImageWithErrorHandler(
                model = photo,
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(color = MaterialTheme.colorScheme.surfaceDim),

                shouldShowEnlargeButton = false,
            )
            IconButton(
                onClick = {
                    onChangePhoto()
                },
                colors = IconButtonDefaults.iconButtonColors().copy(containerColor = Color.White),
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Icon(imageVector = Icons.Filled.Edit, contentDescription = null)
            }
        }
        if (photoError.isNotEmpty()) {
            Text(photoError, modifier = Modifier.padding(top = 8.dp), color = Color.Red)
        }
        Spacer(modifier = Modifier.height(16.dp))


        // Name
        Text(
            stringResource(R.string.profile_lbl_name),
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 4.dp)
        )
        TextField(
            value = name,
            onValueChange = {
                onChangeName(it)
            },
            label = {
                Text(text = stringResource(R.string.profile_hint_name))
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = stringResource(R.string.profile_lbl_name)
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
                    if (nameError.isNotEmpty()) fieldBorder else Modifier
                )
        )

        Spacer(modifier = Modifier.height(16.dp))
        FilledTonalButton(onClick = {
            onSave()
        }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(stringResource(R.string.profile_btn_save))
        }

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
                photoError = "",
                onChangePhoto = {},
                onSave = {},
            )
        }
    }
}