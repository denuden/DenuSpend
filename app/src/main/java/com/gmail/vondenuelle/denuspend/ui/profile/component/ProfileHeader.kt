package com.gmail.vondenuelle.denuspend.ui.profile.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.ui.common.dialog.FullScreenDialog
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.AsyncImageWithErrorHandler
import com.gmail.vondenuelle.denuspend.utils.clickableDelayed

private val BannerHeight = 180.dp
private val AvatarSize = 128.dp

/**
 * A blurred banner of the photo with a round avatar overlapping its bottom edge, so the whole face always shows.
 * Tapping the avatar opens the full photo. Blur needs Android 12+; older versions show the banner unblurred.
 */
@Composable
fun ProfileHeader(
    modifier: Modifier = Modifier,
    photo: String,
    onPopBackStack: () -> Unit,
) {

    var shouldShowFullScreenImage by remember { mutableStateOf(false) }
    // Falls back to the person icon if the photo fails to load. Resets when the photo changes.
    var photoFailed by remember(photo) { mutableStateOf(false) }
    val hasPhoto = photo.isNotEmpty() && !photoFailed


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
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }


    Box(modifier = modifier.fillMaxWidth()) {
        // Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BannerHeight)
                .background(MaterialTheme.colorScheme.primaryContainer)
        ) {
            if (hasPhoto) {
                AsyncImageWithErrorHandler(
                    model = photo,
                    shouldShowEnlargeButton = false,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(24.dp)
                )
            }

            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to MaterialTheme.colorScheme.scrim.copy(alpha = 0.15f),
                                1.0f to MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )
        }

        //Navigation
        val buttonColors = IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            IconButton(
                onClick = {
                    onPopBackStack()
                },
                colors = buttonColors,
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBackIosNew,
                    contentDescription = stringResource(R.string.main_cd_back)
                )
            }
        }

        // Avatar, with a ring in the background color so it stands out from the banner
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = BannerHeight - AvatarSize / 2)
                .size(AvatarSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.background)
                .padding(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .then(
                    if (hasPhoto) Modifier.clickableDelayed { shouldShowFullScreenImage = true }
                    else Modifier
                )
        ) {
            if (hasPhoto) {
                AsyncImageWithErrorHandler(
                    model = photo,
                    shouldShowEnlargeButton = false,
                    contentDescription = stringResource(R.string.profile_cd_view_photo),
                    onError = { photoFailed = true },
                    modifier = Modifier.fillMaxSize()
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
    }

}

@Preview
@Composable
private fun ProfileHeaderPreview() {
    DenuSpendTheme {
        androidx.compose.material3.Surface {
            ProfileHeader(onPopBackStack = {}, photo = "")
        }
    }
}
