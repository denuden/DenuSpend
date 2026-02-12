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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.ui.common.dialog.FullScreenDialog
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.AsyncImageWithBackgroundPalette
import com.gmail.vondenuelle.denuspend.utils.AsyncImageWithErrorHandler

@Composable
fun ProfileHeader(modifier: Modifier = Modifier, onPopBackStack: () -> Unit, onEdit : () -> Unit) {

    var shouldShowFullScreenImage by remember { mutableStateOf(false) }


    FullScreenDialog(
        showDialog = shouldShowFullScreenImage,
        onDismissRequest = { shouldShowFullScreenImage = false }) {
        AsyncImageWithBackgroundPalette(
            model = "wgwgw",
            onEnlargeImage = {
                shouldShowFullScreenImage = false
            }, //since this is from fullscreen, make it a close button instead of enlarge
            enlargeImageIcon = Icons.Default.Close,
            onPaletteBuilderSuccess = { }
        )
    }


    Box(modifier = modifier) {
        // Image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) {
            // Image
            AsyncImageWithErrorHandler(
                modifier = Modifier
                    .height(300.dp)
                    .fillMaxWidth(),
                model = "fewgw",
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
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background
                            ),
                            startY = 60f // controls fade start
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
            ProfileHeader(onPopBackStack = {}) {

            }
        }
    }
}