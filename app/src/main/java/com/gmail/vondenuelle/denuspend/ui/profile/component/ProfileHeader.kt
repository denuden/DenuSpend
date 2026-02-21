package com.gmail.vondenuelle.denuspend.ui.profile.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.ui.common.dialog.FullScreenDialog
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.AsyncImageWithErrorHandler
import com.gmail.vondenuelle.denuspend.utils.clickableDelayed

@Composable
fun ProfileHeader(
    modifier: Modifier = Modifier,
    photo: String,
    onPopBackStack: () -> Unit,
    onEdit: () -> Unit
) {

    var shouldShowFullScreenImage by remember { mutableStateOf(false) }


    FullScreenDialog(
        color = Color.Transparent,
        showDialog = shouldShowFullScreenImage,
        onDismissRequest = { shouldShowFullScreenImage = false }) {
        AsyncImageWithErrorHandler(
            model = photo,
            onEnlargeImage = {
                shouldShowFullScreenImage = false
            }, //since this is from fullscreen, make it a close button instead of enlarge
            enlargeImageIcon = Icons.Default.Close,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxSize()
        )
    }


    Box(modifier = modifier) {
        // Image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(270.dp)
        ) {
            // Image
            AsyncImageWithErrorHandler(
                modifier = Modifier
                    .height(270.dp)
                    .fillMaxWidth()
                    .clickableDelayed() {
                        shouldShowFullScreenImage = true
                    },
                model = photo,
                shouldShowEnlargeButton = false,
                onEnlargeImage = {
                }
            )

            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Transparent,
                                0.6f to Color.Transparent,
                                1.0f to MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )
        }

        //Navigation
        Row(
            modifier = Modifier.padding(16.dp)
        ) {
            IconButton(
                onClick = {
                    onPopBackStack()
                },
                colors = IconButtonDefaults.iconButtonColors().copy(containerColor = Color.White),
            ) {
                Icon(imageVector = Icons.Filled.ArrowBackIosNew, contentDescription = null)
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = {
                    onEdit()
                },
                colors = IconButtonDefaults.iconButtonColors().copy(containerColor = Color.White),
            ) {
                Icon(imageVector = Icons.Filled.Edit, contentDescription = null)
            }
        }
    }

}

@Preview
@Composable
private fun ProfileHeaderPreview() {
    DenuSpendTheme {
        androidx.compose.material3.Surface {
            ProfileHeader(onPopBackStack = {}, photo = "") {

            }
        }
    }
}